package me.wang.happyGhastWar.game.team;

import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.game.party.Party;
import me.wang.happyGhastWar.game.party.PartyManager;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class TeamDivider {

    public static List<Team> dividePlayers(List<Player> players, int numberOfTeams, int minPlayersPerTeam,
                                           Scoreboard scoreboard, PartyManager partyManager) {
        // 参数验证
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("玩家列表不能为空");
        }

        if (numberOfTeams <= 0) {
            throw new IllegalArgumentException("队伍数量必须大于0");
        }

        if (minPlayersPerTeam <= 0) {
            throw new IllegalArgumentException("每队最少人数必须大于0");
        }

        int totalPlayers = players.size();
        int requiredPlayers = numberOfTeams * minPlayersPerTeam;

        if (totalPlayers < requiredPlayers) {
            throw new IllegalArgumentException(
                    String.format("玩家数量不足。需要至少%d个玩家（%d队 × %d人/队），但只有%d个玩家",
                            requiredPlayers, numberOfTeams, minPlayersPerTeam, totalPlayers)
            );
        }

        // 检查队伍数量是否超过可用的队伍颜色
        Arena.Teams[] allTeams = Arena.Teams.values();
        if (numberOfTeams > allTeams.length) {
            throw new IllegalArgumentException(
                    String.format("队伍数量不能超过%d个（可用的队伍颜色数量）", allTeams.length)
            );
        }

        // 创建队伍
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < numberOfTeams; i++) {
            Arena.Teams teamColor = allTeams[i];
            Team team = new Team(teamColor, scoreboard);
            teams.add(team);
        }

        // 1. 按Party分组处理
        List<List<Player>> partyGroups = new ArrayList<>();
        Set<Player> processedPlayers = new HashSet<>();

        // 收集所有Party
        if (partyManager != null) {
            for (Player player : players) {
                if (processedPlayers.contains(player)) continue;

                Party party = partyManager.getParty(player);
                if (party != null) {
                    List<Player> partyMembers = new ArrayList<>();
                    for (Player member : party.getOnlineMembers()) {
                        if (players.contains(member)) {
                            partyMembers.add(member);
                            processedPlayers.add(member);
                        }
                    }
                    if (!partyMembers.isEmpty()) {
                        partyGroups.add(partyMembers);
                    }
                }
            }
        }

        //添加未分组的玩家
        for (Player player : players) {
            if (!processedPlayers.contains(player)) {
                partyGroups.add(Collections.singletonList(player));
            }
        }

        //按人数排序（从大到小），先分配大的Party
        partyGroups.sort((g1, g2) -> Integer.compare(g2.size(), g1.size()));

        //确保每队至少有minPlayersPerTeam人
        //优先分配Party，保持Party成员在同一队
        for (int round = 0; round < minPlayersPerTeam; round++) {
            for (int i = 0; i < numberOfTeams; i++) {
                if (partyGroups.isEmpty()) break;

                // 为这个队伍找到一个合适的Party/玩家
                List<Player> bestGroup = null;
                int bestGroupIndex = -1;

                for (int j = 0; j < partyGroups.size(); j++) {
                    List<Player> group = partyGroups.get(j);
                    if (!group.isEmpty()) {
                        // 检查这个队伍是否还有空间
                        if (teams.get(i).getSize() + group.size() <= getMaxTeamSize(players.size(), numberOfTeams)) {
                            bestGroup = group;
                            bestGroupIndex = j;
                            break;
                        }
                    }
                }

                if (bestGroup != null) {
                    for (Player player : bestGroup) {
                        teams.get(i).addPlayer(player);
                    }
                    partyGroups.remove(bestGroupIndex);
                }
            }
        }

        //分配剩余玩家
        if (!partyGroups.isEmpty()) {
            // 使用优先队列来平衡队伍人数
            PriorityQueue<Team> teamQueue = new PriorityQueue<>(
                    Comparator.comparingInt(Team::getSize)
            );
            teamQueue.addAll(teams);

            for (List<Player> group : partyGroups) {
                if (group.isEmpty()) continue;

                // 尝试将整个Party分配到同一队
                boolean groupAssigned = false;
                for (Team team : teams) {
                    if (team.getSize() + group.size() <= getMaxTeamSize(players.size(), numberOfTeams)) {
                        for (Player player : group) {
                            team.addPlayer(player);
                        }
                        groupAssigned = true;
                        break;
                    }
                }

                // 如果无法整组分配，则拆分分配到不同队伍
                if (!groupAssigned) {
                    for (Player player : group) {
                        Team smallestTeam = teamQueue.poll();
                        if (smallestTeam != null) {
                            smallestTeam.addPlayer(player);
                            teamQueue.add(smallestTeam);
                        }
                    }
                }
            }
        }

        return teams;
    }

    private static int getMaxTeamSize(int totalPlayers, int numberOfTeams) {
        int base = totalPlayers / numberOfTeams;
        int remainder = totalPlayers % numberOfTeams;
        return remainder > 0 ? base + 1 : base;
    }

    public static boolean canStartCountdown(List<Player> players, int numberOfTeams, PartyManager partyManager) {
        int totalPlayers = players.size();

        //总玩家数必须 >= 队伍数
        if (totalPlayers < numberOfTeams) {
            return false;
        }

        //如果没有Party系统，直接通过
        if (partyManager == null) {
            return true;
        }

        //统计独立玩家数量
        int soloPlayers = 0;
        Set<Party> countedParties = new HashSet<>();

        for (Player player : players) {
            Party party = partyManager.getParty(player);
            if (party == null) {
                soloPlayers++;
            } else if (!countedParties.contains(party)) {
                countedParties.add(party);
            }
        }

        // 如果有足够的独立玩家，可以开始
        if (soloPlayers >= numberOfTeams) {
            return true;
        }

        // 如果没有独立玩家，只有Party
        if (soloPlayers == 0) {
            // 检查是否有多个Party
            return countedParties.size() >= Math.min(2, numberOfTeams);
        }

        return true;
    }

    /**
     * 将玩家列表分配到指定数量的队伍中
     * @param players 玩家列表
     * @param numberOfTeams 队伍数量
     * @param minPlayersPerTeam 每队最少人数
     * @return 分配好的队伍列表
     * @throws IllegalArgumentException 如果参数不合法
     */
    public static List<Team> dividePlayers(List<Player> players, int numberOfTeams, int minPlayersPerTeam, Scoreboard scoreboard) {
        // 参数验证
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("玩家列表不能为空");
        }

        if (numberOfTeams <= 0) {
            throw new IllegalArgumentException("队伍数量必须大于0");
        }

        if (minPlayersPerTeam <= 0) {
            throw new IllegalArgumentException("每队最少人数必须大于0");
        }

        int totalPlayers = players.size();
        int requiredPlayers = numberOfTeams * minPlayersPerTeam;

        if (totalPlayers < requiredPlayers) {
            throw new IllegalArgumentException(
                    String.format("玩家数量不足。需要至少%d个玩家（%d队 × %d人/队），但只有%d个玩家",
                            requiredPlayers, numberOfTeams, minPlayersPerTeam, totalPlayers)
            );
        }

        //检查队伍数量是否超过可用的队伍颜色
        Arena.Teams[] allTeams = Arena.Teams.values();
        if (numberOfTeams > allTeams.length) {
            throw new IllegalArgumentException(
                    String.format("队伍数量不能超过%d个（可用的队伍颜色数量）", allTeams.length)
            );
        }

        //打乱玩家列表以确保随机分配
        List<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        //创建队伍
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < numberOfTeams; i++) {
            Arena.Teams teamColor = allTeams[i];
            Team team = new Team(teamColor,scoreboard);
            teams.add(team);
        }

        //确保每队至少有minPlayersPerTeam人
        for (int round = 0; round < minPlayersPerTeam; round++) {
            for (int i = 0; i < numberOfTeams; i++) {
                if (!shuffledPlayers.isEmpty()) {
                    Player player = shuffledPlayers.remove(0);
                    teams.get(i).addPlayer(player);
                }
            }
        }

        //分配剩余玩家
        if (!shuffledPlayers.isEmpty()) {
            // 计算每队可以额外分配多少玩家
            int remainingPlayers = shuffledPlayers.size();
            int baseExtraPlayers = remainingPlayers / numberOfTeams;
            int extraTeams = remainingPlayers % numberOfTeams; // 有些队伍会多一个人

            // 先给每个队伍分配基础额外玩家
            for (int i = 0; i < numberOfTeams; i++) {
                int extraForThisTeam = baseExtraPlayers;
                if (i < extraTeams) {
                    extraForThisTeam++; // 前extraTeams个队伍多分配一个人
                }

                for (int j = 0; j < extraForThisTeam && !shuffledPlayers.isEmpty(); j++) {
                    Player player = shuffledPlayers.remove(0);
                    teams.get(i).addPlayer(player);
                }
            }
        }

        return teams;
    }

    /**
     * 平衡分配玩家（尽量使每队人数相等）
     * @param players 玩家列表
     * @param numberOfTeams 队伍数量
     * @return 分配好的队伍列表
     */
    public static List<Team> dividePlayersBalanced(List<Player> players, int numberOfTeams, Scoreboard scoreboard) {
        return dividePlayers(players, numberOfTeams, 0, scoreboard);
    }

    /**
     * 根据玩家数量和期望的队伍大小自动计算队伍数量
     * @param players 玩家列表
     * @param targetTeamSize 期望的队伍大小
     * @return 分配好的队伍列表
     */
    public static List<Team> autoDividePlayers(List<Player> players, int targetTeamSize, Scoreboard scoreboard) {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("玩家列表不能为空");
        }

        if (targetTeamSize <= 0) {
            throw new IllegalArgumentException("期望的队伍大小必须大于0");
        }

        int totalPlayers = players.size();
        int numberOfTeams = (int) Math.ceil((double) totalPlayers / targetTeamSize);

        // 确保不超过最大队伍数量
        Arena.Teams[] allTeams = Arena.Teams.values();
        if (numberOfTeams > allTeams.length) {
            numberOfTeams = allTeams.length;
        }

        // 重新计算每队最少人数
        int minPlayersPerTeam = totalPlayers / numberOfTeams;

        return dividePlayers(players, numberOfTeams, minPlayersPerTeam, scoreboard);
    }

    /**
     * 获取队伍统计信息
     * @param teams 队伍列表
     * @return 统计信息字符串
     */
    public static String getTeamStats(List<Team> teams) {
        if (teams == null || teams.isEmpty()) {
            return "没有队伍";
        }

        StringBuilder stats = new StringBuilder();
        stats.append("队伍分配统计:\n");

        int totalPlayers = 0;
        int minPlayers = Integer.MAX_VALUE;
        int maxPlayers = 0;

        for (Team team : teams) {
            int teamSize = team.getSize();
            totalPlayers += teamSize;

            if (teamSize < minPlayers) minPlayers = teamSize;
            if (teamSize > maxPlayers) maxPlayers = teamSize;

            String teamColor = team.getTeams().getColor() + team.getTeams().getDisplayName();
            stats.append(String.format("  %s<white>队: %d名玩家\n", teamColor, teamSize));
        }

        stats.append("\n总计: ").append(totalPlayers).append("名玩家\n");
        stats.append("队伍数量: ").append(teams.size()).append("\n");
        stats.append("每队人数: ").append(minPlayers).append(" ~ ").append(maxPlayers).append("\n");

        if (teams.size() > 1) {
            double avgPlayers = (double) totalPlayers / teams.size();
            stats.append("平均每队: ").append(String.format("%.1f", avgPlayers)).append("人\n");
        }

        return stats.toString();
    }

    /**
     * 获取队伍详细信息（包括玩家名称）
     * @param teams 队伍列表
     * @return 详细信息字符串
     */
    public static String getTeamDetails(List<Team> teams) {
        if (teams == null || teams.isEmpty()) {
            return "没有队伍";
        }

        StringBuilder details = new StringBuilder();
        details.append("=== 队伍详情 ===\n");

        for (Team team : teams) {
            Arena.Teams teamEnum = team.getTeams();
            String teamColor = teamEnum.getColor() + teamEnum.getDisplayName();

            details.append("\n").append(teamColor).append("<white>队 (").append(team.getSize()).append("人):\n");

            List<Player> players = team.getPlayers();
            if (players.isEmpty()) {
                details.append("  暂无玩家\n");
            } else {
                for (int i = 0; i < players.size(); i++) {
                    Player player = players.get(i);
                    details.append("  ").append(i + 1).append(". ").append(player.getName()).append("\n");
                }
            }
        }

        return details.toString();
    }

    /**
     * 查找玩家所在的队伍
     * @param teams 队伍列表
     * @param player 要查找的玩家
     * @return 玩家所在的队伍，如果没找到返回null
     */
    public static Team findPlayerTeam(List<Team> teams, Player player) {
        if (teams == null || player == null) return null;

        for (Team team : teams) {
            if (team.getPlayers().contains(player)) {
                return team;
            }
        }
        return null;
    }

    /**
     * 检查是否有队伍为空
     * @param teams 队伍列表
     * @return 如果有队伍没有玩家返回true，否则返回false
     */
    public static boolean hasEmptyTeams(List<Team> teams) {
        if (teams == null) return false;

        for (Team team : teams) {
            if (team.getSize() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 重新平衡队伍（调整玩家以使各队人数更均衡）
     * @param teams 队伍列表
     */
    public static void rebalanceTeams(List<Team> teams) {
        if (teams == null || teams.size() <= 1) return;

        // 收集所有玩家
        List<Player> allPlayers = new ArrayList<>();
        for (Team team : teams) {
            allPlayers.addAll(team.getPlayers());
            team.getPlayers().clear(); // 清空队伍
        }

        // 重新分配
        Collections.shuffle(allPlayers);

        int teamIndex = 0;
        for (Player player : allPlayers) {
            teams.get(teamIndex).addPlayer(player);
            teamIndex = (teamIndex + 1) % teams.size();
        }
    }

    /**
     * 根据队伍大小排序队伍（从小到大）
     * @param teams 队伍列表
     */
    public static void sortTeamsBySize(List<Team> teams) {
        if (teams == null) return;
        teams.sort(Comparator.comparingInt(Team::getSize));
    }

    /**
     * 根据队伍大小排序队伍（从大到小）
     * @param teams 队伍列表
     */
    public static void sortTeamsBySizeDesc(List<Team> teams) {
        if (teams == null) return;
        teams.sort((t1, t2) -> Integer.compare(t2.getSize(), t1.getSize()));
    }

    /**
     * 获取人数最多的队伍
     * @param teams 队伍列表
     * @return 人数最多的队伍，如果有多个返回第一个
     */
    public static Team getLargestTeam(List<Team> teams) {
        if (teams == null || teams.isEmpty()) return null;

        Team largest = teams.get(0);
        for (Team team : teams) {
            if (team.getSize() > largest.getSize()) {
                largest = team;
            }
        }
        return largest;
    }

    /**
     * 获取人数最少的队伍
     * @param teams 队伍列表
     * @return 人数最少的队伍，如果有多个返回第一个
     */
    public static Team getSmallestTeam(List<Team> teams) {
        if (teams == null || teams.isEmpty()) return null;

        Team smallest = teams.get(0);
        for (Team team : teams) {
            if (team.getSize() < smallest.getSize()) {
                smallest = team;
            }
        }
        return smallest;
    }

    /**
     * 移动玩家到另一个队伍
     * @param fromTeam 原队伍
     * @param toTeam 目标队伍
     * @param player 要移动的玩家
     * @return 如果移动成功返回true，否则返回false
     */
    public static boolean movePlayer(Team fromTeam, Team toTeam, Player player) {
        if (fromTeam == null || toTeam == null || player == null) return false;

        if (!fromTeam.getPlayers().contains(player)) return false;

        fromTeam.removePlayer(player);
        toTeam.addPlayer(player);
        return true;
    }

    public static void swapTeams(Team team1, Team team2) {
        if (team1 == null || team2 == null) return;

        List<Player> temp = new ArrayList<>(team1.getPlayers());
        team1.getPlayers().clear();
        team1.getPlayers().addAll(team2.getPlayers());

        team2.getPlayers().clear();
        team2.getPlayers().addAll(temp);
    }
}
