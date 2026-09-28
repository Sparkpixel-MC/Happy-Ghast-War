package top.sparkpixel.hgw.commands.gw.impl.admin;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.arena.ArenaConfig;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetSpawn extends GWCommand {
    public SetSpawn(){
        super("setspawn");
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

        arenaConfig.setTeamSpawn(Arena.Teams.valueOf(params.getFirst()),player.getLocation());

        Text.send(player, ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
