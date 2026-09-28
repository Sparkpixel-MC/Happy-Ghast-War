package top.sparkpixel.hgw.arena;

import top.sparkpixel.hgw.HappyGhastWar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class ArenaConfig {
    private final HappyGhastWar main;
    private File file;
    private YamlConfiguration config;
    private final Logger logger;
    private String name;

    public ArenaConfig(HappyGhastWar ghastWar){
        this.main = ghastWar;
        this.logger = ghastWar.getLogger();
    }

    public void loadArena(String name){
        this.name = name;
        try{
            file = new File(main.getDataFolder()+"/arenas",name);
            config = YamlConfiguration.loadConfiguration(file);
            config.save(file);
            logger.info("Successfully loaded Arena " + name);
        } catch (IOException e) {
            logger.severe("Failed to load Arena "+name);
            throw new RuntimeException(e);
        }

    }

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void reload(){
        if (config == null){
            return;
        }
        try {
            config.save(file);
            config.load(file);
            logger.info("Successfully reloaded Arena " + name);
        } catch (InvalidConfigurationException | IOException e) {
            logger.severe("Failed to reload Arena "+name);
            throw new RuntimeException(e);
        }
    }

    public void init(String name, World world){
        config.set("name",name);
        config.set("worldName",world.getName());
        config.set("game.teams",4);
        config.set("game.teamSize",4);
        config.set("game.timeCount",60);
        config.set("game.ghast-amount",2);
        config.set("game.radius",100);
        config.set("game.target-radius",20);
        // 阶段时长（秒）—— 按《改进建议》平衡性调整后的默认值
        config.set("bossbar.develop",180);
        config.set("bossbar.battle",240);
        config.set("bossbar.reduce",600);
        config.set("bossbar.ultimate",300);
        config.set("game.resource-respawn",45);
        config.set("game.border-damage",1.0);
        config.set("game.fall-damage",true); // true=玩家有摔落伤害（默认），false=取消摔落伤害
        config.set("game.pvp",true); // true=允许玩家互攻（默认），false=关闭 PVP
        config.set("game.starter-kit","modest"); // none=不发资源 / modest=少量启动资源 / full=旧版大量资源
        config.set("game.private-zone-radius",30); // 距队出生点此距离内=私有资源区，其余=中立区(中岛)
        config.set("game.private-zone-lock",false); // true=私有区仅本队可开采
        config.set("chests",new ArrayList<>());
        config.set("neutralZones",new ArrayList<>());

        reload();
    }

    public int getTimeCount(){
        return config.getInt("game.timeCount");
    }

    public String getName(){
        return config.getString("name");
    }

    public int getTeamCount(){
        return config.getInt("game.teams");
    }

    public int getTeamSize(){
        return config.getInt("game.teamSize");
    }

    public String getWorldName(){
        return config.getString("worldName");
    }

    public int getGhastAmount(){
        return config.getInt("game.ghast-amount",2);
    }

    public int getDevelop(){
        return config.getInt("bossbar.develop");
    }

    public int getBattle(){
        return config.getInt("bossbar.battle");
    }

    public int getReduce(){
        return config.getInt("bossbar.reduce");
    }

    public int getUltimate(){
        return config.getInt("bossbar.ultimate");
    }

    /** 普通资源重生时间（秒） */
    public int getResourceRespawnSeconds(){
        return config.getInt("game.resource-respawn",45);
    }

    /** 中立区资源重生时间（秒），比普通区更快 */
    public int getNeutralRespawnSeconds(){
        return Math.max(5,getResourceRespawnSeconds()/2);
    }

    /** 圈外每秒伤害 */
    public double getBorderDamage(){
        return config.getDouble("game.border-damage",1.0);
    }

    /** 玩家是否有摔落伤害（默认开启） */
    public boolean isFallDamageEnabled(){
        return config.getBoolean("game.fall-damage",true);
    }

    /** 是否允许玩家互攻 PVP（默认开启，写入世界属性） */
    public boolean isPvpEnabled(){
        return config.getBoolean("game.pvp",true);
    }

    /**
     * 开局资源档位：
     * none   = 不发放任何资源（纯采集开局）
     * modest = 少量启动资源（默认）
     * full   = 旧版大量资源
     */
    public String getStarterKit(){
        return config.getString("game.starter-kit","modest");
    }

    /**
     * 私有资源区半径：距任一队出生点该距离内的矿为该队发展资源（普通恢复速度、不广播）；
     * 离所有队出生点都远的矿自动划为中立资源区（中岛，恢复更快并广播争夺）。
     * 0 = 禁用分区，全部按中立资源处理。
     */
    public int getPrivateZoneRadius(){
        return config.getInt("game.private-zone-radius",30);
    }

    /** 是否锁定私有资源区（开启后仅本队成员可开采该队出生点附近的矿） */
    public boolean isPrivateZoneLock(){
        return config.getBoolean("game.private-zone-lock",false);
    }

    public void addChest(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        List<String> list = config.getStringList("chests");
        list.add(loc);
        config.set("chests",list);

        reload();
    }

    public void removeChest(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        List<String> list = config.getStringList("chests");
        list.remove(loc);
        config.set("chests",list);

        reload();
    }

    public List<Location> getChests(){
        List<String> list = config.getStringList("chests");
        List<Location> locations = new ArrayList<>();
        list.forEach(s -> {
            locations.add(translateLocation(s));
        });
        return locations;
    }

    /**
     * 获取中立区配置
     * 格式: 世界,x,y,z,资源类型
     */
    public List<String> getNeutralZones(){
        return config.getStringList("neutralZones");
    }

    /**
     * 添加中立区
     */
    public void addNeutralZone(Location location, String resourceType){
        removeNeutralZone(location);
        String loc = String.format("%s,%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ(),resourceType);
        List<String> list = config.getStringList("neutralZones");
        list.add(loc);
        config.set("neutralZones",list);
        reload();
    }

    /**
     * 移除中立区
     */
    public void removeNeutralZone(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        List<String> list = config.getStringList("neutralZones");
        list.removeIf(s -> s.startsWith(loc));
        config.set("neutralZones",list);
        reload();
    }

    public Location translateLocation(String raw){
        String[] data = raw.split(",");
        World world = Bukkit.getWorld(data[0]);
        double x = Double.parseDouble(data[1]);
        double y = Double.parseDouble(data[2]);
        double z = Double.parseDouble(data[3]);
        return new Location(world,x,y,z);
    }

    public Location getGhastSpawn(Arena.Teams teams){
        return translateLocation(Objects.requireNonNull(config.getString("ghast." + teams.name())));
    }

    public void setGhastAmount(int amount){
        config.set("game.ghast-amount",amount);

        reload();
    }

    public Location getSpawn(Arena.Teams teams){
        return translateLocation(Objects.requireNonNull(config.getString("team." + teams.name())));
    }

    public int getRadius(){
        return config.getInt("game.radius");
    }

    public int getTargetRadius(){
        return config.getInt("game.target-radius");
    }

    public Location getCenter(){
        return translateLocation(Objects.requireNonNull(config.getString("game.center")));
    }

    public void setRadius(int r){
        config.set("game.radius",r);

        reload();
    }

    public void setTargetRadius(int r){
        config.set("game.target-radius",r);

        reload();
    }

    public void setCenter(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("game.center",loc);

        reload();
    }

    public void setTeamSpawn(Arena.Teams teams, Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("team."+teams.name(),loc);

        reload();
    }

    public Location getWait(){
        return translateLocation(Objects.requireNonNull(config.getString("wait")));
    }

    /**
     * 返回缺失的必填配置项列表（含对应的设置命令提示）。
     * 列表为空表示场地配置完整、可以加入/开局。
     */
    public List<String> getMissingSetup(){
        List<String> missing = new ArrayList<>();
        if (config == null){
            missing.add("配置文件未加载");
            return missing;
        }
        if (config.getString("wait") == null) missing.add("等待点 (用 /gw admin setwait 设置)");
        if (config.getString("game.center") == null) missing.add("毒圈中心 (用 /gw admin setcenter 设置)");
        if (config.getInt("game.radius") <= 0) missing.add("初始毒圈半径 (用 /gw admin setradius <r> 设置)");
        if (config.getInt("game.target-radius") <= 0) missing.add("缩圈目标半径 (用 /gw admin settargetradius <r> 设置)");
        if (config.getInt("game.teams") < 2) missing.add("队伍数量 (创建时默认 4)");
        if (config.getInt("game.teamSize") < 1) missing.add("每队人数上限");
        if (config.getInt("game.timeCount") < 1) missing.add("开局倒计时秒数");
        if (config.getInt("game.ghast-amount", 0) < 1) missing.add("每队乐魂数量 (用 /gw admin setghastamount <n> 设置)");

        // TeamDivider 按枚举顺序取前 N 个颜色作为队伍，因此这些队伍的出生点必须齐全
        int teamCount = Math.min(config.getInt("game.teams"), Arena.Teams.values().length);
        for (int i = 0; i < teamCount; i++) {
            Arena.Teams team = Arena.Teams.values()[i];
            if (config.getString("team." + team.name()) == null)
                missing.add(team.getDisplayName() + "队(" + team.name() + ")出生点 (用 /gw admin setspawn " + team.name() + " 设置)");
            if (config.getString("ghast." + team.name()) == null)
                missing.add(team.getDisplayName() + "队(" + team.name() + ")乐魂出生点 (用 /gw admin setghastspawn " + team.name() + " 设置)");
        }
        return missing;
    }

    /** 场地配置是否完整（可加入/开局） */
    public boolean isSetupComplete(){
        return getMissingSetup().isEmpty();
    }

    public void setWait(Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("wait",loc);

        reload();
    }

    public void setGhastSpawn(Arena.Teams teams, Location location){
        String loc = String.format("%s,%s,%s,%s",location.getWorld().getName(),location.getX(),location.getY(),location.getZ());
        config.set("ghast."+teams.name(),loc);

        reload();
    }
}
