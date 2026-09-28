package top.sparkpixel.hgw.game.player;

import top.sparkpixel.hgw.game.team.Team;
import org.bukkit.entity.Player;

public class PlayerData {
    private int kills;
    private final Player player;
    private final Team team;

    public PlayerData(Player player,Team team){
        this.player = player;
        this.team = team;
        this.kills = 0;
    }

    public Team getTeam() {
        return team;
    }

    public Player getPlayer() {
        return player;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void addKill(int kills){
        this.kills = this.kills + kills;
    }
}
