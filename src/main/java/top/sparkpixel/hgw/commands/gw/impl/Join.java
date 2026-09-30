package top.sparkpixel.hgw.commands.gw.impl;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.arena.ArenaConfig;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.entity.Player;

import java.util.List;

public class Join extends GWCommand {
    public Join(){
        super("join", true);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (ghastWar.getConfig().getBoolean("bungee.enable",false) && ghastWar.getConfig().getBoolean("bungee.can-select-game",false)){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.cant-select-game"));
            return;
        }
        if (params.isEmpty()){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.missRequireData"));
            return;
        }
        if (!HappyGhastWar.arenas.containsKey(params.getFirst())){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(params.getFirst());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        List<String> missingSetup = arenaConfig.getMissingSetup();
        if (!missingSetup.isEmpty()){
            Text.send(player, "<red>场地 [" + params.getFirst() + "] 配置不完整，无法加入，缺少：");
            for (String item : missingSetup){
                Text.send(player, "<gray> - " + item);
            }
            return;
        }

        player.teleport(arenaConfig.getWait());
        arena.internalAddPlayer(player);
    }


}
