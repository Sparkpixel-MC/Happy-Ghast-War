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

public class GhastSkillTrigger implements Listener {

    private final HappyGhastWar ghastWar;

    public GhastSkillTrigger(HappyGhastWar ghastWar) {
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void onGhastInteract(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof HappyGhast)) return;
        if (!HappyGhastWar.arenas.containsKey(e.getRightClicked().getWorld().getName())) return;
        // 仅潜行+右键触发技能；普通右键由 GhastSkillStatusListener 显示状态
        if (!e.getPlayer().isSneaking()) return;

        Player player = e.getPlayer();
        HappyGhast ghast = (HappyGhast) e.getRightClicked();
        Arena arena = HappyGhastWar.arenas.get(ghast.getWorld().getName());

        // 检查玩家是否是乐魂的骑手（本队成员）
        // 注意：team.getGhasts() 存的是 GameGhast 包装对象，需先由实体取出包装再比较
        GameGhast targetGhast = arena.getGhasts().get(ghast);
        if (targetGhast != null && arena.getPlayerTeam(player) != null
                && arena.getPlayerTeam(player).getGhasts().contains(targetGhast)) {
            // 触发技能
            triggerGhastSkill(arena, ghast, player);
            e.setCancelled(true);
        }
    }

    /**
     * 触发乐魂技能
     */
    private void triggerGhastSkill(Arena arena, HappyGhast ghast, Player player) {
        GameGhast gameGhast = null;
        for (GameGhast g : arena.getGhasts().values()) {
            if (g.getHappyGhast().equals(ghast)) {
                gameGhast = g;
                break;
            }
        }

        if (gameGhast == null) return;

        // 使用技能
        gameGhast.useSkill(player);

        // 显示冷却信息
        String cooldownInfo = gameGhast.getSkillCooldownInfo();
        Text.send(player, "<gold>技能状态: " + cooldownInfo);
    }
}