package top.sparkpixel.hgw.game.upgrade;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.game.team.Team;
import top.sparkpixel.hgw.ghast.GameGhast;
import top.sparkpixel.hgw.util.Language;
import top.sparkpixel.hgw.util.SoundUtil;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.stream.Collectors;

public class UpgradeGUI implements Listener {
    private JavaPlugin plugin;
    public UpgradeManager upgradeManager;
    private Set<UUID> cooldownPlayers = new HashSet<>();
    private Language language = HappyGhastWar.language;
    private Arena arena;

    private Map<Player,GameGhast> ghastMap = new HashMap<>();

    private Map<Player, Team> playerMap = new HashMap<>();

    public UpgradeGUI(JavaPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.upgradeManager = UpgradeManager.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.arena = arena;
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public Material getColor(Team team){
        if (team == null) return Material.BLACK_STAINED_GLASS_PANE;
        return switch (team.getTeams()) {
            case RED -> Material.RED_STAINED_GLASS_PANE;
            case BLUE -> Material.BLUE_STAINED_GLASS_PANE;
            case GREEN -> Material.GREEN_STAINED_GLASS_PANE;
            case YELLOW -> Material.YELLOW_STAINED_GLASS_PANE;
            case AQUA -> Material.CYAN_STAINED_GLASS_PANE;
            case WHITE -> Material.WHITE_STAINED_GLASS_PANE;
            case GOLD -> Material.ORANGE_STAINED_GLASS_PANE;
            case PURPLE -> Material.PURPLE_STAINED_GLASS_PANE;
        };
    }

    public void unregister(){
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }

    public void openUpgradeGUI(Player player,Team team) {
        openUpgradeGUI(player,team,null);
    }

    public void openUpgradeGUI(Player player, Team team, Interaction interaction) {
        Inventory gui = Bukkit.createInventory(null, 6 * 9, Text.legacy(language.getContent("upgrade.gui-title")));
        PlayerUpgrade upgrade = upgradeManager.getPlayerUpgrade(player);

        // 填充背景玻璃板
        ItemStack background = new ItemStack(getColor(team));
        ItemMeta bgMeta = background.getItemMeta();
        bgMeta.displayName(Text.mm(" "));
        background.setItemMeta(bgMeta);

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // 第一行：武器升级
        gui.setItem(10, createSwordItem(player, upgrade));
        gui.setItem(11, createAxeItem(player, upgrade));
        gui.setItem(12, createPickaxeItem(player, upgrade));
        gui.setItem(13, createShovelItem(player, upgrade));

        // 第二行：特殊装备
        gui.setItem(19, createBowItem(player, upgrade));
        gui.setItem(20, createCrossbowItem(player, upgrade, UpgradeManager.UpgradeType.CROSSBOW));
        gui.setItem(21, createCrossbowItem(player, upgrade, UpgradeManager.UpgradeType.RAPID_CROSSBOW));
        gui.setItem(22, createCrossbowItem(player, upgrade, UpgradeManager.UpgradeType.HEAVY_CROSSBOW));

        // 第三行：护甲和特殊
        gui.setItem(28, createShieldItem(player, upgrade));
        gui.setItem(29, createArmorItem(player, upgrade));
        gui.setItem(30, createEnchantItem(player, upgrade, UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIAL));
        gui.setItem(31, createEnchantItem(player, upgrade, UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIALIZED));


        // 第四行
        gui.setItem(37, createTeleportHookItem(player, upgrade));
        gui.setItem(38, createCatapultItem(player, upgrade));
        gui.setItem(39, createCannonItem(player, upgrade));

        gui.setItem(45, createTeamChest());
        gui.setItem(7, createArrowMaker());

        if (team != null){
            gui.setItem(33, createSoulHealthItem(player, upgrade));
            gui.setItem(34, createSoulArmorItem(player, upgrade));

            gui.setItem(53,new ItemStack(Material.AIR));
        }
        if (interaction != null && team != null){
            GameGhast ghast = team.getGhastByInteraction(interaction);
            if (ghast != null){
                if (ghast.getSnowAmount() > 0){
                    gui.setItem(53,new ItemStack(Material.SNOW_BLOCK,ghast.getSnowAmount()));
                }
            }
            ghastMap.put(player,ghast);
        }
        playerMap.put(player,team);
        player.openInventory(gui);
    }

    @EventHandler
    public void close(InventoryCloseEvent e){
        if (!(e.getPlayer() instanceof Player)) return;
        if (!e.getView().getTitle().equals(Text.legacy(language.getContent("upgrade.gui-title")))) return;

        if (!HappyGhastWar.arenas.containsKey(e.getPlayer().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getPlayer().getWorld().getName());

        Player player = (Player) e.getPlayer();
        Team team = arena.getPlayerTeam(player);
        if (team == null) return;
        if (!ghastMap.containsKey(player)) return;
        ItemStack snowblock = e.getInventory().getItem(53);
        GameGhast ghast = ghastMap.get(player);
        if (snowblock == null){
            ghast.setSnowAmount(0);
            ghastMap.remove(player);
            return;
        }
        ghast.setSnowAmount(snowblock.getAmount());
        playerMap.remove(player);
        ghastMap.remove(player);
    }

    private ItemStack createArrowMaker(){
        ItemStack itemStack = new ItemStack(Material.FLETCHING_TABLE);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Text.mm("<gold>制箭"));
        List<String> lore = new ArrayList<>();
        lore.add("<yellow>单击制作5根箭矢，消耗1原木");
        itemMeta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private ItemStack createTeamChest(){
        ItemStack itemStack = new ItemStack(Material.WHITE_HARNESS);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Text.mm("<gold>团队箱子"));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    // 创建剑升级项
    private ItemStack createSwordItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getSwordLevel();
        UpgradeManager.UpgradeType nextUpgrade = getNextSwordUpgrade(upgrade);
        List<String> lore = new ArrayList<>();

        lore.add(language.getContent("upgrade.item-now").replace("{0}",upgrade.getCurrentSwordName()));

        if (nextUpgrade != null) {
            // 显示下一级升级
            lore.add(language.getContent("upgrade.item-next").replace("{0}",upgradeManager.getDescription(nextUpgrade)));
            lore.add("");

            // 价格信息
            Map<Material, Integer> price = upgradeManager.getPrice(nextUpgrade);
            if (price != null && !price.isEmpty()) {
                lore.add(language.getContent("upgrade.item-price"));
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            // 功能描述
            if (nextUpgrade == UpgradeManager.UpgradeType.IRON_SWORD_STORM ||
                    nextUpgrade == UpgradeManager.UpgradeType.NETHERITE_SWORD_STORM) {
                lore.add("<gold>功能: <gray>风暴I附魔");
                lore.add("<gray>暴击时生成风暴弹");
                lore.add("");
            }

            lore.add("<yellow>点击升级");

            ItemStack item = new ItemStack(upgradeManager.getIcon(nextUpgrade));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>剑升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            // 已满级
            lore.add("<green>✔ 已满级");
            lore.add("");
            lore.add("<gold>功能: <gray>风暴I附魔");
            lore.add("<gray>暴击时生成风暴弹");

            ItemStack item = new ItemStack(Material.NETHERITE_SWORD); // 使用满级物品材质
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>剑升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建镐升级项
    private ItemStack createPickaxeItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getPickaxeLevel();
        UpgradeManager.UpgradeType nextUpgrade = getNextPickaxeUpgrade(upgrade);
        List<String> lore = new ArrayList<>();

        lore.add("<gray>当前: <yellow>" + upgrade.getCurrentPickaxeName());

        if (nextUpgrade != null) {
            lore.add("<gray>下一级: <green>" + upgradeManager.getDescription(nextUpgrade));
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(nextUpgrade);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            if (nextUpgrade == UpgradeManager.UpgradeType.IRON_PICKAXE_FORTUNE) {
                lore.add("<gold>功能: <gray>时运III");
                lore.add("");
            }

            lore.add("<yellow>点击升级");

            ItemStack item = new ItemStack(upgradeManager.getIcon(nextUpgrade));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>镐升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<green>✔ 已满级");
            ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>镐升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建斧升级项
    private ItemStack createAxeItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getAxeLevel();
        UpgradeManager.UpgradeType nextUpgrade = getNextAxeUpgrade(upgrade);
        List<String> lore = new ArrayList<>();

        lore.add("<gray>当前: <yellow>" + upgrade.getCurrentAxeName());

        if (nextUpgrade != null) {
            lore.add("<gray>下一级: <green>" + upgradeManager.getDescription(nextUpgrade));
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(nextUpgrade);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            if (nextUpgrade == UpgradeManager.UpgradeType.IRON_AXE_EFFICIENCY) {
                lore.add("<gold>功能: <gray>效率III");
                lore.add("");
            } else if (nextUpgrade == UpgradeManager.UpgradeType.NETHERITE_AXE) {
                lore.add("<gold>功能: <gray>效率VII, 锋利I");
                lore.add("");
            }

            lore.add("<yellow>点击升级");

            ItemStack item = new ItemStack(upgradeManager.getIcon(nextUpgrade));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>斧升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<green>✔ 已满级");
            lore.add("");
            lore.add("<gold>功能: <gray>效率VII, 锋利I");

            ItemStack item = new ItemStack(Material.NETHERITE_AXE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>斧升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建锹升级项
    private ItemStack createShovelItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getShovelLevel();
        UpgradeManager.UpgradeType nextUpgrade = getNextShovelUpgrade(upgrade);
        List<String> lore = new ArrayList<>();

        lore.add("<gray>当前: <yellow>" + upgrade.getCurrentShovelName());

        if (nextUpgrade != null) {
            lore.add("<gray>下一级: <green>" + upgradeManager.getDescription(nextUpgrade));
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(nextUpgrade);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            if (nextUpgrade == UpgradeManager.UpgradeType.IRON_SHOVEL_SILK_TOUCH) {
                lore.add("<gold>功能: <gray>精准采集");
                lore.add("");
            }

            lore.add("<yellow>点击升级");

            ItemStack item = new ItemStack(upgradeManager.getIcon(nextUpgrade));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>锹升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<green>✔ 已满级");
            ItemStack item = new ItemStack(Material.NETHERITE_SHOVEL);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>锹升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建弹射器升级项
    private ItemStack createCatapultItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getCatapultLevel();
        UpgradeManager.UpgradeType nextUpgrade = getNextCatapultUpgrade(upgrade);
        List<String> lore = new ArrayList<>();

        lore.add("<gray>当前: <yellow>" + upgrade.getCurrentCatapultName());

        if (nextUpgrade != null) {
            lore.add("<gray>下一级: <green>" + upgradeManager.getDescription(nextUpgrade));
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(nextUpgrade);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            // 功能描述
            switch (nextUpgrade) {
                case ADVANCED_CATAPULT:
                    lore.add("<gold>功能: <gray>进阶弹射器");
                    lore.add("<gray>使用次数: 3");
                    lore.add("<gray>恢复用时: 3.5s");
                    break;
                case HIGH_CATAPULT:
                    lore.add("<gold>功能: <gray>高阶弹射器");
                    lore.add("<gray>使用次数: 4");
                    lore.add("<gray>恢复用时: 3s");
                    break;
                case WIND_CORE_CATAPULT:
                    lore.add("<gold>功能: <gray>风核弹射器");
                    lore.add("<gray>使用次数: 5");
                    lore.add("<gray>恢复用时: 2.5s");
                    break;
            }
            lore.add("");
            lore.add("<yellow>点击升级");

            ItemStack item = new ItemStack(Material.PISTON); // 弹射器用活塞
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>弹射器"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<green>✔ 已满级");
            lore.add("");
            lore.add("<gold>功能: <gray>风核弹射器");
            lore.add("<gray>使用次数: 5");
            lore.add("<gray>恢复用时: 2.5s");

            ItemStack item = new ItemStack(Material.PISTON);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>弹射器"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建火炮升级项
    private ItemStack createCannonItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getCannonLevel();
        UpgradeManager.UpgradeType nextUpgrade = getNextCannonUpgrade(upgrade);
        List<String> lore = new ArrayList<>();

        lore.add("<gray>当前: <yellow>" + upgrade.getCurrentCannonName());

        if (nextUpgrade != null) {
            lore.add("<gray>下一级: <green>" + upgradeManager.getDescription(nextUpgrade));
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(nextUpgrade);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            // 功能描述
            switch (nextUpgrade) {
                case PRIMARY_CANNON:
                    lore.add("<gold>功能: <gray>初级核心火炮");
                    lore.add("<gray>发射一枚火球");
                    lore.add("<gray>冷却: 6s 蓄力: 1.5s");
                    break;
                case ADVANCED_CANNON:
                    lore.add("<gold>功能: <gray>进阶核心火炮");
                    lore.add("<gray>威力翻倍火球");
                    lore.add("<gray>冷却: 12s 蓄力: 1.5s");
                    break;
                case HIGH_CANNON:
                    lore.add("<gold>功能: <gray>高阶核心火炮");
                    lore.add("<gray>连射3枚威力翻倍火球");
                    lore.add("<gray>冷却: 16s 蓄力: 2.2s");
                    break;
            }
            lore.add("");
            lore.add("<yellow>点击" + (currentLevel == -1 ? "购买" : "升级"));

            ItemStack item = new ItemStack(Material.DISPENSER); // 火炮用发射器
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>核心火炮"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<green>✔ 已满级");
            lore.add("");
            lore.add("<gold>功能: <gray>高阶核心火炮");
            lore.add("<gray>连射3枚威力翻倍火球");
            lore.add("<gray>冷却: 16s 蓄力: 2.2s");

            ItemStack item = new ItemStack(Material.DISPENSER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>核心火炮"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建盾牌升级项
    private ItemStack createShieldItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getShieldLevel();
        UpgradeManager.UpgradeType nextUpgrade = getNextShieldUpgrade(upgrade);
        List<String> lore = new ArrayList<>();

        String currentName = currentLevel == 0 ? "无" : (currentLevel == 1 ? "硬质盾" : "合金盾");
        lore.add("<gray>当前: <yellow>" + currentName);

        if (nextUpgrade != null) {
            lore.add("<gray>下一级: <green>" + upgradeManager.getDescription(nextUpgrade));
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(nextUpgrade);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            if (nextUpgrade == UpgradeManager.UpgradeType.HARD_SHIELD) {
                lore.add("<gold>功能: <gray>硬质盾");
                lore.add("<gray>+2护甲值, +2盔甲韧性");
            } else if (nextUpgrade == UpgradeManager.UpgradeType.ALLOY_SHIELD) {
                lore.add("<gold>功能: <gray>合金盾");
                lore.add("<gray>+3护甲值, +4盔甲韧性");
            }
            lore.add("");
            lore.add("<yellow>点击" + (currentLevel == 0 ? "购买" : "升级"));

            ItemStack item = new ItemStack(Material.SHIELD);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>盾牌"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<green>✔ 已满级");
            lore.add("");
            lore.add("<gold>功能: <gray>合金盾");
            lore.add("<gray>+3护甲值, +4盔甲韧性");

            ItemStack item = new ItemStack(Material.SHIELD);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>盾牌"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    private ItemStack createBowItem(Player player, PlayerUpgrade upgrade) {
        String name = "弓";
        boolean hasItem = upgrade.hasBow();


        List<String> lore = new ArrayList<>();

        if (hasItem) {
            lore.add("<gray>状态: <green>已拥有");
            lore.add("");

            ItemStack item = new ItemStack(Material.BOW);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>" + name));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<gray>状态: <red>未拥有");
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(UpgradeManager.UpgradeType.BOW);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            lore.add("");
            lore.add("<yellow>点击购买");

            ItemStack item = new ItemStack(Material.BOW);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>" + name));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }
    // 创建弩项
    private ItemStack createCrossbowItem(Player player, PlayerUpgrade upgrade, UpgradeManager.UpgradeType type) {
        boolean hasItem = false;
        String name = "";

        switch (type) {
            case CROSSBOW:
                hasItem = upgrade.hasCrossbow();
                name = "弩";
                break;
            case HEAVY_CROSSBOW:
                hasItem = upgrade.hasHeavyCrossbow();
                name = "重型巨弩";
                break;
            case RAPID_CROSSBOW:
                hasItem = upgrade.hasRapidCrossbow();
                name = "连射弩";
                break;
            case BOW:
                hasItem = upgrade.hasBow();
                name = "弓";
                break;
        }

        List<String> lore = new ArrayList<>();

        if (hasItem) {
            lore.add("<gray>状态: <green>已拥有");
            lore.add("");

            // 功能描述
            switch (type) {
                case HEAVY_CROSSBOW:
                    lore.add("<gold>功能: <gray>重型巨弩");
                    lore.add("<gray>13点伤害");
                    lore.add("<gray>箭不会下坠");
                    break;
                case RAPID_CROSSBOW:
                    lore.add("<gold>功能: <gray>连射弩");
                    lore.add("<gray>自动装填");
                    lore.add("<gray>无论是否在手上");
                    break;
            }

            ItemStack item = new ItemStack(Material.CROSSBOW);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>" + name));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<gray>状态: <red>未拥有");
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(type);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            // 功能描述
            switch (type) {
                case CROSSBOW:
                    lore.add("<gold>功能: <gray>弩");
                    break;
                case HEAVY_CROSSBOW:
                    lore.add("<gold>功能: <gray>重型巨弩");
                    lore.add("<gray>13点伤害");
                    lore.add("<gray>箭不会下坠");
                    break;
                case RAPID_CROSSBOW:
                    lore.add("<gold>功能: <gray>连射弩");
                    lore.add("<gray>自动装填");
                    lore.add("<gray>无论是否在手上");
                    break;
            }
            lore.add("");
            lore.add("<yellow>点击购买");

            ItemStack item = new ItemStack(Material.CROSSBOW);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>" + name));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建护甲升级项
    private ItemStack createArmorItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getArmorLevel();
        UpgradeManager.UpgradeType nextUpgrade = getNextArmorUpgrade(upgrade);
        List<String> lore = new ArrayList<>();

        lore.add("<gray>当前: <yellow>" + upgrade.getCurrentArmorName());

        if (nextUpgrade != null) {
            lore.add("<gray>下一级: <green>" + upgradeManager.getDescription(nextUpgrade));
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(nextUpgrade);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            lore.add("<yellow>点击升级");

            ItemStack item = new ItemStack(upgradeManager.getIcon(nextUpgrade));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>护甲升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<green>✔ 已满级");
            ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>护甲升级"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建附魔项
    private ItemStack createEnchantItem(Player player, PlayerUpgrade upgrade, UpgradeManager.UpgradeType type) {
        boolean hasEnchant = false;
        String name = "";

        switch (type) {
            case CHEST_ENCHANT_SPECIAL:
                hasEnchant = upgrade.hasChestEnchantSpecial();
                name = "胸甲强化附魔";
                break;
            case CHEST_ENCHANT_SPECIALIZED:
                hasEnchant = upgrade.hasChestEnchantSpecialized();
                name = "胸甲附魔特化";
                break;
        }

        List<String> lore = new ArrayList<>();

        if (hasEnchant) {
            lore.add("<gray>状态: <green>已拥有");
            lore.add("");

            if (type == UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIAL) {
                lore.add("<gold>功能: <gray>弹射物保护III");
                lore.add("<gray>爆炸保护III");
                lore.add("<red>与保护IV附魔不兼容");
            } else {
                lore.add("<gold>功能: <gray>保护IV");
                lore.add("<red>与其他保护附魔不兼容");
            }

            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>" + name));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<gray>状态: <red>未拥有");
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(type);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            if (type == UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIAL) {
                lore.add("<gold>功能: <gray>弹射物保护III");
                lore.add("<gray>爆炸保护III");
                lore.add("<red>与保护IV附魔不兼容");
            } else {
                lore.add("<gold>功能: <gray>保护IV");
                lore.add("<red>与其他保护附魔不兼容");
            }
            lore.add("");
            lore.add("<yellow>点击购买");

            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>" + name));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建传送钩项
    private ItemStack createTeleportHookItem(Player player, PlayerUpgrade upgrade) {
        List<String> lore = new ArrayList<>();

        if (upgrade.hasTeleportHook()) {
            lore.add("<gray>状态: <green>已拥有");
            lore.add("");
            lore.add("<gold>功能: <gray>传送钩");
            lore.add("<gray>放竿记录位置");
            lore.add("<gray>再次使用传送到鱼钩位置");

            ItemStack item = new ItemStack(Material.FISHING_ROD);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>传送钩"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<gray>状态: <red>未拥有");
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(UpgradeManager.UpgradeType.TELEPORT_HOOK);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            lore.add("<gold>功能: <gray>传送钩");
            lore.add("<gray>放竿记录位置");
            lore.add("<gray>再次使用传送到鱼钩位置");
            lore.add("");
            lore.add("<yellow>点击购买");

            ItemStack item = new ItemStack(Material.FISHING_ROD);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<gold>传送钩"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建乐魂护甲升级项
    private ItemStack createSoulArmorItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getSoulArmorLevel();
        UpgradeManager.UpgradeType nextUpgrade = getNextSoulArmorUpgrade(upgrade);
        List<String> lore = new ArrayList<>();

        lore.add("<gray>当前等级: <yellow>" + currentLevel);

        if (nextUpgrade != null) {
            lore.add("<gray>下一等级: <green>" + (currentLevel + 1));
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(nextUpgrade);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            lore.add("<yellow>点击升级");

            ItemStack item = new ItemStack(Material.WHITE_HARNESS);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<yellow>乐魂护甲提升"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<green>✔ 已满级");
            ItemStack item = new ItemStack(Material.WHITE_HARNESS);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<yellow>乐魂护甲提升"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 创建乐魂血量升级项
    private ItemStack createSoulHealthItem(Player player, PlayerUpgrade upgrade) {
        int currentLevel = upgrade.getSoulHealthLevel();
        List<String> lore = new ArrayList<>();

        lore.add("<gray>当前等级: <yellow>" + currentLevel);

        if (currentLevel == 0) {
            lore.add("<gray>下一等级: <green>1");
            lore.add("");

            Map<Material, Integer> price = upgradeManager.getPrice(UpgradeManager.UpgradeType.SOUL_HEALTH_UPGRADE_1);
            if (price != null && !price.isEmpty()) {
                lore.add("<gold>价格:");
                for (Map.Entry<Material, Integer> entry : price.entrySet()) {
                    String materialName = getMaterialName(entry.getKey());
                    lore.add("<gray>- " + materialName + " x" + entry.getValue());
                }
                lore.add("");
            }

            lore.add("<yellow>点击升级");

            ItemStack item = new ItemStack(Material.SNOW_BLOCK); // 雪块
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<yellow>乐魂血量提升"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        } else {
            lore.add("<green>✔ 已满级");
            ItemStack item = new ItemStack(Material.SNOW_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Text.mm("<yellow>乐魂血量提升"));
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
            item.setItemMeta(meta);
            return item;
        }
    }

    // 获取下一级升级类型的方法
    private UpgradeManager.UpgradeType getNextSwordUpgrade(PlayerUpgrade upgrade) {
        switch (upgrade.getSwordLevel()) {
            case 0: return UpgradeManager.UpgradeType.IRON_SWORD;
            case 1: return UpgradeManager.UpgradeType.IRON_SWORD_STORM;
            case 2: return UpgradeManager.UpgradeType.NETHERITE_SWORD_STORM;
            default: return null;
        }
    }

    private UpgradeManager.UpgradeType getNextPickaxeUpgrade(PlayerUpgrade upgrade) {
        switch (upgrade.getPickaxeLevel()) {
            case 0: return UpgradeManager.UpgradeType.IRON_PICKAXE;
            case 1: return UpgradeManager.UpgradeType.IRON_PICKAXE_FORTUNE;
            case 2: return UpgradeManager.UpgradeType.NETHERITE_PICKAXE;
            default: return null;
        }
    }

    private UpgradeManager.UpgradeType getNextAxeUpgrade(PlayerUpgrade upgrade) {
        switch (upgrade.getAxeLevel()) {
            case 0: return UpgradeManager.UpgradeType.IRON_AXE;
            case 1: return UpgradeManager.UpgradeType.IRON_AXE_EFFICIENCY;
            case 2: return UpgradeManager.UpgradeType.NETHERITE_AXE;
            default: return null;
        }
    }

    private UpgradeManager.UpgradeType getNextShovelUpgrade(PlayerUpgrade upgrade) {
        switch (upgrade.getShovelLevel()) {
            case 0: return UpgradeManager.UpgradeType.STONE_SHOVEL;
            case 1: return UpgradeManager.UpgradeType.IRON_SHOVEL;
            case 2: return UpgradeManager.UpgradeType.IRON_SHOVEL_SILK_TOUCH;
            case 3: return UpgradeManager.UpgradeType.NETHERITE_SHOVEL;
            default: return null;
        }
    }

    private UpgradeManager.UpgradeType getNextCatapultUpgrade(PlayerUpgrade upgrade) {
        switch (upgrade.getCatapultLevel()) {
            case 0: return UpgradeManager.UpgradeType.ADVANCED_CATAPULT;
            case 1: return UpgradeManager.UpgradeType.HIGH_CATAPULT;
            case 2: return UpgradeManager.UpgradeType.WIND_CORE_CATAPULT;
            default: return null;
        }
    }

    private UpgradeManager.UpgradeType getNextCannonUpgrade(PlayerUpgrade upgrade) {
        if (upgrade.getCannonLevel() == -1) {
            return UpgradeManager.UpgradeType.PRIMARY_CANNON;
        }
        switch (upgrade.getCannonLevel()) {
            case 0: return UpgradeManager.UpgradeType.ADVANCED_CANNON;
            case 1: return UpgradeManager.UpgradeType.HIGH_CANNON;
            default: return null;
        }
    }

    private UpgradeManager.UpgradeType getNextShieldUpgrade(PlayerUpgrade upgrade) {
        switch (upgrade.getShieldLevel()) {
            case 0: return UpgradeManager.UpgradeType.HARD_SHIELD;
            case 1: return UpgradeManager.UpgradeType.ALLOY_SHIELD;
            default: return null;
        }
    }

    private UpgradeManager.UpgradeType getNextArmorUpgrade(PlayerUpgrade upgrade) {
        switch (upgrade.getArmorLevel()) {
            case 0: return UpgradeManager.UpgradeType.ARMOR_UPGRADE_I;
            case 1: return UpgradeManager.UpgradeType.ARMOR_UPGRADE_II;
            case 2: return UpgradeManager.UpgradeType.ARMOR_UPGRADE_III;
            default: return null;
        }
    }

    private UpgradeManager.UpgradeType getNextSoulArmorUpgrade(PlayerUpgrade upgrade) {
        switch (upgrade.getSoulArmorLevel()) {
            case 0: return UpgradeManager.UpgradeType.SOUL_ARMOR_UPGRADE_1;
            case 1: return UpgradeManager.UpgradeType.SOUL_ARMOR_UPGRADE_2;
            case 2: return UpgradeManager.UpgradeType.SOUL_ARMOR_UPGRADE_3;
            default: return null;
        }
    }

    private String getMaterialName(Material material) {
        switch (material) {
            case IRON_INGOT: return language.getContent("upgrade.iron-ingot");
            case OAK_LOG: return language.getContent("upgrade.oak-log");
            case NETHERITE_INGOT: return language.getContent("upgrade.netherite-ingot");
            case COAL: return language.getContent("upgrade.coal");
            case COPPER_INGOT: return language.getContent("upgrade.copper-ingot");
            case SNOW_BLOCK: return language.getContent("upgrade.snow");
            default: return material.toString();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(Text.legacy(language.getContent("upgrade.gui-title")))) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() == InventoryType.PLAYER) return;

        if (!HappyGhastWar.arenas.containsKey(event.getWhoClicked().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(event.getWhoClicked().getWorld().getName());

        if (arena.getName() != this.arena.getName()){
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot < 0 || slot >= 54) return;

        // 防重复点击：如果玩家已经在处理中，直接返回
        if (cooldownPlayers.contains(player.getUniqueId())) {
            return;
        }
        cooldownPlayers.add(player.getUniqueId());

        Team team = playerMap.get(player);


        if (slot == 53){
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.SNOW_BLOCK){
                event.setCancelled(false);
            }
        }
        PlayerUpgrade upgrade = upgradeManager.getPlayerUpgrade(player);

        // 根据槽位处理不同的升级
        switch (slot) {
            case 10: handleSwordUpgrade(player, upgrade, team); break;
            case 11: handleAxeUpgrade(player, upgrade, team); break;
            case 12: handlePickaxeUpgrade(player, upgrade, team); break;
            case 13: handleShovelUpgrade(player, upgrade, team); break;
            case 19: handleBowPurchase(player, upgrade, team); break;
            case 20: handleCrossbowPurchase(player, upgrade, UpgradeManager.UpgradeType.CROSSBOW, team); break;
            case 21: handleCrossbowPurchase(player, upgrade, UpgradeManager.UpgradeType.RAPID_CROSSBOW, team); break;
            case 22: handleCrossbowPurchase(player, upgrade, UpgradeManager.UpgradeType.HEAVY_CROSSBOW, team); break;
            case 28: handleShieldUpgrade(player, upgrade, team); break;
            case 29: handleArmorUpgrade(player, upgrade, team); break;
            case 30: handleEnchantPurchase(player, upgrade, UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIAL, team); break;
            case 31: handleEnchantPurchase(player, upgrade, UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIALIZED, team); break;
            case 37: handleTeleportHookPurchase(player, upgrade, team); break;
            case 38: handleCatapultUpgrade(player, upgrade, team); break;
            case 39: handleCannonUpgrade(player, upgrade, team); break;

            case 7: handleArrowMaker(player); break;
            case 45: handleTeamChest(player); break;

            case 34: handleSoulArmorUpgrade(player, upgrade, team); break;
            case 33: handleSoulHealthUpgrade(player, upgrade, team); break;

        }

        SoundUtil.play(player, "upgrade-click", Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        // 延迟一小段时间后移除冷却（500毫秒）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cooldownPlayers.remove(player.getUniqueId());
        }, 5L); // 0.5秒后移除
    }

    private void handleTeamChest(Player player){
        if (!HappyGhastWar.arenas.containsKey(player.getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
        arena.getTeams().forEach(team -> {
            if (team.getPlayers().contains(player)){
                arena.teamChest.openTeamChest(player,team);
            }
        });
    }

    private void handleArrowMaker(Player player){
        if (!player.getInventory().contains(Material.OAK_LOG)){
            Text.send(player, "<red>资源不足！");
            return;
        }
        int slot = player.getInventory().first(Material.OAK_LOG);
        ItemStack itemStack = player.getInventory().getItem(slot);
        if (itemStack.getAmount() <= 1){
            player.getInventory().setItem(slot,new ItemStack(Material.AIR));
        }else {
            itemStack.setAmount(itemStack.getAmount()-1);
        }
        player.getInventory().addItem(new ItemStack(Material.ARROW,5));
    }

    // 处理各种升级的方法（每次只升一级）
    private void handleSwordUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        // 检查是否已经满级
        if (upgrade.getSwordLevel() >= 3) {
            Text.send(player, "<red>剑已经达到最高等级了！");
            return;
        }

        UpgradeManager.UpgradeType nextUpgrade = getNextSwordUpgrade(upgrade);
        if (nextUpgrade == null) {
            Text.send(player, "<red>无法升级！");
            return;
        }

        // 检查材料是否足够
        if (!upgradeManager.purchaseUpgrade(player, nextUpgrade)) {
            return;
        }

        // 执行升级
        if (upgrade.upgradeSword()) {
            Text.send(player, "<green>剑升级成功！");
            // 给予新的剑，替换旧的
            giveSwordToPlayer(player, upgrade.getSwordLevel());
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        // 重新打开GUI，更新显示
        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player, team);
        });
    }

    private void removeOldItems(Player player, Material... materials) {
        for (Material material : materials) {
            if (material == null) continue;

            // 检查并移除背包中的物品
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    player.getInventory().remove(item);
                    break; // 只移除一个
                }
            }
        }
    }

    private void handlePickaxeUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        if (upgrade.getPickaxeLevel() >= 3) {
            Text.send(player, "<red>镐已经达到最高等级了！");
            return;
        }

        UpgradeManager.UpgradeType nextUpgrade = getNextPickaxeUpgrade(upgrade);
        if (nextUpgrade == null) {
            Text.send(player, "<red>无法升级！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, nextUpgrade)) {
            return;
        }

        if (upgrade.upgradePickaxe()) {
            Text.send(player, "<green>镐升级成功！");
            givePickaxeToPlayer(player, upgrade.getPickaxeLevel());
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player, team);
        });
    }

    private void handleAxeUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        if (upgrade.getAxeLevel() >= 3) {
            Text.send(player, "<red>斧已经达到最高等级了！");
            return;
        }

        UpgradeManager.UpgradeType nextUpgrade = getNextAxeUpgrade(upgrade);
        if (nextUpgrade == null) {
            Text.send(player, "<red>无法升级！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, nextUpgrade)) {
            return;
        }

        if (upgrade.upgradeAxe()) {
            Text.send(player, "<green>斧升级成功！");
            giveAxeToPlayer(player, upgrade.getAxeLevel());
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player, team);
        });
    }

    private void handleShovelUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        if (upgrade.getShovelLevel() >= 4) {
            Text.send(player, "<red>锹已经达到最高等级了！");
            return;
        }

        UpgradeManager.UpgradeType nextUpgrade = getNextShovelUpgrade(upgrade);
        if (nextUpgrade == null) {
            Text.send(player, "<red>无法升级！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, nextUpgrade)) {
            return;
        }

        if (upgrade.upgradeShovel()) {
            Text.send(player, "<green>锹升级成功！");
            giveShovelToPlayer(player, upgrade.getShovelLevel());
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player, team);
        });
    }

    private void handleCatapultUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        if (upgrade.getCatapultLevel() >= 3) {
            Text.send(player, "<red>弹射器已经达到最高等级了！");
            return;
        }

        UpgradeManager.UpgradeType nextUpgrade = getNextCatapultUpgrade(upgrade);
        if (nextUpgrade == null) {
            Text.send(player, "<red>无法升级！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, nextUpgrade)) {
            return;
        }

        if (upgrade.upgradeCatapult()) {
            Text.send(player, "<green>弹射器升级成功！");
            giveCatapultToPlayer(player, upgrade.getCatapultLevel());
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player,team);
        });
    }

    private void handleCannonUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        if (upgrade.getCannonLevel() >= 2) {
            Text.send(player, "<red>火炮已经达到最高等级了！");
            return;
        }

        UpgradeManager.UpgradeType nextUpgrade = getNextCannonUpgrade(upgrade);
        if (nextUpgrade == null) {
            Text.send(player, "<red>无法升级！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, nextUpgrade)) {
            return;
        }

        if (upgrade.upgradeCannon()) {
            Text.send(player, "<green>" + (upgrade.getCannonLevel() == 0 ? "购买" : "升级") + "火炮成功！");
            giveCannonToPlayer(player, upgrade.getCannonLevel());
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player, team);
        });
    }

    private void handleShieldUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        if (upgrade.getShieldLevel() >= 2) {
            Text.send(player, "<red>盾牌已经达到最高等级了！");
            return;
        }

        UpgradeManager.UpgradeType nextUpgrade = getNextShieldUpgrade(upgrade);
        if (nextUpgrade == null) {
            Text.send(player, "<red>无法升级！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, nextUpgrade)) {
            return;
        }

        if (upgrade.upgradeShield()) {
            Text.send(player, "<green>" + (upgrade.getShieldLevel() == 1 ? "购买" : "升级") + "盾牌成功！");
            giveShieldToPlayer(player, upgrade.getShieldLevel());
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player,team);
        });
    }

    private void handleCrossbowPurchase(Player player, PlayerUpgrade upgrade, UpgradeManager.UpgradeType type, Team team) {
        boolean hasItem = false;
        switch (type) {
            case CROSSBOW: hasItem = upgrade.hasCrossbow(); break;
            case HEAVY_CROSSBOW: hasItem = upgrade.hasHeavyCrossbow(); break;
            case RAPID_CROSSBOW: hasItem = upgrade.hasRapidCrossbow(); break;
        }

        if (hasItem) {
            Text.send(player, "<red>您已经拥有这个物品了！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, type)) {
            return;
        }

        switch (type) {
            case CROSSBOW:
                if (upgrade.buyCrossbow()) {
                    Text.send(player, "<green>购买弩成功！");
                    giveCrossbowToPlayer(player, type);
                    upgradeManager.savePlayerUpgrade(player, upgrade);
                }
                break;
            case HEAVY_CROSSBOW:
                if (upgrade.buyHeavyCrossbow()) {
                    Text.send(player, "<green>购买重型巨弩成功！");
                    giveCrossbowToPlayer(player, type);
                    upgradeManager.savePlayerUpgrade(player, upgrade);
                }
                break;
            case RAPID_CROSSBOW:
                if (upgrade.buyRapidCrossbow()) {
                    Text.send(player, "<green>购买连射弩成功！");
                    giveCrossbowToPlayer(player, type);
                    upgradeManager.savePlayerUpgrade(player, upgrade);
                }
                break;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player,team);
        });
    }

    private void handleArmorUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        if (upgrade.getArmorLevel() >= 3) {
            Text.send(player, "<red>护甲已经达到最高等级了！");
            return;
        }

        UpgradeManager.UpgradeType nextUpgrade = getNextArmorUpgrade(upgrade);
        if (nextUpgrade == null) {
            Text.send(player, "<red>无法升级！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, nextUpgrade)) {
            return;
        }

        if (upgrade.upgradeArmor()) {
            Text.send(player, "<green>护甲升级成功！");
            applyArmorUpgrade(player, upgrade.getArmorLevel());
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player,team);
        });
    }

    private void handleEnchantPurchase(Player player, PlayerUpgrade upgrade, UpgradeManager.UpgradeType type, Team team) {
        boolean hasEnchant = false;
        switch (type) {
            case CHEST_ENCHANT_SPECIAL: hasEnchant = upgrade.hasChestEnchantSpecial(); break;
            case CHEST_ENCHANT_SPECIALIZED: hasEnchant = upgrade.hasChestEnchantSpecialized(); break;
        }

        if (hasEnchant) {
            Text.send(player, "<red>您已经拥有这个附魔了！");
            return;
        }

        // 检查不兼容性
        if (type == UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIAL && upgrade.hasChestEnchantSpecialized()) {
            Text.send(player, "<red>胸甲强化附魔与保护IV不兼容！");
            return;
        }
        if (type == UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIALIZED && upgrade.hasChestEnchantSpecial()) {
            Text.send(player, "<red>保护IV与其他保护附魔不兼容！");
            return;
        }

        // 先确认玩家拥有胸甲，避免扣了材料却无处附魔
        if (!hasChestplate(player)) {
            Text.send(player, "<red>您没有胸甲可以附魔！请先购买护甲升级");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, type)) {
            return;
        }

        switch (type) {
            case CHEST_ENCHANT_SPECIAL:
                if (upgrade.buyChestEnchantSpecial()) {
                    Text.send(player, "<green>购买胸甲强化附魔成功！");
                    applyChestEnchant(player, type);
                    upgradeManager.savePlayerUpgrade(player, upgrade);
                }
                break;
            case CHEST_ENCHANT_SPECIALIZED:
                if (upgrade.buyChestEnchantSpecialized()) {
                    Text.send(player, "<green>购买胸甲附魔特化成功！");
                    applyChestEnchant(player, type);
                    upgradeManager.savePlayerUpgrade(player, upgrade);
                }
                break;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player, team);
        });
    }

    private void handleTeleportHookPurchase(Player player, PlayerUpgrade upgrade, Team team) {
        if (upgrade.hasTeleportHook()) {
            Text.send(player, "<red>您已经拥有传送钩了！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, UpgradeManager.UpgradeType.TELEPORT_HOOK)) {
            return;
        }

        if (upgrade.buyTeleportHook()) {
            Text.send(player, "<green>购买传送钩成功！");
            giveTeleportHookToPlayer(player);
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player,team);
        });
    }

    private void handleBowPurchase(Player player, PlayerUpgrade upgrade, Team team) {
        if (upgrade.hasBow()) {
            Text.send(player, "<red>您已经拥有弓了！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, UpgradeManager.UpgradeType.BOW)) {
            return;
        }

        if (upgrade.buyBow()) {
            Text.send(player, "<green>购买弓成功！");
            giveBowToPlayer(player);
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player, team);
        });
    }

    private void handleSoulArmorUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        if (team == null) return;
        if (upgrade.getSoulArmorLevel() >= 3) {
            Text.send(player, "<red>乐魂护甲已经达到最高等级了！");
            return;
        }

        UpgradeManager.UpgradeType nextUpgrade = getNextSoulArmorUpgrade(upgrade);
        if (nextUpgrade == null) {
            Text.send(player, "<red>无法升级！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, nextUpgrade)) {
            return;
        }
        arena.getStatistics().recordUpgradePerformed(player);

        if (upgrade.upgradeSoulArmor()) {
            Text.send(player, "<green>乐魂护甲升级成功！");
            // 实装乐魂护甲：提升本队所有乐魂的真实护甲等级（Protection 中按 5%/级 减伤）
            int newLevel = upgrade.getSoulArmorLevel();
            for (GameGhast ghast : team.getGhasts()){
                ghast.setArmorLevel(newLevel);
            }
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player, team);
        });
    }

    private void handleSoulHealthUpgrade(Player player, PlayerUpgrade upgrade, Team team) {
        if (team == null) return;
        if (upgrade.getSoulHealthLevel() > 0) {
            Text.send(player, "<red>乐魂血量已经达到最高等级了！");
            return;
        }

        if (!upgradeManager.purchaseUpgrade(player, UpgradeManager.UpgradeType.SOUL_HEALTH_UPGRADE_1)) {
            return;
        }
        arena.getStatistics().recordUpgradePerformed(player);

        if (upgrade.upgradeSoulHealth()) {
            Text.send(player, "<green>乐魂血量升级成功！");
            for (GameGhast ghast : team.getGhasts()){
                ghast.addMaxHealth(20);
                ghast.addHealth(20);
            }
            upgradeManager.savePlayerUpgrade(player, upgrade);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            openUpgradeGUI(player, team);
        });
    }

    // 给予物品的方法（替换低级物品）
    private void giveSwordToPlayer(Player player, int level) {
        // 先移除旧的剑
        removeOldSwords(player, level);

        removeOldItems(player,
                Material.STONE_SWORD,
                Material.IRON_SWORD,
                Material.NETHERITE_SWORD
        );

        ItemStack sword;
        ItemMeta meta;

        switch (level) {
            case 1: // 铁剑
                sword = new ItemStack(Material.IRON_SWORD);
                meta = sword.getItemMeta();
                meta.displayName(Text.mm("<gray>铁剑"));
                sword.setItemMeta(meta);
                break;
            case 2: // 铁剑(风暴I)
                sword = new ItemStack(Material.IRON_SWORD);
                meta = sword.getItemMeta();
                meta.displayName(Text.mm("<gray>铁剑(风暴I)"));
                // 这里添加风暴I附魔逻辑
                meta.addEnchant(Enchantment.WIND_BURST, 1, true);
                sword.setItemMeta(meta);
                break;
            case 3: // 下界合金剑(风暴I)
                sword = new ItemStack(Material.NETHERITE_SWORD);
                meta = sword.getItemMeta();
                meta.displayName(Text.mm("<dark_purple>下界合金剑(风暴I)"));
                // 这里添加风暴I附魔逻辑
                meta.addEnchant(Enchantment.WIND_BURST, 1, true);
                sword.setItemMeta(meta);
                break;
            default:
                return;
        }

        player.getInventory().addItem(sword);
    }

    private void removeOldSwords(Player player, int newLevel) {
        // 移除所有石剑、铁剑、下界合金剑
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                if (item.getType() == Material.STONE_SWORD ||
                        item.getType() == Material.IRON_SWORD ||
                        item.getType() == Material.NETHERITE_SWORD) {
                    player.getInventory().remove(item);
                }
            }
        }
    }

    private void givePickaxeToPlayer(Player player, int level) {
        // 移除旧的镐
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.STONE_PICKAXE ||
                    item.getType() == Material.IRON_PICKAXE ||
                    item.getType() == Material.NETHERITE_PICKAXE)) {
                player.getInventory().remove(item);
            }
        }

        ItemStack pickaxe;
        ItemMeta meta;

        switch (level) {
            case 1: // 铁镐
                pickaxe = new ItemStack(Material.IRON_PICKAXE);
                meta = pickaxe.getItemMeta();
                meta.displayName(Text.mm("<gray>铁镐"));
                pickaxe.setItemMeta(meta);
                break;
            case 2: // 铁镐(时运III)
                pickaxe = new ItemStack(Material.IRON_PICKAXE);
                meta = pickaxe.getItemMeta();
                meta.displayName(Text.mm("<gray>铁镐(时运III)"));
                meta.addEnchant(Enchantment.FORTUNE, 3, true);
                pickaxe.setItemMeta(meta);
                break;
            case 3: // 下界合金镐
                pickaxe = new ItemStack(Material.NETHERITE_PICKAXE);
                meta = pickaxe.getItemMeta();
                meta.displayName(Text.mm("<dark_purple>下界合金镐"));
                pickaxe.setItemMeta(meta);
                break;
            default:
                return;
        }

        player.getInventory().addItem(pickaxe);
    }

    private void giveAxeToPlayer(Player player, int level) {
        // 移除旧的斧
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.STONE_AXE ||
                    item.getType() == Material.IRON_AXE ||
                    item.getType() == Material.NETHERITE_AXE)) {
                player.getInventory().remove(item);
            }
        }

        ItemStack axe;
        ItemMeta meta;

        switch (level) {
            case 1: // 铁斧
                axe = new ItemStack(Material.IRON_AXE);
                meta = axe.getItemMeta();
                meta.displayName(Text.mm("<gray>铁斧"));
                axe.setItemMeta(meta);
                break;
            case 2: // 铁斧(效率III)
                axe = new ItemStack(Material.IRON_AXE);
                meta = axe.getItemMeta();
                meta.displayName(Text.mm("<gray>铁斧(效率III)"));
                meta.addEnchant(Enchantment.EFFICIENCY, 3, true);
                axe.setItemMeta(meta);
                break;
            case 3: // 下界合金斧(效率VII,锋利I)
                axe = new ItemStack(Material.NETHERITE_AXE);
                meta = axe.getItemMeta();
                meta.displayName(Text.mm("<dark_purple>下界合金斧(效率VII,锋利I)"));
                meta.addEnchant(Enchantment.EFFICIENCY, 7, true);
                meta.addEnchant(Enchantment.SHARPNESS, 1, true);
                axe.setItemMeta(meta);
                break;
            default:
                return;
        }

        player.getInventory().addItem(axe);
    }

    private void giveShovelToPlayer(Player player, int level) {
        // 移除旧的锹
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.WOODEN_SHOVEL ||
                    item.getType() == Material.STONE_SHOVEL ||
                    item.getType() == Material.IRON_SHOVEL ||
                    item.getType() == Material.NETHERITE_SHOVEL)) {
                player.getInventory().remove(item);
            }
        }

        ItemStack shovel;
        ItemMeta meta;

        switch (level) {
            case 1: // 石锹
                shovel = new ItemStack(Material.STONE_SHOVEL);
                meta = shovel.getItemMeta();
                meta.displayName(Text.mm("<gray>石锹"));
                shovel.setItemMeta(meta);
                break;
            case 2: // 铁锹
                shovel = new ItemStack(Material.IRON_SHOVEL);
                meta = shovel.getItemMeta();
                meta.displayName(Text.mm("<gray>铁锹"));
                shovel.setItemMeta(meta);
                break;
            case 3: // 铁锹(精准采集)
                shovel = new ItemStack(Material.IRON_SHOVEL);
                meta = shovel.getItemMeta();
                meta.displayName(Text.mm("<gray>铁锹(精准采集)"));
                meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                shovel.setItemMeta(meta);
                break;
            case 4: // 下界合金锹
                shovel = new ItemStack(Material.NETHERITE_SHOVEL);
                meta = shovel.getItemMeta();
                meta.displayName(Text.mm("<dark_purple>下界合金锹"));
                shovel.setItemMeta(meta);
                break;
            default:
                return;
        }

        player.getInventory().addItem(shovel);
    }

    public void giveCatapultToPlayer(Player player, int level) {
        removeOldItems(player, Material.PISTON);

        ItemStack catapult = new ItemStack(Material.PISTON);
        ItemMeta meta = catapult.getItemMeta();

        switch (level) {
            case 0:
                meta.displayName(Text.mm("<gray>初级弹射器"));
                break;
            case 1: // 进阶弹射器
                meta.displayName(Text.mm("<gray>进阶弹射器"));
                break;
            case 2: // 高阶弹射器
                meta.displayName(Text.mm("<gray>高阶弹射器"));
                break;
            case 3: // 风核弹射器
                meta.displayName(Text.mm("<dark_purple>风核弹射器"));
                break;
            default:
                return;
        }

        List<String> lore = new ArrayList<>();
        lore.add("<gold>右键给予推力");
        switch (level) {
            case 1:
                lore.add("<gray>使用次数: 3");
                lore.add("<gray>恢复用时: 3.5s");
                break;
            case 2:
                lore.add("<gray>使用次数: 4");
                lore.add("<gray>恢复用时: 3s");
                break;
            case 3:
                lore.add("<gray>使用次数: 5");
                lore.add("<gray>恢复用时: 2.5s");
                break;
        }
        meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
        catapult.setItemMeta(meta);

        player.getInventory().addItem(catapult);
    }

    public void giveCannonToPlayer(Player player, int level) {
        removeOldItems(player, Material.DISPENSER);

        ItemStack cannon = new ItemStack(Material.DISPENSER);
        ItemMeta meta = cannon.getItemMeta();

        switch (level) {
            case 0: // 初级核心火炮
                meta.displayName(Text.mm("<gray>初级核心火炮"));
                break;
            case 1: // 进阶核心火炮
                meta.displayName(Text.mm("<gray>进阶核心火炮"));
                break;
            case 2: // 高阶核心火炮
                meta.displayName(Text.mm("<gray>高阶核心火炮"));
                break;
            default:
                return;
        }

        List<String> lore = new ArrayList<>();
        lore.add("<gold>右键蓄力发射火球");
        switch (level) {
            case 0:
                lore.add("<gray>发射一枚火球");
                lore.add("<gray>冷却: 6s 蓄力: 1.5s");
                break;
            case 1:
                lore.add("<gray>威力翻倍火球");
                lore.add("<gray>冷却: 12s 蓄力: 1.5s");
                break;
            case 2:
                lore.add("<gray>连射3枚威力翻倍火球");
                lore.add("<gray>冷却: 16s 蓄力: 2.2s");
                break;
        }
        meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
        cannon.setItemMeta(meta);

        player.getInventory().addItem(cannon);
    }

    private void giveShieldToPlayer(Player player, int level) {
        removeOldItems(player, Material.SHIELD);

        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta meta = shield.getItemMeta();

        if (level == 1) { // 硬质盾
            meta.displayName(Text.mm("<gray>硬质盾"));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>+2护甲值, +2盔甲韧性");
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
        } else if (level == 2) { // 合金盾
            meta.displayName(Text.mm("<dark_purple>合金盾"));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>+3护甲值, +4盔甲韧性");
            meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
        }

        shield.setItemMeta(meta);
        player.getInventory().addItem(shield);
    }

    private void giveCrossbowToPlayer(Player player, UpgradeManager.UpgradeType type) {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();

        switch (type) {
            case CROSSBOW:
                meta.displayName(Text.mm("<gray>弩"));
                break;
            case HEAVY_CROSSBOW:
                meta.displayName(Text.mm("<gray>重型巨弩"));
                List<String> lore = new ArrayList<>();
                lore.add("<gold>13点伤害");
                lore.add("<gray>箭不会下坠");
                meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
                break;
            case RAPID_CROSSBOW:
                meta.displayName(Text.mm("<gray>连射弩"));
                List<String> lore2 = new ArrayList<>();
                lore2.add("<gray>自动装填");
                lore2.add("<gray>无论是否在手上");
                meta.lore(lore2.stream().map(Text::mm).collect(Collectors.toList()));
                break;
            default:
                return;
        }

        crossbow.setItemMeta(meta);
        player.getInventory().addItem(crossbow);
    }

    private void applyArmorUpgrade(Player player, int level) {
        // 移除旧的护甲
        removeOldItems(player,
                Material.IRON_CHESTPLATE,
                Material.DIAMOND_CHESTPLATE,
                Material.NETHERITE_CHESTPLATE
        );

        ItemStack chestplate;
        ItemMeta meta;

        switch (level) {
            case 1: // 铁制胸甲
                chestplate = new ItemStack(Material.IRON_CHESTPLATE);
                meta = chestplate.getItemMeta();
                meta.displayName(Text.mm("<gray>护甲升级I(铁制)"));
                chestplate.setItemMeta(meta);
                break;
            case 2: // 钻石胸甲
                chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
                meta = chestplate.getItemMeta();
                meta.displayName(Text.mm("<gray>护甲升级II(钻石)"));
                chestplate.setItemMeta(meta);
                break;
            case 3: // 下界合金胸甲
                chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
                meta = chestplate.getItemMeta();
                meta.displayName(Text.mm("<gray>护甲升级III(下界合金)"));
                chestplate.setItemMeta(meta);
                break;
            default:
                return;
        }

        player.getInventory().setChestplate(chestplate);
        Text.send(player, "<green>护甲已升级到等级 " + level);
    }

    private void applyChestEnchant(Player player, UpgradeManager.UpgradeType type) {
        // 查找玩家的胸甲
        ItemStack chestplate = null;
        boolean isEquipped = false;
        // 先检查玩家穿着的胸甲
        ItemStack equippedChestplate = player.getInventory().getChestplate();
        if (equippedChestplate != null && isChestplateType(equippedChestplate.getType())) {
            chestplate = equippedChestplate;
            isEquipped = true;
        } else {
            // 检查背包中的胸甲
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && isChestplateType(item.getType())) {
                    chestplate = item;
                    break;
                }
            }
        }

        if (chestplate == null) {
            // 玩家没有胸甲，给予提示并保存附魔状态
            Text.send(player, "<red>您没有胸甲可以附魔！请先购买胸甲");
            return;
        }

        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(chestplate.getType());
        }

        if (type == UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIAL) {
            // 胸甲强化附魔：弹射物保护III,爆炸保护III
            applySpecialProtection(player, meta, chestplate);
        } else if (type == UpgradeManager.UpgradeType.CHEST_ENCHANT_SPECIALIZED) {
            // 胸甲附魔特化：保护IV
            applySpecializedProtection(player, meta, chestplate);
        }

        // 更新胸甲
        chestplate.setItemMeta(meta);

        // 如果胸甲是穿着的，需要重新设置
        if (isEquipped) {
            player.getInventory().setChestplate(chestplate);
        }
    }

    // 判断物品类型是否为胸甲
    private boolean isChestplateType(Material material) {
        return material == Material.LEATHER_CHESTPLATE ||
                material == Material.IRON_CHESTPLATE ||
                material == Material.CHAINMAIL_CHESTPLATE ||
                material == Material.GOLDEN_CHESTPLATE ||
                material == Material.DIAMOND_CHESTPLATE ||
                material == Material.NETHERITE_CHESTPLATE;
    }

    // 应用胸甲强化附魔（弹射物保护III,爆炸保护III）
    private void applySpecialProtection(Player player, ItemMeta meta, ItemStack chestplate) {
        // 移除不兼容的保护IV附魔
        if (meta.hasEnchant(Enchantment.PROTECTION)) {
            Text.send(player, "<yellow>你已经附魔保护IV了");
            return;
        }

        // 添加弹射物保护III
        meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 3, true);

        // 添加爆炸保护III
        meta.addEnchant(Enchantment.BLAST_PROTECTION, 3, true);


        Text.send(player, "<green>已应用胸甲强化附魔！");
    }

    // 应用胸甲附魔特化（保护IV）
    private void applySpecializedProtection(Player player, ItemMeta meta, ItemStack chestplate) {
        // 移除所有其他保护类附魔
        List<Enchantment> protectionEnchants = Arrays.asList(
                Enchantment.PROJECTILE_PROTECTION,
                Enchantment.BLAST_PROTECTION
        );

        for (Enchantment enchant : protectionEnchants) {
            if (meta.hasEnchant(enchant)) {
                Text.send(player, "<yellow>你已经附魔弹射物保护III和爆炸保护III了");
                return;
            }
        }

        // 添加保护IV
        meta.addEnchant(Enchantment.PROTECTION, 4, true);


        Text.send(player, "<green>已应用胸甲附魔特化！");
    }

    // 获取默认的胸甲名称
    private String getDefaultChestplateName(Material material) {
        switch (material) {
            case LEATHER_CHESTPLATE: return "<gray>皮革胸甲";
            case IRON_CHESTPLATE: return "<gray>铁胸甲";
            case CHAINMAIL_CHESTPLATE: return "<gray>锁链胸甲";
            case GOLDEN_CHESTPLATE: return "<gray>金胸甲";
            case DIAMOND_CHESTPLATE: return "<gray>钻石胸甲";
            case NETHERITE_CHESTPLATE: return "<dark_purple>下界合金胸甲";
            default: return "<gray>胸甲";
        }
    }

    // 在购买前检查玩家是否有胸甲
    private boolean hasChestplate(Player player) {
        // 检查穿着的胸甲
        ItemStack equipped = player.getInventory().getChestplate();
        if (equipped != null && isChestplateType(equipped.getType())) {
            return true;
        }

        // 检查背包中的胸甲
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isChestplateType(item.getType())) {
                return true;
            }
        }

        return false;
    }

    private void giveBowToPlayer(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        player.getInventory().addItem(bow);
    }

    private void giveTeleportHookToPlayer(Player player) {
        ItemStack hook = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = hook.getItemMeta();
        meta.displayName(Text.mm("<gray>传送钩"));
        List<String> lore = new ArrayList<>();
        lore.add("<gold>放竿记录位置");
        lore.add("<gray>再次使用传送到鱼钩位置");
        meta.lore(lore.stream().map(Text::mm).collect(Collectors.toList()));
        hook.setItemMeta(meta);

        player.getInventory().addItem(hook);
    }
}
