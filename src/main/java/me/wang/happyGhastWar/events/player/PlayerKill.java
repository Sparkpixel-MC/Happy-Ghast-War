package me.wang.happyGhastWar.events.player;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.game.stats.GameStatistics;
import me.wang.happyGhastWar.util.SoundUtil;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 击杀奖励与击杀特效。
 * <p>
 * 死亡消息、击杀数记录与复活逻辑由 {@link PlayerDeath} 负责，
 * 本监听器只做三件事：击杀奖励、连杀公告、统计记录，避免与 PlayerDeath 双计。
 */
public class PlayerKill implements Listener {

    private final HappyGhastWar ghastWar;

    /** 连杀计数：killer -> 当前连杀数 */
    private final Map<UUID, Integer> killStreaks = new HashMap<>();

    /** 连杀公告阈值 */
    private static final int[] STREAK_THRESHOLDS = {2, 3, 5, 8};

    public PlayerKill(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void death(PlayerDeathEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getEntity().getLocation().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getEntity().getLocation().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        if (!arena.getPlayers().contains(e.getEntity())) return;

        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity();
        GameStatistics statistics = arena.getStatistics();

        // 被击杀者连杀清零
        killStreaks.remove(victim.getUniqueId());

        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        // 击杀统计（PlayerData.kills 在 PlayerDeath 中记录）
        if (statistics != null) {
            statistics.recordKill(killer);
        }

        // 击杀奖励：随机 2~5 个铁锭或 1 个下界合金碎片
        Random random = new Random();
        int rewardType = random.nextInt(3); // 0: 铁锭(2-5个), 1: 铁锭(2-5个), 2: 下界合金碎片(1个)

        if (rewardType < 2) {
            // 66% 概率获得铁锭
            int ironAmount = random.nextInt(4) + 2; // 2-5个
            ItemStack ironReward = new ItemStack(org.bukkit.Material.IRON_INGOT, ironAmount);
            killer.getInventory().addItem(ironReward);
            Text.send(killer, "<yellow>击杀奖励：+" + ironAmount + " 个铁锭");
        } else {
            // 33% 概率获得下界合金碎片
            ItemStack alloyReward = new ItemStack(org.bukkit.Material.NETHERITE_SCRAP, 1);
            killer.getInventory().addItem(alloyReward);
            Text.send(killer, "<gold>击杀奖励：+1 个下界合金碎片");
        }

        // 击杀音效和粒子特效
        SoundUtil.play(killer, "player-kill", Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        killer.getWorld().spawnParticle(Particle.FLAME, killer.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);

        // 连杀公告
        int streak = killStreaks.getOrDefault(killer.getUniqueId(), 0) + 1;
        killStreaks.put(killer.getUniqueId(), streak);
        announceStreak(arena, killer, streak);
    }

    private void announceStreak(Arena arena, Player killer, int streak) {
        String key = null;
        for (int threshold : STREAK_THRESHOLDS) {
            if (streak == threshold) {
                key = "game.kill-streak-" + threshold;
                break;
            }
        }
        if (key == null) return;

        String color = "";
        var team = arena.getPlayerTeam(killer);
        if (team != null) {
            color = team.getTeams().getColor().toString();
        }

        for (Player player : arena.getPlayers()) {
            String message = ghastWar.getLanguage(player).getContent(key)
                    .replace("{0}", color)
                    .replace("{1}", killer.getName());
            Text.send(player, message);
        }
    }

    /** 玩家退出/换世界时清理连杀计数 */
    @EventHandler
    public void quit(org.bukkit.event.player.PlayerQuitEvent e) {
        killStreaks.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void changeWorld(org.bukkit.event.player.PlayerChangedWorldEvent e) {
        killStreaks.remove(e.getPlayer().getUniqueId());
    }
}
