package top.sparkpixel.hgw.game.upgrade;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerUpgrade {
    private UUID playerId;

    // 武器升级状态
    private int swordLevel = 0; // 0:石剑, 1:铁剑, 2:铁剑(风暴I), 3:下界合金剑(风暴I)
    private int pickaxeLevel = 0; // 0:石镐, 1:铁镐, 2:铁镐(时运III), 3:下界合金镐
    private int axeLevel = 0; // 0:石斧, 1:铁斧, 2:铁斧(效率III), 3:下界合金斧(效率VII,锋利I)
    private int shovelLevel = 0; // 0:木锹, 1:石锹, 2:铁锹, 3:铁锹(精准采集), 4:下界合金锹
    private int catapultLevel = 0; // 0:初级, 1:进阶, 2:高阶, 3:风核
    private int cannonLevel = -1; // -1:未购买, 0:初级, 1:进阶, 2:高阶

    // 其他装备状态
    private boolean hasCrossbow = false;
    private boolean hasBow = false;
    private boolean hasHeavyCrossbow = false;
    private boolean hasRapidCrossbow = false;
    private int shieldLevel = 0; // 0:无, 1:硬质盾, 2:合金盾
    private boolean hasTeleportHook = false;

    // 护甲升级状态
    private int armorLevel = 0; // 0:无, 1:铁制, 2:钻石, 3:下界合金
    private boolean hasChestEnchantSpecial = false; // 胸甲强化附魔
    private boolean hasChestEnchantSpecialized = false; // 胸甲附魔特化

    // 乐魂升级
    private int soulArmorLevel = 0; // 乐魂护甲等级
    private int soulHealthLevel = 0; // 乐魂血量等级

    public PlayerUpgrade(Player player) {
        this.playerId = player.getUniqueId();
    }

    public UUID getPlayerId() { return playerId; }
    public int getSwordLevel() { return swordLevel; }
    public int getPickaxeLevel() { return pickaxeLevel; }
    public int getAxeLevel() { return axeLevel; }
    public int getShovelLevel() { return shovelLevel; }
    public int getCatapultLevel() { return catapultLevel; }
    public int getCannonLevel() { return cannonLevel; }
    public boolean hasCrossbow() { return hasCrossbow; }
    public boolean hasBow() { return hasBow; }
    public boolean hasHeavyCrossbow() { return hasHeavyCrossbow; }
    public boolean hasRapidCrossbow() { return hasRapidCrossbow; }
    public int getShieldLevel() { return shieldLevel; }
    public boolean hasTeleportHook() { return hasTeleportHook; }
    public int getArmorLevel() { return armorLevel; }
    public boolean hasChestEnchantSpecial() { return hasChestEnchantSpecial; }
    public boolean hasChestEnchantSpecialized() { return hasChestEnchantSpecialized; }
    public int getSoulArmorLevel() { return soulArmorLevel; }
    public int getSoulHealthLevel() { return soulHealthLevel; }

    // 升级方法（每次只升一级）
    public boolean upgradeSword() {
        if (swordLevel < 3) {
            swordLevel++;
            return true;
        }
        return false;
    }

    public boolean upgradePickaxe() {
        if (pickaxeLevel < 3) {
            pickaxeLevel++;
            return true;
        }
        return false;
    }

    public boolean upgradeAxe() {
        if (axeLevel < 3) {
            axeLevel++;
            return true;
        }
        return false;
    }

    public boolean upgradeShovel() {
        if (shovelLevel < 4) {
            shovelLevel++;
            return true;
        }
        return false;
    }

    public boolean upgradeCatapult() {
        if (catapultLevel < 3) {
            catapultLevel++;
            return true;
        }
        return false;
    }

    public boolean upgradeCannon() {
        if (cannonLevel < 2) {
            if (cannonLevel == -1) {
                cannonLevel = 0;
            } else {
                cannonLevel++;
            }
            return true;
        }
        return false;
    }

    public boolean buyCrossbow() {
        if (!hasCrossbow) {
            hasCrossbow = true;
            return true;
        }
        return false;
    }

    public boolean buyBow() {
        if (!hasBow) {
            hasBow = true;
            return true;
        }
        return false;
    }

    public boolean buyHeavyCrossbow() {
        if (!hasHeavyCrossbow) {
            hasHeavyCrossbow = true;
            return true;
        }
        return false;
    }

    public boolean buyRapidCrossbow() {
        if (!hasRapidCrossbow) {
            hasRapidCrossbow = true;
            return true;
        }
        return false;
    }

    public boolean upgradeShield() {
        if (shieldLevel < 2) {
            shieldLevel++;
            return true;
        }
        return false;
    }

    public boolean buyTeleportHook() {
        if (!hasTeleportHook) {
            hasTeleportHook = true;
            return true;
        }
        return false;
    }

    public boolean upgradeArmor() {
        if (armorLevel < 3) {
            armorLevel++;
            return true;
        }
        return false;
    }

    public boolean buyChestEnchantSpecial() {
        if (!hasChestEnchantSpecial) {
            hasChestEnchantSpecial = true;
            return true;
        }
        return false;
    }

    public boolean buyChestEnchantSpecialized() {
        if (!hasChestEnchantSpecialized) {
            hasChestEnchantSpecialized = true;
            return true;
        }
        return false;
    }

    public boolean upgradeSoulArmor() {
        if (soulArmorLevel < 3) {
            soulArmorLevel++;
            return true;
        }
        return false;
    }

    public boolean upgradeSoulHealth() {
        if (soulHealthLevel < 1) {
            soulHealthLevel++;
            return true;
        }
        return false;
    }

    // 获取当前装备名称
    public String getCurrentSwordName() {
        switch (swordLevel) {
            case 0: return "石剑";
            case 1: return "铁剑";
            case 2: return "铁剑(风暴I)";
            case 3: return "下界合金剑(风暴I)";
            default: return "未知";
        }
    }

    public String getCurrentPickaxeName() {
        switch (pickaxeLevel) {
            case 0: return "石镐";
            case 1: return "铁镐";
            case 2: return "铁镐(时运III)";
            case 3: return "下界合金镐";
            default: return "未知";
        }
    }

    public String getCurrentAxeName() {
        switch (axeLevel) {
            case 0: return "石斧";
            case 1: return "铁斧";
            case 2: return "铁斧(效率III)";
            case 3: return "下界合金斧(效率VII,锋利I)";
            default: return "未知";
        }
    }

    public String getCurrentShovelName() {
        switch (shovelLevel) {
            case 0: return "木锹";
            case 1: return "石锹";
            case 2: return "铁锹";
            case 3: return "铁锹(精准采集)";
            case 4: return "下界合金锹";
            default: return "未知";
        }
    }

    public String getCurrentCatapultName() {
        switch (catapultLevel) {
            case 0: return "初级弹射器";
            case 1: return "进阶弹射器";
            case 2: return "高阶弹射器";
            case 3: return "风核弹射器";
            default: return "未知";
        }
    }

    public String getCurrentCannonName() {
        switch (cannonLevel) {
            case -1: return "未拥有";
            case 0: return "初级核心火炮";
            case 1: return "进阶核心火炮";
            case 2: return "高阶核心火炮";
            default: return "未知";
        }
    }

    public String getCurrentArmorName() {
        switch (armorLevel) {
            case 0: return "无护甲升级";
            case 1: return "护甲升级I(铁制)";
            case 2: return "护甲升级II(钻石)";
            case 3: return "护甲升级III(下界合金)";
            default: return "未知";
        }
    }



    public Material getCurrentPickaxeMaterial() {
        switch (pickaxeLevel) {
            case 0: return Material.STONE_PICKAXE;
            case 1: return Material.IRON_PICKAXE;
            case 2: return Material.IRON_PICKAXE;
            case 3: return Material.NETHERITE_PICKAXE;
            default: return Material.STONE_PICKAXE;
        }
    }

    public Material getCurrentAxeMaterial() {
        switch (axeLevel) {
            case 0: return Material.STONE_AXE;
            case 1: return Material.IRON_AXE;
            case 2: return Material.IRON_AXE;
            case 3: return Material.NETHERITE_AXE;
            default: return Material.STONE_AXE;
        }
    }

    public Material getCurrentShovelMaterial() {
        switch (shovelLevel) {
            case 0: return Material.WOODEN_SHOVEL;
            case 1: return Material.STONE_SHOVEL;
            case 2: return Material.IRON_SHOVEL;
            case 3: return Material.IRON_SHOVEL;
            case 4: return Material.NETHERITE_SHOVEL;
            default: return Material.WOODEN_SHOVEL;
        }
    }

    public Material getCurrentArmorMaterial() {
        switch (armorLevel) {
            case 0: return null; // 无护甲升级
            case 1: return Material.IRON_CHESTPLATE;
            case 2: return Material.DIAMOND_CHESTPLATE;
            case 3: return Material.NETHERITE_CHESTPLATE;
            default: return null;
        }
    }
}