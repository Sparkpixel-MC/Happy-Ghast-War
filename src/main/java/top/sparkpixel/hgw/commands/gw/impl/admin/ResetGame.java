package top.sparkpixel.hgw.commands.gw.impl.admin;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.commands.gw.GWCommand;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /gw admin resetgame - 强制重置当前竞技场的对局（世界重载 + 状态复位）
 */
public class ResetGame extends GWCommand {

    public ResetGame(){
        super("resetgame", true, "reset");
    }

    @Override
    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());

        if (arena == null) {
            player.sendMessage("你不在任何竞技场中");
            return;
        }

        // 检查权限
        if (!player.hasPermission("happyghastwar.admin.reset") && !player.hasPermission("happyghastwar.admin")) {
            player.sendMessage("你没有权限执行此操作");
            return;
        }

        try {
            Text.send(player, "<yellow>游戏重置中，正在重载世界...");
            arena.forceReset();
            Text.send(player, "<green>游戏重置成功！");
            Bukkit.broadcastMessage(Text.legacy("<gold>游戏已重置，正在重新加载竞技场..."));
        } catch (Exception e) {
            Text.send(player, "<red>重置失败: " + e.getMessage());
            ghastWar.getLogger().severe("游戏重置失败: " + e.getMessage());
        }
    }
}
