package top.sparkpixel.hgw.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.api.ClientDataReceiveEvent;
import top.sparkpixel.hgw.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProxyDataEvent implements Listener {

    private static Map<String,JsonObject> joinMap = new HashMap<>();

    private HappyGhastWar ghastWar;

    public ProxyDataEvent(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void proxydata(ClientDataReceiveEvent e){
        try {
            JsonObject jsonObject = JsonParser.parseString(e.getData()).getAsJsonObject();
            if (!jsonObject.has("type")) return;
            switch (jsonObject.get("type").getAsString()){
                case "join" -> {
                    if (jsonObject.has("player")) {
                        joinMap.put(jsonObject.get("player").getAsString(),jsonObject);
                    }
                }
            }
        } catch (Exception ex) {
            ghastWar.getLogger().warning("Received malformed proxy data: " + e.getData());
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent e){
        if (joinMap.containsKey(e.getPlayer().getUniqueId().toString())){
            handleJoin(joinMap.get(e.getPlayer().getUniqueId().toString()));
            joinMap.remove(e.getPlayer().getUniqueId().toString());
        }
    }

    public void handleJoin(JsonObject jsonObject){
        Player player;
        try {
            player = Bukkit.getPlayer(UUID.fromString(jsonObject.get("player").getAsString()));
        } catch (Exception ex) {
            ghastWar.getLogger().warning("Invalid join payload, missing player uuid.");
            return;
        }
        if (player == null) return;
        if (!HappyGhastWar.arenas.containsKey(jsonObject.get("arena").getAsString())) return;
        Arena arena = HappyGhastWar.arenas.get(jsonObject.get("arena").getAsString());
        arena.internalAddPlayer(player);
    }
}
