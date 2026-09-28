package me.wang.happyGhastWar.tab;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import me.neznamy.tab.api.placeholder.PlaceholderManager;
import me.neznamy.tab.api.tablist.HeaderFooterManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.game.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TAB 插件对接管理器（TAB-API 6.1.2）。
 * <p>
 * 负责：
 * <ul>
 *   <li>注册 %gw_*% 自定义占位符（供 TAB 配置使用）</li>
 *   <li>通过 {@link HeaderFooterManager} 设置游戏状态相关的 Tab 头/尾</li>
 *   <li>通过 {@link TabListFormatManager} 为队员名单染色</li>
 *   <li>监听 TAB 的 {@link PlayerLoadEvent} / {@link TabLoadEvent}，
 *       在玩家加载完成或 TAB 重载后重新应用所有 API 调用</li>
 * </ul>
 * TAB 的功能均可在其配置中关闭，对应管理器会返回 null，本类全部做了判空，
 * TAB 未安装/功能关闭时安全降级为不做任何事。
 */
public class TabManager {
    private final JavaPlugin plugin;

    // 延迟获取：TAB 可能比本插件更晚完成加载
    private boolean tabHooked = false;

    public TabManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化：注册占位符 + 事件监听。可在任意线程调用。
     */
    public void initialize() {
        registerGlobalPlaceholders();

        try {
            // 玩家在 TAB 侧加载完成（加入或 TAB 重载）时应用个性化显示
            TabAPI.getInstance().getEventBus().register(PlayerLoadEvent.class, event ->
                    plugin.getServer().getScheduler().runTask(plugin, () -> applyToPlayer(event.getPlayer())));

            // TAB 插件加载/重载后重新应用全部玩家显示（API 调用会因重载失效）
            TabAPI.getInstance().getEventBus().register(TabLoadEvent.class, event ->
                    plugin.getServer().getScheduler().runTask(plugin, this::updateAllTabs));

            tabHooked = true;
            plugin.getLogger().info("Hooked into TAB API.");
        } catch (Throwable t) {
            // TAB 未安装时 getEventBus() 可能返回 null 或抛异常
            plugin.getLogger().info("TAB not detected, tab-list integration disabled (scoreboards fall back to Bukkit).");
        }
    }

    public void shutdown() {
        if (!tabHooked) return;
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                TabPlayer tp = TabAPI.getInstance().getPlayer(player.getUniqueId());
                if (tp == null) continue;

                HeaderFooterManager hf = TabAPI.getInstance().getHeaderFooterManager();
                if (hf != null) {
                    hf.setHeaderAndFooter(tp, null, null);
                }
                TabListFormatManager tf = TabAPI.getInstance().getTabListFormatManager();
                if (tf != null) {
                    tf.setPrefix(tp, null);
                    tf.setSuffix(tp, null);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 对单个玩家应用 Tab 头/尾与队名格式（主线程调用）。
     */
    public void applyToPlayer(TabPlayer tabPlayer) {
        if (tabPlayer == null) return;
        Player player = (Player) tabPlayer.getPlayer();
        if (player == null || !player.isOnline()) return;

        HeaderFooterManager hf = TabAPI.getInstance().getHeaderFooterManager();
        if (hf != null) {
            String[] headerFooter = createHeaderFooter(player);
            hf.setHeaderAndFooter(tabPlayer, headerFooter[0], headerFooter[1]);
        }

        applyTabFormat(tabPlayer, player);
    }

    /**
     * 更新所有在线玩家的 Tab 显示。
     */
    public void updateAllTabs() {
        if (!tabHooked) return;
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                TabPlayer tp = TabAPI.getInstance().getPlayer(player.getUniqueId());
                if (tp != null && tp.isLoaded()) {
                    applyToPlayer(tp);
                }
            }
        } catch (Throwable ignored) {
            // TAB 未安装或已卸载
        }
    }

    /**
     * 更新单个玩家的 Tab 显示。
     */
    public void updatePlayerTab(Player player) {
        if (!tabHooked) return;
        try {
            TabPlayer tp = TabAPI.getInstance().getPlayer(player.getUniqueId());
            if (tp != null && tp.isLoaded()) {
                applyToPlayer(tp);
            }
        } catch (Throwable ignored) {
            // TAB 未安装或已卸载
        }
    }

