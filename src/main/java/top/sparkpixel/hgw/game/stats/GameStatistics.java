package top.sparkpixel.hgw.game.stats;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 游戏统计系统（每场对局独立，开局清零）
 */
public class GameStatistics {

    private final HappyGhastWar ghastWar;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public GameStatistics(HappyGhastWar ghastWar) {
        this.ghastWar = ghastWar;
    }

    /**
     * 玩家统计数据
     */
    public static class PlayerStats {
        private int kills;
        private int deaths;
        private int ghastRides;
        private int airdropsCollected;
        private int resourcesMined;
        private int upgradesPerformed;
        private long playTime;

        public PlayerStats() {
            this.kills = 0;
            this.deaths = 0;
            this.ghastRides = 0;
            this.airdropsCollected = 0;
            this.resourcesMined = 0;
            this.upgradesPerformed = 0;
            this.playTime = 0;
        }

        public int getKills() {
            return kills;
        }

        public void addKill() {
            this.kills++;
        }

        public int getDeaths() {
            return deaths;
        }

        public void addDeath() {
            this.deaths++;
        }

        public int getGhastRides() {
            return ghastRides;
        }

        public void addGhastRide() {
            this.ghastRides++;
        }

        public int getAirdropsCollected() {
            return airdropsCollected;
        }

        public void addAirdropCollected() {
            this.airdropsCollected++;
        }

        public int getResourcesMined() {
            return resourcesMined;
        }

        public void addResourceMined() {
            this.resourcesMined++;
        }

        public int getUpgradesPerformed() {
            return upgradesPerformed;
        }

        public void addUpgradePerformed() {
            this.upgradesPerformed++;
        }

        public long getPlayTime() {
            return playTime;
        }

        public void addPlayTime(long minutes) {
            this.playTime += minutes;
        }

        public double getKDRatio() {
            if (deaths == 0) return kills;
            return (double) kills / deaths;
        }
    }

    /**
     * 获取玩家统计
     */
    public PlayerStats getPlayerStats(Player player) {
        UUID uuid = player.getUniqueId();
        return playerStats.computeIfAbsent(uuid, k -> new PlayerStats());
    }

    /** 开局开始追踪一名玩家（记录入场时间用于计算游戏时长） */
    public void startTracking(Player player) {
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
        getPlayerStats(player);
    }

    /** 结束追踪一名玩家（结算游戏时长） */
    public void stopTracking(Player player) {
        UUID uuid = player.getUniqueId();
        Long join = joinTimes.remove(uuid);
        if (join != null) {
            long minutes = Math.max(0, (System.currentTimeMillis() - join) / 60000);
            getPlayerStats(player).addPlayTime(minutes);
        }
    }

    /**
     * 记录击杀
     */
    public void recordKill(Player killer) {
        PlayerStats stats = getPlayerStats(killer);
        stats.addKill();
    }

    /**
     * 记录死亡
     */
    public void recordDeath(Player victim) {
        PlayerStats stats = getPlayerStats(victim);
        stats.addDeath();
    }

    /**
     * 记录骑乘乐魂
     */
    public void recordGhastRide(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.addGhastRide();
    }

    /**
     * 记录收集空投
     */
    public void recordAirdropCollected(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.addAirdropCollected();
    }

    /**
     * 记录开采资源
     */
    public void recordResourceMined(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.addResourceMined();
    }

    /**
     * 记录升级操作
     */
    public void recordUpgradePerformed(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.addUpgradePerformed();
    }

    /**
     * 记录游戏时间
     */
    public void recordPlayTime(Player player, long minutes) {
        PlayerStats stats = getPlayerStats(player);
        stats.addPlayTime(minutes);
    }

