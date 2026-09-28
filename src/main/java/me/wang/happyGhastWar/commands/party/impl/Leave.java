package me.wang.happyGhastWar.commands.party.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.commands.party.PartyCommand;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.entity.Player;

import java.util.List;

public class Leave extends PartyCommand {
    public Leave(){
        super("leave",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            Text.send(player, HappyGhastWar.language.getContent("party.unable-use-in-game"));
            return;
        }
        if (ghastWar.getPartyManager().leaveParty(player)) {
            Text.send(player, "<yellow>已离开队伍!");
        } else {
            Text.send(player, "<red>你不在队伍中!");
        }
    }
}