    /**
     * 按游戏状态生成头/尾文本（MiniMessage 标签，出边界时转 legacy '§'）。
     */
    private String[] createHeaderFooter(Player player) {
        String bar = "<dark_gray><strikethrough>                                                ";
        String header = bar;
        String footer = bar;

        Arena arena = getArenaOf(player);
        if (arena == null) {
            header = "<gold><bold>乐魂战 <gray>Happy Ghast War";
            footer = "<gray>www.example.com";
        } else {
            switch (getGameState(player)) {
                case WAITING -> {
                    header = "<gold><bold>乐魂战 <gray>- <yellow>等待玩家中...";
                    footer = "<gray>使用 <yellow>/gw leave <gray>离开";
                }
                case COUNTDOWN -> {
                    header = "<yellow><bold>乐魂战 <gray>- <green>即将开始！";
                    footer = "<gray>准备战斗！";
                }
                case PLAYING, ENDING -> {
                    header = "<red><bold>乐魂战 <gray>- <red>战斗中！";
                    Team team = getTeamOf(player);
                    if (team != null) {
                        footer = team.getTeams().getColor() + "你的队伍: <white>" + team.getTeams().getDisplayName()
                                + " <gray>存活: <green>" + team.getSize();
                    } else {
                        footer = "<gray>存活到最后即胜利！";
                    }
                }
            }
        }

        // TAB 完全支持 legacy '§' 格式：MiniMessage 标签出边界时统一转换
        return new String[]{
                me.wang.happyGhastWar.util.Text.legacy(header),
                me.wang.happyGhastWar.util.Text.legacy(footer)
        };
    }

    /**
     * 为玩家名单（Tab 列表）设置队伍前缀，使同队玩家在列表中带颜色。
     */
    private void applyTabFormat(TabPlayer tabPlayer, Player player) {
        try {
            TabListFormatManager tf = TabAPI.getInstance().getTabListFormatManager();
            if (tf == null) return;

            Team team = getTeamOf(player);
            if (team != null) {
                String color = team.getTeams().getColor();
                tf.setPrefix(tabPlayer, me.wang.happyGhastWar.util.Text.legacy(color + "[" + team.getTeams().getDisplayName() + "]<reset> "));
            } else {
                // 离开队伍后重置（null = 恢复 TAB 配置的原始值）
                if (tf.getCustomPrefix(tabPlayer) != null) {
                    tf.setPrefix(tabPlayer, null);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to set tab format for player " + player.getName() + ": " + t.getMessage());
        }
    }

    /**
     * 注册 %gw_*% 占位符（标识符必须以 % 包裹）。
     */
    private void registerGlobalPlaceholders() {
        try {
            PlaceholderManager pm = TabAPI.getInstance().getPlaceholderManager();

            // 在线玩家数量（服务器级，1 秒刷新）
            pm.registerServerPlaceholder("%gw_online%", 1000,
                    () -> String.valueOf(Bukkit.getOnlinePlayers().size()));

            // 场地总数
            pm.registerServerPlaceholder("%gw_arenas%", 1000,
                    () -> String.valueOf(HappyGhastWar.getArenas().size()));

            // 玩家所在场地的游戏状态（玩家级，500ms 刷新）
            pm.registerPlayerPlaceholder("%gw_gamestate%", 500, tp -> {
                Player player = (Player) tp.getPlayer();
                if (player == null) return "LOBBY";
                Arena arena = getArenaOf(player);
                if (arena == null) return "LOBBY";
                return getGameState(player).name();
            });

            // 玩家所在场地名称
            pm.registerPlayerPlaceholder("%gw_map%", 1000, tp -> {
                Player player = (Player) tp.getPlayer();
                if (player == null) return "-";
                Arena arena = getArenaOf(player);
                return arena == null ? "-" : arena.getName();
            });

            plugin.getLogger().info("Registered %gw_*% placeholders into TAB.");
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register placeholders: " + t.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 查询辅助
    // ------------------------------------------------------------------

    /** 玩家当前所在的场地（不在任何场地时返回 null） */
    private Arena getArenaOf(Player player) {
        Arena byWorld = HappyGhastWar.getArenas().get(player.getWorld().getName());
        if (byWorld != null) {
            return byWorld;
        }
        // 兜底：玩家已加入但还没被传送进场地世界
        for (Arena arena : HappyGhastWar.getArenas().values()) {
            if (arena.getPlayers().contains(player)) {
                return arena;
            }
        }
        return null;
    }

    private me.wang.happyGhastWar.scoreboard.GameScoreboard.GameState getGameState(Player player) {
        Arena arena = getArenaOf(player);
        if (arena == null) {
            return me.wang.happyGhastWar.scoreboard.GameScoreboard.GameState.WAITING;
        }
        return arena.getGameState();
    }

    private Team getTeamOf(Player player) {
        Arena arena = getArenaOf(player);
        return arena == null ? null : arena.getTeam(player);
    }
}
