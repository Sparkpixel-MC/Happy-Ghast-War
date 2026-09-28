package top.sparkpixel.hgw.events.ghast;

import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.util.SoundUtil;
import top.sparkpixel.hgw.util.Text;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class AirdropListener implements Listener {

    private final HappyGhastWar ghastWar;

    public AirdropListener(HappyGhastWar ghastWar) {
        this.ghastWar = ghastWar;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock().getType() != Material.CHEST) return;

        if (!(e.getClickedBlock().getState() instanceof Chest chest)) return;

        // 检查是否是空投箱子（PDC 标识，与 Arena.spawnAirdropCrate 对应）
        if (!isAirdropChest(chest)) return;

        e.setCancelled(true);
        handleAirdropOpen(e.getPlayer(), chest);
    }

    /**
     * 检查是否是空投箱子
     */
    private boolean isAirdropChest(Chest chest) {
        return chest.getPersistentDataContainer().has(HappyGhastWar.airdropKey, PersistentDataType.BYTE);
    }

    /**
     * 处理打开空投箱子
     */
    private void handleAirdropOpen(Player player, Chest chest) {
        Arena arena = null;

        // 找到玩家所在的竞技场
        for (Arena a : HappyGhastWar.arenas.values()) {
            if (a.getPlayers().contains(player)) {
                arena = a;
                break;
            }
        }

        if (arena == null) return;

        // 播放开启音效
        SoundUtil.play(player, "airdrop-open", Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, chest.getLocation().clone().add(0.5, 1, 0.5), 10, 0.5, 0.5, 0.5, 0);

        // 记录获得的物品
        List<String> obtainedItems = new ArrayList<>();

        // 遍历箱子内的所有物品
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                // 添加到玩家背包
                player.getInventory().addItem(item);

                // 记录物品信息
                String itemName = item.getType().name().toLowerCase().replace('_', ' ');
                obtainedItems.add("<gray>- " + item.getAmount() + "x " + itemName);
            }
        }

        // 发送获得物品列表
        if (!obtainedItems.isEmpty()) {
            Text.send(player, "<green><bold>【空投获得】<reset> 你获得了：");
            for (String item : obtainedItems) {
                Text.send(player, item);
            }
        } else {
            Text.send(player, "<gray>箱子是空的...");
        }

        // 广播空投被打开的信息
        String teamColor = "";
        if (arena.getPlayerTeam(player) != null) {
            teamColor = arena.getPlayerTeam(player).getTeams().getColor().toString();
        }
        for (Player arenaPlayer : arena.getPlayers()) {
            Text.send(arenaPlayer, teamColor + player.getName() + " <green>获得了空投补给！");
        }

        // 统计
        arena.getStatistics().recordAirdropCollected(player);

        // 额外经验奖励
        player.giveExp(50);
        Text.send(player, "<yellow>额外奖励：+50 经验值");

        // 清空并移除箱子（removeAirdrop 负责清空背包、移除方块与记录）
        chest.getInventory().clear();
        org.bukkit.Location blockLoc = chest.getBlock().getLocation();
        arena.consumeAirdrop(blockLoc);
        arena.removeAirdrop(blockLoc, null);

        // 移除附近漂浮的空投名称
        for (org.bukkit.entity.Entity entity : chest.getWorld().getNearbyEntities(blockLoc.clone().add(0.5, 1, 0.5), 2, 2, 2)) {
            if (entity instanceof org.bukkit.entity.TextDisplay textDisplay && "§6§l空投补给箱".equals(textDisplay.getText())) {
                entity.remove();
            }
        }
    }
}
