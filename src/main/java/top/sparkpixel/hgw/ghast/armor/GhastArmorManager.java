package top.sparkpixel.hgw.ghast.armor;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.ghast.GameGhast;
import top.sparkpixel.hgw.game.team.Team;
import top.sparkpixel.hgw.util.SoundUtil;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * 乐魂护甲管理器
 */
public class GhastArmorManager {

    private final HappyGhastWar ghastWar;
    private final Arena arena;
    private final Map<GameGhast, ArmorData> armorDataMap = new HashMap<>();

    public GhastArmorManager(HappyGhastWar ghastWar, Arena arena) {
        this.ghastWar = ghastWar;
        this.arena = arena;
    }

    /**
     * 护甲数据
     */
    private static class ArmorData {
        private int armorLevel;
        private int maxArmorLevel = 5; // 最大护甲等级
        private long lastUpgradeTime;
        private final long upgradeCooldown = 30000L; // 30秒冷却时间

        public ArmorData() {
            this.armorLevel = 0;
            this.lastUpgradeTime = 0;
        }

        public int getArmorLevel() {
            return armorLevel;
        }

        public void setArmorLevel(int armorLevel) {
            this.armorLevel = Math.min(armorLevel, maxArmorLevel);
        }

        public boolean canUpgrade() {
            return System.currentTimeMillis() - lastUpgradeTime >= upgradeCooldown;
        }

        public void upgrade() {
            if (canUpgrade()) {
                this.armorLevel++;
                this.lastUpgradeTime = System.currentTimeMillis();
            }
        }

        public long getTimeRemaining() {
            long remaining = upgradeCooldown - (System.currentTimeMillis() - lastUpgradeTime);
            return Math.max(0, remaining / 1000); // 返回秒数
        }
    }

    /**
     * 注册乐魂护甲系统
     */
    public void registerArmorSystem() {
        // 为每个乐魂设置护甲数据
        for (GameGhast ghast : arena.getGhasts().values()) {
            armorDataMap.put(ghast, new ArmorData());
        }
    }

