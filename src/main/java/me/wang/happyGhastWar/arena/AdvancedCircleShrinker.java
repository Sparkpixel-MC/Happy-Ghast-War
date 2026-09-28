package me.wang.happyGhastWar.arena;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AdvancedCircleShrinker {

    private final JavaPlugin plugin;
    private final me.wang.happyGhastWar.arena.Arena arena;
    private final CircleShrinker circleShrinker;
    private boolean isActive;
    private boolean shrinkAnimation;
    private double damagePerSecond;
    private boolean restarting; // 重启缩圈时抑制"缩圈已停止"误报
    private int shrinkTaskId;
    private int damageTaskId;
    private int animationTaskId;

    private final Set<UUID> damageCooldown = new HashSet<>();

    /**
     * 构造函数
     * @param plugin 插件主类
     * @param center 中心位置
     * @param initialRadius 初始半径
     * @param arena 所属场地（用于按玩家语言广播消息）
     */
    public AdvancedCircleShrinker(JavaPlugin plugin, Location center, double initialRadius,
                                  me.wang.happyGhastWar.arena.Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.circleShrinker = new CircleShrinker(center, initialRadius);
        this.isActive = false;
        this.shrinkAnimation = true;
        this.damagePerSecond = 1.0; // 默认每秒1点伤害
    }

    /**
     * 开始缩圈过程
     * @param targetRadius 目标半径
     * @param shrinkDuration 缩圈持续时间（秒）
     * @param damagePerSecond 每秒伤害（对圈外生物）
     */
    public void startShrinking(double targetRadius, int shrinkDuration, double damagePerSecond) {
        if (isActive) {
            restarting = true;
            stopShrinking();
            restarting = false;
        }

        this.isActive = true;
        this.damagePerSecond = Math.max(0, damagePerSecond);

        // 计算缩圈速度（每tick减少的半径）
        double totalShrinkAmount = circleShrinker.getRadius() - targetRadius;
        double shrinkPerTick = totalShrinkAmount / (shrinkDuration * 20); // 20 ticks = 1秒

        // 启动缩圈任务
        shrinkTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                boolean reachedTarget = circleShrinker.shrinkTo(targetRadius, shrinkPerTick);

                if (reachedTarget) {
                    cancel();
                    // 缩圈完成后停止相关任务
                    if (damagePerSecond == 0) {
                        stopShrinking();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L).getTaskId(); // 每tick执行一次

        // 如果设置了伤害，启动伤害任务
        if (damagePerSecond > 0) {
            startDamageTask();
        }

        // 如果启用了动画，启动动画任务
        if (shrinkAnimation) {
            startAnimationTask();
        }

        // 播放开始音效
        broadcastSound("shrink-start", Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        broadcastMessageKey("border.shrink-start");
    }

    /**
     * 停止所有缩圈相关任务
     */
    public void stopShrinking() {
        isActive = false;

        // 取消所有任务
        if (shrinkTaskId != 0) {
            plugin.getServer().getScheduler().cancelTask(shrinkTaskId);
            shrinkTaskId = 0;
        }

        if (damageTaskId != 0) {
            plugin.getServer().getScheduler().cancelTask(damageTaskId);
            damageTaskId = 0;
        }

        if (animationTaskId != 0) {
            plugin.getServer().getScheduler().cancelTask(animationTaskId);
            animationTaskId = 0;
        }

        // 清空调息列表
        damageCooldown.clear();

        // 播放结束音效（重启缩圈时不广播"已停止"）
        if (!restarting) {
            broadcastSound("shrink-stop", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            broadcastMessageKey("border.shrink-stop");
        }
    }

    /**
     * 启动伤害任务
     */
    private void startDamageTask() {
        damageTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive) {
                    cancel();
                    return;
                }

                // 获取圈外所有生物
                List<Entity> outsideEntities = getOutsideLivingEntities();

                for (Entity entity : outsideEntities) {
                    if (entity instanceof LivingEntity living) {
                        UUID uuid = living.getUniqueId();

                        // 检查冷却
                        if (damageCooldown.contains(uuid)) {
                            continue;
                        }

                        // 施加伤害
                        living.damage(damagePerSecond);

                        // 添加冷却（防止一tick内多次伤害）
                        damageCooldown.add(uuid);

                        // 给玩家发送警告
                        if (living instanceof Player player) {
                            double distance = circleShrinker.getDistanceToCircleEdge(player);
                            String message = me.wang.happyGhastWar.HappyGhastWar.language.getContent("border.outside-damage")
                                    .replace("{damage}", String.format("%.1f", damagePerSecond))
                                    .replace("{distance}", String.format("%.1f", Math.max(0, distance)));
                            me.wang.happyGhastWar.util.Text.send(player, message);

                            // 屏幕抖动效果
                            me.wang.happyGhastWar.util.SoundUtil.play(player, "border-damage", Sound.ENTITY_GENERIC_HURT, 1.0f, 1.0f);
                        }
                    }
                }

                // 清空调息，准备下一轮
                damageCooldown.clear();
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId(); // 每秒执行一次
    }

    /**
     * 启动边界动画任务
     */
    private void startAnimationTask() {
        animationTaskId = new BukkitRunnable() {
            private int angle = 0;

            @Override
            public void run() {
                if (!isActive) {
                    cancel();
                    return;
                }

                World world = circleShrinker.getCenter().getWorld();
                double radius = circleShrinker.getRadius();

                // 每5度显示一个粒子
                for (int i = 0; i < 72; i++) { // 72 * 5 = 360度
                    double currentAngle = Math.toRadians(angle + i * 5);

                    // 计算边界位置
                    Location center = circleShrinker.getCenter();
                    double x = center.getX() + Math.cos(currentAngle) * radius;
                    double y = center.getY() + 1; // 离地面1格高
                    double z = center.getZ() + Math.sin(currentAngle) * radius;

                    Location particleLoc = new Location(world, x, y, z);

                    // 显示粒子效果
                    world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0, 0);

                }

                angle = (angle + 1) % 360;
            }
        }.runTaskTimer(plugin, 0L, 2L).getTaskId(); // 每2tick执行一次
    }

    /**
     * 获取圈外所有生物实体
     */
    private List<Entity> getOutsideLivingEntities() {
        List<Entity> outsideEntities = new ArrayList<>();
        World world = circleShrinker.getCenter().getWorld();

        for (Entity entity : world.getEntities()) {
            if (entity instanceof LivingEntity && !entity.isDead()) {
                if (circleShrinker.isOutsideCircle(entity)) {
                    outsideEntities.add(entity);
                }
            }
        }

        return outsideEntities;
    }

    /**
     * 向所有圈内玩家广播消息（按玩家语言）
     */
    private void broadcastMessageKey(String key) {
        me.wang.happyGhastWar.HappyGhastWar ghastWarPlugin = (me.wang.happyGhastWar.HappyGhastWar) plugin;
        for (Player player : circleShrinker.getPlayersInsideCircle()) {
            me.wang.happyGhastWar.util.Text.send(player, ghastWarPlugin.getLanguage(player).getContent(key));
        }
    }

    /**
     * 向所有圈内玩家播放音效（音效可经 config.yml 的 sounds.<key> 配置）
     */
    private void broadcastSound(String key, Sound fallback, float volume, float pitch) {
        Sound sound = me.wang.happyGhastWar.util.SoundUtil.get(key, fallback);
        for (Player player : circleShrinker.getPlayersInsideCircle()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * 检测玩家是否在圈外（快捷方法）
     */
    public boolean isPlayerOutside(Player player) {
        return circleShrinker.isPlayerOutsideCircle(player);
    }

    /**
     * 获取当前圈信息
     */
    public String getStatus() {
        return circleShrinker.getInfo();
    }

    /**
     * 获取基础缩圈器
     */
    public CircleShrinker getCircleShrinker() {
        return circleShrinker;
    }

    /**
     * 是否正在缩圈
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * 设置是否启用动画
     */
    public void setAnimationEnabled(boolean enabled) {
        this.shrinkAnimation = enabled;
    }
}