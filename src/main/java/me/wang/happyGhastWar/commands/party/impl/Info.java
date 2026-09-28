package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class Info extends PartyCommand {
    public Info(){
        super("info",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        Party party = ghastWar.getPartyManager().getParty(player);
        if (party == null) {
            Text.send(player, "<yellow>你不在任何队伍中");
            return;
        }

        Text.send(player, "<gold>=== 队伍信息 ===");
        Text.send(player, "<yellow>队长: <green>" + party.getLeader().getName());
        Text.send(player, "<yellow>成员 (" + party.getMemberCount() + "/" + party.getMaxSize() + "):");

        for (java.util.Map.Entry<java.util.UUID, Party.PartyRole> entry : party.getMembers().entrySet()) {
            Player member = Bukkit.getPlayer(entry.getKey());
            if (member != null) {
                String role = entry.getValue() == Party.PartyRole.LEADER ?
                        "<red>[队长]" : "<gray>[队员]";
                Text.send(player, "  " + role + " <white>" + member.getName());
            }
        }
    }
}
