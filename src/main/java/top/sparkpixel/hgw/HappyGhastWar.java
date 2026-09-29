package top.sparkpixel.hgw;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.loaders.file.FileLoader;
import top.sparkpixel.hgw.arena.Arena;
import top.sparkpixel.hgw.arena.ArenaConfig;
import top.sparkpixel.hgw.arena.ArenaSelector;
import top.sparkpixel.hgw.commands.gw.GWCommandRouter;
import top.sparkpixel.hgw.commands.party.PartyCommandRouter;
import top.sparkpixel.hgw.events.ProxyDataEvent;
import top.sparkpixel.hgw.events.game.*;
import top.sparkpixel.hgw.events.ghast.AirdropListener;
import top.sparkpixel.hgw.events.player.GhastSkillStatusListener;
import top.sparkpixel.hgw.events.player.GhastSkillTrigger;
import top.sparkpixel.hgw.events.ghast.GhastDeath;
import top.sparkpixel.hgw.events.player.PlayerDeath;
import top.sparkpixel.hgw.events.player.PlayerKill;
import top.sparkpixel.hgw.game.party.PartyManager;
import top.sparkpixel.hgw.game.prop.ItemFunctions;
import top.sparkpixel.hgw.tab.TabManager;
import top.sparkpixel.hgw.util.Language;
import top.sparkpixel.hgw.util.Metrics;
import top.sparkpixel.hgw.util.SocketClient;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public final class HappyGhastWar extends JavaPlugin {

    public static FileConfiguration config;

    public static Language language;

    public static Map<String , Arena> arenas = new HashMap<>();

    public static Map<String, SlimeWorldInstance> slimeWorldInstances = new HashMap<>();

    public static Map<String, SlimeWorld> slimeWorlds = new HashMap<>();

    /** 空投箱子的标识键（PersistentDataContainer） */
    public static NamespacedKey airdropKey;

    public static ItemFunctions itemFunctions;

    private PartyManager partyManager;

    private ArenaSelector arenaSelector;

    public SlimeLoader loader;

    private TabManager tabManager;

    public List<ItemStack> chest_items = new ArrayList<>(Arrays.asList(
            new ItemStack(Material.WIND_CHARGE),
            new ItemStack(Material.FIREWORK_ROCKET),
            new ItemStack(Material.GOLDEN_APPLE),
            new ItemStack(Material.ARROW),
            new ItemStack(Material.SNOW_BLOCK),
            new ItemStack(Material.TOTEM_OF_UNDYING),
            new ItemStack(Material.IRON_INGOT),
            new ItemStack(Material.COAL),
            new ItemStack(Material.COPPER_INGOT)
    ));

    public SocketClient socketClient;

    public Map<String,Language> languageMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        int pluginId = 29369;
        Metrics metrics = new Metrics(this, pluginId);

        airdropKey = new NamespacedKey(this, "airdrop");

        socketClient = new SocketClient(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getLogger().info("Loading configs...");
        saveDefaultConfig();
        config = getConfig();
        language = loadLanguage("default",getConfig().getString("language","zh_cn.yml"));
        loadLanguage("zh_cn","zh_cn.yml");
        loadLanguage("en_us","en_us.yml");
        loadLanguage("zh_tw","zh_tw.yml");
        loadLanguage("de_de","de_de.yml");
        loadLanguage("es_es","es_es.yml");
        loadLanguage("fr_fr","fr_fr.yml");
        loadLanguage("ru_ru","ru_ru.yml");

        if (config.getBoolean("bungee.enable",false)){
            getLogger().info("Trying to connect to the Proxy Server...");
            // Socket 连接是阻塞 I/O，放到异步线程，避免卡主线程
            String host = config.getString("bungee.host");
            int port = config.getInt("bungee.port");
            String serverName = config.getString("bungee.serverName");
            getServer().getScheduler().runTaskAsynchronously(this, () -> socketClient.connect(host, port, serverName));
        }

        getLogger().info("Loading commands...");
        registerCommands();

        getLogger().info("Loading Arenas...");
        File arenaFolder = new File(getDataFolder(),"arenas");
        if (!arenaFolder.exists()){
            arenaFolder.mkdirs();
        }

        // Create and initialize SlimeLoader (must manage loaders ourselves)
        try {
            AdvancedSlimePaperAPI asp = AdvancedSlimePaperAPI.instance();
            // Use FileLoader - must shade this into plugin.
            // SlimeWorld 目录：默认为服务器根目录下的 slime_worlds（SlimeWorldManager 默认目录），
            // 可通过 config.yml 的 slime-worlds-folder 覆盖（相对路径基于服务器根目录）。
            String worldsFolder = config.getString("slime-worlds-folder", "slime_worlds");
            File slimeWorldsDir = new File(worldsFolder);
            if (!slimeWorldsDir.isAbsolute()) {
                slimeWorldsDir = new File(getDataFolder().getParentFile().getParent(), worldsFolder);
            }
            if (!slimeWorldsDir.exists()) {
                slimeWorldsDir.mkdirs();
            }
            loader = new FileLoader(slimeWorldsDir);
            getLogger().info("Initialized SlimeLoader at " + slimeWorldsDir.getAbsolutePath());

            File[] arenaFiles = arenaFolder.listFiles();
            if (arenaFiles != null) {
                for (File file : arenaFiles) {
                    if (!file.getName().endsWith(".yml")) {
                        // Only treat .yml files in the arenas folder as configs
                        continue;
                    }
                    // 每个竞技场独立 try/catch：单个竞技场失败不能中断其余竞技场的加载
                    try {
                        String fileName = file.getName();
                        String worldName = fileName.replace(".yml", "");

                        // Check whether the SlimeWorld exists in the loader's directory
                        // (<slime-worlds-folder>/<name>.slime), not the arenas folder.
                        if (!loader.worldExists(worldName)) {
                            getLogger().warning("SlimeWorld file not found for arena " + worldName
                                    + " in " + slimeWorldsDir.getAbsolutePath() + ", skipping...");
                            continue;
                        }

                        SlimeWorldInstance instance = asp.getLoadedWorld(worldName);
                        SlimeWorld world;
                        World bukkitWorld;
                        if (instance != null) {
                            // The world was already loaded before us (server bukkit.yml entry
                            // or another plugin); loadWorld() would throw "already loaded".
                            getLogger().warning("SlimeWorld " + worldName + " is already loaded, reusing it.");
                            world = instance;
                            bukkitWorld = instance.getBukkitWorld();
                        } else if (Bukkit.getWorld(worldName) != null) {
                            // A regular (non-slime) world with this name is loaded and ASP
                            // can't take it over. resetWorld() will swap in the slime world
                            // on first reset, so the arena can still run on it meanwhile.
                            getLogger().warning("World " + worldName + " is already loaded as a regular world, reusing it.");
                            world = null;
                            bukkitWorld = Bukkit.getWorld(worldName);
                        } else {
                            // Read world synchronously
                            world = asp.readWorld(loader, worldName, false, new SlimePropertyMap());

                            // Load world synchronously on main thread
                            instance = asp.loadWorld(world, true);
                            bukkitWorld = instance.getBukkitWorld();
                        }

                        ArenaConfig arenaConfig = new ArenaConfig(HappyGhastWar.this);
                        arenaConfig.loadArena(fileName);
                        Arena arena = new Arena(HappyGhastWar.this, bukkitWorld, arenaConfig);

                        arenas.put(bukkitWorld.getName(), arena);
                        if (world != null) {
                            slimeWorldInstances.put(worldName, instance);
                            slimeWorlds.put(worldName, world);
                        }
                        getLogger().info("Loaded arena " + worldName);
                    } catch (Exception e) {
                        getLogger().severe("Failed to load arena " + file.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            getLogger().info("Loaded " + arenas.size() + " arena(s) in total.");
        } catch (Exception e) {
            getLogger().severe("Failed to load arenas: " + e.getMessage());
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(new ClickFurnaceEvent(this),this);
        getServer().getPluginManager().registerEvents(new JoinGameEvent(this),this);
        getServer().getPluginManager().registerEvents(new LeaveGameEvent(this),this);
        getServer().getPluginManager().registerEvents(new GhastDeath(this),this);
        getServer().getPluginManager().registerEvents(new PlayerDeath(this),this);
        getServer().getPluginManager().registerEvents(new AirdropListener(this),this);
        getServer().getPluginManager().registerEvents(new GhastSkillStatusListener(this),this);
        getServer().getPluginManager().registerEvents(new GhastSkillTrigger(this),this);
        getServer().getPluginManager().registerEvents(new PlayerKill(this),this);
        getServer().getPluginManager().registerEvents(new Protection(this),this);
        getServer().getPluginManager().registerEvents(new Resource(this),this);
        getServer().getPluginManager().registerEvents(new ProxyDataEvent(this),this);
        getServer().getPluginManager().registerEvents(new Waiting(this),this);

        itemFunctions = new ItemFunctions(this);
        itemFunctions.startAllTasks();
        partyManager = new PartyManager();
        arenaSelector = new ArenaSelector(this);

        // 初始化 TAB 管理器（TAB 未安装时内部会安全降级）
        tabManager = new TabManager(this);
        tabManager.initialize();

    }

    public Language loadLanguage(String name,String filename){
        Language lang = new Language(this);
        lang.loadLanguage(filename);
        languageMap.put(name,lang);
        if (name.equals("default")){
            getLogger().info("Loaded language "+name);
        }
        return lang;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Unloading arenas...");

        // Stop arenas first so their periodic tasks stop touching the worlds.
        for (Arena arena : arenas.values()) {
            try {
                arena.stop();
            } catch (Exception e) {
                getLogger().severe("Failed to stop arena " + arena.getName() + ": " + e.getMessage());
            }
        }

        // Save + unload worlds loaded by this plugin.
        // Per AdvancedSlimePaper docs: save via the API (blocking) then unload via
        // Bukkit.unloadWorld(name, false) - do NOT use save=true during onDisable.
        AdvancedSlimePaperAPI api = AdvancedSlimePaperAPI.instance();
        for (SlimeWorld slimeWorld : slimeWorlds.values()) {
            String worldName = slimeWorld.getName();
            try {
                // Save world first (recommended by docs)
                api.saveWorld(slimeWorld);
                getLogger().info("Saved SlimeWorld " + worldName);
            } catch (Exception e) {
                getLogger().severe("Failed to save SlimeWorld " + worldName + ": " + e.getMessage());
            }
            try {
                Bukkit.unloadWorld(worldName, false);
                getLogger().info("Unloaded SlimeWorld " + worldName);
            } catch (Exception e) {
                getLogger().severe("Failed to unload SlimeWorld " + worldName + ": " + e.getMessage());
            }
        }
        slimeWorlds.clear();
        slimeWorldInstances.clear();

        if (tabManager != null) {
            tabManager.shutdown();
        }

        socketClient.disconnect();
    }

    public void registerCommands(){
        PluginCommand pluginCommand = this.getCommand("ghastwar");
        if (pluginCommand != null) {
            GWCommandRouter router = new GWCommandRouter(this);
            pluginCommand.setExecutor(router);
            pluginCommand.setTabCompleter(router);
        }
        PluginCommand partyCommand = this.getCommand("party");
        if (partyCommand != null) {
            PartyCommandRouter router = new PartyCommandRouter(this);
            partyCommand.setExecutor(router);
            partyCommand.setTabCompleter(router);
        }
    }

    public static HappyGhastWar getInstance(){
        return HappyGhastWar.getPlugin(HappyGhastWar.class);
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public static Map<String, Arena> getArenas() {
        return arenas;
    }

    public ArenaSelector getArenaSelector() {
        return arenaSelector;
    }

    public Location translateLocation(String raw){
        String[] data = raw.split(",");
        World world = Bukkit.getWorld(data[0]);
        double x = Double.parseDouble(data[1]);
        double y = Double.parseDouble(data[2]);
        double z = Double.parseDouble(data[3]);
        return new Location(world,x,y,z);
    }

    public Location getLobby(){
        String s = config.getString("lobby","unknown");
        if (s.equals("unknown")){
            World world = Bukkit.getWorld("world");
            if (world == null){
                // 没有名为 world 的主世界时，取第一个已加载世界
                world = Bukkit.getWorlds().getFirst();
            }
            return world.getSpawnLocation();
        }
        return translateLocation(s);
    }

    public void setLobby(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("lobby",loc);
        saveConfig();
        reloadConfig();
        config = getConfig();
    }

    /**
     * 按玩家客户端语言返回语言包，找不到则回退到默认语言。
     * 控制台/占位符等非玩家上下文传 null 时同样返回默认语言。
     */
    public Language getLanguage(Player player){
        if (player == null){
            return language;
        }
        String locale = player.getLocale();
        if (locale == null){
            return language;
        }
        String key = locale.toLowerCase(Locale.ROOT);
        Language lang = languageMap.get(key);
        if (lang == null && key.contains("_")){
            lang = languageMap.get(key.split("_")[0]);
        }
        if (lang == null){
            return language;
        }
        return lang;
    }

    public SocketClient getSocketClient() {
        return socketClient;
    }

    public TabManager getTabManager() {
        return tabManager;
    }
}
