package top.sparkpixel.hgw.commands.gw.impl;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /gw stats - 查看个人本场游戏统计
 */
public class Stats extends GWCommand {

    public Stats(){
        super("stats", true, "statistic");
    }

    @Override
    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        Arena arena = findArena(player);
        if (arena == null) {
            player.sendMessage("你不在任何竞技场中");
            return;
        }

        arena.getStatistics().showPlayerStats(player);
    }

    static Arena findArena(Player player) {
        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
        if (arena != null) {
            return arena;
        }
        // 兜底：玩家已加入但尚未传送
        for (Arena a : HappyGhastWar.arenas.values()) {
            if (a.getPlayers().contains(player)) {
                return a;
            }
        }
        return null;
    }
}
