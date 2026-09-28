package me.wang.happyGhastWar.game.prop;


import me.wang.happyGhastWar.game.upgrade.UpgradeManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class ItemFunctions implements Listener {
    private JavaPlugin plugin;
    private UpgradeManager upgradeManager;

    // 冷却时间管理
    private Map<UUID, Long> cannonCooldowns = new HashMap<>();
    private Map<UUID, Long> catapultCooldowns = new HashMap<>();
    private Map<UUID, Integer> catapultUses = new HashMap<>();
    private Map<UUID, Long> catapultRecovery = new HashMap<>();

    // 传送钩位置存储
    private Map<UUID, Location> hookLocations = new HashMap<>();
    private Map<UUID, FishHook> activeHooks = new HashMap<>();

    // 重型巨弩标识
    private Set<UUID> heavyCrossbowArrows = new HashSet<>();

    public ItemFunctions(JavaPlugin plugin) {
        this.plugin = plugin;
        this.upgradeManager = UpgradeManager.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

    }

    //剑的风暴I附魔
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player player = (Player) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (weapon == null) return;

        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) return;

        String displayName = meta.getDisplayName();
        if (displayName == null) return;

        // 检查是否是有风暴I附魔的剑
        boolean isStormSword = displayName.equals("§7铁剑(风暴I)") ||
                displayName.equals("§5下界合金剑(风暴I)");

        if (!isStormSword) return;

        // 检查是否为暴击（玩家在空中且未在梯子/藤蔓上）
        boolean isCritical = !player.isOnGround() &&
                !player.isClimbing() &&
                player.getFallDistance() > 0;

        if (isCritical) {
            // 生成风暴弹
            createStormProjectile(event.getEntity());
        }
    }

    private void createStormProjectile(Entity entity) {
        WindCharge windCharge = entity.getWorld().spawn(entity.getLocation(),WindCharge.class);
        windCharge.explode();
    }

    //弹射器功能
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (item == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String displayName = meta.getDisplayName();
        if (displayName == null) return;

        // 检查是否是弹射器
        if (displayName.equals("§7初级弹射器") ||
                displayName.equals("§7进阶弹射器") ||
                displayName.equals("§7高阶弹射器") ||
                displayName.equals("§5风核弹射器")) {

            event.setCancelled(true);
            useCatapult(player, displayName);
        }

        // 检查是否是火炮
        if (displayName.equals("§7初级核心火炮") ||
                displayName.equals("§7进阶核心火炮") ||
                displayName.equals("§7高阶核心火炮")) {

            event.setCancelled(true);
            useCannon(player, displayName, event.getAction());
        }
    }

    private void useCatapult(Player player, String catapultName) {
        UUID playerId = player.getUniqueId();
        if (player.getCooldown(Material.PISTON) > 0) return;
        // 获取弹射器等级
        int level = 0;
        if (catapultName.equals("§7初级弹射器")) level = 0;
        else if (catapultName.equals("§7进阶弹射器")) level = 1;
        else if (catapultName.equals("§7高阶弹射器")) level = 2;
        else if (catapultName.equals("§5风核弹射器")) level = 3;

        // 获取使用次数和恢复时间
        int maxUses = getCatapultMaxUses(level);
        int recoveryTime = getCatapultRecoveryTime(level);

        // 检查使用次数
        int currentUses = catapultUses.getOrDefault(playerId, maxUses);

        // 使用弹射器
        currentUses--;
        catapultUses.put(playerId, currentUses);
        catapultCooldowns.put(playerId, System.currentTimeMillis());

        // 给予推力
        Vector direction = player.getLocation().getDirection();
        double power = 1.5 + (level * 0.2); // 等级越高推力越大
        player.setVelocity(direction.multiply(power));

        // 效果
        me.wang.happyGhastWar.util.SoundUtil.play(player, "prop-jump", Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

        // 显示剩余使用次数
        String message = "<green>" + "■".repeat(currentUses) + "<gray>" + "■".repeat(maxUses-currentUses);
        player.sendActionBar(me.wang.happyGhastWar.util.Text.mm(message));

        // 如果使用次数用完，开始恢复计时
        if (currentUses <= 0) {
            player.setCooldown(Material.PISTON,recoveryTime);
            maxUses = getCatapultMaxUses(level);
            catapultUses.put(playerId, maxUses);
        }
    }



    private int getCatapultMaxUses(int level) {
        switch (level) {
            case 0: return 2;  // 初级
            case 1: return 3;  // 进阶
            case 2: return 4;  // 高阶
            case 3: return 5;  // 风核
            default: return 2;
        }
    }

    private int getCatapultRecoveryTime(int level) {
        switch (level) {
            case 0: return 100;   // 初级: 5秒
            case 1: return 70;   // 进阶: 3.5秒
            case 2: return 60;   // 高阶: 3秒
            case 3: return 50;   // 风核: 2.5秒
            default: return 100;
        }
    }

    private ItemStack getCatapultFromPlayer(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.PISTON) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String name = meta.getDisplayName();
                    if (name.contains("弹射器")) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    //传送钩功能
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.FISHING_ROD) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        if (!meta.getDisplayName().equals("§7传送钩")) return;
        if (player.getCooldown(item) > 0) return;

        PlayerFishEvent.State state = event.getState();
        FishHook hook = event.getHook();

        if (state == PlayerFishEvent.State.FISHING && !hookLocations.containsKey(player.getUniqueId())) {
            // 玩家抛竿，记录鱼钩
            activeHooks.put(player.getUniqueId(), hook);
            // 必须在主线程轮询：isOnGround/getState/getHookedEntity 都是主线程 API
            BukkitRunnable hookTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!hook.isValid() || hook.isDead()) {
                        activeHooks.remove(player.getUniqueId());
                        cancel();
                        return;
                    }
                    if (hookLocations.containsKey(player.getUniqueId())) {
                        cancel();
                        return;
                    }
                    if (hook.isOnGround()){
                        hookLocations.put(player.getUniqueId(), hook.getLocation().clone());
                        activeHooks.remove(player.getUniqueId());
                    }else if (hook.getState().equals(FishHook.HookState.HOOKED_ENTITY) && hook.getHookedEntity() != null){
                        hookLocations.put(player.getUniqueId(), hook.getHookedEntity().getLocation());
                        activeHooks.remove(player.getUniqueId());
                    }else{
                        return;
                    }
                    me.wang.happyGhastWar.util.Text.send(player, "<green>位置已记录");
                    hook.remove();
                }
            };
            BukkitTask task = hookTask.runTaskTimer(plugin, 0, 2);
            plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                if (!task.isCancelled()){
                    task.cancel();
                }
            },10*20);
        }

        else if (state == PlayerFishEvent.State.REEL_IN || hookLocations.containsKey(player.getUniqueId())) {
            // 玩家收竿，检查是否有记录的位置
            Location recordedLocation = hookLocations.get(player.getUniqueId());

            if (recordedLocation != null && !hook.isDead()) {
                event.setCancelled(true);

                // 收回鱼竿
                hook.remove();

                player.setCooldown(item, 60 * 20);
                // 延迟传送
                new BukkitRunnable() {
                    @Override
                    public void run() {

                        // 执行传送
                        player.teleport(recordedLocation);

                        // 清除记录的位置
                        hookLocations.remove(player.getUniqueId());
                    }
                }.runTaskLater(plugin, 5L);
            } else {
                // 没有记录的位置或者鱼钩已消失
                if (hook != null && !hook.isDead()) {
                    // 正常收竿
                    me.wang.happyGhastWar.util.Text.send(player, "<red>鱼钩位置未记录！请等待鱼钩落地后再收竿。");
                }
            }
        }
        else if (state == PlayerFishEvent.State.CAUGHT_FISH) {
            // 钓到鱼，不触发传送，取消事件
            event.setCancelled(true);

            // 收回鱼竿
            if (hook != null) {
                hook.remove();
            }

            // 检查是否有记录的位置
            Location recordedLocation = hookLocations.get(player.getUniqueId());
            if (recordedLocation != null) {
                // 有记录的位置，询问是否传送
                me.wang.happyGhastWar.util.Text.send(player, "<red>钓到了鱼！鱼钩位置已丢失。");
                me.wang.happyGhastWar.util.Text.send(player, "<gray>如果鱼钩落地，请直接收竿（不钓鱼）来传送。");

                // 清除记录的位置
                hookLocations.remove(player.getUniqueId());
            }
        }
    }

    //核心火炮功能
    private Map<UUID, Long> cannonChargeStart = new HashMap<>();
    private Map<UUID, BukkitRunnable> cannonChargeTasks = new HashMap<>();

    private void useCannon(Player player, String cannonName, Action action) {
        UUID playerId = player.getUniqueId();

        // 获取火炮等级
        int level;
        if (cannonName.equals("§7初级核心火炮")) level = 0;
        else if (cannonName.equals("§7进阶核心火炮")) level = 1;
        else if (cannonName.equals("§7高阶核心火炮")) level = 2;
        else {
            level = 0;
        }

        // 检查冷却
        if (player.getCooldown(Material.DISPENSER) > 0) {
            return;
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // 开始蓄力
            if (!cannonChargeStart.containsKey(playerId)) {
                cannonChargeStart.put(playerId, System.currentTimeMillis());
                me.wang.happyGhastWar.util.Text.send(player, "<gold>蓄力中...");

                // 播放蓄力声音
                me.wang.happyGhastWar.util.SoundUtil.play(player, "prop-fireball-charge", Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);

                // 蓄力任务
                BukkitRunnable chargeTask = new BukkitRunnable() {
                    int chargeTicks = 0;

                    @Override
                    public void run() {
                        if (!player.isOnline() || !cannonChargeStart.containsKey(playerId)) {
                            this.cancel();
                            cannonChargeStart.remove(playerId);
                            cannonChargeTasks.remove(playerId);
                            return;
                        }

                        chargeTicks++;

                        // 检查蓄力时间
                        long chargeStart = cannonChargeStart.get(playerId);
                        long currentTime = System.currentTimeMillis();
                        float chargeTime = getCannonChargeTime(level) * 1000;

                        if (currentTime - chargeStart >= chargeTime) {
                            // 蓄力完成，发射
                            this.cancel();
                            cannonChargeStart.remove(playerId);
                            cannonChargeTasks.remove(playerId);
                            fireCannon(player, level, true);
                        }
                    }
                };

                chargeTask.runTaskTimer(plugin, 0L, 1L); // 每tick运行
                cannonChargeTasks.put(playerId, chargeTask);
            }
        }
    }


    //检查玩家是否还在右键
    public void startCannonChargeCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();

                    if (cannonChargeStart.containsKey(playerId)) {
                        // 检查玩家是否还在手持火炮
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item == null || item.getType() != Material.DISPENSER) {
                            // 玩家切换了物品，取消蓄力
                            cancelCannonCharge(playerId);
                            me.wang.happyGhastWar.util.Text.send(player, "<red>火炮蓄力已取消");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void cancelCannonCharge(UUID playerId) {
        if (cannonChargeTasks.containsKey(playerId)) {
            cannonChargeTasks.get(playerId).cancel();
        }
        cannonChargeStart.remove(playerId);
        cannonChargeTasks.remove(playerId);
    }

    private void fireCannon(Player player, int level, boolean fullyCharged) {
        UUID playerId = player.getUniqueId();

        if (!fullyCharged) {
            me.wang.happyGhastWar.util.Text.send(player, "<red>蓄力不足！");
            return;
        }

        // 设置冷却
        player.setCooldown(Material.DISPENSER,getCannonCooldown(level));

        // 根据等级发射火球
        int fireballCount = 1;
        float power = 1.0f;

        switch (level) {
            case 0: // 初级：1个火球
                fireballCount = 1;
                power = 1.0f;
                break;
            case 1: // 进阶：1个威力翻倍的火球
                fireballCount = 1;
                power = 2.0f;
                break;
            case 2: // 高阶：3个威力翻倍的火球
                fireballCount = 3;
                power = 2.0f;
                break;
        }

        // 发射火球
        for (int i = 0; i < fireballCount; i++) {
            // 延迟发射以实现连射效果
            final int index = i;
            float finalPower = power;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location spawnLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(2));
                    spawnLoc.setY(spawnLoc.getY() - 0.5); // 稍微降低高度

                    Fireball fireball = player.getWorld().spawn(spawnLoc, Fireball.class);
                    fireball.setShooter(player);
                    fireball.setDirection(player.getLocation().getDirection());
                    fireball.setYield(finalPower); // 设置爆炸威力
                    fireball.setIsIncendiary(true); // 点燃方块

                    // 设置火球速度
                    fireball.setVelocity(player.getLocation().getDirection().multiply(1.5));

                    // 效果
                    me.wang.happyGhastWar.util.SoundUtil.play(player, "prop-fireball", Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
                    player.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 20, 0.1, 0.1, 0.1, 0.05);
                    player.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 10, 0.1, 0.1, 0.1, 0.05);

                }
            }.runTaskLater(plugin, i * 5L); // 每个火球间隔5 tick
        }
    }

    private float getCannonChargeTime(int level) {
        switch (level) {
            case 0: return 1.5f;  // 初级：1.5秒
            case 1: return 1.5f;  // 进阶：1.5秒
            case 2: return 2.2f;  // 高阶：2.2秒
            default: return 1.5f;
        }
    }

    private int getCannonCooldown(int level) {
        switch (level) {
            case 0: return 120;   // 初级：6秒
            case 1: return 240;  // 进阶：12秒
            case 2: return 320;  // 高阶：16秒
            default: return 120;
        }
    }

    //重型巨弩功能
    @EventHandler
    public void onProjectileLaunch(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Arrow arrow = (Arrow) event.getEntity();
        Player player = (Player) arrow.getShooter();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.CROSSBOW) return;
        if (player.getCooldown(item) > 0) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        if (meta.getDisplayName().equals("§7重型巨弩")) {
            // 标记这支箭是重型巨弩射出的
            heavyCrossbowArrows.add(arrow.getUniqueId());
            player.setCooldown(item,4 * 20);
            // 设置箭的属性
            arrow.setCritical(false); // 取消重力影响
            arrow.setGravity(false);  // 无重力
            arrow.setDamage(4.0);    // 13点伤害

        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();

            if (heavyCrossbowArrows.contains(arrow.getUniqueId())) {
                // 移除标记
                heavyCrossbowArrows.remove(arrow.getUniqueId());
            }
        }
    }

    //连射弩功能
    private Map<UUID, Long> rapidCrossbowCooldowns = new HashMap<>();

    //检查并装填连射弩
    public void startRapidCrossbowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkAndReloadRapidCrossbow(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒检查一次
    }

    @EventHandler
    public void useRapidCrossbow(PlayerInteractEvent e){
        if (e.getItem() == null) return;
        if (e.getItem().getItemMeta() == null) return;
        if (!"§7连射弩".equals(e.getItem().getItemMeta().getDisplayName())) return;
        if (!(e.getItem().getItemMeta() instanceof org.bukkit.inventory.meta.CrossbowMeta crossbowMeta)) return;
        if (!crossbowMeta.hasChargedProjectiles()) return;
        Bukkit.getServer().getScheduler().runTaskLater(plugin, ()-> {
            checkAndReloadRapidCrossbow(e.getPlayer());
        },20L);
    }

    private void checkAndReloadRapidCrossbow(Player player) {
        // 检查玩家是否有连射弩
        boolean hasRapidCrossbow = false;
        ItemStack rapidCrossbow = null;

        if (!player.getInventory().contains(Material.ARROW)) return;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.CROSSBOW) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.CrossbowMeta &&
                        meta.hasDisplayName() &&
                        "§7连射弩".equals(meta.getDisplayName())) {
                    hasRapidCrossbow = true;
                    rapidCrossbow = item;
                    break;
                }
            }
        }

        if (!hasRapidCrossbow || rapidCrossbow == null) return;

        // 检查弩是否已装填
        org.bukkit.inventory.meta.CrossbowMeta crossbowMeta = (org.bukkit.inventory.meta.CrossbowMeta) rapidCrossbow.getItemMeta();
        if (crossbowMeta == null) return;

        if (!crossbowMeta.hasChargedProjectiles()) {
            // 自动装填：从背包消耗一支箭
            if (!player.getInventory().contains(Material.ARROW)) return;
            player.getInventory().removeItem(new ItemStack(Material.ARROW, 1));

            ItemStack arrow = new ItemStack(Material.ARROW, 1);
            List<ItemStack> projectiles = new ArrayList<>();
            projectiles.add(arrow);

            crossbowMeta.setChargedProjectiles(projectiles);
            rapidCrossbow.setItemMeta(crossbowMeta);
            me.wang.happyGhastWar.util.SoundUtil.play(player, "crossbow-load", Sound.ITEM_CROSSBOW_LOADING_END, 0.6f, 1.4f);
        }
    }

    //盾牌功能
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查副手是否有盾牌
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() == Material.SHIELD) {
            ItemMeta meta = offhand.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String name = meta.getDisplayName();

                if (name.equals("§7硬质盾")) {
                    // 硬质盾：+2护甲值，+2盔甲韧性
                    event.setDamage(event.getDamage() * 0.85); // 减少15%伤害

                    // 添加吸收效果
                    if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                        event.setDamage(event.getDamage() * 0.7); // 对弹射物额外减伤
                    }
                }
                else if (name.equals("§5合金盾")) {
                    // 合金盾：+3护甲值，+4盔甲韧性
                    event.setDamage(event.getDamage() * 0.75); // 减少25%伤害

                    // 添加吸收效果
                    if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE ||
                            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                        event.setDamage(event.getDamage() * 0.6); // 对弹射物和爆炸额外减伤
                    }

                    // 反弹效果
                    if (event instanceof EntityDamageByEntityEvent) {
                        EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) event;
                        Entity damager = entityEvent.getDamager();

                        if (damager instanceof Projectile) {
                            // 反弹弹射物
                            Vector direction = damager.getVelocity().multiply(-1.5);
                            damager.setVelocity(direction);
                        }
                    }
                }
            }
        }
    }

    //初始化任务
    public void startAllTasks() {
        startCannonChargeCheck();
        startRapidCrossbowTask();
    }

    //玩家退出清理
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 清理数据
        cannonChargeStart.remove(playerId);
        if (cannonChargeTasks.containsKey(playerId)) {
            cannonChargeTasks.get(playerId).cancel();
            cannonChargeTasks.remove(playerId);
        }

        cannonCooldowns.remove(playerId);
        catapultCooldowns.remove(playerId);
        catapultUses.remove(playerId);
        catapultRecovery.remove(playerId);
        hookLocations.remove(playerId);
        if (activeHooks.containsKey(playerId)) {
            FishHook hook = activeHooks.get(playerId);
            if (hook != null && !hook.isDead()) {
                hook.remove();
            }
            activeHooks.remove(playerId);
        }
        rapidCrossbowCooldowns.remove(playerId);

        // 清理重型巨弩的箭标记
        Iterator<UUID> iterator = heavyCrossbowArrows.iterator();
        while (iterator.hasNext()) {
            UUID arrowId = iterator.next();
            iterator.remove();
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 清理数据
        cannonChargeStart.remove(playerId);
        if (cannonChargeTasks.containsKey(playerId)) {
            cannonChargeTasks.get(playerId).cancel();
            cannonChargeTasks.remove(playerId);
        }

        cannonCooldowns.remove(playerId);
        catapultCooldowns.remove(playerId);
        catapultUses.remove(playerId);
        catapultRecovery.remove(playerId);
        hookLocations.remove(playerId);
        if (activeHooks.containsKey(playerId)) {
            FishHook hook = activeHooks.get(playerId);
            if (hook != null && !hook.isDead()) {
                hook.remove();
            }
            activeHooks.remove(playerId);
        }
        rapidCrossbowCooldowns.remove(playerId);

        // 清理重型巨弩的箭标记
        Iterator<UUID> iterator = heavyCrossbowArrows.iterator();
        while (iterator.hasNext()) {
            UUID arrowId = iterator.next();
            iterator.remove();
        }
    }
}
