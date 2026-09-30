package top.sparkpixel.hgw.commands.gw.impl;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.game.party.Party;
import top.sparkpixel.hgw.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class Leave extends GWCommand {
    public Leave(){
        super("leave", true);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        World world = player.getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            Text.send(player, HappyGhastWar.language.getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(world.getName());

        Party party = HappyGhastWar.getInstance().getPartyManager().getParty(player);
        HappyGhastWar.getInstance();

        if (party != null && party.isLeader(player)) {

            List<Player> partyMembers = party.getOnlineMembers();

            for (Player member : partyMembers) {
                tpToLobby(member,ghastWar);
                arena.removePlayer(member);
            }
        } else {
            if (party != null && !party.isLeader(player)) {
                if (arena.getPlayers().contains(party.getLeader())) {
                    player.sendMessage(
                            Component.text("请等待队长 ")
                                    .append(Component.text(party.getLeader().getName()))
                                    .append(Component.text(" 先离开游戏!"))
                                    .color(NamedTextColor.RED)
                    );
                    return;
                }
            }
            tpToLobby(player,ghastWar);
            arena.removePlayer(player);
        }
    }

    public void tpToLobby(Player player,HappyGhastWar ghastWar){
        if (!ghastWar.getConfig().getBoolean("bungee.enable",false)){
            player.teleport(ghastWar.getLobby());
        }else {
            player.teleport(ghastWar.getLobby());
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(ghastWar.getConfig().getString("bungee.lobby","hub"));
            player.sendPluginMessage(ghastWar, "BungeeCord", out.toByteArray());
        }
    }

}
