package me.wang.happyGhastWar.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.game.party.Party;

public class MessageTranslate {

    private final HappyGhastWar ghastWar;

    public MessageTranslate(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    public JsonObject FromArena(Arena arena){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("server",ghastWar.getConfig().getString("bungee.serverName","null"));
        jsonObject.addProperty("type","arena");
        jsonObject.addProperty("status",arena.status.name());
        jsonObject.addProperty("name",arena.getName());
        jsonObject.addProperty("arena",arena.getWorld().getName());
        jsonObject.addProperty("team_size",arena.getArenaConfig().getTeamSize());
        jsonObject.addProperty("team_count",arena.getArenaConfig().getTeamCount());
        jsonObject.addProperty("player_size",arena.getPlayers().size());
        return jsonObject;
    }

    public JsonObject FromParty(Party party){
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("server",ghastWar.getConfig().getString("bungee.serverName","null"));
        jsonObject.addProperty("type","party");
        jsonObject.addProperty("uuid",party.getPartyId().toString());
        jsonObject.addProperty("members",gson.toJson(party.members));
        jsonObject.addProperty("leader",party.leader.getUniqueId().toString());
        return jsonObject;
    }
}
