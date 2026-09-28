package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class BackUp extends GWCommand {
    public BackUp(){
        super("backup");
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (params.isEmpty()){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.missRequireData"));
            return;
        }
        World world = Bukkit.getWorld(params.getFirst());
        if (world != null && !HappyGhastWar.arenas.containsKey(world.getName())) {
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = null;
        if (world != null) {
            arena = HappyGhastWar.arenas.get(world.getName());
        }

        if (arena != null) {
            arena.backupWorld();
        }

        Text.send(player, ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
