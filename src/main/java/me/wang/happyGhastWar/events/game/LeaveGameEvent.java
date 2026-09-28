package me.wang.happyGhastWar.events.game;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LeaveGameEvent implements Listener {

    private HappyGhastWar ghastWar;

    public LeaveGameEvent(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void leaveFromWorld(PlayerChangedWorldEvent e){
        World world = e.getFrom();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            return;
        }
        Arena arena = HappyGhastWar.arenas.get(world.getName());
        arena.removePlayer(e.getPlayer());

    }

    @EventHandler
    public void leaveFromServer(PlayerQuitEvent e){
        World world = e.getPlayer().getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            return;
        }
        Arena arena = HappyGhastWar.arenas.get(world.getName());
        arena.removePlayer(e.getPlayer());
    }
}
