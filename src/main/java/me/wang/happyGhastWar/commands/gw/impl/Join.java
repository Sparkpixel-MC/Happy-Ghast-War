package me.wang.happyGhastWar.commands.gw.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.util.Text;
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

        // 场地配置不完整（如还没设置等待点）时给出明确提示，而不是 NPE 崩溃
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
