package me.wang.happyGhastWar.events.player;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.game.player.PlayerData;
import me.wang.happyGhastWar.game.team.Team;
import me.wang.happyGhastWar.util.Language;
import me.wang.happyGhastWar.util.Text;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerDeath implements Listener {

    private HappyGhastWar ghastWar;

    public PlayerDeath(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    private List<Material> keepItems = new ArrayList<>(Arrays.asList(
            Material.OAK_LOG,
            Material.IRON_INGOT,
            Material.COPPER_INGOT,
            Material.COAL,
            Material.NETHERITE_INGOT,
            Material.RAW_IRON,
            Material.RAW_COPPER,
            Material.SNOW_BLOCK
    ));

    @EventHandler
    public void death(PlayerDeathEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getEntity().getLocation().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getEntity().getLocation().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        if (!arena.getPlayers().contains(e.getEntity())) return;
        Player killer = e.getEntity().getKiller();
        if (e.getEntity().getKiller() != null){

            Map<Material,Integer> items = new HashMap<>();
            for (ItemStack itemStack : e.getEntity().getInventory().getContents()){
                if (itemStack == null) continue;
                if (!keepItems.contains(itemStack.getType())) continue;
                killer.getInventory().addItem(itemStack);
                if (!items.containsKey(itemStack.getType())){
                    items.put(itemStack.getType(),itemStack.getAmount());
                }else {
                    items.put(itemStack.getType(),items.get(itemStack.getType())+itemStack.getAmount());
                }
            }
            Player finalKiller = killer;
            items.forEach((item, integer) -> {
                String message = ghastWar.getLanguage(finalKiller).getContent("game.reward-add").replace("{0}",String.valueOf(integer));
                finalKiller.spigot().sendMessage(new TranslatableComponent(item.translationKey()),new TextComponent(Text.legacy(message)));
            });
        }
        e.setKeepInventory(true);
        e.getEntity().getInventory().clear();
        e.getEntity().spigot().respawn();

        Team team = null;

        Player victim = e.getEntity();
        String killer_color = "";
        String killer_name = "";
        for (Team team1 : arena.getTeams()){
            if (team1.getPlayers().contains(e.getEntity())){
                team = team1;
            }
            if (killer != null){
                if (team1.getPlayers().contains(killer)){
                    killer_color = team1.getTeams().getColor().toString();
                    killer_name = killer.getDisplayName();
                }
            }
        }
        if (team == null) return;

        // 记录死亡统计
        arena.getStatistics().recordDeath(victim);

        if (killer != null){
            PlayerData data = arena.getPlayerDatas().get(killer);
            if (data != null){
                data.addKill(1);
                arena.getPlayerDatas().put(killer,data);
            }
        }

        Language language = HappyGhastWar.language;
        String message = !team.isCanRespawn() ? language.getContent("game.player-die-unknown-final") : language.getContent("game.player-die-unknown-regular");
        EntityDamageEvent damageEvent = e.getEntity().getLastDamageCause();

        if (damageEvent != null) {
            if (damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {

                if (killer == null) {
                    message = !team.isCanRespawn() ? language.getContent("game.player-die-bomb2-final") : language.getContent("game.player-die-bomb2-regular");
                } else {
                    if (killer != victim) {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-bomb-final") : language.getContent("game.player-die-bomb-regular");
                    } else {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-bomb-final") : language.getContent("game.player-die-bomb-regular");
                    }
                }

            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.VOID) {

                if (killer == null) {
                    message = !team.isCanRespawn() ? language.getContent("game.player-die-void-final") : language.getContent("game.player-die-void-regular");
                } else {
                    if (killer != victim) {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-knocked-void-final") : language.getContent("game.player-die-knocked-void-regular");
                    } else {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-void-final") : language.getContent("game.player-die-void-regular");
                    }
                }
            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                if (killer != null) {
                    message = !team.isCanRespawn() ? language.getContent("game.player-die-attack-final") : language.getContent("game.player-die-attack-regular");
                }
            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                if (killer != null) {
                    message = !team.isCanRespawn() ? language.getContent("game.player-die-shoot-final") : language.getContent("game.player-die-shoot-regular");
                }
            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.FALL) {

                if (killer != null && killer.getUniqueId().equals(victim.getUniqueId())) killer = null;
                if (killer != null) {
                    if (killer != victim) {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-knocked-fall-final") : language.getContent("game.player-die-knocked-fall-regular");
                    } else {
                        message = !team.isCanRespawn() ? language.getContent("game.player-die-void-final") : language.getContent("game.player-die-void-regular");
                    }
                }
            }
        }

        e.setDeathMessage(null);
        message = message
                .replace("{0}",team.getTeams().getColor().toString())
                .replace("{1}",victim.getDisplayName())
                .replace("{2}",killer_color)
                .replace("{3}",killer_name);
        for (Player player : arena.getPlayers()){
            Text.send(player, message);
        }
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getPlayer().getLocation().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getPlayer().getLocation().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        if (!arena.getPlayers().contains(e.getPlayer())) return;
        arena.upgradeGUI.upgradeManager.removePlayer(e.getPlayer());
        arena.getTeams().forEach(team -> {
            if (team.getPlayers().contains(e.getPlayer())){
                e.setRespawnLocation(arena.getArenaConfig().getSpawn(team.getTeams()));
                e.getPlayer().setGameMode(GameMode.SPECTATOR);
                if (team.isCanRespawn()){
                    e.getPlayer().sendTitle(Text.legacy(ghastWar.getLanguage(e.getPlayer()).getContent("game.respawn-title")),Text.legacy(HappyGhastWar.language.getContent("game.respawn-subtitle")));
                    Bukkit.getServer().getScheduler().runTaskLater(HappyGhastWar.getPlugin(HappyGhastWar.class), ()->{
                        e.getPlayer().teleport(arena.getArenaConfig().getSpawn(team.getTeams()));
                        arena.giveEquipment(e.getPlayer(), team);
                        e.getPlayer().setGameMode(GameMode.SURVIVAL);
                    },3 * 20);
                }else {
                    e.getPlayer().sendTitle(Text.legacy(ghastWar.getLanguage(e.getPlayer()).getContent("game.cant-respawn-title")),Text.legacy(HappyGhastWar.language.getContent("game.cant-respawn-subtitle")));
                    team.removePlayer(e.getPlayer());
                    arena.getGameScoreboard().updateTeam(team);
                }

            }
        });
    }

    @EventHandler
    public void move(PlayerMoveEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getPlayer().getLocation().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getPlayer().getLocation().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.PLAYING) return;
        if (!arena.getPlayers().contains(e.getPlayer())) return;
        if (e.getPlayer().getLocation().getY() > -100) return;
        e.getPlayer().damage(100.0);
    }
}
