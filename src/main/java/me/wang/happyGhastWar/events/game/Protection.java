package me.wang.happyGhastWar.events.game;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.game.team.Team;
import me.wang.happyGhastWar.ghast.GameGhast;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Protection implements Listener {

    private HappyGhastWar ghastWar;

    public Protection(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void damage(EntityDamageByEntityEvent e){
        if (e.getEntity().getType() != EntityType.HAPPY_GHAST) return;
        if (!HappyGhastWar.arenas.containsKey(e.getEntity().getLocation().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getEntity().getLocation().getWorld().getName());
        if (!arena.getGhasts().containsKey((HappyGhast) e.getEntity())) return;
        GameGhast ghast = arena.getGhasts().get((HappyGhast) e.getEntity());
        Team ghast_team = null;
        Team damager_team = null;
        Entity damager;
        switch (e.getDamager().getType()){
            case PLAYER -> damager = e.getDamager();
            case ARROW -> damager = (Entity) ((Arrow)e.getDamager()).getShooter();
            case FIREBALL -> damager = (Entity) ((Fireball) e.getDamager()).getShooter();
            case TNT -> damager = (Entity) ((TNTPrimed) e.getDamager()).getSource();
            default -> {
                return;
            }
        }
        if (!(damager instanceof Player)) return;
        for (Team team : arena.getTeams()){
            if (team.getGhasts().contains(ghast)){
                ghast_team = team;
            }
            if (team.getPlayers().contains((Player) damager)){
                damager_team = team;
            }
        }



        if (ghast_team == null || damager_team == null) return;

        if (ghast_team.equals(damager_team)){
            me.wang.happyGhastWar.util.Text.send(e.getDamager(), ghastWar.getLanguage(damager instanceof Player ? (Player) damager : null).getContent("game.protect-ghast"));
            e.setCancelled(true);
        } else {
            // 应用护甲减伤
            double damage = e.getDamage();
            double reduction = ghast.getArmorLevel() * 0.05; // 每级护甲减少5%伤害
            double reducedDamage = damage * (1 - reduction);

            if (reducedDamage < damage) {
                // 记录伤害减免信息
                me.wang.happyGhastWar.util.Text.send((Player) damager,
                        "<gray>乐魂护甲减免了 " + String.format("%.1f", damage - reducedDamage) + " 点伤害!");
            }

            e.setDamage(reducedDamage);
        }
    }

    @EventHandler
    public void enter(VehicleEnterEvent e){
        if (e.getVehicle().getType() != EntityType.HAPPY_GHAST) return;
        if (!(e.getEntered() instanceof Player)) return;
        if (!HappyGhastWar.arenas.containsKey(e.getVehicle().getLocation().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getVehicle().getLocation().getWorld().getName());
        if (!arena.getGhasts().containsKey((HappyGhast) e.getVehicle())) return;
        GameGhast ghast = arena.getGhasts().get((HappyGhast) e.getVehicle());
        Team ghast_team = null;
        Team enter_team = null;

        for (Team team : arena.getTeams()){
            if (team.getGhasts().contains(ghast)){
                ghast_team = team;
            }
            if (team.getPlayers().contains((Player) e.getEntered())){
                enter_team = team;
            }
        }

        if (ghast_team == null || enter_team == null) return;

        if (!ghast_team.equals(enter_team)){
            e.setCancelled(true);
            me.wang.happyGhastWar.util.Text.send(e.getEntered(), ghastWar.getLanguage((Player) e.getEntered()).getContent("game.enter-other-ghast"));
        }
    }

    @EventHandler
    public void blockExplode(BlockExplodeEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getBlock().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getBlock().getWorld().getName());
        if (arena.status != Arena.GameStatus.PLAYING){
            e.setCancelled(true);
            return;
        }
        e.blockList().removeIf(block -> block.getType() == Material.FURNACE || block.getType() == Material.BLAST_FURNACE);
        for (Block block : e.blockList()){
            block.setType(Material.AIR);
        }
        e.blockList().clear();
    }

    @EventHandler
    public void entityExplode(EntityExplodeEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getEntity().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getEntity().getWorld().getName());
        if (arena.status != Arena.GameStatus.PLAYING){
            e.setCancelled(true);
            return;
        }
        e.blockList().removeIf(block -> block.getType() == Material.FURNACE || block.getType() == Material.BLAST_FURNACE);
        for (Block block : e.blockList()){
            block.setType(Material.AIR);
        }
        e.blockList().clear();
    }

    @EventHandler
    public void breakBlock(BlockBreakEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getBlock().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getBlock().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void fallDamage(EntityDamageEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getEntity().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getEntity().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        // 摔落伤害默认开启（game.fall-damage: true），仅当场地配置显式关闭时才取消
        if (!arena.getArenaConfig().isFallDamageEnabled()) {
            e.setCancelled(true);
        }
    }

    private List<Integer> disabledSlot = new ArrayList<>(Arrays.asList(36,37,38,39));

    @EventHandler
    public void clickInv(InventoryClickEvent e){
        if (e.getClickedInventory() == null) return;
        Player player = (Player) e.getWhoClicked();
        if (!HappyGhastWar.arenas.containsKey(player.getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        if (e.getClickedInventory().getType() != InventoryType.PLAYER) return;
        if (!disabledSlot.contains(e.getSlot())) return;
        e.setCancelled(true);

    }

    @EventHandler
    public void interact(PlayerInteractEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getPlayer().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getPlayer().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING){
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void entitySpawn(EntitySpawnEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getEntity().getWorld().getName())) return;
        List<String> white = HappyGhastWar.config.getStringList("entity-white-list");
        if (white.contains(e.getEntity().getType().name())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void playerDrop(PlayerDropItemEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getPlayer().getWorld().getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void weather(WeatherChangeEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getWorld().getName())) return;
        e.setCancelled(true);
    }

}
