package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class Accept extends PartyCommand {
    public Accept(){
        super("accept",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            Text.send(player, HappyGhastWar.language.getContent("party.unable-use-in-game"));
            return;
        }
        Party party = ghastWar.getPartyManager().getParty(player);
        if (party != null) {
            Text.send(player, "<red>你已经在队伍中!");
            return;
        }
        if (params.isEmpty()) return;
        String inviterName = params.get(0);
        // 查找邀请者的队伍
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

        if (inviterParty.acceptInvite(player)) {
            ghastWar.getPartyManager().playerParties.put(player.getUniqueId(), inviterParty);
        }
    }
}
