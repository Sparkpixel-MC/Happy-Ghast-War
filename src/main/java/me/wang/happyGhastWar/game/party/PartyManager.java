package me.wang.happyGhastWar.game.party;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {

    public final Map<UUID, Party> playerParties;
    private final Map<UUID, Party> parties;

    public PartyManager() {
        this.playerParties = new HashMap<>();
        this.parties = new HashMap<>();
    }

    /**
     * 创建队伍
     */
    public Party createParty(Player leader) {
        // 检查玩家是否已经在队伍中
        if (hasParty(leader)) {
            return null;
        }

        Party party = new Party(leader);
        parties.put(party.getPartyId(), party);
        playerParties.put(leader.getUniqueId(), party);

        return party;
    }

    /**
     * 解散队伍
     */
    public boolean disbandParty(Player leader) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader)) {
            return false;
        }

        // 从所有映射中移除
        for (UUID memberId : party.getMembers().keySet()) {
            playerParties.remove(memberId);
        }
        parties.remove(party.getPartyId());

        party.disband(leader);
        return true;
    }

    /**
     * 获取玩家的队伍
     */
    public Party getParty(Player player) {
        return playerParties.get(player.getUniqueId());
    }

    /**
     * 通过ID获取队伍
     */
    public Party getParty(UUID partyId) {
        return parties.get(partyId);
    }

    /**
     * 检查玩家是否有队伍
     */
    public boolean hasParty(Player player) {
        return playerParties.containsKey(player.getUniqueId());
    }

    /**
     * 玩家离开队伍
     */
    public boolean leaveParty(Player player) {
        Party party = getParty(player);
        if (party == null) {
            return false;
        }

        boolean success = party.leaveParty(player);
        if (success) {
            playerParties.remove(player.getUniqueId());

            // 如果队伍为空，清理队伍
            if (party.getMemberCount() == 0) {
                parties.remove(party.getPartyId());
            }
        }

        return success;
    }

    /**
     * 踢出玩家
     */
    public boolean kickFromParty(Player kicker, String targetName) {
        Party party = getParty(kicker);
        if (party == null) {
            return false;
        }

        // 先按名字在成员表中解析出被踢玩家的 UUID（kick 成功后该成员已不在 members 里）
        UUID targetId = null;
        for (UUID memberId : party.getMembers().keySet()) {
            Player member = org.bukkit.Bukkit.getPlayer(memberId);
            if (member != null && member.getName().equalsIgnoreCase(targetName)) {
                targetId = memberId;
                break;
            }
        }

        boolean success = party.kickPlayer(kicker, targetName);
        if (success && targetId != null) {
            playerParties.remove(targetId);
        }

        return success;
    }

    /**
     * 获取所有队伍
     */
    public Map<UUID, Party> getAllParties() {
        return new HashMap<>(parties);
    }

    /**
     * 清理所有队伍（服务器关闭时）
     */
    public void cleanupAll() {
        for (Party party : parties.values()) {
            party.disband(null);
        }
        playerParties.clear();
        parties.clear();
    }
}