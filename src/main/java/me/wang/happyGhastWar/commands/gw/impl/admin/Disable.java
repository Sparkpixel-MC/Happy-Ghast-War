package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class Disable extends GWCommand {
    public Disable(){
        super("disable");
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(world.getName());
        arena.setEnable(false);

        Text.send(player, ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
