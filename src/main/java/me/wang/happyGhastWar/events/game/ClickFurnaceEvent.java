package me.wang.happyGhastWar.events.game;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ClickFurnaceEvent implements Listener {

    private HappyGhastWar ghastWar;

    public ClickFurnaceEvent(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void click(PlayerInteractEntityEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getPlayer().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getPlayer().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        if (e.getRightClicked().getType() != EntityType.INTERACTION){
            return;
        }
        e.setCancelled(true);
        arena.upgradeGUI.openUpgradeGUI(e.getPlayer(), arena.getPlayerTeam(e.getPlayer()), (Interaction) e.getRightClicked());
    }

    @EventHandler
    public void clickAtBlock(PlayerInteractEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getPlayer().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getPlayer().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        if (!e.hasBlock()) return;
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.FURNACE && e.getClickedBlock().getType() != Material.BLAST_FURNACE) return;
        e.setCancelled(true);
        arena.upgradeGUI.openUpgradeGUI(e.getPlayer(), null);
    }
}
