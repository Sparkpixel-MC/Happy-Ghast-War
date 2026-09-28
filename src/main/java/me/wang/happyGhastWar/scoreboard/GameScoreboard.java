package me.wang.happyGhastWar.scoreboard;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.wang.happyGhastWar.HappyGhastWar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

/**
 * 基于 TAB 插件管理的计分板。
 * <p>
 * 侧边栏 (sidebar) 优先交给 TAB 的 ScoreboardManager 渲染（TAB-API 6.1.2，
 * 文本使用 TAB 支持的 legacy '§' 颜色格式）。当 TAB 未安装或其 scoreboard
 * 功能关闭（{@code getScoreboardManager()} 返回 null）时，自动回退为
 * Bukkit 原生 Objective 侧边栏，保证无 TAB 环境下计分板仍可用。
 * <p>
 * {@link #getScoreboard()} 返回的 Bukkit {@link Scoreboard} 同时作为队伍名牌
 * (name tag) 的数据容器供 {@code TeamDivider} 使用；玩家加入游戏时会被
 * 切换到该记分板，离开时恢复主记分板。
 */
public class GameScoreboard {
    private final HappyGhastWar plugin;
    private final String roomName;
    private final Scoreboard scoreboard;

    /** TAB 的计分板管理器（TAB scoreboard 功能关闭时为 null，走 Bukkit 回退） */
    private final me.neznamy.tab.api.scoreboard.ScoreboardManager tabScoreboardManager;

    // 游戏状态
    private GameState gameState = GameState.WAITING;
    private int countdownSeconds = 0;
    private int requiredPlayers;
    private int currentPlayers = 0;
    private final String serverName;

    private final String serverIp;

    private final Map<String, me.wang.happyGhastWar.game.team.Team> teams = new HashMap<>();

    /** 当前正在观看该计分板的玩家 */
    private final Set<Player> players = new LinkedHashSet<>();

    /** 当前展示给玩家的 TAB 自定义计分板（随状态重建） */
    private me.neznamy.tab.api.scoreboard.Scoreboard currentBoard;
    private GameState currentBoardState = null;

    /** Bukkit 回退侧边栏的 Objective 名称 */
    private static final String FALLBACK_OBJECTIVE = "gw_sidebar";

    public enum GameState {
        WAITING, COUNTDOWN, PLAYING, ENDING
    }

    public GameScoreboard(HappyGhastWar plugin, String roomName, int teamCount, String serverIp) {
        this(plugin, roomName, teamCount, serverIp, "");
    }

    public GameScoreboard(HappyGhastWar plugin, String roomName, int teamCount, String serverIp, String serverName) {
        this.plugin = plugin;
        this.roomName = roomName;
        this.requiredPlayers = teamCount;
        this.serverName = serverName;
        this.serverIp = serverIp;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        me.neznamy.tab.api.scoreboard.ScoreboardManager manager = null;
        try {
            manager = TabAPI.getInstance().getScoreboardManager();
        } catch (Throwable ignored) {
            // TAB 未安装
        }
        this.tabScoreboardManager = manager;
    }

    public void init() {
        // 重建内部状态（游戏重置时调用）
        if (currentBoard != null && tabScoreboardManager != null) {
            try {
                tabScoreboardManager.removeScoreboard(currentBoard);
            } catch (IllegalArgumentException ignored) {
                // 尚未注册过同名计分板
            }
        }
        currentBoard = null;
        currentBoardState = null;
        for (Player player : new ArrayList<>(players)) {
            showTo(player, getLines(player));
        }
    }

    public void addTeam(me.wang.happyGhastWar.game.team.Team team) {
        teams.put(team.getTeams().getDisplayName(), team);
        updateAllScoreboards();
    }

    /**
     * 为玩家显示计分板并接管其 Bukkit 记分板（队伍名牌生效）
     */
    public void createScoreboard(Player player) {
        players.add(player);
        // 接管玩家的记分板，使 TeamDivider 注册的队伍名牌/碰撞规则生效
        if (player.getScoreboard() != scoreboard) {
            player.setScoreboard(scoreboard);
        }
        showTo(player, getLines(player));
    }

