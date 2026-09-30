package top.sparkpixel.hgw.events.game;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import top.sparkpixel.hgw.game.team.Team;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Resource implements Listener {

    private final HappyGhastWar ghastWar;

    public Resource(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    /**
     * 全部可采集方块 -> 应有掉落物（方块保留原地，仅产出掉落物）。
     * 覆盖所有矿物（含深板岩/下界变体）、粗金属块与各类原木。
     */
    private static final Map<Material, ItemStack> RESOURCE_DROPS = buildResourceDrops();

    private static Map<Material, ItemStack> buildResourceDrops() {
        Map<Material, ItemStack> drops = new EnumMap<>(Material.class);
        put(drops, Material.COAL, 3, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE);
        put(drops, Material.IRON_INGOT, 2, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE);
        put(drops, Material.COPPER_INGOT, 2, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE);
        put(drops, Material.GOLD_INGOT, 2, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE);
        put(drops, Material.REDSTONE, 4, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE);
        put(drops, Material.LAPIS_LAZULI, 4, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE);
        put(drops, Material.DIAMOND, 1, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE);
        put(drops, Material.EMERALD, 1, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);
        put(drops, Material.QUARTZ, 2, Material.NETHER_QUARTZ_ORE);
        put(drops, Material.IRON_INGOT, 6, Material.RAW_IRON_BLOCK);
        put(drops, Material.COPPER_INGOT, 6, Material.RAW_COPPER_BLOCK);
        put(drops, Material.GOLD_INGOT, 6, Material.RAW_GOLD_BLOCK);
        put(drops, Material.OAK_LOG, 2,
                Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
                Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG,
                Material.CHERRY_LOG, Material.PALE_OAK_LOG);
        return drops;
    }

    private static void put(Map<Material, ItemStack> drops, Material drop, int amount, Material... blocks) {
        for (Material block : blocks) {
            drops.put(block, new ItemStack(drop, amount));
        }
    }

    @EventHandler
    public void destroy(BlockBreakEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getBlock().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getBlock().getWorld().getName());
        if (!arena.isEnable()) return;

        Block block = e.getBlock();
        Player player = e.getPlayer();

        // 创造/旁观模式不参与资源系统：
        // 创造用于搭建地图（破坏直接消失、不恢复不掉落）；旁观者不能破坏方块
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        // 非游戏中：破坏由 Protection 取消（生存模式），资源逻辑不介入
        if (arena.status != Arena.GameStatus.PLAYING) return;

        // ---------------------------------------------------------------
        // 资源分区（全自动，无需手动绑定）：
        //   私有资源区 = 距任一队出生点 private-zone-radius 内 → 本队发展资源
        //   中立资源区 = 手动 addzone 富矿点 或 离所有队出生点都远的中部地区（中岛）
        // ---------------------------------------------------------------
        Arena.Teams ownerTeam = ownerTeamOf(arena, block.getLocation());
        boolean manualNeutral = isInManualNeutralZone(arena, block.getLocation());
        boolean neutral = manualNeutral || ownerTeam == null;

        // 私有资源区领地保护（可选开关）：非本队成员不可开采
        if (!neutral && ownerTeam != null && arena.getArenaConfig().isPrivateZoneLock()) {
            Team playerTeam = arena.getPlayerTeam(player);
            if (playerTeam == null || playerTeam.getTeams() != ownerTeam) {
                e.setCancelled(true);
                Text.send(player, ghastWar.getLanguage(player).getContent("game.private-zone-locked")
                        .replace("{0}", ownerTeam.getColor())
                        .replace("{1}", ownerTeam.getDisplayName()));
                return;
            }
        }

        ItemStack dropTemplate = RESOURCE_DROPS.get(block.getType());

        // ---------------------------------------------------------------
        // 地图不可破坏：非资源方块（含炉子/箱子等地图结构）一律取消破坏
        // ---------------------------------------------------------------
        if (dropTemplate == null) {
            e.setCancelled(true);
            return;
        }

        // 中立资源区：开采时向全图广播争夺消息（每队每局只广播一次，防刷屏）
        if (neutral) {
            Team team = arena.getPlayerTeam(player);
            if (team != null && arena.neutralMiningAnnounced.add(team.getTeams())) {
                String teamColor = team.getTeams().getColor();
                for (Player arenaPlayer : arena.getPlayers()) {
                    Text.send(arenaPlayer, ghastWar.getLanguage(arenaPlayer).getContent("game.neutral-zone-mining")
                            .replace("{0}", teamColor)
                            .replace("{1}", team.getTeams().getDisplayName()));
                }
            }
        }

        // ---------------------------------------------------------------
        // 资源方块：取消原版破坏 → 方块保留原地不消失，仅产出应有掉落物
        // 掉落物生成在挖矿玩家所在位置（玩家身体占据的必是空气格）：
        // 地图矿石四周/上方多为实心方块，若生成在方块内会被原版
        // "卡方块自救"一路顶到地图表面，玩家无法拾取
        // ---------------------------------------------------------------
        e.setCancelled(true);
        player.getWorld().dropItem(player.getLocation().add(0, 0.5, 0), dropTemplate.clone());
        arena.getStatistics().recordResourceMined(player);

    }

    /**
     * 判断方块属于哪个队的私有资源区：距该队出生点 private-zone-radius 内即为其领地。
     * 返回该队的枚举标识（Arena.Teams）；不在任何队半径内时返回 null（即中部/公共区域）。
     */
    private Arena.Teams ownerTeamOf(Arena arena, Location location) {
        int radius = arena.getArenaConfig().getPrivateZoneRadius();
        if (radius <= 0 || location.getWorld() == null) return null;

        double radiusSq = (double) radius * radius;
        for (Team team : arena.getTeams()) {
            Location spawn = teamSpawnOf(arena, team.getTeams());
            if (spawn == null || spawn.getWorld() == null || !spawn.getWorld().equals(location.getWorld())) continue;
            if (location.distanceSquared(spawn) <= radiusSq) {
                return team.getTeams();
            }
        }
        return null;
    }

    /** 读取某队出生点配置（未设置时返回 null，不抛异常） */
    private Location teamSpawnOf(Arena arena, Arena.Teams team) {
        String raw = arena.getArenaConfig().getConfig().getString("team." + team.name());
        return raw == null ? null : arena.getArenaConfig().translateLocation(raw);
    }

    /**
     * 检查位置是否在手动标记的中立富矿点内（addzone 指定，10格范围）。
     * 用于强制把某片区域划为中岛（即使它靠近某队出生点）。
     */
    private boolean isInManualNeutralZone(Arena arena, Location location) {
        List<String> neutralZones = arena.getArenaConfig().getNeutralZones();
        if (neutralZones == null || neutralZones.isEmpty()) return false;

        for (String zone : neutralZones) {
            String[] data = zone.split(",");
            if (data.length >= 4) {
                String worldName = data[0];
                try {
                    double x = Double.parseDouble(data[1]);
                    double y = Double.parseDouble(data[2]);
                    double z = Double.parseDouble(data[3]);

                    if (location.getWorld() != null && location.getWorld().getName().equals(worldName)) {
                        Location zoneLocation = new Location(location.getWorld(), x, y, z);
                        if (location.distance(zoneLocation) <= 10) { // 10格范围
                            return true;
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }
}
