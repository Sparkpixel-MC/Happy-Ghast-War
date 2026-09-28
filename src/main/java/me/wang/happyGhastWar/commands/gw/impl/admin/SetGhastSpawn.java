package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetGhastSpawn extends GWCommand {
    public SetGhastSpawn(){
        super("setghastspawn");
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }
        if (params.isEmpty()){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.missRequireData"));
            return;
        }
        List<String> teams = new ArrayList<>();
        Arrays.stream(Arena.Teams.values()).forEach(teams1 -> teams.add(teams1.name()));

        if (!teams.contains(params.getFirst())){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.teamNotFound"));
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.availableTeams"));
            return;
        }
        Arena arena = HappyGhastWar.arenas.get(world.getName());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        arenaConfig.setGhastSpawn(Arena.Teams.valueOf(params.getFirst()),player.getLocation());

        Text.send(player, ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
