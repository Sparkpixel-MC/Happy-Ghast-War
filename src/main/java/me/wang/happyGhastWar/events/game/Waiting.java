package me.wang.happyGhastWar.events.game;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.game.party.PartyManager;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public class Waiting implements Listener {

    private final HappyGhastWar ghastWar;

    public Waiting(HappyGhastWar happyGhastWar){
        this.ghastWar = happyGhastWar;
    }

    private void leave(Player player){
        World world = player.getWorld();
        if (!HappyGhastWar.arenas.containsKey(world.getName())){
            Text.send(player, ghastWar.getLanguage(player).getContent("commands.gameNotFound"));
            return;
        }

        Arena arena = HappyGhastWar.arenas.get(world.getName());

        Party party = HappyGhastWar.getInstance().getPartyManager().getParty(player);

        if (party != null && party.isLeader(player)) {
            // 队长加入，检查整个Party
            List<Player> partyMembers = party.getOnlineMembers();

            for (Player member : partyMembers) {
                tpToLobby(member,ghastWar);
                arena.removePlayer(member);
            }
        } else {

            // 如果是Party成员但不是队长，检查队长是否已经加入
            if (party != null && !party.isLeader(player)) {
                if (arena.getPlayers().contains(party.getLeader())) {
                    Text.send(player, "<red>请等待队长 " + party.getLeader().getName() + " 先离开游戏!");
                    return;
                }
            }
            tpToLobby(player,ghastWar);
            arena.removePlayer(player);
        }
    }

    public void tpToLobby(Player player,HappyGhastWar ghastWar){
        if (!ghastWar.getConfig().getBoolean("bungee.enable",false)){
            player.teleport(ghastWar.getLobby());
        }else {
            player.teleport(ghastWar.getLobby());
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(ghastWar.getConfig().getString("bungee.lobby","hub"));
            player.sendPluginMessage(ghastWar, "BungeeCord", out.toByteArray());
        }
    }

    @EventHandler
    public void clickInv(InventoryClickEvent e){
        if (e.getClickedInventory() == null) return;
        Player player = (Player) e.getWhoClicked();
        if (!HappyGhastWar.arenas.containsKey(player.getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.WAIT) return;
        e.setCancelled(true);
        if (e.getCurrentItem() != null){
            if (e.getCurrentItem().getType() == Material.RED_BED){
                leave(player);
            }
        }
    }

    @EventHandler
    public void clickbed(PlayerInteractEvent e){
        Player player = e.getPlayer();
        if (!HappyGhastWar.arenas.containsKey(player.getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.WAIT) return;
        e.setCancelled(true);
        if (e.hasItem()){
            if (e.getItem() != null && e.getItem().getType() == Material.RED_BED){
                leave(player);
            }
        }
    }

    @EventHandler
    public void damage(EntityDamageEvent e){
        if (!HappyGhastWar.arenas.containsKey(e.getEntity().getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(e.getEntity().getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.WAIT && arena.status != Arena.GameStatus.COUNTING) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void move(PlayerMoveEvent e){
        Player player = e.getPlayer();
        if (!HappyGhastWar.arenas.containsKey(player.getWorld().getName())) return;
        Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
        if (!arena.isEnable()) return;
        if (arena.status != Arena.GameStatus.WAIT && arena.status != Arena.GameStatus.COUNTING) return;
        if (player.getLocation().getY() > -100) return;
        ArenaConfig arenaConfig = arena.getArenaConfig();
        player.teleport(arenaConfig.getWait());
    }
}
