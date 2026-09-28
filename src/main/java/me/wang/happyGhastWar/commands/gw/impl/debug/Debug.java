package me.wang.happyGhastWar.commands.gw.impl.debug;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.game.team.Team;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * /gw debug - 管理员调试命令
 * <ul>
 *   <li>/gw debug info  - 打印当前场地内部状态（阶段/状态机/队伍/乐魂/资源点数量）</li>
 *   <li>/gw debug start - 跳过等待，立刻将倒计时压到 5 秒</li>
 * </ul>
 */
public class Debug extends GWCommand {
    private static final List<GWCommand> COMMANDS = ImmutableList.of(new DebugInfo(), new DebugStart());
    private final Map<String,GWCommand> commands;

    public Debug(){
        super("debug",true);

        ImmutableMap.Builder<String, GWCommand> commands = ImmutableMap.builder();

        for(GWCommand command : COMMANDS) {
            command.getCommands().forEach((label) -> commands.put(label, command));
        }

        this.commands = commands.build();
    }

    @Override
    public void evaluate(HappyGhastWar happyGhastWar, Player commandSender, String s, List<String> params) throws IOException {

        if (params.isEmpty()) {
            Text.send(commandSender, "<gold>/gw debug info <gray>- 打印场地内部状态");
            Text.send(commandSender, "<gold>/gw debug start <gray>- 跳过等待立刻开始倒计时");
        } else {
            String search = params.getFirst().toLowerCase(Locale.ROOT);
            GWCommand target = this.commands.get(search);
            if (target == null) {
                Text.send(commandSender, "<red>Unknown command <gray>debug " + search);
            } else {
                String permission = target.getPermission();
                if (permission != null && !permission.isEmpty() && !commandSender.hasPermission(permission)) {
                    Text.send(commandSender, "<red>You do not have permission to do this!");
                }else {
                    target.evaluate(happyGhastWar, commandSender, search, params.subList(1, params.size()));
                }
            }
        }
    }

    @Override
    public void complete(HappyGhastWar plugin, CommandSender sender, String alias, List<String> params, List<String> suggestions) {
        if (params.size() <= 1) {
            Stream<String> targets = filterByPermission(sender, this.commands.values().stream()).map(GWCommand::getCommands).flatMap(Collection::stream);
            suggestByParameter(targets, suggestions, params.isEmpty() ? null : params.getFirst());
        }
    }

    static {
        COMMANDS.forEach((command) -> command.setPermission("gw.debug." + command.getCommand()));
    }

    /** /gw debug info */
    private static class DebugInfo extends GWCommand {
        DebugInfo() {
            super("info", true);
        }

        @Override
        public void evaluate(HappyGhastWar happyGhastWar, Player player, String s, List<String> params) {
            Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
            if (arena == null) {
                Text.send(player, "<red>你不在任何竞技场世界");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<gold>=== Debug: ").append(arena.getName()).append(" ===\n");
            sb.append("<yellow>状态: <white>").append(arena.status).append("  <yellow>阶段: <white>").append(arena.stage)
              .append("  <yellow>BossBar时间: <white>").append(arena.bossBarTime).append("\n");
            sb.append("<yellow>玩家: <white>").append(arena.getPlayers().size())
              .append("  <yellow>队伍: <white>").append(arena.getTeams().size())
              .append("  <yellow>乐魂: <white>").append(arena.getGhasts().size()).append("\n");
            sb.append("<yellow>资源点: <white>").append(arena.getResources().size())
              .append("  <yellow>空投: <white>").append(arena.getActiveAirdrops().size())
              .append("  <yellow>缩圈: <white>").append(arena.circleShrinker == null ? "未启动" : arena.circleShrinker.getStatus()).append("\n");
            for (Team team : arena.getTeams()) {
                sb.append(team.getTeams().getColor()).append("[").append(team.getTeams().getDisplayName()).append("]")
                  .append("<white> 玩家x").append(team.getSize()).append(" 乐魂x").append(team.getGhastSize())
                  .append(" 复活:").append(team.isCanRespawn()).append(" 存活:").append(team.isAlive()).append("\n");
            }
            Text.send(player, sb.toString());
        }
    }

    /** /gw debug start */
    private static class DebugStart extends GWCommand {
        DebugStart() {
            super("start", true);
        }

        @Override
        public void evaluate(HappyGhastWar happyGhastWar, Player player, String s, List<String> params) {
            Arena arena = HappyGhastWar.arenas.get(player.getWorld().getName());
            if (arena == null) {
                Text.send(player, "<red>你不在任何竞技场世界");
                return;
            }
            if (arena.status != Arena.GameStatus.WAIT && arena.status != Arena.GameStatus.COUNTING) {
                Text.send(player, "<red>游戏已经开始，无法跳过等待");
                return;
            }
            if (arena.getPlayers().isEmpty()) {
                Text.send(player, "<red>场地内没有玩家，先 /gw join 加入");
                return;
            }
            // 把状态拉回 WAIT 以触发 handleWait 中的倒计时启动逻辑
            arena.status = Arena.GameStatus.WAIT;
            Text.send(player, "<green>已跳过等待，倒计时即将开始（如人数不足仍无法开始）");
        }
    }
}
