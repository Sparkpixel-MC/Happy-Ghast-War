package me.wang.happyGhastWar.game.party;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.util.SoundUtil;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Party {

    public enum PartyRole {
        LEADER,
        MEMBER
    }

    public enum InviteStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED
    }

    public final UUID partyId;
    public Player leader;
    public final Map<UUID, PartyRole> members;
    private final Map<UUID, PartyInvite> pendingInvites;
    private final Set<UUID> partyChatEnabled;
    private BukkitTask cleanupTask;
    public String partyName;

    public Party(Player leader) {
        this.partyId = UUID.randomUUID();
        this.leader = leader;
        this.members = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
        this.partyChatEnabled = ConcurrentHashMap.newKeySet();
        this.partyName = leader.getName() + "'s Party";

        // 添加队长为成员
        members.put(leader.getUniqueId(), PartyRole.LEADER);

        // 启动清理任务
        startCleanupTask();
    }



    /**
     * 邀请玩家加入队伍
     */
    public boolean invitePlayer(Player inviter, Player target) {
        // 检查目标是否已经在队伍中
        if (isMember(target)) {
            Text.send(inviter, "<red>" + target.getName() + " 已经在队伍中!");
            return false;
        }

        // 检查目标是否已经有待处理的邀请
        if (pendingInvites.containsKey(target.getUniqueId())) {
            Text.send(inviter, "<yellow>" + target.getName() + " 已经有待处理的邀请!");
            return false;
        }

        // 创建邀请
        PartyInvite invite = new PartyInvite(inviter.getUniqueId(), target.getUniqueId(), partyId);
        pendingInvites.put(target.getUniqueId(), invite);

        // 发送消息
        String message = HappyGhastWar.getInstance().getLanguage(inviter).getContent("party.invite-sent")
                .replace("{inviter}", inviter.getName())
                .replace("{target}", target.getName());
        sendMessageToAll("<green>" + message);

        // 发送邀请消息给目标玩家
        String inviteMsg = HappyGhastWar.getInstance().getLanguage(target).getContent("party.invite-received")
                .replace("{inviter}", inviter.getName())
                .replace("{party}", partyName);
        Text.send(target, "<gold>" + inviteMsg);
        Text.send(target, "<yellow>使用 <green>/party accept " + inviter.getName() + "<yellow> 接受邀请");
        Text.send(target, "<yellow>使用 <red>/party decline " + inviter.getName() + "<yellow> 拒绝邀请");

        // 播放声音（如果支持）
        SoundUtil.play(target, "party-notify", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        return true;
    }

    /**
     * 接受邀请
     */
    public boolean acceptInvite(Player player) {
        PartyInvite invite = pendingInvites.get(player.getUniqueId());
        if (invite == null) {
            Text.send(player, "<red>没有待处理的邀请!");
            return false;
        }

        // 检查邀请是否过期
        if (System.currentTimeMillis() - invite.getInviteTime() > 120000) { // 2分钟过期
            pendingInvites.remove(player.getUniqueId());
            Text.send(player, "<red>邀请已过期!");
            return false;
        }

        // 添加到队伍
        members.put(player.getUniqueId(), PartyRole.MEMBER);
        pendingInvites.remove(player.getUniqueId());

        // 发送加入消息
        String joinMessage = HappyGhastWar.getInstance().getLanguage(player).getContent("party.player-joined")
                .replace("{player}", player.getName());
        sendMessageToAll("<green>" + joinMessage);

        return true;
    }

    /**
     * 拒绝邀请
     */
    public boolean declineInvite(Player player) {
        PartyInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null) {
            Text.send(player, "<red>没有待处理的邀请!");
            return false;
        }

        Player inviter = Bukkit.getPlayer(invite.getInviterId());
        if (inviter != null && inviter.isOnline()) {
            String declineMsg = HappyGhastWar.getInstance().getLanguage(inviter).getContent("party.invite-declined")
                    .replace("{player}", player.getName());
            Text.send(inviter, "<red>" + declineMsg);
        }

        Text.send(player, "<yellow>已拒绝组队邀请");
        return true;
    }

    /**
     * 踢出玩家
     */
    public boolean kickPlayer(Player kicker, String targetName) {
        // 检查权限
        if (members.get(kicker.getUniqueId()) != PartyRole.LEADER) {
            Text.send(kicker, "<red>只有队长可以踢出队员!");
            return false;
        }

        // 查找目标玩家
        Player target = null;
        for (UUID memberId : members.keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.getName().equalsIgnoreCase(targetName)) {
                target = member;
                break;
            }
        }

        if (target == null) {
            Text.send(kicker, "<red>找不到玩家 " + targetName + "!");
            return false;
        }

        // 不能踢出自己
        if (target.getUniqueId().equals(kicker.getUniqueId())) {
            Text.send(kicker, "<red>不能踢出自己!");
            return false;
        }

        // 踢出玩家
        members.remove(target.getUniqueId());

        // 发送消息
        String kickMsg = HappyGhastWar.getInstance().getLanguage(kicker).getContent("party.player-kicked")
                .replace("{player}", target.getName())
                .replace("{kicker}", kicker.getName());
        sendMessageToAll("<red>" + kickMsg);

        Text.send(target, "<red>你被踢出了队伍!");

        return true;
    }

    /**
     * 离开队伍
     */
    public boolean leaveParty(Player player) {
        // 队长离开需要转让队长权限或解散队伍
        if (members.get(player.getUniqueId()) == PartyRole.LEADER) {
            // 如果有其他成员，转让队长权限
            if (members.size() > 1) {
                // 查找第一个其他成员作为新队长
                for (UUID memberId : members.keySet()) {
                    if (!memberId.equals(player.getUniqueId())) {
                        Player newLeader = Bukkit.getPlayer(memberId);
                        if (newLeader != null) {
                            setLeader(newLeader);
                            break;
                        }
                    }
                }
            }
        }

        // 移除玩家
        members.remove(player.getUniqueId());
        partyChatEnabled.remove(player.getUniqueId());

        String leaveMsg = HappyGhastWar.getInstance().getLanguage(player).getContent("party.player-left")
                .replace("{player}", player.getName());
        sendMessageToAll("<yellow>" + leaveMsg);

        return true;
    }

    /**
     * 转让队长权限
     */
    public boolean transferLeadership(Player currentLeader, String newLeaderName) {
        // 检查权限
        if (members.get(currentLeader.getUniqueId()) != PartyRole.LEADER) {
            Text.send(currentLeader, "<red>只有队长可以转让权限!");
            return false;
        }

        // 查找新队长
        Player newLeader = null;
        for (UUID memberId : members.keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.getName().equalsIgnoreCase(newLeaderName)) {
                newLeader = member;
                break;
            }
        }

        if (newLeader == null) {
            Text.send(currentLeader, "<red>找不到队员 " + newLeaderName + "!");
            return false;
        }

        // 转让权限
        setLeader(newLeader);

        String transferMsg = HappyGhastWar.getInstance().getLanguage(newLeader).getContent("party.leadership-transferred")
                .replace("{oldLeader}", currentLeader.getName())
                .replace("{newLeader}", newLeader.getName());
        sendMessageToAll("<gold>" + transferMsg);

        return true;
    }

    /**
     * 解散队伍
     */
    public void disband(Player disbander) {
        // 发送解散消息
        String disbandMsg = HappyGhastWar.getInstance().getLanguage(disbander).getContent("party.disbanded")
                .replace("{player}", disbander.getName());
        sendMessageToAll("<red>" + disbandMsg);

        // 清理所有邀请
        pendingInvites.clear();

        // 取消清理任务
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
    }

    /**
     * 发送消息给所有成员
     */
    public void sendMessageToAll(String message) {
        for (UUID memberId : members.keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                Text.send(member, message);
            }
        }
    }

    /**
     * 设置新队长
     */
    private void setLeader(Player newLeader) {
        members.put(leader.getUniqueId(), PartyRole.MEMBER);
        members.put(newLeader.getUniqueId(), PartyRole.LEADER);
        leader = newLeader;
    }


    /**
     * 启动邀请清理任务
     */
    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                HappyGhastWar.getInstance(),
                this::cleanupExpiredInvites,
                20L * 30, // 30秒后开始
                20L * 30  // 每30秒执行一次
        );
    }

    /**
     * 清理过期的邀请
     */
    private void cleanupExpiredInvites() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PartyInvite>> iterator = pendingInvites.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, PartyInvite> entry = iterator.next();
            PartyInvite invite = entry.getValue();

            if (currentTime - invite.getInviteTime() > 120000) { // 2分钟过期
                iterator.remove();

                Player target = Bukkit.getPlayer(entry.getKey());
                if (target != null && target.isOnline()) {
                    Text.send(target, "<red>组队邀请已过期!");
                }
            }
        }
    }


    public UUID getPartyId() {
        return partyId;
    }

    public Player getLeader() {
        return leader;
    }

    public Map<UUID, PartyRole> getMembers() {
        return new HashMap<>(members);
    }

    public List<Player> getOnlineMembers() {
        List<Player> onlineMembers = new ArrayList<>();
        for (UUID memberId : members.keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                onlineMembers.add(member);
            }
        }
        return onlineMembers;
    }

    public int getMemberCount() {
        return members.size();
    }

    public int getMaxSize() {
        return 8; // 可以配置的最大队伍人数
    }

    public boolean isMember(Player player) {
        return members.containsKey(player.getUniqueId());
    }

    public boolean isLeader(Player player) {
        return leader.getUniqueId().equals(player.getUniqueId());
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public Map<UUID, PartyInvite> getPendingInvites() {
        return new HashMap<>(pendingInvites);
    }

    /**
     * 邀请信息类
     */
    public static class PartyInvite {
        private final UUID inviterId;
        private final UUID targetId;
        private final UUID partyId;
        private final long inviteTime;

        public PartyInvite(UUID inviterId, UUID targetId, UUID partyId) {
            this.inviterId = inviterId;
            this.targetId = targetId;
            this.partyId = partyId;
            this.inviteTime = System.currentTimeMillis();
        }

        public UUID getInviterId() {
            return inviterId;
        }

        public UUID getTargetId() {
            return targetId;
        }

        public UUID getPartyId() {
            return partyId;
        }

        public long getInviteTime() {
            return inviteTime;
        }
    }
}