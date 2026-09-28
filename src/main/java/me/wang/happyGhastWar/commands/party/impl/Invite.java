package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class Invite extends PartyCommand {
    public Invite(){
        super("invite",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            Text.send(player, HappyGhastWar.language.getContent("party.unable-use-in-game"));
            return;
        }
        Party party = ghastWar.getPartyManager().getParty(player);
        if (party == null) {
            Text.send(player, "<red>你没有队伍! 使用 /party create 创建队伍");
            return;
        }
        if (params.isEmpty()) return;
        Player target = Bukkit.getPlayer(params.get(0));
        if (target == null || !target.isOnline()) {
            Text.send(player, "<red>玩家 " + params.get(0) + " 不在线!");
            return;
        }

        party.invitePlayer(player, target);
    }
}
