package top.sparkpixel.hgw.commands.gw.impl.admin;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.arena.ArenaConfig;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /gw admin removezone - 移除视线所指方块所在的中立资源据点
 */
public class RemoveZone extends GWCommand {

    public RemoveZone(){
        super("removezone");
    }

    @Override
    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (!HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        Block block = player.getTargetBlockExact(6);
        if (block == null){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.target-empty"));
            return;
        }

        int before = arenaConfig.getNeutralZones().size();
        arenaConfig.removeNeutralZone(block.getLocation());
        int after = arenaConfig.getNeutralZones().size();

        if (before == after) {
            Text.send(player, "<red>该位置不是中立区中心");
        } else {
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.setSuccess"));
        }
    }
}
