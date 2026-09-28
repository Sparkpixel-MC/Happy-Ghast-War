package top.sparkpixel.hgw.util;


import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.api.ClientDataReceiveEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.*;

public class SocketClient extends BukkitRunnable{
    private final HappyGhastWar plugin;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = false;
    private volatile boolean nameRegistered = false;
    private volatile boolean scheduled = false;
    private Thread receiveThread;
    private String clientName;
    private String serverAddress;
    private MessageListener messageListener;

    public SocketClient(HappyGhastWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        sendMessage("ping");
    }

    public interface MessageListener {
        void onMessageReceived(String message);
        void onConnected();
        void onDisconnected();
        void onError(String error);
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public boolean connect(String host, int port, String name) {
        if (name == null || name.trim().isEmpty()) {
            plugin.getLogger().warning("客户端名称不能为空");
            if (messageListener != null) {
                messageListener.onError("客户端名称不能为空");
            }
            return false;
        }

        this.clientName = name;
        this.serverAddress = host + ":" + port;

        try {
            plugin.getLogger().info("尝试连接到代理端: " + host + ":" + port);

            // 解析主机地址
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                plugin.getLogger().severe("无法解析主机地址: " + host);
                if (messageListener != null) {
                    messageListener.onError("无法解析主机地址: " + host);
                }
                return false;
            }

            // 创建Socket并设置连接超时
            socket = new Socket();
            int connectionTimeout = 1000 * 60;
            socket.setSoTimeout(connectionTimeout);

            // 尝试连接
            socket.connect(new InetSocketAddress(inetAddress, port), connectionTimeout);

            // 设置保持活动状态
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            // 启动接收消息的线程
            receiveThread = new Thread(this::receiveMessages, "SocketClient-ReceiveThread-" + name);
            receiveThread.setDaemon(true); // 设置为守护线程
            receiveThread.start();

            // 发送注册信息
            sendMessage("REGISTER:" + name);

            plugin.getLogger().info("正在注册...");
            this.runTaskTimer(plugin,0,30 * 20);
            scheduled = true;
            return true;

        } catch (ConnectException e) {
            plugin.getLogger().severe("连接被拒绝，请确保服务器正在运行: " + e.getMessage());
            if (messageListener != null) {
                messageListener.onError("连接被拒绝，请确保服务器正在运行");
            }
        } catch (SocketTimeoutException e) {
            plugin.getLogger().severe("连接超时，请检查网络和服务器状态: " + e.getMessage());
            if (messageListener != null) {
                messageListener.onError("连接超时");
            }
        } catch (BindException e) {
            plugin.getLogger().severe("无法绑定本地端口，可能端口被占用: " + e.getMessage());
            if (messageListener != null) {
                messageListener.onError("本地端口被占用");
            }
        } catch (NoRouteToHostException e) {
            plugin.getLogger().severe("无法路由到主机，网络不可达: " + e.getMessage());
            if (messageListener != null) {
                messageListener.onError("网络不可达");
            }
        } catch (IOException e) {
            String errorMsg = e.getMessage();
            plugin.getLogger().severe("连接失败: " + errorMsg);

            // 提供更详细的错误信息
            if (errorMsg != null && errorMsg.contains("Cannot assign requested address")) {
                plugin.getLogger().severe("无法分配请求的地址，可能原因:");
                plugin.getLogger().severe("1. 主机地址错误或无法访问");
                plugin.getLogger().severe("2. 端口被占用");
                plugin.getLogger().severe("3. 网络配置问题");
                if (messageListener != null) {
                    messageListener.onError("地址无法分配，请检查主机地址和端口");
                }
            } else {
                if (messageListener != null) {
                    messageListener.onError("连接失败: " + errorMsg);
                }
            }
        }

        return false;
    }

    public void sendMessage(String message) {
        if (connected && writer != null) {
            writer.println(message);
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while (connected && (message = reader.readLine()) != null) {
                final String finalMessage = message;

                // 处理注册响应
                if (finalMessage.startsWith("REGISTER_SUCCESS:")) {
                    nameRegistered = true;
                    plugin.getLogger().info("注册成功!");

                    // 在主线程通知连接成功
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (messageListener != null) {
                                messageListener.onConnected();
                            }
                        }
                    }.runTask(plugin);
                    continue;
                }

                // 处理错误消息
                if (finalMessage.startsWith("ERROR:")) {
                    plugin.getLogger().warning("服务器错误: " + finalMessage.substring(6));

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (messageListener != null) {
                                messageListener.onError(finalMessage.substring(6));
                            }
                        }
                    }.runTask(plugin);

                    // 如果是注册错误，断开连接
                    if (!nameRegistered) {
                        disconnect();
                    }
                    continue;
                }

                if (message.equalsIgnoreCase("ping")) continue;

                // 普通消息 - 触发客户端接收事件
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 触发客户端接收事件
                        ClientDataReceiveEvent event = new ClientDataReceiveEvent(
                                clientName,
                                serverAddress,
                                finalMessage
                        );
                        Bukkit.getPluginManager().callEvent(event);

                        // 如果事件没有被取消，继续处理消息
                        if (!event.isCancelled()) {
                            if (messageListener != null) {
                                messageListener.onMessageReceived(finalMessage);
                            }
                        }
                    }
                }.runTask(plugin);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("客户端 '" + clientName + "' 处理数据时出错: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void disconnect() {
        if (scheduled) {
            try {
                this.cancel();
                scheduled = false;
            } catch (IllegalStateException ignored) {
                // 任务尚未调度
            }
        }
        if (connected) {
            connected = false;
            nameRegistered = false;

            try {
                if (socket != null) socket.close();
                if (receiveThread != null && receiveThread.isAlive()) {
                    receiveThread.interrupt();
                }
            } catch (IOException e) {
                // 忽略关闭时的错误
            }

            plugin.getLogger().info("代理端连接已断开");
        }
    }

    public boolean isConnected() {
        return connected && nameRegistered;
    }

    public String getClientName() {
        return clientName;
    }

    public String getServerAddress() {
        return serverAddress;
    }
}