    /**
     * 移除玩家计分板并让其恢复默认计分板
     */
    public void removeScoreboard(Player player) {
        players.remove(player);
        if (tabScoreboardManager != null) {
            TabPlayer tp = TabAPI.getInstance().getPlayer(player.getUniqueId());
            if (tp != null) {
                tabScoreboardManager.resetScoreboard(tp);
            }
        } else if (player.getScoreboard() == scoreboard) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /**
     * 更新所有玩家的计分板
     */
    public void updateAllScoreboards() {
        if (players.isEmpty()) return;
        // TAB 的计分板是共享结构，以首个观看玩家的语言为准构建
        Player ref = players.iterator().next();
        List<String> lines = getLines(ref);
        for (Player player : new ArrayList<>(players)) {
            showTo(player, lines);
        }
    }

    private void showTo(Player player, List<String> lines) {
        // 标题同样是 MiniMessage 标签：与行一致，出边界前转 legacy '§'
        String title = me.wang.happyGhastWar.util.Text.legacy(plugin.getLanguage(player).getContent("scoreboard.title"));
        if (tabScoreboardManager != null) {
            showTabBoard(player, title, lines);
        } else {
            showBukkitFallback(title, lines);
        }
    }

    /**
     * TAB 模式：状态变化时重建计分板，其余时候仅更新标题/行。
     */
    private void showTabBoard(Player player, String title, List<String> lines) {
        if (currentBoard == null || currentBoardState != gameState) {
            if (currentBoard != null) {
                try {
                    tabScoreboardManager.removeScoreboard(currentBoard);
                } catch (IllegalArgumentException ignored) {
                }
            }
            String uniqueName = "gw_" + roomName + "_" + gameState.name().toLowerCase(Locale.ROOT);
            currentBoard = tabScoreboardManager.createScoreboard(uniqueName, title, new ArrayList<>(lines));
            currentBoardState = gameState;
        } else {
            currentBoard.setTitle(title);
            currentBoard.setLines(new ArrayList<>(lines));
        }

        TabPlayer tp = TabAPI.getInstance().getPlayer(player.getUniqueId());
        if (tp != null) {
            tabScoreboardManager.showScoreboard(tp, currentBoard);
        }
    }

    /**
     * Bukkit 回退模式：无 TAB 时用原生 Objective 渲染侧边栏。
     */
    private void showBukkitFallback(String title, List<String> lines) {
        Objective old = scoreboard.getObjective(FALLBACK_OBJECTIVE);
        if (old != null) {
            old.unregister();
        }
        Objective obj = scoreboard.registerNewObjective(FALLBACK_OBJECTIVE, "dummy", title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int total = Math.min(lines.size(), 15);
        for (int i = 0; i < total; i++) {
            String line = lines.get(i);
            if (line.isEmpty()) {
                // 空行用不可见色码补齐，保证多条空行互不相同（Bukkit 按 entry 去重）
                line = "§" + "0123456789abcdef".charAt(i % 16);
            }
            // score 越大越靠上：第 i 行（顶部为 0）给 score total-i
            obj.getScore(limitLength(line, 40)).setScore(total - i);
        }
    }

    private String limitLength(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    /**
     * 获取当前应该显示的计分板行（MiniMessage 标签，出边界前统一转 legacy '§'）
     */
    private List<String> getLines(Player player) {
        List<String> lines = switch (gameState) {
            case WAITING -> getWaitingLines(player);
            case COUNTDOWN -> getCountdownLines(player);
            case PLAYING, ENDING -> getPlayingLines(player);
        };
        // TAB 与 Bukkit Objective 均只接受 legacy '§' 字符串：MiniMessage 标签在此统一转换
        lines.replaceAll(me.wang.happyGhastWar.util.Text::legacy);
        return lines;
    }

    /**
     * 等待状态计分板内容
     */
    private List<String> getWaitingLines(Player player) {
        List<String> lines = plugin.getLanguage(player).getTranslatedList("scoreboard.waiting");
        String date = getDate();
        List<String> processedLines = new ArrayList<>();

        for (String line : lines) {
            processedLines.add(line.replace("{date}", date)
                    .replace("{server}", serverName)
                    .replace("{map}", roomName)
                    .replace("{on}", String.valueOf(currentPlayers))
                    .replace("{max}", String.valueOf(requiredPlayers))
                    .replace("{serverIp}", serverIp));
        }

        return processedLines;
    }

    /**
     * 倒计时状态计分板内容
     */
    private List<String> getCountdownLines(Player player) {
        List<String> lines = plugin.getLanguage(player).getTranslatedList("scoreboard.starting");
        String date = getDate();
        List<String> processedLines = new ArrayList<>();

        for (String line : lines) {
            processedLines.add(line.replace("{date}", date)
                    .replace("{server}", serverName)
                    .replace("{map}", roomName)
                    .replace("{on}", String.valueOf(currentPlayers))
                    .replace("{max}", String.valueOf(requiredPlayers))
                    .replace("{time}", String.valueOf(countdownSeconds))
                    .replace("{serverIp}", serverIp));
        }

        return processedLines;
    }

    /**
     * 游戏中状态计分板内容
     */
    private List<String> getPlayingLines(Player player) {
        List<String> lines = plugin.getLanguage(player).getTranslatedList("scoreboard.playing");
        String date = getDate();
        List<String> processedLines = new ArrayList<>();

        // 生成所有可能的占位符替换
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{date}", date);
        replacements.put("{server}", serverName);
        replacements.put("{map}", roomName);
        replacements.put("{on}", String.valueOf(currentPlayers));
        replacements.put("{max}", String.valueOf(requiredPlayers));
        replacements.put("{time}", String.valueOf(countdownSeconds));
        replacements.put("{serverIp}", serverIp);

        // 为每个可能的队伍位置生成显示内容；不存在的队伍替换为空串并丢弃该行
        for (int i = 1; i <= 8; i++) {
            replacements.put("{team" + i + "}", getTeamDisplay(i, player));
        }

        for (String line : lines) {
            String processedLine = line;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                processedLine = processedLine.replace(entry.getKey(), entry.getValue());
            }

            // 整行只包含一个队伍占位符且队伍不存在（替换后为空）时，跳过该行
            if (processedLine.isEmpty() && line.contains("{team")) {
                boolean onlyTeam = true;
                for (int i = 1; i <= 8; i++) {
                    onlyTeam &= line.equals("{team" + i + "}");
                }
                if (onlyTeam) continue;
            }

            processedLines.add(processedLine);
        }

        return processedLines;
    }

    private String getDate() {
        return new java.text.SimpleDateFormat("yy/MM/dd").format(new Date());
    }

    private String getTeamDisplay(int index, Player player) {
        List<me.wang.happyGhastWar.game.team.Team> teamList = new ArrayList<>(teams.values());

        if (index <= teamList.size()) {
            me.wang.happyGhastWar.game.team.Team team = teamList.get(index - 1);
            // MiniMessage 颜色标签（出 getLines 边界时统一转 legacy）
            String colorCode = team.getTeams().getColor();
            String aliveMark = team.isAlive() ?
                    (team.isCanRespawn() ? "<green>✔" : "<gray>" + team.getSize()) :
                    "";
            String deadMark = team.isAlive() ? "" : "<red>✗";
            return colorCode + team.getTeams().getDisplayName()
                    + plugin.getLanguage(player).getContent("scoreboard.team")
                    + " " + aliveMark + deadMark;
        }
        // 队伍不存在：返回空串，所在行会被移除
        return "";
    }

    /**
     * 更新倒计时 (每秒调用)
     */
    public void updateCountdown(int seconds) {
        // 只有秒数变化时才更新
        if (this.countdownSeconds != seconds || this.gameState != GameState.COUNTDOWN) {
            this.countdownSeconds = seconds;
            this.gameState = GameState.COUNTDOWN;
            updateAllScoreboards();
        }
    }

    /**
     * 更新玩家数量
     */
    public void updatePlayerCount(int current, int required) {
        // 只有玩家数量变化时才更新
        if (this.currentPlayers != current || this.requiredPlayers != required) {
            this.currentPlayers = current;
            this.requiredPlayers = required;
            updateAllScoreboards();
        }
    }

    /**
     * 设置游戏状态
     */
    public void setGameState(GameState state) {
        // 只有状态变化时才更新
        if (this.gameState != state) {
            this.gameState = state;
            updateAllScoreboards();
        }
    }

    /**
     * 更新队伍
     */
    public void updateTeam(me.wang.happyGhastWar.game.team.Team team) {
        me.wang.happyGhastWar.game.team.Team existingTeam = teams.get(team.getTeams().getDisplayName());

        // 只有队伍数据变化时才更新
        if (existingTeam == null || !isTeamDataEqual(existingTeam, team)) {
            teams.put(team.getTeams().getDisplayName(), team);
            updateAllScoreboards();
        }
    }

    /**
     * 比较两个队伍数据是否相同
     */
    private boolean isTeamDataEqual(me.wang.happyGhastWar.game.team.Team team1, me.wang.happyGhastWar.game.team.Team team2) {
        return team1.isAlive() == team2.isAlive() &&
                team1.isCanRespawn() == team2.isCanRespawn() &&
                team1.getSize() == team2.getSize();
    }

    /**
     * 提供一个 Bukkit 计分板作为队伍名牌 (name tag) 的数据容器。
     * 该计分板被 {@code TeamDivider} 用于给玩家分色标签；
     * 无 TAB 时它同时承载回退侧边栏。
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void update() {
        updateAllScoreboards();
    }

    /**
     * 获取所有队伍
     */
    public Collection<me.wang.happyGhastWar.game.team.Team> getAllTeams() {
        return teams.values();
    }

    /**
     * 清空缓存的队伍（Arena.init 完成反注册后调用，
     * 避免残留旧 Team 对象导致重复反注册或状态错乱）
     */
    public void clearTeams() {
        teams.clear();
    }

}
