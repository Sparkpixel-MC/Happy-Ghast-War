package top.sparkpixel.hgw.events.player;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.ghast.GameGhast;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class GhastSkillStatusListener implements Listener {

    private final HappyGhastWar ghastWar;

    public GhastSkillStatusListener(HappyGhastWar ghastWar) {
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void onGhastInteract(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof HappyGhast)) return;
        if (!HappyGhastWar.arenas.containsKey(e.getRightClicked().getWorld().getName())) return;
        // 潜行+右键由 GhastSkillTrigger 触发技能，这里只处理普通右键（显示状态）
        if (e.getPlayer().isSneaking()) return;

        Player player = e.getPlayer();
        HappyGhast ghast = (HappyGhast) e.getRightClicked();
        Arena arena = HappyGhastWar.arenas.get(ghast.getWorld().getName());

        // 检查玩家是否是乐魂的骑手（本队成员）
        // 注意：team.getGhasts() 存的是 GameGhast 包装对象，需先由实体取出包装再比较
        GameGhast targetGhast = arena.getGhasts().get(ghast);
        if (targetGhast != null && arena.getPlayerTeam(player) != null
                && arena.getPlayerTeam(player).getGhasts().contains(targetGhast)) {
            // 显示技能状态
            showSkillStatus(arena, ghast, player);
            e.setCancelled(true);
        }
    }

    /**
     * 显示技能状态
     */
    private void showSkillStatus(Arena arena, HappyGhast ghast, Player player) {
        GameGhast gameGhast = null;
        for (GameGhast g : arena.getGhasts().values()) {
            if (g.getHappyGhast().equals(ghast)) {
                gameGhast = g;
                break;
            }
        }

        if (gameGhast == null) return;

        // 获取技能状态
        String skillStatus = gameGhast.getSkillCooldownInfo();

        // 发送技能状态消息
        Text.send(player, "<gold>=== 乐魂技能状态 ===");
        Text.send(player, "<yellow>技能: " + skillStatus);
        Text.send(player, "<yellow>护甲: " + gameGhast.getArmorUpgradeInfo());
        Text.send(player, "<gray>潜行+右键点击乐魂使用技能");
    }
}