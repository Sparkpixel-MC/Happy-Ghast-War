package me.wang.happyGhastWar.game.upgrade;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class UpgradeManager {
    private static UpgradeManager instance;
    private Map<UUID, PlayerUpgrade> playerUpgrades = new HashMap<>();

    /** 缓存价格表：getPrice 调用频繁，不必每次重建 */
    private Map<UpgradeType, Map<Material, Integer>> priceCache;

    public enum UpgradeType {
        // 剑系列
        IRON_SWORD,
        IRON_SWORD_STORM,
        NETHERITE_SWORD_STORM,

        // 镐系列
        IRON_PICKAXE,
        IRON_PICKAXE_FORTUNE,
        NETHERITE_PICKAXE,

        // 斧系列
        IRON_AXE,
        IRON_AXE_EFFICIENCY,
        NETHERITE_AXE,

        // 锹系列
        STONE_SHOVEL,
        IRON_SHOVEL,
        IRON_SHOVEL_SILK_TOUCH,
        NETHERITE_SHOVEL,

        // 弹射器系列
        ADVANCED_CATAPULT,
        HIGH_CATAPULT,
        WIND_CORE_CATAPULT,

        // 火炮系列
        PRIMARY_CANNON,
        ADVANCED_CANNON,
        HIGH_CANNON,

        // 其他装备
        CROSSBOW,
        HEAVY_CROSSBOW,
        RAPID_CROSSBOW,
        HARD_SHIELD,
        ALLOY_SHIELD,
        TELEPORT_HOOK,
        BOW,

        // 护甲升级
        ARMOR_UPGRADE_I,
        ARMOR_UPGRADE_II,
        ARMOR_UPGRADE_III,

        // 胸甲附魔
        CHEST_ENCHANT_SPECIAL,
        CHEST_ENCHANT_SPECIALIZED,

        // 乐魂升级
        SOUL_ARMOR_UPGRADE_1,
        SOUL_ARMOR_UPGRADE_2,
        SOUL_ARMOR_UPGRADE_3,
        SOUL_HEALTH_UPGRADE_1,
        SOUL_HEALTH_UPGRADE_2,
        SOUL_HEALTH_UPGRADE_3
    }

    // 商品价格配置
    public Map<Material, Integer> getPrice(UpgradeType type) {
        if (priceCache == null) {
            priceCache = buildPrices();
        }
        return priceCache.get(type);
    }

    private Map<UpgradeType, Map<Material, Integer>> buildPrices() {
        Map<UpgradeType, Map<Material, Integer>> prices = new HashMap<>();

        // 剑系列
        Map<Material, Integer> ironSwordPrice = new HashMap<>();
        ironSwordPrice.put(Material.IRON_INGOT, 5);
        prices.put(UpgradeType.IRON_SWORD, ironSwordPrice);

        Map<Material, Integer> ironSwordStormPrice = new HashMap<>();
        ironSwordStormPrice.put(Material.IRON_INGOT, 8);
        ironSwordStormPrice.put(Material.OAK_LOG, 15);
        prices.put(UpgradeType.IRON_SWORD_STORM, ironSwordStormPrice);

        Map<Material, Integer> netheriteSwordStormPrice = new HashMap<>();
        netheriteSwordStormPrice.put(Material.NETHERITE_INGOT, 8);
        prices.put(UpgradeType.NETHERITE_SWORD_STORM, netheriteSwordStormPrice);

        // 镐系列
        Map<Material, Integer> ironPickaxePrice = new HashMap<>();
        ironPickaxePrice.put(Material.IRON_INGOT, 5);
        prices.put(UpgradeType.IRON_PICKAXE, ironPickaxePrice);

        Map<Material, Integer> ironPickaxeFortunePrice = new HashMap<>();
        ironPickaxeFortunePrice.put(Material.IRON_INGOT, 10);
        ironPickaxeFortunePrice.put(Material.OAK_LOG, 20);
        prices.put(UpgradeType.IRON_PICKAXE_FORTUNE, ironPickaxeFortunePrice);

        Map<Material, Integer> netheritePickaxePrice = new HashMap<>();
        netheritePickaxePrice.put(Material.NETHERITE_INGOT, 5);
        netheritePickaxePrice.put(Material.COAL, 20);
        prices.put(UpgradeType.NETHERITE_PICKAXE, netheritePickaxePrice);

        // 斧系列
        Map<Material, Integer> ironAxePrice = new HashMap<>();
        ironAxePrice.put(Material.IRON_INGOT, 5);
        prices.put(UpgradeType.IRON_AXE, ironAxePrice);

        Map<Material, Integer> ironAxeEfficiencyPrice = new HashMap<>();
        ironAxeEfficiencyPrice.put(Material.IRON_INGOT, 8);
        ironAxeEfficiencyPrice.put(Material.OAK_LOG, 12);
        prices.put(UpgradeType.IRON_AXE_EFFICIENCY, ironAxeEfficiencyPrice);

        Map<Material, Integer> netheriteAxePrice = new HashMap<>();
        netheriteAxePrice.put(Material.NETHERITE_INGOT, 10);
        netheriteAxePrice.put(Material.OAK_LOG, 20);
        prices.put(UpgradeType.NETHERITE_AXE, netheriteAxePrice);

        // 锹系列
        Map<Material, Integer> stoneShovelPrice = new HashMap<>();
        stoneShovelPrice.put(Material.IRON_INGOT, 5);
        prices.put(UpgradeType.STONE_SHOVEL, stoneShovelPrice);

        Map<Material, Integer> ironShovelPrice = new HashMap<>();
        ironShovelPrice.put(Material.IRON_INGOT, 8);
        ironShovelPrice.put(Material.OAK_LOG, 15);
        prices.put(UpgradeType.IRON_SHOVEL, ironShovelPrice);

        Map<Material, Integer> ironShovelSilkTouchPrice = new HashMap<>();
        ironShovelSilkTouchPrice.put(Material.IRON_INGOT, 10);
        ironShovelSilkTouchPrice.put(Material.OAK_LOG, 20);
        prices.put(UpgradeType.IRON_SHOVEL_SILK_TOUCH, ironShovelSilkTouchPrice);

        Map<Material, Integer> netheriteShovelPrice = new HashMap<>();
        netheriteShovelPrice.put(Material.NETHERITE_INGOT, 5);
        netheriteShovelPrice.put(Material.IRON_INGOT, 10);
        prices.put(UpgradeType.NETHERITE_SHOVEL, netheriteShovelPrice);

        // 弹射器系列（修正为活塞）
        Map<Material, Integer> advancedCatapultPrice = new HashMap<>();
        advancedCatapultPrice.put(Material.COPPER_INGOT, 10);
        advancedCatapultPrice.put(Material.OAK_LOG, 5);
        prices.put(UpgradeType.ADVANCED_CATAPULT, advancedCatapultPrice);

        Map<Material, Integer> highCatapultPrice = new HashMap<>();
        highCatapultPrice.put(Material.IRON_INGOT, 15);
        highCatapultPrice.put(Material.OAK_LOG, 20);
        prices.put(UpgradeType.HIGH_CATAPULT, highCatapultPrice);

        Map<Material, Integer> windCoreCatapultPrice = new HashMap<>();
        windCoreCatapultPrice.put(Material.NETHERITE_INGOT, 10);
        windCoreCatapultPrice.put(Material.OAK_LOG, 20);
        prices.put(UpgradeType.WIND_CORE_CATAPULT, windCoreCatapultPrice);

        // 火炮系列（修正为发射器）
        Map<Material, Integer> primaryCannonPrice = new HashMap<>();
        primaryCannonPrice.put(Material.COAL, 35);
        primaryCannonPrice.put(Material.IRON_INGOT, 10);
        prices.put(UpgradeType.PRIMARY_CANNON, primaryCannonPrice);

        Map<Material, Integer> advancedCannonPrice = new HashMap<>();
        advancedCannonPrice.put(Material.COAL, 25);
        advancedCannonPrice.put(Material.COPPER_INGOT, 5);
        prices.put(UpgradeType.ADVANCED_CANNON, advancedCannonPrice);

        Map<Material, Integer> highCannonPrice = new HashMap<>();
        highCannonPrice.put(Material.COAL, 50);
        highCannonPrice.put(Material.NETHERITE_INGOT, 15);
        prices.put(UpgradeType.HIGH_CANNON, highCannonPrice);

        // 其他装备
        Map<Material, Integer> crossbowPrice = new HashMap<>();
        crossbowPrice.put(Material.IRON_INGOT, 5);
        crossbowPrice.put(Material.OAK_LOG, 5);
        prices.put(UpgradeType.CROSSBOW, crossbowPrice);

        Map<Material, Integer> bowPrice = new HashMap<>();
        bowPrice.put(Material.OAK_LOG, 5);
        prices.put(UpgradeType.BOW, bowPrice);

        Map<Material, Integer> heavyCrossbowPrice = new HashMap<>();
        heavyCrossbowPrice.put(Material.IRON_INGOT, 15);
        heavyCrossbowPrice.put(Material.OAK_LOG, 20);
        prices.put(UpgradeType.HEAVY_CROSSBOW, heavyCrossbowPrice);

        Map<Material, Integer> rapidCrossbowPrice = new HashMap<>();
        rapidCrossbowPrice.put(Material.IRON_INGOT, 15);
        rapidCrossbowPrice.put(Material.OAK_LOG, 15);
        prices.put(UpgradeType.RAPID_CROSSBOW, rapidCrossbowPrice);

        Map<Material, Integer> hardShieldPrice = new HashMap<>();
        hardShieldPrice.put(Material.OAK_LOG, 20);
        hardShieldPrice.put(Material.IRON_INGOT, 5);
        prices.put(UpgradeType.HARD_SHIELD, hardShieldPrice);

        Map<Material, Integer> alloyShieldPrice = new HashMap<>();
        alloyShieldPrice.put(Material.OAK_LOG, 15);
        alloyShieldPrice.put(Material.NETHERITE_INGOT, 5);
        prices.put(UpgradeType.ALLOY_SHIELD, alloyShieldPrice);

        Map<Material, Integer> teleportHookPrice = new HashMap<>();
        teleportHookPrice.put(Material.IRON_INGOT, 5);
        teleportHookPrice.put(Material.OAK_LOG, 40);
        prices.put(UpgradeType.TELEPORT_HOOK, teleportHookPrice);

        // 护甲升级
        Map<Material, Integer> armorUpgrade1Price = new HashMap<>();
        armorUpgrade1Price.put(Material.IRON_INGOT, 5);
        prices.put(UpgradeType.ARMOR_UPGRADE_I, armorUpgrade1Price);

        Map<Material, Integer> armorUpgrade2Price = new HashMap<>();
        armorUpgrade2Price.put(Material.IRON_INGOT, 7);
        armorUpgrade2Price.put(Material.COAL, 10);
        prices.put(UpgradeType.ARMOR_UPGRADE_II, armorUpgrade2Price);

        Map<Material, Integer> armorUpgrade3Price = new HashMap<>();
        armorUpgrade3Price.put(Material.NETHERITE_INGOT, 7);
        armorUpgrade3Price.put(Material.IRON_INGOT, 10);
        prices.put(UpgradeType.ARMOR_UPGRADE_III, armorUpgrade3Price);

        // 胸甲附魔
        Map<Material, Integer> chestEnchantSpecialPrice = new HashMap<>();
        chestEnchantSpecialPrice.put(Material.OAK_LOG, 20);
        prices.put(UpgradeType.CHEST_ENCHANT_SPECIAL, chestEnchantSpecialPrice);

        Map<Material, Integer> chestEnchantSpecializedPrice = new HashMap<>();
        chestEnchantSpecializedPrice.put(Material.IRON_INGOT, 10);
        prices.put(UpgradeType.CHEST_ENCHANT_SPECIALIZED, chestEnchantSpecializedPrice);

        // 乐魂升级（修正为白色挽具和雪块）
        Map<Material, Integer> soulArmor1Price = new HashMap<>();
        soulArmor1Price.put(Material.OAK_LOG, 20);
        prices.put(UpgradeType.SOUL_ARMOR_UPGRADE_1, soulArmor1Price);

        Map<Material, Integer> soulArmor2Price = new HashMap<>();
        soulArmor2Price.put(Material.OAK_LOG, 50);
        prices.put(UpgradeType.SOUL_ARMOR_UPGRADE_2, soulArmor2Price);

        Map<Material, Integer> soulArmor3Price = new HashMap<>();
        soulArmor3Price.put(Material.IRON_INGOT, 30);
        prices.put(UpgradeType.SOUL_ARMOR_UPGRADE_3, soulArmor3Price);

        Map<Material, Integer> soulHealth1Price = new HashMap<>();
        soulHealth1Price.put(Material.SNOW_BLOCK, 60);
        prices.put(UpgradeType.SOUL_HEALTH_UPGRADE_1, soulHealth1Price);

        return prices;
    }

    // 获取商品描述
    public String getDescription(UpgradeType type) {
        Map<UpgradeType, String> descriptions = new HashMap<>();

        descriptions.put(UpgradeType.IRON_SWORD, "铁剑");
        descriptions.put(UpgradeType.IRON_SWORD_STORM, "铁剑(风暴I)");
        descriptions.put(UpgradeType.NETHERITE_SWORD_STORM, "下界合金剑(风暴I)");

        descriptions.put(UpgradeType.IRON_PICKAXE, "铁镐");
        descriptions.put(UpgradeType.IRON_PICKAXE_FORTUNE, "铁镐(时运III)");
        descriptions.put(UpgradeType.NETHERITE_PICKAXE, "下界合金镐");

        descriptions.put(UpgradeType.IRON_AXE, "铁斧");
        descriptions.put(UpgradeType.IRON_AXE_EFFICIENCY, "铁斧(效率III)");
        descriptions.put(UpgradeType.NETHERITE_AXE, "下界合金斧(效率VII,锋利I)");

        descriptions.put(UpgradeType.STONE_SHOVEL, "石锹");
        descriptions.put(UpgradeType.IRON_SHOVEL, "铁锹");
        descriptions.put(UpgradeType.IRON_SHOVEL_SILK_TOUCH, "铁锹(精准采集)");
        descriptions.put(UpgradeType.NETHERITE_SHOVEL, "下界合金锹");

        descriptions.put(UpgradeType.ADVANCED_CATAPULT, "进阶弹射器");
        descriptions.put(UpgradeType.HIGH_CATAPULT, "高阶弹射器");
        descriptions.put(UpgradeType.WIND_CORE_CATAPULT, "风核弹射器");

        descriptions.put(UpgradeType.PRIMARY_CANNON, "初级核心火炮");
        descriptions.put(UpgradeType.ADVANCED_CANNON, "进阶核心火炮");
        descriptions.put(UpgradeType.HIGH_CANNON, "高阶核心火炮");

        descriptions.put(UpgradeType.CROSSBOW, "弩");
        descriptions.put(UpgradeType.HEAVY_CROSSBOW, "重型巨弩");
        descriptions.put(UpgradeType.RAPID_CROSSBOW, "连射弩");

        descriptions.put(UpgradeType.HARD_SHIELD, "硬质盾");
        descriptions.put(UpgradeType.ALLOY_SHIELD, "合金盾");
        descriptions.put(UpgradeType.TELEPORT_HOOK, "传送钩");

        descriptions.put(UpgradeType.ARMOR_UPGRADE_I, "护甲升级I(铁制)");
        descriptions.put(UpgradeType.ARMOR_UPGRADE_II, "护甲升级II(钻石)");
        descriptions.put(UpgradeType.ARMOR_UPGRADE_III, "护甲升级III(下界合金)");

        descriptions.put(UpgradeType.CHEST_ENCHANT_SPECIAL, "胸甲强化附魔");
        descriptions.put(UpgradeType.CHEST_ENCHANT_SPECIALIZED, "胸甲附魔特化");

        descriptions.put(UpgradeType.SOUL_ARMOR_UPGRADE_1, "乐魂护甲提升(一级)");
        descriptions.put(UpgradeType.SOUL_ARMOR_UPGRADE_2, "乐魂护甲提升(二级)");
        descriptions.put(UpgradeType.SOUL_ARMOR_UPGRADE_3, "乐魂护甲提升(三级)");
        descriptions.put(UpgradeType.SOUL_HEALTH_UPGRADE_1, "乐魂血量提升(一级)");

        return descriptions.getOrDefault(type, "未知商品");
    }

    // 获取商品图标（修正材质）
    public Material getIcon(UpgradeType type) {
        switch (type) {
            // 剑系列
            case IRON_SWORD:
            case IRON_SWORD_STORM:
                return Material.IRON_SWORD;
            case NETHERITE_SWORD_STORM:
                return Material.NETHERITE_SWORD;

            // 镐系列
            case IRON_PICKAXE:
            case IRON_PICKAXE_FORTUNE:
                return Material.IRON_PICKAXE;
            case NETHERITE_PICKAXE:
                return Material.NETHERITE_PICKAXE;

            // 斧系列
            case IRON_AXE:
            case IRON_AXE_EFFICIENCY:
                return Material.IRON_AXE;
            case NETHERITE_AXE:
                return Material.NETHERITE_AXE;

            // 锹系列
            case STONE_SHOVEL:
                return Material.STONE_SHOVEL;
            case IRON_SHOVEL:
            case IRON_SHOVEL_SILK_TOUCH:
                return Material.IRON_SHOVEL;
            case NETHERITE_SHOVEL:
                return Material.NETHERITE_SHOVEL;

            // 弹射器系列（改为活塞）
            case ADVANCED_CATAPULT:
            case HIGH_CATAPULT:
            case WIND_CORE_CATAPULT:
                return Material.PISTON;

            // 火炮系列（改为发射器）
            case PRIMARY_CANNON:
            case ADVANCED_CANNON:
            case HIGH_CANNON:
                return Material.DISPENSER;

            // 弩系列
            case CROSSBOW:
            case HEAVY_CROSSBOW:
            case RAPID_CROSSBOW:
                return Material.CROSSBOW;

            // 盾牌
            case HARD_SHIELD:
            case ALLOY_SHIELD:
                return Material.SHIELD;

            // 传送钩
            case TELEPORT_HOOK:
                return Material.FISHING_ROD;

            // 护甲升级
            case ARMOR_UPGRADE_I:
                return Material.IRON_CHESTPLATE;
            case ARMOR_UPGRADE_II:
                return Material.DIAMOND_CHESTPLATE;
            case ARMOR_UPGRADE_III:
                return Material.NETHERITE_CHESTPLATE;

            // 胸甲附魔
            case CHEST_ENCHANT_SPECIAL:
            case CHEST_ENCHANT_SPECIALIZED:
                return Material.ENCHANTED_BOOK;

            // 乐魂升级（改为白色挽具和雪块）
            case SOUL_ARMOR_UPGRADE_1:
            case SOUL_ARMOR_UPGRADE_2:
            case SOUL_ARMOR_UPGRADE_3:
                return Material.LEATHER_HORSE_ARMOR; // 白色挽具
            case SOUL_HEALTH_UPGRADE_1:
                return Material.SNOW_BLOCK;

            default:
                return Material.BARRIER;
        }
    }

    private UpgradeManager() {}

    public static UpgradeManager getInstance() {
        if (instance == null) {
            instance = new UpgradeManager();
        }
        return instance;
    }

    public PlayerUpgrade getPlayerUpgrade(Player player) {
        return playerUpgrades.computeIfAbsent(player.getUniqueId(), k -> new PlayerUpgrade(player));
    }

    public void savePlayerUpgrade(Player player, PlayerUpgrade upgrade) {
        playerUpgrades.put(player.getUniqueId(), upgrade);
    }

    public void removePlayerUpgrade(Player player) {
        playerUpgrades.remove(player.getUniqueId());
    }

    // 检查玩家是否有足够的材料
    public boolean hasEnoughMaterials(Player player, Map<Material, Integer> required) {
        // 未配置价格的商品视为不可购买，防止误变成免费
        if (required == null) return false;

        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == entry.getKey()) {
                    count += item.getAmount();
                }
            }
            if (count < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // 扣除材料
    public void deductMaterials(Player player, Map<Material, Integer> required) {
        if (required == null) return;

        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            int remaining = entry.getValue();

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == entry.getKey()) {
                    if (item.getAmount() <= remaining) {
                        remaining -= item.getAmount();
                        player.getInventory().remove(item);
                    } else {
                        item.setAmount(item.getAmount() - remaining);
                        remaining = 0;
                    }

                    if (remaining <= 0) break;
                }
            }
        }
    }

    // 通用购买方法
    public boolean purchaseUpgrade(Player player, UpgradeType type) {
        Map<Material, Integer> price = getPrice(type);

        // 检查材料是否足够
        if (!hasEnoughMaterials(player, price)) {
            me.wang.happyGhastWar.util.Text.send(player, "<red>材料不足！");
            return false;
        }

        // 扣除材料
        deductMaterials(player, price);
        return true;
    }

    public void removePlayer(Player p){
        playerUpgrades.remove(p.getUniqueId());
    }
}