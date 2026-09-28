package top.sparkpixel.hgw.commands.gw.impl.admin;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.arena.ArenaConfig;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class SetRadius extends GWCommand {
    public SetRadius(){
        super("setradius");
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

        Arena arena = HappyGhastWar.arenas.get(world.getName());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        try {
            arenaConfig.setRadius(Integer.parseInt(params.getFirst()));
        } catch (NumberFormatException e) {
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.missRequireData"));
            return;
        }

        Text.send(player, ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
