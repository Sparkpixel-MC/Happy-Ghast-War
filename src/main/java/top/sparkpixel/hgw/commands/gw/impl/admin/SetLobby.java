package top.sparkpixel.hgw.commands.gw.impl.admin;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class SetLobby extends GWCommand {
    public SetLobby(){
        super("setlobby");
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (HappyGhastWar.arenas.containsKey(world.getName())){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.lobby-not-allow"));
            return;
        }
        ghastWar.setLobby(player.getLocation());

        Text.send(player, ghastWar.getLanguage(player).getContent("commands.setSuccess"));
    }
}
