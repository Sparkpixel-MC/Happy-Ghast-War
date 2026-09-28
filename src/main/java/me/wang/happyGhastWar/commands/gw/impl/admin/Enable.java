package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class Enable extends GWCommand {
    public Enable(){
        super("enable");
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(world.getName());
        arena.setEnable(true);

        Text.send(player, ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
