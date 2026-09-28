package me.wang.happyGhastWar.events.ghast;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.game.team.Team;
import me.wang.happyGhastWar.ghast.GameGhast;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class GhastDeath implements Listener {

    private HappyGhastWar ghastWar;

    public GhastDeath(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void death(EntityDeathEvent e){
        if (e.getEntity() == null) return;
        if (e.getEntity().getType() != EntityType.HAPPY_GHAST) return;

        if (!HappyGhastWar.arenas.containsKey(e.getEntity().getLocation().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getEntity().getLocation().getWorld().getName());
        if (!arena.getGhasts().containsKey((HappyGhast) e.getEntity())) return;
        GameGhast ghast = arena.getGhasts().get((HappyGhast) e.getEntity());
        String killer_color = "";
        String killer_name = "";
        if (e.getEntity().getKiller() != null){
            for (Team team : arena.getTeams()){
                if (team.getPlayers().contains(e.getEntity().getKiller())){
                    killer_color = team.getTeams().getColor();
                    killer_name = e.getEntity().getKiller().getDisplayName();
                    break;
                }
            }
        }

        String finalKiller_color = killer_color;
        String finalKiller_name = killer_name;
        arena.getTeams().forEach(team -> {
            if (team.getGhasts().contains(ghast)){
                team.removeGhast(ghast);

                for (Player player : arena.getPlayers()){
                    String message = ghastWar.getLanguage(player).getContent("game.ghast-kill-chat")
                            .replace("{0}",team.getTeams().getColor().toString())
                            .replace("{1}",team.getTeams().getDisplayName())
                            .replace("{2}", finalKiller_color)
                            .replace("{3}", finalKiller_name);
                    Text.send(player, message);

                    // 显示被击杀乐魂的护甲等级（仅升级过时显示）
                    if (ghast.getArmorLevel() > 0) {
                        Text.send(player, "<yellow>护甲等级: <red>" + ghast.getArmorLevel());
                    }
                }
                checkGhast(team,arena);
            }
        });

        arena.removeGhast(ghast);
        ghast.unregister();

    }

    public void checkGhast(Team team,Arena arena){
        if (!team.getGhasts().isEmpty()) return;
        for (Player player : team.getPlayers()){
            player.sendTitle(Text.legacy(ghastWar.getLanguage(player).getContent("game.respawn-unavailable-title")),
                    Text.legacy(ghastWar.getLanguage(player).getContent("game.respawn-unavailable-subtitle")));
        }
        team.setCanRespawn(false);
        arena.getGameScoreboard().updateTeam(team);
    }
}
