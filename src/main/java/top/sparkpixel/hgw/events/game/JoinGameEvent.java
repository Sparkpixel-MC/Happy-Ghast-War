package top.sparkpixel.hgw.events.game;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinGameEvent implements Listener {

    private HappyGhastWar ghastWar;

    public JoinGameEvent(HappyGhastWar ghastWar){
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void joinFromWorld(PlayerChangedWorldEvent e){
        World world = e.getPlayer().getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(world.getName());
        //System.out.println(arena.getName());
        //arena.addPlayer(e.getPlayer());
    }

    @EventHandler
    public void joinFromServer(PlayerJoinEvent e){
        // 主动调用一次，确保语言包已按该玩家 locale 缓存
        ghastWar.getLanguage(e.getPlayer());
    }
}
