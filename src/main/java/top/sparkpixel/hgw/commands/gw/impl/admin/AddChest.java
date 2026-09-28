package top.sparkpixel.hgw.commands.gw.impl.admin;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.arena.ArenaConfig;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public class AddChest extends GWCommand {
    public AddChest(){
        super("addchest");
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            Text.send(player, HappyGhastWar.language.getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(world.getName());
        ArenaConfig arenaConfig = arena.getArenaConfig();

        Block block = player.getTargetBlockExact(5);
        if (block == null){
            Text.send(player, HappyGhastWar.language.getContent("commands.target-empty"));
            return;
        }
        if (block.getType() != Material.CHEST){
            Text.send(player, HappyGhastWar.language.getContent("commands.type-not-chest"));
            return;
        }

        arenaConfig.addChest(block.getLocation());

        Text.send(player, HappyGhastWar.language.getContent("commands.setSuccess"));
    }
}
