package me.wang.happyGhastWar.events.game;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Resource implements Listener {

    private final HappyGhastWar ghastWar;

    public Resource(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    /**
     * 全部可采集方块 -> 应有掉落物（挖取后自动恢复原方块）。
     * 覆盖所有矿物（含深板岩/下界变体）、粗金属块与各类原木。
     */
    private static final Map<Material, ItemStack> RESOURCE_DROPS = buildResourceDrops();

    /** 粗金属块 -> 挖取后降级成的对应矿石（到点仍恢复为原粗金属块） */
    private static final Map<Material, Material> RAW_DEGRADE = Map.of(
            Material.RAW_IRON_BLOCK, Material.IRON_ORE,
            Material.RAW_COPPER_BLOCK, Material.COPPER_ORE,
            Material.RAW_GOLD_BLOCK, Material.GOLD_ORE
    );

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

    /** 矿石 -> 被粗金属块降级覆盖时恢复应变成的粗金属块（非降级矿石返回 null） */
    private static Material rawBlockOf(Material ore) {
        return switch (ore) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.RAW_IRON_BLOCK;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER_BLOCK;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.RAW_GOLD_BLOCK;
            default -> null;
        };
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
        //   私有资源区 = 距任一队出生点 private-zone-radius 内 → 本队发展资源（普通恢复速度）
        //   中立资源区 = 手动 addzone 富矿点 或 离所有队出生点都远的中部地区（中岛）→ 恢复更快+全图广播
        // ---------------------------------------------------------------
        Arena.Teams ownerTeam = ownerTeamOf(arena, block.getLocation());
        boolean manualNeutral = isInManualNeutralZone(arena, block.getLocation());
        boolean neutral = manualNeutral || ownerTeam == null;

        // 私有资源区领地保护（可选开关）：非本队成员不可开采
        if (!neutral && ownerTeam != null && arena.getArenaConfig().isPrivateZoneLock()) {
            me.wang.happyGhastWar.game.team.Team playerTeam = arena.getPlayerTeam(player);
            if (playerTeam == null || playerTeam.getTeams() != ownerTeam) {
                e.setCancelled(true);
                Text.send(player, ghastWar.getLanguage(player).getContent("game.private-zone-locked")
                        .replace("{0}", ownerTeam.getColor())
                        .replace("{1}", ownerTeam.getDisplayName()));
                return;
            }
        }

        // 中立资源区：开采时向全图广播争夺消息
        if (neutral) {
            me.wang.happyGhastWar.game.team.Team team = arena.getPlayerTeam(player);
            if (team != null) {
                String teamColor = team.getTeams().getColor();
                for (Player arenaPlayer : arena.getPlayers()) {
                    Text.send(arenaPlayer, ghastWar.getLanguage(arenaPlayer).getContent("game.neutral-zone-mining")
                            .replace("{0}", teamColor)
                            .replace("{1}", team.getTeams().getDisplayName()));
                }
            }
        }

        if (block.getType() == Material.FURNACE || block.getType() == Material.BLAST_FURNACE) return;

        if (block.getType() == Material.CHEST){
            Chest chest = (Chest) block.getState();
            for (ItemStack itemStack : chest.getBlockInventory().getContents()){
                if (itemStack != null){
                    player.getWorld().dropItem(chest.getLocation(),itemStack);
                }
            }
            e.setDropItems(false);
            return;
        }

        // ---------------------------------------------------------------
        // 自动绑定：地图内所有对应矿石/原木自动成为资源点（无需手动逐个 addzone）
        // 挖取 -> 掉落该方块的应有资源 -> 到点恢复为原有方块
        // ---------------------------------------------------------------
        ItemStack dropTemplate = RESOURCE_DROPS.get(block.getType());
        if (dropTemplate == null) return; // 非资源方块：按原版逻辑处理

        Material originalType = block.getType();
        Material degraded = RAW_DEGRADE.get(originalType); // 粗金属块 -> 降级为对应矿石

        // 资源以掉落物形式产出（走近自动拾取），不再直接塞进背包
        e.setDropItems(false);
        Location dropLoc = block.getLocation().clone().add(0.5, 0.5, 0.5);
        block.getWorld().dropItemNaturally(dropLoc, dropTemplate.clone());

        if (degraded != null) {
            // 粗金属块：取消原版破坏并降级为对应矿石，到点恢复为原粗金属块
            e.setCancelled(true);
            block.setType(degraded);
            arena.getRawBlocks().remove(block.getLocation());
        } else if (arena.getRawBlocks().contains(block.getLocation())) {
            // 该位置是粗金属块降级出的矿石：恢复时升级回原粗金属块
            Material raw = rawBlockOf(originalType);
            if (raw != null) {
                originalType = raw;
            }
            arena.getRawBlocks().remove(block.getLocation());
        }

        arena.getResources().put(block.getLocation(), originalType);
        arena.getStatistics().recordResourceMined(player);

        // 中立资源区（中岛）恢复更快；私有资源区按普通速度；所有资源点都会自动恢复
        int respawnSeconds = neutral
                ? arena.getArenaConfig().getNeutralRespawnSeconds()
                : arena.getArenaConfig().getResourceRespawnSeconds();

        final Material restoreType = originalType;
        Bukkit.getServer().getScheduler().runTaskLater(HappyGhastWar.getPlugin(HappyGhastWar.class),() -> {
            // 仅当该位置仍登记为资源点时再生（防止世界重置后残留）
            if (!arena.getResources().containsKey(block.getLocation())){
                return;
            }
            arena.getResources().remove(block.getLocation());
            block.getLocation().getBlock().setType(restoreType);
        }, respawnSeconds * 20L);

    }

    /**
     * 判断方块属于哪个队的私有资源区：距该队出生点 private-zone-radius 内即为其领地。
     * 返回该队的枚举标识（Arena.Teams）；不在任何队半径内时返回 null（即中部/公共区域）。
     */
    private Arena.Teams ownerTeamOf(Arena arena, Location location) {
        int radius = arena.getArenaConfig().getPrivateZoneRadius();
        if (radius <= 0 || location.getWorld() == null) return null;

        double radiusSq = (double) radius * radius;
        for (me.wang.happyGhastWar.game.team.Team team : arena.getTeams()) {
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
