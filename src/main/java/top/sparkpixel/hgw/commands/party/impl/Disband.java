package top.sparkpixel.hgw.commands.party.impl;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.commands.party.PartyCommand;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.entity.Player;

import java.util.List;

public class Disband extends PartyCommand {
    public Disband(){
        super("disband",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (ghastWar.getPartyManager().disbandParty(player)) {
            Text.send(player, "<red>已解散队伍!");
        } else {
            Text.send(player, "<red>你不是队长或没有队伍!");
        }
    }
}
