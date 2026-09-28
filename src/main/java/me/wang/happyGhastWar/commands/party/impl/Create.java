package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.entity.Player;

import java.util.List;

public class Create extends PartyCommand {
    public Create(){
        super("create",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            Text.send(player, ghastWar.getLanguage(player).getContent("party.unable-use-in-game"));
            return;
        }
        Party party = ghastWar.getPartyManager().createParty(player);
        if (party == null) {
            Text.send(player, "<red>你已经在队伍中!");
            return;
        }

        Text.send(player, "<green>已创建队伍!");
        Text.send(player, "<yellow>使用 /party invite <玩家名> 邀请其他玩家");
    }
}