    /**
     * 按综合表现（击杀 > 采集 > 升级 > 空投）取前 n 名玩家统计
     */
    public List<Map.Entry<Player, PlayerStats>> getTopStats(int n) {
        List<Map.Entry<Player, PlayerStats>> list = new ArrayList<>();
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                list.add(new AbstractMap.SimpleEntry<>(player, entry.getValue()));
            }
        }
        list.sort((a, b) -> {
            PlayerStats sa = a.getValue();
            PlayerStats sb = b.getValue();
            int byKills = Integer.compare(sb.getKills(), sa.getKills());
            if (byKills != 0) return byKills;
            int byMine = Integer.compare(sb.getResourcesMined(), sa.getResourcesMined());
            if (byMine != 0) return byMine;
            int byUpgrade = Integer.compare(sb.getUpgradesPerformed(), sa.getUpgradesPerformed());
            if (byUpgrade != 0) return byUpgrade;
            return Integer.compare(sb.getAirdropsCollected(), sa.getAirdropsCollected());
        });
        return list.subList(0, Math.min(n, list.size()));
    }

    /**
     * 为指定玩家构建 MVP 头衔摘要（矿工大师/战神/乐魂守护者），
     * 没有任何可展示数据时返回空串。
     */
    public String buildMvpSummary(Player viewer, List<Map.Entry<Player, PlayerStats>> statTop) {
        if (statTop.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<gold>—— 本场统计亮点 ——\n");

        Player bestMiner = null;
        Player bestKiller = null;
        Player bestUpgrader = null;
        int maxMined = 0;
        int maxKills = 0;
        int maxUpgrades = 0;
        for (Map.Entry<Player, PlayerStats> entry : statTop) {
            PlayerStats s = entry.getValue();
            if (s.getResourcesMined() > maxMined) {
                maxMined = s.getResourcesMined();
                bestMiner = entry.getKey();
            }
            if (s.getKills() > maxKills) {
                maxKills = s.getKills();
                bestKiller = entry.getKey();
            }
            if (s.getUpgradesPerformed() > maxUpgrades) {
                maxUpgrades = s.getUpgradesPerformed();
                bestUpgrader = entry.getKey();
            }
        }

        if (bestKiller != null && maxKills > 0) {
            sb.append("<red>⚔ 战神: ").append("<white>").append(bestKiller.getName())
                    .append("<gray>").append(" (").append(maxKills).append(" 击杀)\n");
        }
        if (bestMiner != null && maxMined > 0) {
            sb.append("<aqua>⛏ 矿工大师: ").append("<white>").append(bestMiner.getName())
                    .append("<gray>").append(" (采集 ").append(maxMined).append(" 次)\n");
        }
        if (bestUpgrader != null && maxUpgrades > 0) {
            sb.append("<light_purple>🛡 乐魂守护者: ").append("<white>").append(bestUpgrader.getName())
                    .append("<gray>").append(" (升级 ").append(maxUpgrades).append(" 次)");
        }

        return sb.toString();
    }

    /**
     * 显示玩家统计
     */
    public void showPlayerStats(Player player) {
        PlayerStats stats = getPlayerStats(player);

        StringBuilder statsMessage = new StringBuilder();
        statsMessage.append("<gold>=== 个人游戏统计 ===\n");
        statsMessage.append("<yellow>击杀数: ").append("<white>").append(stats.getKills()).append("\n");
        statsMessage.append("<yellow>死亡数: ").append("<white>").append(stats.getDeaths()).append("\n");
        statsMessage.append("<yellow>K/D 比率: ").append("<white>").append(String.format("%.2f", stats.getKDRatio())).append("\n");
        statsMessage.append("<yellow>骑乘乐魂数: ").append("<white>").append(stats.getGhastRides()).append("\n");
        statsMessage.append("<yellow>收集空投: ").append("<white>").append(stats.getAirdropsCollected()).append("\n");
        statsMessage.append("<yellow>开采资源: ").append("<white>").append(stats.getResourcesMined()).append("\n");
        statsMessage.append("<yellow>升级次数: ").append("<white>").append(stats.getUpgradesPerformed()).append("\n");
        statsMessage.append("<yellow>游戏时间: ").append("<white>").append(stats.getPlayTime()).append(" 分钟");

        Text.send(player, statsMessage.toString());
    }

    /**
     * 清理统计数据
     */
    public void cleanup() {
        playerStats.clear();
        joinTimes.clear();
    }
}
