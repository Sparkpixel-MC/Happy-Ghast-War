package top.sparkpixel.hgw.commands.party.impl;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.commands.party.PartyCommand;
import top.sparkpixel.hgw.game.party.Party;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.entity.Player;

import java.util.List;

public class Transfer extends PartyCommand {
    public Transfer(){
        super("transfer",true, new String[0]);
    }

    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        if (HappyGhastWar.arenas.containsKey(player.getWorld().getName())){
            Text.send(player, HappyGhastWar.language.getContent("party.unable-use-in-game"));
            return;
        }
        if (params.isEmpty()) return;
        Party party = ghastWar.getPartyManager().getParty(player);
        if (party == null) {
            Text.send(player, "<red>你没有队伍!");
            return;
        }

        party.transferLeadership(player, params.get(0));
    }
}
