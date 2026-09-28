package me.wang.happyGhastWar.commands.gw.impl.admin;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.arena.ArenaConfig;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

public class Create extends GWCommand {
    public Create(){
        super("create");
    }

    public void evaluate(HappyGhastWar ghastWar, Player sender, String s, List<String> params) throws IOException {
        if (params.size() < 2){
            Text.send(sender, ghastWar.getLanguage(sender).getContent("commands.missRequireData"));
            return;
        }
        sender.sendMessage("Creating...");
        String arenaName = params.get(0);
        String mapName = params.get(1);

        // 防止重复创建同一世界
        if (HappyGhastWar.arenas.containsKey(arenaName)){
            Text.send(sender, ghastWar.getLanguage(sender).getContent("commands.gameCreateSuccess").replace("&a","&c 已存在"));
            return;
        }

        // 加载器必须先初始化（例如 onEnable 成功）才能进行检查/读取
        SlimeLoader loader = ghastWar.loader;
        if (loader == null){
            Text.send(sender, ghastWar.getLanguage(sender).getContent("commands.world-load-failed"));
            return;
        }

        // 检查加载器的目录中是否存在 Slime 世界
        // （config.yml 的 slime-worlds-folder，默认为服务器根目录 slime_worlds）
        if (!loader.worldExists(arenaName)){
            Text.send(sender, ghastWar.getLanguage(sender).getContent("commands.world-not-found"));
            sender.sendMessage("请先将 " + arenaName + ".slime 放入服务端 slime_worlds 目录后再试（/swm create " + arenaName + "）");
            return;
        }

        // 检测是否已经加载
        World world = ghastWar.getServer().getWorld(arenaName);
        if (world == null){
            Text.send(sender, ghastWar.getLanguage(sender).getContent("commands.world-loading"));
            // 尝试加载世界
            try {
                AdvancedSlimePaperAPI api = AdvancedSlimePaperAPI.instance();
                SlimeWorld slimeWorld = api.readWorld(loader, arenaName, false, new SlimePropertyMap());
                SlimeWorldInstance instance = api.loadWorld(slimeWorld, true);
                world = instance.getBukkitWorld();

                // 在插件中注册，确保在卸载/注册时为异步操作
                HappyGhastWar.slimeWorlds.put(arenaName, slimeWorld);
                HappyGhastWar.slimeWorldInstances.put(arenaName, instance);
                Text.send(sender, ghastWar.getLanguage(sender).getContent("commands.world-loaded"));
            } catch (Exception e) {
                Text.send(sender, ghastWar.getLanguage(sender).getContent("commands.world-load-failed") + " " + e.getMessage());
                return;
            }
        }

        sender.teleport(world.getSpawnLocation());

        world.setGameRule(GameRule.DO_MOB_SPAWNING,false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN,true);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING,false);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING,false);
        world.setGameRule(GameRule.DO_WARDEN_SPAWNING,false);
        world.setGameRule(GameRule.DO_FIRE_TICK,false);

        sender.setGameMode(GameMode.CREATIVE);

        // 创建配置
        ArenaConfig arenaConfig = new ArenaConfig(ghastWar);
        arenaConfig.loadArena(arenaName + ".yml");
        arenaConfig.init(mapName, world);

        // 创建实例
        Arena arena = new Arena(ghastWar, world, arenaConfig);
        arena.backupWorld();
        HappyGhastWar.arenas.put(arenaName, arena);

        Text.send(sender, ghastWar.getLanguage(sender).getContent("commands.gameCreateSuccess"));
    }
}
