package top.sparkpixel.hgw.commands.gw.impl;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.entity.Player;

import java.util.List;

public class GUI extends GWCommand {

    public GUI(){
        super("gui", true);
    }

    @Override
    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (ghastWar.getConfig().getBoolean("bungee.enable",false) && ghastWar.getConfig().getBoolean("bungee.can-select-game",false)){
            Text.send(player, HappyGhastWar.language.getContent("commands.cant-select-game"));
            return;
        }
        ghastWar.getArenaSelector().openSelector(player);
    }
}
