package me.wang.happyGhastWar.commands.gw.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /gw gui - 打开场地选择菜单
 */
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
