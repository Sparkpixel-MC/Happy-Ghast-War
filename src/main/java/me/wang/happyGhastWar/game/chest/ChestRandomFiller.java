package me.wang.happyGhastWar.game.chest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

/**
 * 箱子物品随机放置器
 * 用于向指定箱子随机放入指定物品
 */
public class ChestRandomFiller {

    private static final Random RANDOM = new Random();

    public void fillChestsRandomly(List<Location> chestLocations, List<ItemStack> possibleItems) {
        if (chestLocations == null || chestLocations.isEmpty()) {
            Bukkit.getLogger().warning("箱子坐标列表为空或null！");
            return;
        }

        if (possibleItems == null || possibleItems.isEmpty()) {
            Bukkit.getLogger().warning("物品列表为空或null！");
            return;
        }


        int filledChests = 0;
        for (Location location : chestLocations) {
            if (fillSingleChestRandomly(location, possibleItems)) {
                filledChests++;
            }
        }

    }


    private boolean fillSingleChestRandomly(Location location, List<ItemStack> possibleItems) {
        try {
            // 确保世界已加载
            if (location.getWorld() == null) {
                Bukkit.getLogger().warning("世界未加载！位置：" + location);
                return false;
            }

            Block block = location.getBlock();

            if (!(block.getState() instanceof Container container)) {
                Bukkit.getLogger().warning("位置 " + location + " 处的方块不是容器！方块类型：" + block.getType());
                return false;
            }

            Inventory inventory = container.getInventory();

            // 清空箱子
            inventory.clear();

            // 随机生成物品数量（6-10个）
            int itemCount = getRandomNumber(6, 10);

            int placedItems = 0;
            for (int i = 0; i < itemCount; i++) {
                // 随机选择一个物品
                ItemStack randomItem = getRandomItem(possibleItems);

                if (randomItem == null || randomItem.getType() == Material.AIR) {
                    continue;
                }

                // 创建物品副本
                ItemStack itemToAdd = randomItem.clone();

                // 随机数量（1-物品最大堆叠数）
                int maxStackSize = itemToAdd.getType().getMaxStackSize();
                int randomAmount = getRandomNumber(1, Math.min(maxStackSize, 16));
                itemToAdd.setAmount(randomAmount);

                // 放置物品
                if (placeItemInRandomSlot(inventory, itemToAdd)) {
                    placedItems++;
                }
            }

            return true;

        } catch (Exception e) {
            Bukkit.getLogger().severe("填充箱子时发生错误，位置：" + location);
            e.printStackTrace();
            return false;
        }
    }


    private boolean placeItemInRandomSlot(Inventory inventory, ItemStack item) {
        int maxAttempts = 50;
        int attempts = 0;

        while (attempts < maxAttempts) {
            // 随机选择一个槽位
            int randomSlot = RANDOM.nextInt(inventory.getSize());

            // 检查槽位是否为空
            ItemStack slotItem = inventory.getItem(randomSlot);
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                inventory.setItem(randomSlot, item);
                return true;
            }

            // 或者合并到已有物品堆叠中
            if (slotItem.isSimilar(item) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                int totalAmount = slotItem.getAmount() + item.getAmount();
                int maxStackSize = slotItem.getMaxStackSize();

                if (totalAmount <= maxStackSize) {
                    slotItem.setAmount(totalAmount);
                    inventory.setItem(randomSlot, slotItem);
                    return true;
                } else {
                    // 如果超出最大堆叠
                    int remaining = totalAmount - maxStackSize;
                    slotItem.setAmount(maxStackSize);
                    inventory.setItem(randomSlot, slotItem);
                    item.setAmount(remaining);
                    attempts++;
                    continue;
                }
            }

            attempts++;
        }

        // 如果找不到合适槽位，尝试按顺序放置
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slotItem = inventory.getItem(i);
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                inventory.setItem(i, item);
                return true;
            }
        }

        Bukkit.getLogger().warning("箱子已满，无法放置物品：" + item.getType());
        return false;
    }


    private ItemStack getRandomItem(List<ItemStack> possibleItems) {
        if (possibleItems.isEmpty()) {
            return null;
        }
        int index = RANDOM.nextInt(possibleItems.size());
        return possibleItems.get(index);
    }

    private int getRandomNumber(int min, int max) {
        return RANDOM.nextInt(max - min + 1) + min;
    }
}