    /**
     * 升级乐魂护甲
     */
    public void upgradeGhastArmor(GameGhast ghast, Player rider) {
        ArmorData armorData = armorDataMap.get(ghast);
        if (armorData == null) {
            Text.send(rider, "<red>护甲系统未初始化");
            return;
        }

        if (!armorData.canUpgrade()) {
            long remaining = armorData.getTimeRemaining();
            Text.send(rider, "<yellow>护甲升级冷却中，还需 " + remaining + " 秒");
            return;
        }

        // 检查是否达到最大等级
        if (armorData.getArmorLevel() >= armorData.maxArmorLevel) {
            Text.send(rider, "<red>护甲已达到最大等级 " + armorData.maxArmorLevel);
            return;
        }

        // 消耗材料：下界合金碎片
        if (!consumeUpgradeMaterials(rider)) {
            Text.send(rider, "<red>升级失败：需要 1 个下界合金碎片");
            return;
        }

        // 升级护甲
        armorData.upgrade();
        ghast.setArmorLevel(armorData.getArmorLevel());

        // 更新乐魂外观
        updateGhastAppearance(ghast);

        // 发送升级成功消息
        Text.send(rider, "<green>护甲升级成功！当前等级: " + armorData.getArmorLevel() + "/" + armorData.maxArmorLevel);
        SoundUtil.play(rider, "armor-upgrade", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // 广播升级消息
        Team team = arena.getPlayerTeam(rider);
        if (team != null) {
            String teamColor = team.getTeams().getColor();
            Bukkit.broadcast(Text.mm(teamColor + rider.getName() + " 的乐魂护甲升级到 " + armorData.getArmorLevel() + " 级！"));
        }
    }

    /**
     * 消耗升级材料
     */
    private boolean consumeUpgradeMaterials(Player player) {
        // 检查玩家背包中是否有下界合金碎片
        ItemStack alloyScrap = new ItemStack(Material.NETHERITE_SCRAP, 1);

        if (player.getInventory().containsAtLeast(alloyScrap, 1)) {
            // 移除材料
            player.getInventory().removeItem(alloyScrap);
            return true;
        }

        return false;
    }

    /**
     * 更新乐魂外观
     */
    private void updateGhastAppearance(GameGhast ghast) {
        HappyGhast entity = ghast.getHappyGhast();
        int armorLevel = ghast.getArmorLevel();

        // 根据护甲等级设置外观
        switch (armorLevel) {
            case 0:
                // 无护甲：普通外观
                entity.setGlowing(false);
                break;
            case 1:
                // 1级护甲：轻微发光
                entity.setGlowing(true);
                break;
            case 2:
                // 2级护甲：中等发光
                entity.setGlowing(true);
                // 添加粒子效果
                addArmorParticles(ghast, Particle.END_ROD, 2);
                break;
            case 3:
                // 3级护甲：强发光
                entity.setGlowing(true);
                addArmorParticles(ghast, Particle.END_ROD, 4);
                break;
            case 4:
                // 4级护甲：超强发光
                entity.setGlowing(true);
                addArmorParticles(ghast, Particle.END_ROD, 6);
                break;
            case 5:
                // 5级护甲：终极发光
                entity.setGlowing(true);
                addArmorParticles(ghast, Particle.END_ROD, 8);
                // 添加特殊效果
                addUltimateEffects(ghast);
                break;
        }
    }

    /**
     * 添加护甲粒子效果（粒子跟随乐魂当前位置）
     */
    private void addArmorParticles(GameGhast ghast, Particle particle, int count) {
        HappyGhast entity = ghast.getHappyGhast();

        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (duration >= 100 || !entity.isValid()) { // 5秒
                    cancel();
                    return;
                }

                Location location = entity.getLocation();
                for (int i = 0; i < count; i++) {
                    double angle = (duration * 0.1 + i * Math.PI * 2) / count;
                    double x = Math.cos(angle) * 1.5;
                    double z = Math.sin(angle) * 1.5;

                    location.getWorld().spawnParticle(
                        particle,
                        location.clone().add(x, 1, z),
                        1, 0, 0, 0, 0
                    );
                }

                duration += 5;
            }
        }.runTaskTimer(ghastWar, 0, 5);
    }

    /**
     * 添加终极护甲效果（粒子跟随乐魂当前位置）
     */
    private void addUltimateEffects(GameGhast ghast) {
        HappyGhast entity = ghast.getHappyGhast();

        // 终极护甲光环效果
        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (duration >= 200 || !entity.isValid()) { // 10秒
                    cancel();
                    return;
                }

                Location location = entity.getLocation();
                // 创建多层光环
                for (int layer = 0; layer < 3; layer++) {
                    double radius = 2 + layer * 0.5;
                    for (int i = 0; i < 16; i++) {
                        double angle = (duration * 0.05 + i * Math.PI * 2) / 16;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        location.getWorld().spawnParticle(
                            Particle.END_ROD,
                            location.clone().add(x, 1.5 + layer * 0.3, z),
                            1, 0, 0, 0, 0
                        );
                    }
                }

                duration += 5;
            }
        }.runTaskTimer(ghastWar, 0, 5);

        // 播放特殊音效
        SoundUtil.playAt(entity.getWorld(), entity.getLocation(), "armor-ultimate", Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
    }

    /**
     * 获取护甲升级信息
     */
    public String getArmorUpgradeInfo(GameGhast ghast) {
        ArmorData armorData = armorDataMap.get(ghast);
        if (armorData == null) return "未注册";

        int currentLevel = armorData.getArmorLevel();
        int maxLevel = armorData.maxArmorLevel;

        if (armorData.canUpgrade()) {
            return "护甲等级: " + currentLevel + "/" + maxLevel + " (可升级)";
        } else {
            return "护甲等级: " + currentLevel + "/" + maxLevel + " (冷却中: " + armorData.getTimeRemaining() + "秒)";
        }
    }

    /**
     * 清理护甲数据
     */
    public void cleanup() {
        armorDataMap.clear();
    }
}