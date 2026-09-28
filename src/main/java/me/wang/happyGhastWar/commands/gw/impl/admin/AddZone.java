package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /gw admin addzone <资源类型> - 将视线所指方块设置为中立资源据点中心（10格范围）。
 * 中立区内的资源刷新速度为普通区域的两倍，且开采会向全图广播（改进建议 3.1）。
 * 资源类型（用于展示标识）: iron / copper / coal / wood
 */
public class AddZone extends GWCommand {

    private static final List<String> TYPES = List.of("iron", "copper", "coal", "wood");

    public AddZone(){
        super("addzone");
    }

    @Override
    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (!HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }

        if (params.isEmpty()){
            Text.send(player, "<red>用法: /gw admin addzone <iron|copper|coal|wood>");
            return;
        }
        String type = params.getFirst().toLowerCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            Text.send(player, "<red>资源类型必须是 iron / copper / coal / wood");
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        Block block = player.getTargetBlockExact(6);
        if (block == null){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.target-empty"));
            return;
        }

        arenaConfig.addNeutralZone(block.getLocation(), type);
        Text.send(player, ghastWar.getLanguage(player).getContent("commands.setSuccess")
                + " <gray>(中立区: " + type + " @ " + block.getX() + "," + block.getY() + "," + block.getZ() + ")");
    }

    @Override
    public void complete(HappyGhastWar happyGhastWar, org.bukkit.command.CommandSender sender, String alias, List<String> params, List<String> suggestions) {
        if (params.size() <= 1) {
            suggestByParameter(TYPES.stream(), suggestions, params.isEmpty() ? null : params.getFirst());
        }
    }
}
