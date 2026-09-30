package top.sparkpixel.hgw.arena;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import top.sparkpixel.hgw.HappyGhastWar;
import top.sparkpixel.hgw.game.chest.ChestRandomFiller;
import top.sparkpixel.hgw.game.party.Party;
import top.sparkpixel.hgw.game.party.PartyManager;
import top.sparkpixel.hgw.game.player.PlayerData;
import top.sparkpixel.hgw.game.stats.GameStatistics;
import top.sparkpixel.hgw.game.team.Team;
import top.sparkpixel.hgw.game.team.TeamChest;
import top.sparkpixel.hgw.game.team.TeamDivider;
import top.sparkpixel.hgw.game.upgrade.AlloyMaker;
import top.sparkpixel.hgw.game.upgrade.UpgradeGUI;
import top.sparkpixel.hgw.ghast.GameGhast;
import top.sparkpixel.hgw.ghast.armor.GhastArmorManager;
import top.sparkpixel.hgw.ghast.skill.GhastSkillManager;
import top.sparkpixel.hgw.scoreboard.GameScoreboard;
import top.sparkpixel.hgw.tab.TabManager;
import top.sparkpixel.hgw.util.MessageTranslate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import top.sparkpixel.hgw.util.SoundUtil;
import top.sparkpixel.hgw.util.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Arena extends BukkitRunnable {

    // 空投相关
    private BukkitTask airdropTask;
    private final int AIRDROP_INTERVAL = 180; // 3分钟（180秒）
    private final List<Location> activeAirdrops = new ArrayList<>();

    /** 空投箱漂浮名称（MiniMessage 标签） */
    public static final String AIRDROP_LABEL = "<gold><bold>空投补给箱";

    public enum GameStatus{
        WAIT,
        STARTING,
        PLAYING,
        ENDING,
        PROCESSING,
        COUNTING
    }

    public enum Teams{
        RED("红","<red>",net.kyori.adventure.text.format.NamedTextColor.RED),
        BLUE("蓝","<blue>",net.kyori.adventure.text.format.NamedTextColor.BLUE),
        GREEN("绿","<green>",net.kyori.adventure.text.format.NamedTextColor.GREEN),
        YELLOW("黄","<yellow>",net.kyori.adventure.text.format.NamedTextColor.YELLOW),
        PURPLE("紫","<light_purple>",net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE),
        WHITE("白","<white>",net.kyori.adventure.text.format.NamedTextColor.WHITE),
        GOLD("橙","<gold>",net.kyori.adventure.text.format.NamedTextColor.GOLD),
        AQUA("青蓝","<aqua>",net.kyori.adventure.text.format.NamedTextColor.AQUA);

        private final String displayName;

        /** MiniMessage 颜色标签，用于聊天/标题等 MiniMessage 文本 */
        private final String tag;

        /** Adventure 命名色，用于记分板队伍（Team.color） */
        private final net.kyori.adventure.text.format.NamedTextColor namedColor;

        Teams(String displayName, String tag, net.kyori.adventure.text.format.NamedTextColor namedColor) {
            this.displayName = displayName;
            this.tag = tag;
            this.namedColor = namedColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        /** MiniMessage 颜色标签（如 {@code <red>}），可直接拼进 MiniMessage 文本 */
        public String getColor() {
            return tag;
        }

        /** 记分板队伍颜色（legacy ChatColor 的替代） */
        public net.kyori.adventure.text.format.NamedTextColor getNamedColor() {
            return namedColor;
        }
    }

    private List<Player> players = new ArrayList<>();

    private Map<Player, PlayerData> playerDatas = new HashMap<>();

    private final String name;
    private World world;
    public GameStatus status;
    private YamlConfiguration config;
    private HappyGhastWar ghastWar;
    private final TabManager tabManager;
    private Path worldPath;
    private SlimeWorld slimeWorld;

    private final ArenaConfig arenaConfig;

    private List<Team> teams = new ArrayList<>();

    private Map<HappyGhast, GameGhast> ghasts = new HashMap<>();

    private final GameScoreboard gameScoreboard;

    private final org.bukkit.scoreboard.Scoreboard scoreboard;

    private int timeCount = -1;

    public BukkitTask countTask;

    public Map<Location,Material> resources = new HashMap<>();

    /** 本局已广播过"开采中立矿脉"的队伍：每队每局只广播一次，防刷屏 */
    public final Set<Teams> neutralMiningAnnounced = EnumSet.noneOf(Teams.class);

    public List<Location> rawBlocks = new ArrayList<>();

    public AlloyMaker alloyMaker;

    public TeamChest teamChest;

    public BossBar bossBar;

    public BukkitTask bossBarCount;

    public boolean enable = true;

    public int bossBarTime = -1;

    public NamespacedKey bossBarKey;

    public AdvancedCircleShrinker circleShrinker;

    public UpgradeGUI upgradeGUI;
    private final GhastSkillManager skillManager;
    private final GhastArmorManager armorManager;
    private final GameStatistics statistics;

    public UpgradeGUI getUpgradeGUI() {
        return upgradeGUI;
    }

    public boolean isEnable() {
        return enable;
    }

    public World getWorld() {
        return world;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getName() {
        return name;
    }

    public Map<Location, Material> getResources() {
        return resources;
    }

    public List<Location> getRawBlocks() {
        return rawBlocks;
    }

    public Map<HappyGhast, GameGhast> getGhasts(){
        return ghasts;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public GhastSkillManager getSkillManager() {
        return skillManager;
    }

    public GhastArmorManager getArmorManager() {
        return armorManager;
    }

    public GameStatistics getStatistics() {
        return statistics;
    }

    public enum gameStage{
        Development,
        Battle,
        Reduce,
        Ultimate,
        WAIT,
        END,
        COUNT
    }

    public gameStage stage;


    @Override
    public void run() {
        if (config == null){
            return;
        }
        // 本任务运行在主线程（见 startSchedule），可以直接操作实体/方块/记分板
        teleportBlockDisplays();
        updateBossBar();
        switch (status){
            case WAIT -> handleWait();
            case STARTING -> {
                try {
                    handleStarting();
                } catch (Exception e) {
                    // 开局中途异常会把场地留在半初始化状态：只把状态拨回 WAIT 的话，
                    // 人数满足时倒计时会被立刻再次触发，表现为反复传送+刷"游戏开始"。
                    // 必须做一次完整清理并重载世界
                    ghastWar.getLogger().severe("Game start failed for arena " + name + ": " + e.getMessage());
                    e.printStackTrace();
                    status = GameStatus.WAIT;
                    forceReset();
                }
            }
            case PLAYING -> handlePlaying();
            case ENDING -> handleEnding();
        }
    }

    public Team getPlayerTeam(Player player){
        for (Team team : teams){
            if (team.getPlayers().contains(player)){
                return team;
            }
        }
        return null;
    }

    public void updateBossBar(){
        switch (stage){
            case Development -> onDevelopment();
            case Battle -> onBattle();
            case Reduce -> onReduce();
            case Ultimate -> onUltimate();
        }
    }

    public void checkGhast(Team team,Arena arena){
        if (!team.getGhasts().isEmpty()) return;
        for (Player player : team.getPlayers()){
            player.sendTitle(Text.legacy(ghastWar.getLanguage(player).getContent("game.respawn-unavailable-title")),
                    Text.legacy(ghastWar.getLanguage(player).getContent("game.respawn-unavailable-subtitle")));
        }
        team.setCanRespawn(false);
        arena.getGameScoreboard().updateTeam(team);
    }

    public void onUltimate(){
        stage = gameStage.COUNT;
        bossBarTime = arenaConfig.getUltimate();
        bossBarCount = Bukkit.getServer().getScheduler().runTaskTimer(ghastWar,()->{
            bossBarTime--;
            if (bossBarTime < 1){
                stage = gameStage.END;
                Bukkit.getServer().getScheduler().runTask(ghastWar,()-> {
                    for (Team team : teams) {
                        // 迭代副本，避免遍历中修改集合
                        for (GameGhast ghast : new ArrayList<>(team.getGhasts())) {
                            team.removeGhast(ghast);
                            ghasts.remove(ghast.getHappyGhast());
                            ghast.unregister();
                        }
                        checkGhast(team, this);
                    }
                });
                bossBar.setProgress(1);
                bossBar.setTitle("");
                bossBarCount.cancel();
                return;
            }
            bossBar.setTitle(Text.legacy(ghastWar.getLanguage(null).getContent("bossbar.ultimate")).replace("{0}",String.valueOf(bossBarTime)));
            bossBar.setProgress(calculateDecimalPercentage(bossBarTime,arenaConfig.getUltimate()));
        },0,20);
    }

    public void onReduce(){
        stage = gameStage.COUNT;
        bossBarTime = arenaConfig.getReduce();
        bossBar.setColor(BarColor.RED);
        circleShrinker.startShrinking(arenaConfig.getTargetRadius(),arenaConfig.getReduce(),arenaConfig.getBorderDamage());
        bossBarCount = Bukkit.getServer().getScheduler().runTaskTimer(ghastWar,()->{
            bossBarTime--;
            if (bossBarTime < 1){
                stage = gameStage.Ultimate;
                bossBarCount.cancel();
                return;
            }
            bossBar.setTitle(Text.legacy(ghastWar.getLanguage(null).getContent("bossbar.reduce")).replace("{0}",String.valueOf(bossBarTime)));
            bossBar.setProgress(calculateDecimalPercentage(bossBarTime,arenaConfig.getReduce()));
        },0,20);
    }

    public void onBattle(){
        stage = gameStage.COUNT;
        bossBarTime = arenaConfig.getBattle();
        bossBar.setColor(BarColor.YELLOW);
        bossBarCount = Bukkit.getServer().getScheduler().runTaskTimer(ghastWar,()->{
            bossBarTime--;
            if (bossBarTime < 1){
                stage = gameStage.Reduce;
                bossBarCount.cancel();
                return;
            }
            bossBar.setTitle(Text.legacy(ghastWar.getLanguage(null).getContent("bossbar.battle")).replace("{0}",String.valueOf(bossBarTime)));
            bossBar.setProgress(calculateDecimalPercentage(bossBarTime,arenaConfig.getBattle()));
        },0,20);
    }

    public void onDevelopment(){
        stage = gameStage.COUNT;
        bossBarTime = arenaConfig.getDevelop();
        bossBar.setColor(BarColor.BLUE);
        bossBarCount = Bukkit.getServer().getScheduler().runTaskTimer(ghastWar,()->{
            bossBarTime--;
            if (bossBarTime < 1){
                stage = gameStage.Battle;
                bossBarCount.cancel();
                return;
            }
            bossBar.setTitle(Text.legacy(ghastWar.getLanguage(null).getContent("bossbar.develop")).replace("{0}",String.valueOf(bossBarTime)));
            bossBar.setProgress(calculateDecimalPercentage(bossBarTime,arenaConfig.getDevelop()));
        },0,20);
    }

    public double calculateDecimalPercentage(double current, double max) {
        if (max <= 0) return 0.0;

        double percentage = current / max;

        // 限制在0.0-1.0之间
        if (percentage < 0) return 0.0;
        if (percentage > 1) return 1.0;

        return percentage;
    }

    public void teleportBlockDisplays(){
        ghasts.forEach((happyGhast, gameGhast) -> {
            BlockDisplay blockDisplay = gameGhast.getBlockDisplay();

            if (!happyGhast.isValid() || !blockDisplay.isValid()) return;

            BoundingBox ghastBox = happyGhast.getBoundingBox();
            Vector3f ghastWorldCenter = new Vector3f(
                    (float)(ghastBox.getMinX() + ghastBox.getWidthX() / 2),
                    (float)(ghastBox.getMinY() + ghastBox.getHeight() / 2),
                    (float)(ghastBox.getMinZ() + ghastBox.getWidthZ() / 2)
            );

            Vector3f modelCenterOffset = new Vector3f(0.5f, 0.5f, 0.5f);

            float heightAbove = 2.5f;
            Location targetLocation = new Location(
                    happyGhast.getWorld(),
                    ghastWorldCenter.x,
                    ghastWorldCenter.y + heightAbove,
                    ghastWorldCenter.z
            );

            blockDisplay.teleport(targetLocation);

            TextDisplay textDisplay = gameGhast.getTextDisplay();
            textDisplay.teleport(targetLocation.clone().add(0,1,0));

            Interaction interaction = gameGhast.getInteraction();
            interaction.teleport(targetLocation.clone().add(0,-0.5,0));

            Location ghastLoc = happyGhast.getLocation();
            float yaw = ghastLoc.getYaw();
            float pitch = 0;

            float yawRad = (float) Math.toRadians(-yaw);
            float pitchRad = (float) Math.toRadians(pitch);
            Quaternionf rotation = new Quaternionf()
                    .rotateYXZ(yawRad, pitchRad, 0);

            Vector3f rotatedOffset = rotation.transform(new Vector3f(
                    modelCenterOffset.x, modelCenterOffset.y, modelCenterOffset.z
            ));
            Transformation newTransformation = getTransformation(rotatedOffset, blockDisplay, rotation);
            blockDisplay.setTransformation(newTransformation);

            World ghastWorld = happyGhast.getWorld();

            ghastWorld.spawnParticle(Particle.DUST,
                    targetLocation,
                    2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.BLACK, 1.0f));
        });
    }

    private static @NonNull Transformation getTransformation(Vector3f rotatedOffset, BlockDisplay blockDisplay, Quaternionf rotation) {
        Vector3f translation = new Vector3f(
                -rotatedOffset.x, -rotatedOffset.y, -rotatedOffset.z
        );

        Transformation currentTrans = blockDisplay.getTransformation();
        Transformation newTransformation = new Transformation(
                translation, // 应用补偿偏移
                rotation,    // 应用乐魂的旋转
                currentTrans.getScale(),
                new Quaternionf()
        );
        return newTransformation;
    }

    public void stop(){
        sendPlayingMessage();
        try {
            this.cancel();
        } catch (IllegalStateException ignored) {
            // 任务可能尚未调度
        }
        this.alloyMaker.stopChecking();
        if (this.bossBarCount != null){
            this.bossBarCount.cancel();
        }
        if (this.countTask != null){
            this.countTask.cancel();
        }
        cleanupAirdropSystem();
        skillManager.cleanup();
        armorManager.cleanup();
        statistics.cleanup();
        Bukkit.removeBossBar(bossBarKey);
    }

    /**
     * 强制重置对局（管理员命令 /gw admin resetgame 使用）：
     * 踢回所有玩家、清理实体与数据，随后异步重载世界并把状态复位到 WAIT。
     * 与 stop() 不同，主循环任务保持运行，场地可以继续开局。
     */
    public void forceReset(){
        if (status == GameStatus.PROCESSING) return;
        status = GameStatus.PROCESSING;
        if (bossBarCount != null){
            bossBarCount.cancel();
            bossBarCount = null;
        }
        if (countTask != null){
            countTask.cancel();
            countTask = null;
        }
        if (circleShrinker != null){
            circleShrinker.stopShrinking();
        }
        for (Player player : new ArrayList<>(players)){
            tpToLobby(player, ghastWar);
            removePlayer(player);
        }
        players.clear();
        playerDatas.clear();
        for (GameGhast ghast : new ArrayList<>(ghasts.values())){
            ghast.unregister();
        }
        ghasts.clear();
        for (Team team : new ArrayList<>(teams)){
            team.unRegister();
        }
        teams.clear();
        cleanupAirdropSystem();
        skillManager.cleanup();
        armorManager.cleanup();
        statistics.cleanup();
        try {
            resetWorld();
        } catch (IOException e) {
            ghastWar.getLogger().severe("forceReset failed for arena " + name + ": " + e.getMessage());
        }
    }

    public void handleEnding(){
        status = GameStatus.PROCESSING;

        for (Player player : world.getPlayers()){
            Text.send(player, ghastWar.getLanguage(player).getContent("game.arena-close"));
        }
        ghastWar.getServer().getScheduler().runTaskLater(ghastWar, () -> {
            for (Player player : new ArrayList<>(world.getPlayers())){
                tpToLobby(player,ghastWar);
                removePlayer(player);
            }
            players.clear();
            playerDatas.clear();

            for (GameGhast ghast : new ArrayList<>(ghasts.values())){
                ghast.unregister();
            }

            ghasts.clear();
            teams.clear();

            // resetWorld() unloads + asynchronously reloads the world from .slime, then
            // chains init() back on the main thread to reset game state.
            ghastWar.getServer().getScheduler().runTaskLater(ghastWar, () -> {
                try {
                    resetWorld();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },20L);
        },15 * 20);

    }

    public GameScoreboard getGameScoreboard() {
        return gameScoreboard;
    }

    /**
     * 获取游戏状态，与 GameScoreboard 的状态同步
     */
    public GameScoreboard.GameState getGameState() {
        switch (status) {
            case WAIT:
                return GameScoreboard.GameState.WAITING;
            case COUNTING:
                return GameScoreboard.GameState.COUNTDOWN;
            case PLAYING:
                return GameScoreboard.GameState.PLAYING;
            case ENDING:
                return GameScoreboard.GameState.ENDING;
            case STARTING:
            case PROCESSING:
            default:
                return GameScoreboard.GameState.PLAYING;
        }
    }

    /**
     * 获取玩家所在的队伍
     */
    public Team getTeam(Player player) {
        return gameScoreboard.getAllTeams().stream()
            .filter(team -> team.getPlayers().contains(player))
            .findFirst()
            .orElse(null);
    }

    /**
     * 更新所有玩家的 Tab 显示
     */
    public void updateAllTabs() {
        if (tabManager != null) {
            tabManager.updateAllTabs();
        }
    }

    /**
     * 更新特定玩家的 Tab 显示
     */
    public void updatePlayerTab(Player player) {
        if (tabManager != null) {
            tabManager.updatePlayerTab(player);
        }
    }

    public Map<Player, PlayerData> getPlayerDatas() {
        return playerDatas;
    }

    public void handlePlaying(){

        // 迭代副本：淘汰队伍时会从 teams 移除
        for (Team team : new ArrayList<>(teams)){
            if (team.getPlayers().isEmpty()){
                if (!team.isAlive()){
                    continue;
                }

                for (Player player : players){
                    String message = ghastWar.getLanguage(player).getContent("game.team-eliminated")
                            .replace("{0}",team.getTeams().getColor())
                            .replace("{1}",team.getTeams().displayName);
                    Text.send(player, message);
                }
                team.setAlive(false);
                gameScoreboard.updateTeam(team);
                teams.remove(team);

            }
        }

        gameScoreboard.update();

        if (teams.size() == 1){
            Team winTeam = teams.getFirst();

            endGame(winTeam);
        } else if (teams.isEmpty()){
            // 所有队伍团灭（含全员退出导致 playerDatas 已被清空的情形）都必须结束：
            // 不结束的话场地永远停在 PLAYING，清理与世界重载不会执行，
            // 上一局的乐魂/炉子就会残留到下一局
            endGame(null);
        }
    }

    /**
     * 结算：广播获胜队伍（可为 null，平局）、TOP3 击杀与统计 MVP，然后进入 ENDING
     */
    private void endGame(Team winTeam){
        // 获取前3名
        List<Map.Entry<Player, PlayerData>> topThree = playerDatas.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().getKills() - e1.getValue().getKills())
                .limit(3)
                .toList();

        String winnerNames = winTeam == null ? "-" :
                winTeam.getPlayers().stream().map(Player::getDisplayName).collect(Collectors.joining(", "));

        // 赛后统计 MVP（伤害/采集/升级 维度）
        List<Map.Entry<Player, GameStatistics.PlayerStats>> statTop = statistics.getTopStats(3);

        for (Player player : players){
            String color = winTeam == null ? "<gray>" : winTeam.getTeams().getColor();
            String displayName = winTeam == null ? "-" : winTeam.getTeams().getDisplayName();

            String winMessage = ghastWar.getLanguage(player).getContent("game.end-winner-team")
                    .replace("{0}",color)
                    .replace("{1}",displayName);

            String first = ghastWar.getLanguage(player).getContent("game.null-player"), firstKills = "0";
            String second = ghastWar.getLanguage(player).getContent("game.null-player"), secondKills = "0";
            String third = ghastWar.getLanguage(player).getContent("game.null-player"), thirdKills = "0";

            if (!topThree.isEmpty()) {
                first = topThree.getFirst().getKey().getName();
                firstKills = String.valueOf(topThree.getFirst().getValue().getKills());
            }
            if (topThree.size() > 1) {
                second = topThree.get(1).getKey().getName();
                secondKills = String.valueOf(topThree.get(1).getValue().getKills());
            }
            if (topThree.size() > 2) {
                third = topThree.get(2).getKey().getName();
                thirdKills = String.valueOf(topThree.get(2).getValue().getKills());
            }

            String topMessage = ghastWar.getLanguage(player).getContent("game.end-top-chat")
                    .replace("{0}",color)
                    .replace("{1}",displayName)
                    .replace("{2}",winnerNames)
                    .replace("{firstName}",first)
                    .replace("{firstKills}",firstKills)
                    .replace("{secondName}",second)
                    .replace("{secondKills}",secondKills)
                    .replace("{thirdName}",third)
                    .replace("{thirdKills}",thirdKills);

            if (winTeam != null && winTeam.getPlayers().contains(player)){
                player.sendTitle(Text.legacy(ghastWar.getLanguage(player).getContent("game.end-victory-title")),"");
            }else {
                player.sendTitle(Text.legacy(ghastWar.getLanguage(player).getContent("game.end-game-over-title")),"");
            }
            Text.send(player, winMessage);
            Text.send(player, topMessage);

            // 统计 MVP 头衔（矿工大师/战神/乐魂守护者 等，由 GameStatistics 计算）
            if (!statTop.isEmpty()){
                String mvpLine = statistics.buildMvpSummary(player, statTop);
                if (mvpLine != null && !mvpLine.isEmpty()){
                    Text.send(player, mvpLine);
                }
            }
        }

        status = GameStatus.ENDING;
    }

    public Color getColor(Teams teams){
        switch (teams){
            case RED -> {
                return Color.RED;
            }
            case BLUE -> {
                return Color.BLUE;
            }
            case GREEN -> {
                return Color.GREEN;
            }
            case YELLOW -> {
                return Color.YELLOW;
            }
            case AQUA -> {
                return Color.AQUA;
            }
            case WHITE -> {
                return Color.WHITE;
            }
            case GOLD -> {
                return Color.ORANGE;
            }
            case PURPLE -> {
                return Color.PURPLE;
            }
            default -> {
                return Color.BLACK;
            }
        }
    }

    public void giveWaitEquipment(Player player){
        ItemStack lobby = new ItemStack(Material.RED_BED);
        org.bukkit.inventory.meta.ItemMeta lobbyMeta = lobby.getItemMeta();
        lobbyMeta.displayName(Text.mm(ghastWar.getLanguage(player).getContent("item.lobby-bed-name")));
        lobbyMeta.lore(ghastWar.getLanguage(player).getTranslatedList("item.lobby-bed-lore").stream()
                .map(Text::mm)
                .collect(java.util.stream.Collectors.toList()));
        lobby.setItemMeta(lobbyMeta);

        player.getInventory().setItem(0,lobby);
    }

    public void giveEquipment(Player player,Team team){
        player.getInventory().clear();
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        helmetMeta.setColor(getColor(team.getTeams()));
        helmet.setItemMeta(helmetMeta);

        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chest.getItemMeta();
        chestMeta.setColor(getColor(team.getTeams()));
        chest.setItemMeta(chestMeta);

        ItemStack leg = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legMeta = (LeatherArmorMeta) leg.getItemMeta();
        legMeta.setColor(getColor(team.getTeams()));
        leg.setItemMeta(legMeta);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(getColor(team.getTeams()));
        boots.setItemMeta(bootsMeta);

        PlayerInventory inv = player.getInventory();
        inv.setHelmet(helmet);
        inv.setChestplate(chest);
        inv.setLeggings(leg);
        inv.setBoots(boots);

        inv.addItem(new ItemStack(Material.STONE_PICKAXE));
        inv.addItem(new ItemStack(Material.STONE_SWORD));
        inv.addItem(new ItemStack(Material.STONE_AXE));
        inv.addItem(new ItemStack(Material.WOODEN_SHOVEL));
        inv.addItem(new ItemStack(Material.SPYGLASS));

        upgradeGUI.giveCatapultToPlayer(player,0);

        // 开局资源按场地配置发放：none=不发 / modest=少量启动资源 / full=旧版大量资源
        String starterKit = arenaConfig.getStarterKit();
        if ("full".equalsIgnoreCase(starterKit)){
            inv.addItem(new ItemStack(Material.OAK_LOG,64 * 4));
            inv.addItem(new ItemStack(Material.IRON_INGOT, 64));
            inv.addItem(new ItemStack(Material.COPPER_INGOT, 50));
            inv.addItem(new ItemStack(Material.COAL, 50));
        } else if (!"none".equalsIgnoreCase(starterKit)){
            // modest（默认）：够起步合成，但采矿仍是资源主要来源
            inv.addItem(new ItemStack(Material.OAK_LOG,16));
            inv.addItem(new ItemStack(Material.IRON_INGOT, 8));
            inv.addItem(new ItemStack(Material.COPPER_INGOT, 8));
            inv.addItem(new ItemStack(Material.COAL, 8));
        }
    }

    public void handleStarting(){
        if (timeCount > 0 || timeCount == -1){
            return;
        }
        status = GameStatus.PROCESSING;
        sendPlayingMessage();
        List<Team> gteams;
        try {
            gteams = TeamDivider.dividePlayers(
                    players,
                    arenaConfig.getTeamCount(),
                    1,
                    scoreboard,
                    HappyGhastWar.getInstance().getPartyManager()
            );
        } catch (IllegalArgumentException e) {
            // 分队失败（人数不足等）：回到等待状态，别把场地卡死在 PROCESSING
            ghastWar.getLogger().warning("Game start aborted for arena " + name + ": " + e.getMessage());
            status = GameStatus.WAIT;
            timeCount = -1;
            return;
        }
        this.teams = gteams;


        gameScoreboard.setGameState(GameScoreboard.GameState.PLAYING);
        // 更新所有玩家的 Tab 显示
        updateAllTabs();

        // 重置统计（每局独立）
        statistics.cleanup();
        neutralMiningAnnounced.clear();

        // 防御：异常重开时上一次对局的乐魂实体可能仍残留，先全部移除，
        // 避免旧炉子/旧乐魂堆进新对局
        for (GameGhast oldGhast : new ArrayList<>(ghasts.values())){
            oldGhast.unregister();
        }
        ghasts.clear();

        world.setGameRule(GameRule.KEEP_INVENTORY,true);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN,true);
        world.setDifficulty(Difficulty.EASY);

        ChestRandomFiller chestRandomFiller = new ChestRandomFiller();
        chestRandomFiller.fillChestsRandomly(arenaConfig.getChests(),ghastWar.chest_items);

        this.circleShrinker = new AdvancedCircleShrinker(ghastWar,arenaConfig.getCenter(),arenaConfig.getRadius(),this);
        for (Team team : teams){
            gameScoreboard.addTeam(team);
            Location spawn = arenaConfig.getSpawn(team.getTeams());

            for (int i = 0;i < arenaConfig.getGhastAmount(); i++){
                Location ghastSpawn = arenaConfig.getGhastSpawn(team.getTeams());
                GameGhast ghast = spawnGhast(team.getTeams(),ghastSpawn);
                ghast.getHappyGhast().setAI(false);
                team.addGhast(ghast);
            }

            for (Player player : team.getPlayers()){
                player.teleport(spawn);
                player.setPlayerTime(1000,false);
                player.setPlayerWeather(WeatherType.CLEAR);
                PlayerData playerData = new PlayerData(player,team);
                playerDatas.put(player,playerData);
                player.setGlowing(true);
                giveEquipment(player,team);
                alloyMaker.addPlayer(player);
                bossBar.addPlayer(player);
                upgradeGUI.upgradeManager.removePlayer(player);
                statistics.startTracking(player);
            }
        }

        // 为所有乐魂注册技能冷却与护甲数据
        skillManager.registerSkills();
        armorManager.registerArmorSystem();

        for (Player player : players){
            String startTitle = ghastWar.getLanguage(player).getContent("game.game-start-title");
            String startSubTitle = ghastWar.getLanguage(player).getContent("game.game-start-subtitle");
            player.sendTitle(Text.legacy(startTitle), Text.legacy(startSubTitle));
            Text.send(player, ghastWar.getLanguage(player).getContent("game.game-start-message"));
            SoundUtil.play(player, "game-start", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            SoundUtil.play(player, "game-start-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
        this.stage = gameStage.Development;
        status = GameStatus.PLAYING;
    }

    public void handleWait(){

        int size = players.size();
        int teamCount = arenaConfig.getTeamCount();
        int teamSize = arenaConfig.getTeamSize();
        timeCount = arenaConfig.getTimeCount();
        int minPlayers = teamCount * 1;


        gameScoreboard.updatePlayerCount(size,teamCount * teamSize);

        if (size >= minPlayers){
            // 已有倒计时在跑时绝不能再建一个：被覆盖的旧任务会泄漏，
            // 之后每秒把 status 拨回 STARTING，导致开局流程无限重演
            if (countTask != null && !countTask.isCancelled()){
                return;
            }
            status = GameStatus.COUNTING;
            boolean canStart = TeamDivider.canStartCountdown(
                    players,
                    teamCount,
                    HappyGhastWar.getInstance().getPartyManager()
            );
            if (!canStart){
                status = GameStatus.WAIT;
                return;
            }

            for (Player player : players){
                Text.send(player, ghastWar.getLanguage(player).getContent("game.getMinPlayers").replace("{time}",timeCount+""));
            }
            gameScoreboard.setGameState(GameScoreboard.GameState.COUNTDOWN);
            // 更新所有玩家的 Tab 显示
            updateAllTabs();
            // 用匿名 BukkitRunnable 以便任务内部 this.cancel() 精确自毁：
            // 在 lambda 里引用 countTask 字段取消的可能已经是新建的另一个任务
            countTask = new BukkitRunnable(){
                @Override
                public void run(){
                    // 孤儿任务自毁：场地已不在倒计时状态（被重置/开局/调试指令改动）时，
                    // 绝不能把 status 拨回 STARTING，否则开局流程会被反复触发
                    if (status != GameStatus.COUNTING){
                        this.cancel();
                        return;
                    }
                    gameScoreboard.updateCountdown(timeCount);
                    if (players.size() < minPlayers){
                        for (Player player : players){
                            Text.send(player, ghastWar.getLanguage(player).getContent("game.game-cancel-message"));
                        }
                        this.cancel();
                        status = GameStatus.WAIT;
                        gameScoreboard.setGameState(GameScoreboard.GameState.WAITING);
                        // 更新所有玩家的 Tab 显示
                        updateAllTabs();
                        timeCount = -1;
                        return;
                    }
                    if (timeCount > 0 && timeCount <= 5){
                        String subtitle = translateCount(timeCount);

                        for (Player player : players){
                            String title = ghastWar.getLanguage(player).getContent("game.arena-start-countdown-title");
                            player.sendTitle(Text.legacy(title), Text.legacy(subtitle));
                            SoundUtil.play(player, "countdown", Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                            Text.send(player, ghastWar.getLanguage(player).getContent("game.getMinPlayers").replace("{time}",timeCount+""));
                        }
                    }
                    if (timeCount < 1){
                        timeCount = 0;
                        this.cancel();
                        status = GameStatus.STARTING;
                        return;
                    }
                    timeCount--;
                }
            }.runTaskTimer(ghastWar,0,20);
        }
    }

    public String translateCount(int c){
        return switch (c) {
            case 5 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-5");
            case 4 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-4");
            case 3 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-3");
            case 2 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-2");
            case 1 -> HappyGhastWar.language.getContent("game.arena-start-countdown-subtitle-1");
            default -> "";
        };
    }

    public Material getHarness(Teams teams){
        return switch (teams) {
            case RED -> Material.RED_HARNESS;
            case BLUE -> Material.BLUE_HARNESS;
            case GREEN -> Material.GREEN_HARNESS;
            case YELLOW -> Material.YELLOW_HARNESS;
            case AQUA -> Material.CYAN_HARNESS;
            case WHITE -> Material.WHITE_HARNESS;
            case GOLD -> Material.ORANGE_HARNESS;
            case PURPLE -> Material.PURPLE_HARNESS;
        };
    }

    public ArenaConfig getArenaConfig(){
        return this.arenaConfig;
    }

    public GameGhast spawnGhast(Teams team, Location location){
        HappyGhast happyGhast = location.getWorld().spawn(location, HappyGhast.class);
        happyGhast.getEquipment().setItem(EquipmentSlot.BODY,new ItemStack(getHarness(team)));
        happyGhast.setGlowing(true);
        happyGhast.setMaxHealth(100);
        happyGhast.setHealth(100);

        BlockDisplay display = location.getWorld().spawn(happyGhast.getLocation(), BlockDisplay.class);
        display.setBlock((Material.FURNACE.createBlockData()));

        TextDisplay textDisplay = location.getWorld().spawn(happyGhast.getLocation(),TextDisplay.class);
        textDisplay.text(Text.mm(team.getColor() + team.getDisplayName() + "队<white>乐魂"));

        textDisplay.setBillboard(Display.Billboard.CENTER);  // 始终面向玩家
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.setSeeThrough(true); // 可穿透看到后面
        textDisplay.setShadowed(true);   // 有阴影

        Interaction interaction = (Interaction) world.spawnEntity(location, EntityType.INTERACTION);

        float visualScale = 1f;
        interaction.setInteractionWidth(visualScale); // 宽度 (X/Z轴)
        interaction.setInteractionHeight(visualScale); // 高度 (Y轴)

        GameGhast gameGhast = new GameGhast(happyGhast,display,textDisplay,interaction, skillManager, armorManager);
        ghasts.put(happyGhast,gameGhast);
        return gameGhast;
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


    public Arena(HappyGhastWar ghastWar, World world, ArenaConfig arenaConfig){
        this.world = world;
        this.status = GameStatus.WAIT;
        this.ghastWar = ghastWar;
        this.tabManager = ghastWar.getTabManager();
        this.worldPath = world.getWorldFolder().toPath();
        this.arenaConfig = arenaConfig;

        this.name = arenaConfig.getName();

        // 世界基础设置（PVP 等）依赖 arenaConfig，必须在其赋值之后应用
        applyWorldSettings();

        // Wire the SlimeWorld/instance for this arena (if already loaded by the plugin)
        // so the first reset() can unload+reload correctly instead of trying to load an
        // already-loaded world (would throw IllegalArgumentException).
        this.slimeWorld = ghastWar.slimeWorlds.get(world.getName());

        if (ghastWar.getConfig().getBoolean("bungee.enable")){
            this.gameScoreboard = new GameScoreboard(ghastWar,name,arenaConfig.getTeamCount(),ghastWar.getConfig().getString("serverIp"),ghastWar.getConfig().getString("bungee.serverName"));
        }else {
            this.gameScoreboard = new GameScoreboard(ghastWar,name,arenaConfig.getTeamCount(),ghastWar.getConfig().getString("serverIp"));
        }

        gameScoreboard.setGameState(GameScoreboard.GameState.WAITING);
        // 更新所有玩家的 Tab 显示
        updateAllTabs();
        this.scoreboard = this.gameScoreboard.getScoreboard();
        this.alloyMaker = new AlloyMaker(ghastWar, new ArrayList<>());
        alloyMaker.startChecking();
        this.teamChest = new TeamChest(ghastWar);
        this.skillManager = new GhastSkillManager(ghastWar, this);
        this.armorManager = new GhastArmorManager(ghastWar, this);
        this.statistics = new GameStatistics(ghastWar);
        this.stage = gameStage.WAIT;
        this.bossBarKey = new NamespacedKey(ghastWar,world.getName());
        this.bossBar = ghastWar.getServer().createBossBar(bossBarKey,"Waiting", BarColor.WHITE, BarStyle.SOLID);
        this.upgradeGUI = new UpgradeGUI(ghastWar,this);
        loadConfig();
        startSchedule();
        sendReadyMessage();
    }



    /**
     * 世界（重新）加载后应用与玩法相关的基础世界设置：
     * PVP 按场地配置 game.pvp（默认开启）显式写入——slime 世界文件里的 pvp 属性
     * 可能为 false，不显式设置会导致游戏内 PVP 默认关闭。
     */
    private void applyWorldSettings(){
        world.setPVP(arenaConfig.isPvpEnabled());
        world.setGameRule(GameRule.KEEP_INVENTORY,true);
    }

    public void sendReadyMessage(){
        if (!ghastWar.getConfig().getBoolean("bungee.enable",false)) return;
        JsonObject jsonObject = new MessageTranslate(ghastWar).FromArena(this);
        jsonObject.addProperty("action","ready");
        ghastWar.socketClient.sendMessage(jsonObject.toString());
    }

    public void sendRefreshMessage(){
        if (!ghastWar.getConfig().getBoolean("bungee.enable",false)) return;
        JsonObject jsonObject = new MessageTranslate(ghastWar).FromArena(this);
        jsonObject.addProperty("action","refresh");
        ghastWar.socketClient.sendMessage(jsonObject.toString());
    }

    public void sendPlayingMessage(){
        if (!ghastWar.getConfig().getBoolean("bungee.enable",false)) return;
        JsonObject jsonObject = new MessageTranslate(ghastWar).FromArena(this);
        jsonObject.addProperty("action","playing");
        ghastWar.socketClient.sendMessage(jsonObject.toString());
    }

    public void startSchedule(){
        // 主循环必须跑在主线程：run() 里直接操作实体、方块、记分板等主线程 API
        this.runTaskTimer(this.ghastWar,0,1);
        // 启动空投系统
        startAirdropSystem();
    }

    public void backupWorld(){
        ghastWar.getLogger().info("Backing up game: "+name);

        File folder = new File(this.ghastWar.getDataFolder(),"backups");
        if (!folder.exists()){
            folder.mkdirs();
        }
        Path backup = new File(this.ghastWar.getDataFolder()+"/backups",this.world.getName()).toPath();
        try {
            copyDirectory(this.worldPath,backup);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetWorld() throws IOException {
        String worldName = world.getName();

        AdvancedSlimePaperAPI api = AdvancedSlimePaperAPI.instance();

        // Always save + unload the loaded world before reloading it.
        // slimeWorld may be null on first reset, but the Bukkit world exists and is
        // loaded, so we must still unload it or loadWorld() below will throw
        // IllegalArgumentException (a world with that name is already loaded).
        if (slimeWorld != null) {
            api.saveWorld(slimeWorld);
        }
        boolean unloaded = Bukkit.unloadWorld(worldName, false);
        if (!unloaded){
            // 卸载失败基本都是世界里还有玩家。直接继续的话 loadWorld 会抛
            // "already loaded" 被 catch 吞掉，场地卡在 PROCESSING 且旧实体全部残留。
            // 先把残留玩家送回大厅再试一次，仍失败则放弃本次重置
            World existing = Bukkit.getWorld(worldName);
            if (existing != null){
                for (Player leftover : new ArrayList<>(existing.getPlayers())){
                    tpToLobby(leftover, ghastWar);
                }
                unloaded = Bukkit.unloadWorld(worldName, false);
            }
        }
        if (!unloaded){
            ghastWar.getLogger().severe("Failed to unload world " + worldName + " for reset; aborting reset to avoid stale entities.");
            status = GameStatus.WAIT;
            return;
        }

        // Per the AdvancedSlimePaper docs: readWorld (I/O) should run off the main
        // thread; loadWorld (server-interacting) MUST run on the main thread. So do
        // the read async, then chain the load + state reset back onto the main thread.
        CompletableFuture.supplyAsync(() -> {
            try {
                // Re-read the pristine world from the .slime source (user-selected
                // strategy: use .slime as the master source, not the anvil backup).
                return api.readWorld(ghastWar.loader, worldName, false, new SlimePropertyMap());
            } catch (Exception e) {
                ghastWar.getLogger().severe("Failed to read world " + worldName + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(newSlimeWorld -> {
            if (newSlimeWorld == null) {
                return;
            }
            try {
                SlimeWorldInstance instance = api.loadWorld(newSlimeWorld, true);
                world = instance.getBukkitWorld();
                applyWorldSettings();
                slimeWorld = newSlimeWorld;
                this.worldPath = world.getWorldFolder().toPath();

                // Keep the plugin's instance maps in sync so onDisable saves the right world.
                ghastWar.slimeWorlds.put(worldName, newSlimeWorld);
                ghastWar.slimeWorldInstances.put(worldName, instance);

                ghastWar.getLogger().info("Game reset successful: " + name);
                gameScoreboard.setGameState(GameScoreboard.GameState.WAITING);
                // 更新所有玩家的 Tab 显示
                updateAllTabs();
                status = GameStatus.WAIT;

                // Reset the in-memory game state now that the world is freshly loaded.
                try {
                    init();
                } catch (Exception e) {
                    ghastWar.getLogger().severe("Failed to reset game state: " + e.getMessage());
                    e.printStackTrace();
                    // 兜底：重置失败也别把场地永久卡在 PROCESSING（否则交互/挖掘全被
                    // Protection 取消，表现为"乐魂不可点击、挖方块不掉落不消失"）
                    status = GameStatus.WAIT;
                }
            } catch (Exception e) {
                ghastWar.getLogger().severe("Failed to load world " + worldName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, runnable -> ghastWar.getServer().getScheduler().runTask(ghastWar, runnable));
    }

    public void init() throws IOException {
        status = GameStatus.PROCESSING;
        ghastWar.getLogger().info("Resetting game: "+name);
        this.stage = gameStage.WAIT;

        // 队伍清理必须最先做：后面任何一步抛异常，都不能让上一局的
        // 记分板队伍残留（否则下一局开局 registerNewTeam 会报 already in use，
        // 且场地会卡在 PROCESSING：挖方块被取消、乐魂不可交互）
        for (Team team : gameScoreboard.getAllTeams()){
            try {
                team.unRegister();
            } catch (Exception e) {
                ghastWar.getLogger().warning("Failed to unregister team " + team.getTeams().getDisplayName() + ": " + e.getMessage());
            }
        }
        gameScoreboard.clearTeams();
        teams.clear();

        if (this.bossBarCount != null){
            this.bossBarCount.cancel();
        }
        this.bossBarTime = -1;
        if (this.countTask != null){
            this.countTask.cancel();
        }
        upgradeGUI.unregister();
        if (circleShrinker != null){
            circleShrinker.stopShrinking();
        }

        rawBlocks.clear();
        teamChest.reset();
        cleanupAirdropSystem();
        skillManager.cleanup();
        armorManager.cleanup();
        statistics.cleanup();

        // The world has already been freshly reloaded from .slime by resetWorld(),
        // so init() only resets the in-memory game state (no world reload here).
        gameScoreboard.setGameState(GameScoreboard.GameState.WAITING);
        // 更新所有玩家的 Tab 显示
        updateAllTabs();
        timeCount = -1;

        players.clear();
        playerDatas.clear();
        ghasts.clear();
        gameScoreboard.init();
        resources.clear();
        neutralMiningAnnounced.clear();
        upgradeGUI = new UpgradeGUI(ghastWar,this);
        ghastWar.getLogger().info("Game reset successful: "+name);
        status = GameStatus.WAIT;
        sendReadyMessage();
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                //throw new RuntimeException("复制文件失败: " + sourcePath, e);
            }
        });
    }

    private void loadConfig(){
        File file = new File(this.ghastWar.getDataFolder()+"/arenas/"+this.world.getName()+".yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public List<Player> getPlayers() {
        return this.players;
    }

    public void addPlayer(Player player){
        Party party = HappyGhastWar.getInstance().getPartyManager().getParty(player);
        PartyManager partyManager = HappyGhastWar.getInstance().getPartyManager();

        if (party != null && party.isLeader(player)) {
            // 队长加入，检查整个Party
            List<Player> partyMembers = party.getOnlineMembers();
            int partySize = partyMembers.size();

            // 1. 检查Party人数是否超过队伍最大人数
            if (partySize > arenaConfig.getTeamSize()) {
                Text.send(player, ghastWar.getLanguage(player).getContent("game.party-too-large")
                        .replace("{0}", String.valueOf(partySize))
                        .replace("{1}", String.valueOf(arenaConfig.getTeamSize())));
                return;
            }

            // 2. 检查Party成员是否都可以加入
            for (Player member : partyMembers) {
                if (players.contains(member)) {
                    Text.send(player, "<red>" + member.getName() + " 已经在游戏中!");
                    return;
                }
            }

            // 3. 快速检查：加入Party后是否还能保证每个队伍都有玩家
            List<Player> allPlayersAfterJoin = new ArrayList<>(players);
            allPlayersAfterJoin.addAll(partyMembers);

            if (!TeamDivider.canStartCountdown(allPlayersAfterJoin, arenaConfig.getTeamCount(), partyManager)) {
                Text.send(player, ghastWar.getLanguage(player).getContent("game.party-not-enough-space"));
                return;
            }

            // 所有检查通过，加入所有Party成员
            for (Player member : partyMembers) {
                internalAddPlayer(member);
            }
        } else {
            // 独立玩家或非队长成员加入
            // 检查是否有空间（简单检查）
            List<Player> allPlayersAfterJoin = new ArrayList<>(players);
            allPlayersAfterJoin.add(player);

            if (!TeamDivider.canStartCountdown(allPlayersAfterJoin, arenaConfig.getTeamCount(), partyManager)) {
                Text.send(player, ghastWar.getLanguage(player).getContent("game.party-not-enough-space"));
                return;
            }

            // 如果是Party成员但不是队长，检查队长是否已经加入
            if (party != null && !party.isLeader(player)) {
                if (!players.contains(party.getLeader())) {
                    Text.send(player, "<red>请等待队长 " + party.getLeader().getName() + " 先加入游戏!");
                    return;
                }
            }

            internalAddPlayer(player);
        }
    }


    public void internalAddPlayer(Player player) {
        if (status != GameStatus.WAIT && status != GameStatus.COUNTING){

            joinAsSpectator(player);
            return;
        }
        for (Player player1 : players){
            Text.send(player1, ghastWar.getLanguage(player1).getContent("game.player-join-game").replace("{name}", player.getName()));
        }
        player.setPlayerTime(1000,false);
        player.setPlayerWeather(WeatherType.CLEAR);
        player.getInventory().clear();
        giveWaitEquipment(player);
        gameScoreboard.createScoreboard(player);
        players.add(player);
        player.teleport(arenaConfig.getWait());
        sendRefreshMessage();
    }

    public void joinAsSpectator(Player player){
        player.setPlayerTime(1000,false);
        player.setPlayerWeather(WeatherType.CLEAR);
        player.getInventory().clear();
        giveWaitEquipment(player);
        gameScoreboard.createScoreboard(player);
        player.teleport(arenaConfig.getWait());
        Text.send(player, ghastWar.getLanguage(player).getContent("commands.join-spectator"));
    }

    public void removePlayer(Player player){
        for (Player player1 : players){
            Text.send(player1, ghastWar.getLanguage(player1).getContent("game.player-leave-game").replace("{name}", player.getName()));
        }
        player.getInventory().clear();
        player.setGlowing(false);
        gameScoreboard.removeScoreboard(player);
        players.remove(player);
        playerDatas.remove(player);
        alloyMaker.removePlayer(player);
        bossBar.removePlayer(player);
        statistics.stopTracking(player);
        for (Team team : new ArrayList<>(teams)) {
            if (team.getPlayers().contains(player)){
                team.removePlayer(player);
            }
        }
        sendRefreshMessage();
    }

    public void removeGhast(GameGhast ghast){
        ghasts.remove(ghast.getHappyGhast());
    }

    // ======================================================================
    // 空投系统
    // ======================================================================

    /**
     * 启动空投系统
     */
    public void startAirdropSystem() {
        if (airdropTask != null) {
            airdropTask.cancel();
        }

        // 每3分钟检查一次，实际投放带随机延迟（3~5分钟）
        airdropTask = Bukkit.getScheduler().runTaskTimer(ghastWar, this::scheduleRandomAirdrop,
                AIRDROP_INTERVAL * 20L, AIRDROP_INTERVAL * 20L);
    }

    /**
     * 调度随机空投
     */
    private void scheduleRandomAirdrop() {
        // 仅战斗期/缩圈期/终局期触发空投
        if (status != GameStatus.PLAYING) return;
        if (stage != gameStage.Battle && stage != gameStage.Reduce && stage != gameStage.Ultimate && stage != gameStage.COUNT) return;

        Random random = new Random();
        // 随机延迟0-120秒（0-2分钟）
        int delay = random.nextInt(120) * 20;
        Bukkit.getScheduler().runTaskLater(ghastWar, this::createAirdrop, delay);
    }

    /**
     * 创建空投
     */
    private void createAirdrop() {
        if (world == null || status != GameStatus.PLAYING) return;

        Location airdropLocation = findSafeAirdropLocation();
        if (airdropLocation == null) return;

        broadcastAirdropWarning(airdropLocation);

        // 粒子下落动画 3 秒后落地生成箱子
        final Location dropLoc = airdropLocation;
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (status != GameStatus.PLAYING || ticks >= 60) {
                    cancel();
                    if (ticks >= 60) {
                        spawnAirdropCrate(dropLoc);
                    }
                    return;
                }
                // 从空中到地面的粒子轨迹
                double progress = ticks / 60.0;
                Location particleLoc = dropLoc.clone().add(0, 30 * (1 - progress), 0);
                world.spawnParticle(Particle.CLOUD, particleLoc, 8, 0.3, 0.3, 0.3, 0.01);
                ticks += 2;
            }
        }.runTaskTimer(ghastWar, 0L, 2L);
    }

    /**
     * 查找安全的空投位置（地表 + 圈内）
     */
    private Location findSafeAirdropLocation() {
        if (world == null) return null;

        Location center = arenaConfig.getCenter();
        if (center == null) return null;

        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            double x = center.getX() + random.nextInt(101) - 50;
            double z = center.getZ() + random.nextInt(101) - 50;

            Location testLoc = new Location(world, x, 0, z);
            testLoc.setY(world.getHighestBlockYAt((int) x, (int) z) + 1);

            // 检查是否在边界内且不在虚空上
            if (isWithinArenaBounds(testLoc) && testLoc.getY() > world.getMinHeight() + 1) {
                return testLoc;
            }
        }

        // 如果找不到安全位置，使用中心地表
        Location fallback = center.clone();
        fallback.setY(world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ()) + 1);
        return fallback;
    }

    /**
     * 检查位置是否在竞技场边界内
     */
    private boolean isWithinArenaBounds(Location location) {
        Location center = arenaConfig.getCenter();
        int radius = arenaConfig.getRadius();

        if (center == null) return false;
        if (location.getWorld() == null || !location.getWorld().equals(center.getWorld())) return false;

        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return Math.sqrt(dx * dx + dz * dz) <= radius;
    }

    /**
     * 广播空投警告
     */
    private void broadcastAirdropWarning(Location location) {
        for (Player player : world.getPlayers()) {
            String message = ghastWar.getLanguage(player).getContent("game.airdrop-warning")
                    .replace("{x}", String.valueOf(location.getBlockX()))
                    .replace("{y}", String.valueOf(location.getBlockY()))
                    .replace("{z}", String.valueOf(location.getBlockZ()));
            Text.send(player, message);
            SoundUtil.play(player, "airdrop-warning", Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f);
        }
        world.spawnParticle(Particle.SMOKE, location.clone().add(0, 20, 0), 40, 3, 3, 3, 0.02);
    }

    /**
     * 落地生成空投箱子（真实方块 + PDC 标识，供 AirdropListener 识别）
     */
    private void spawnAirdropCrate(Location location) {
        // 播放空降音效
        SoundUtil.playAt(world, location, "airdrop-land-pop", Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        SoundUtil.playAt(world, location, "airdrop-land", Sound.BLOCK_ANVIL_LAND, 0.6f, 1.4f);
        world.spawnParticle(Particle.CLOUD, location, 50, 2, 1, 2, 0.05);
        world.spawnParticle(Particle.GLOW, location, 20, 1, 1, 1, 0);

        Block block = location.getBlock();
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest chest) {
            // PDC 标识：这是空投箱
            chest.getPersistentDataContainer().set(HappyGhastWar.airdropKey, PersistentDataType.BYTE, (byte) 1);
            chest.update();

            fillAirdropChest(chest.getInventory());
        }

        // 箱子上方漂浮名称
        TextDisplay label = world.spawn(location.clone().add(0.5, 1.2, 0), TextDisplay.class);
        label.text(Text.mm(AIRDROP_LABEL));
        label.setBillboard(Display.Billboard.CENTER);
        label.setShadowed(true);

        activeAirdrops.add(block.getLocation());

        // 5分钟后自动消失
        Bukkit.getScheduler().runTaskLater(ghastWar, () -> removeAirdrop(block.getLocation(), label), 600L);
    }

    /**
     * 移除空投箱子及其漂浮名称
     */
    public void removeAirdrop(Location blockLocation, TextDisplay label) {
        if (label != null && label.isValid()) {
            label.remove();
        }
        if (blockLocation != null) {
            Block block = blockLocation.getBlock();
            if (block.getType() == Material.CHEST
                    && block.getState() instanceof Chest chest
                    && chest.getPersistentDataContainer().has(HappyGhastWar.airdropKey, PersistentDataType.BYTE)) {
                // 剩余物品掉落，然后移除方块
                for (ItemStack item : chest.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        blockLocation.getWorld().dropItemNaturally(blockLocation.clone().add(0.5, 1, 0.5), item);
                    }
                }
                chest.getInventory().clear();
                chest.getPersistentDataContainer().remove(HappyGhastWar.airdropKey);
                chest.update();
                block.setType(Material.AIR);
            }
            activeAirdrops.remove(blockLocation);
        }
    }

    /**
     * 填充空投箱子（高级物资 + 真实附魔书）
     */
    private void fillAirdropChest(org.bukkit.inventory.Inventory inventory) {
        Random random = new Random();

        // 空投物品配置
        ItemStack[] airdropItems = new ItemStack[]{
            // 下界合金锭（稀有）
            new ItemStack(Material.NETHERITE_INGOT, random.nextInt(2) + 1),
            // 附魔书
            createRandomEnchantedBook(random),
            // 金苹果（回复）
            new ItemStack(Material.GOLDEN_APPLE, random.nextInt(2) + 1),
            // 经验瓶
            new ItemStack(Material.EXPERIENCE_BOTTLE, random.nextInt(8) + 4),
            // 金锭
            new ItemStack(Material.GOLD_INGOT, random.nextInt(8) + 4),
            // 钻石
            new ItemStack(Material.DIAMOND, random.nextInt(3) + 1),
            // 铁锭
            new ItemStack(Material.IRON_INGOT, random.nextInt(12) + 8)
        };

        // 随机放入物品
        for (ItemStack item : airdropItems) {
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(item);
            }
        }
    }

    /**
     * 创建真实附魔的随机附魔书
     */
    private ItemStack createRandomEnchantedBook(Random random) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);

        Enchantment[] pool = {
                Enchantment.PROTECTION, Enchantment.SHARPNESS, Enchantment.POWER,
                Enchantment.UNBREAKING, Enchantment.FEATHER_FALLING, Enchantment.THORNS,
                Enchantment.EFFICIENCY, Enchantment.MENDING
        };
        Enchantment enchantment = pool[random.nextInt(pool.length)];
        int level = Math.min(enchantment.getMaxLevel(), 2 + random.nextInt(2));

        if (book.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            meta.addStoredEnchant(enchantment, level, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            book.setItemMeta(meta);
        }

        return book;
    }

    /**
     * 获取活动空投位置列表
     */
    public List<Location> getActiveAirdrops() {
        return new ArrayList<>(activeAirdrops);
    }

    /**
     * 玩家开启空投后移除记录
     */
    public void consumeAirdrop(Location blockLocation) {
        activeAirdrops.remove(blockLocation);
    }

    /**
     * 清理空投系统
     */
    public void cleanupAirdropSystem() {
        if (airdropTask != null) {
            airdropTask.cancel();
            airdropTask = null;
        }

        // 移除所有已落地的空投箱（含剩余物品掉落与漂浮名称）
        for (Location location : new ArrayList<>(activeAirdrops)) {
            if (location.getWorld() == null) continue;
            Block block = location.getBlock();
            if (block.getType() == Material.CHEST && block.getState() instanceof Chest chest) {
                for (ItemStack item : chest.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        location.getWorld().dropItemNaturally(location.clone().add(0.5, 1, 0.5), item);
                    }
                }
            }
            if (block.getType() == Material.CHEST) {
                block.setType(Material.AIR);
            }
            // 清掉附近漂浮的空投名称
            for (Entity entity : location.getWorld().getNearbyEntities(location.clone().add(0.5, 1, 0.5), 2, 2, 2)) {
                if (entity instanceof TextDisplay textDisplay && Text.mm(AIRDROP_LABEL).equals(textDisplay.text())) {
                    entity.remove();
                }
            }
        }
        activeAirdrops.clear();
    }
}
