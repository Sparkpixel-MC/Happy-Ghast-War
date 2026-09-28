package top.sparkpixel.hgw.commands.gw.impl.admin;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.ghast.GameGhast;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /gw admin upgradeghastarmor - 升级当前骑乘的乐魂护甲
 */
public class UpgradeGhastArmor extends GWCommand {

    public UpgradeGhastArmor(){
        super("upgradeghastarmor", true, "ua");
    }

    @Override
    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());

        if (arena == null) {
            player.sendMessage("你不在任何竞技场中");
            return;
        }

        // 检查玩家是否骑乘乐魂
        GameGhast gameGhast = null;
        for (GameGhast g : arena.getGhasts().values()) {
            if (g.getHappyGhast().getPassengers().contains(player)) {
                gameGhast = g;
                break;
            }
        }

        if (gameGhast == null) {
            player.sendMessage("你需要骑乘乐魂才能升级护甲");
            return;
        }

        // 升级护甲
        gameGhast.upgradeArmor(player);
    }
}
