package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class Decline extends PartyCommand {
    public Decline(){
        super("decline",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            Text.send(player, HappyGhastWar.language.getContent("party.unable-use-in-game"));
            return;
        }
        if (HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            Text.send(player, HappyGhastWar.language.getContent("party.unable-use-in-game"));
            return;
        }
        if (params.isEmpty()) return;
        String inviterName = params.get(0);
        Player inviter = Bukkit.getPlayer(inviterName);
        if (inviter == null || !inviter.isOnline()) {
            Text.send(player, "<red>玩家 " + inviterName + " 不在线!");
            return;
        }

        Party inviterParty = ghastWar.getPartyManager().getParty(inviter);
        if (inviterParty == null) {
            Text.send(player, "<red>" + inviterName + " 没有队伍!");
            return;
        }

        inviterParty.declineInvite(player);
    }
}
