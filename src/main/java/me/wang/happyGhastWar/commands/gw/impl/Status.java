package me.wang.happyGhastWar.commands.gw.impl;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.commands.gw.GWCommand;
import me.wang.happyGhastWar.game.team.Team;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /gw status - 查看当前场地状态（阶段/队伍/乐魂/空投/技能）
 */
public class Status extends GWCommand {

    public Status(){
        super("status", true, "info");
    }

    @Override
    public void evaluate(HappyGhastWar ghastWar, Player player, String s, List<String> params) {
        Arena arena = Stats.findArena(player);
        if (arena == null) {
            player.sendMessage("你不在任何竞技场中");
            return;
        }

        showGameStatus(player, arena);
    }

    /**
     * 显示游戏状态
     */
    private void showGameStatus(Player player, Arena arena) {
        StringBuilder statusMessage = new StringBuilder();

        statusMessage.append("<gold>=== 游戏状态 ===\n");
        statusMessage.append("<yellow>竞技场: <white>").append(arena.getName()).append("\n");
        statusMessage.append("<yellow>游戏阶段: <white>").append(getStageName(arena.stage)).append("\n");
        statusMessage.append("<yellow>游戏状态: <white>").append(arena.status.name()).append("\n");

        // 显示时间信息
        if (arena.bossBarTime > 0) {
            statusMessage.append("<yellow>剩余时间: <white>").append(arena.bossBarTime).append("秒\n");
        }

        // 显示队伍信息
        statusMessage.append("\n<gold>=== 队伍信息 ===\n");
        for (Team team : arena.getTeams()) {
            String teamColor = team.getTeams().getColor().toString();
            statusMessage.append(teamColor).append(team.getTeams().getDisplayName())
                    .append(" <white>- 玩家: ").append(team.getPlayers().size())
                    .append(", 乐魂: ").append(team.getGhasts().size())
                    .append(", 复活: ").append(team.isCanRespawn() ? "可用" : "不可用").append("\n");
        }

        // 显示空投信息
        statusMessage.append("\n<gold>=== 空投信息 ===\n");
        statusMessage.append("<yellow>活动空投: <white>").append(arena.getActiveAirdrops().size()).append(" 个\n");

        // 显示技能冷却信息（如果玩家骑乘乐魂）
        for (java.util.Map.Entry<HappyGhast, me.wang.happyGhastWar.ghast.GameGhast> entry : arena.getGhasts().entrySet()) {
            if (entry.getKey().getPassengers().contains(player)) {
                statusMessage.append("\n<gold>=== 技能状态 ===\n");
                statusMessage.append("<yellow>技能: <white>").append(entry.getValue().getSkillCooldownInfo()).append("\n");
                statusMessage.append("<yellow>护甲: <white>").append(entry.getValue().getArmorUpgradeInfo());
                break;
            }
        }

        Text.send(player, statusMessage.toString());
    }

    /**
     * 获取阶段名称
     */
    private String getStageName(Arena.gameStage stage) {
        return switch (stage) {
            case Development -> "开发阶段";
            case Battle -> "战斗阶段";
            case Reduce -> "收缩阶段";
            case Ultimate -> "终极阶段";
            case WAIT -> "等待阶段";
            case END -> "结束阶段";
            case COUNT -> "倒计时阶段";
        };
    }
}
