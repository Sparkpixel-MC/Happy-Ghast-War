package me.wang.happyGhastWar.commands.gw.impl.admin;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public class RemoveChest extends GWCommand {
    public RemoveChest(){
        super("removechest");
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

        arenaConfig.removeChest(block.getLocation());

        Text.send(player, HappyGhastWar.language.getContent("commands.setSuccess"));
    }
}
