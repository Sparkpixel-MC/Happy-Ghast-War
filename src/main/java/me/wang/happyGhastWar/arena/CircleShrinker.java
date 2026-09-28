package me.wang.happyGhastWar.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 缩圈检测类
 * 用于检测生物是否在指定圆形区域外
 */
public class CircleShrinker {

    private final Location center;
    private double radius;
    private final boolean checkYAxis;
    private double minY;
    private double maxY;

    public CircleShrinker(Location center, double radius) {
        this.center = center.clone();
        this.radius = Math.max(0, radius);
        this.checkYAxis = false;
    }

    public CircleShrinker(Location center, double radius, double minY, double maxY) {
        this.center = center.clone();
        this.radius = Math.max(0, radius);
        this.checkYAxis = true;
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
    }

    public boolean isOutsideCircle(Entity entity) {
        if (entity == null || entity.isDead()) return false;

        Location entityLoc = entity.getLocation();

        // 检查世界是否相同
        if (!center.getWorld().equals(entityLoc.getWorld())) {
            return true; // 不同世界视为在圈外
        }

        // 计算水平距离
        double distanceSquared = calculateHorizontalDistanceSquared(center, entityLoc);

        // 二维检测
        if (!checkYAxis) {
            return distanceSquared > radius * radius;
        }

        // 三维检测 - 检查Y轴
        boolean insideYRange = entityLoc.getY() >= minY && entityLoc.getY() <= maxY;
        boolean insideHorizontal = distanceSquared <= radius * radius;

        // 在圈内需要同时满足水平距离和Y轴范围
        return !(insideHorizontal && insideYRange);
    }

    public boolean isPlayerOutsideCircle(Player player) {
        return isOutsideCircle((Entity) player);
    }

    public double getDistanceToCircleEdge(Player player) {
        if (player == null || player.isDead()) return Double.NaN;

        Location playerLoc = player.getLocation();

        if (!center.getWorld().equals(playerLoc.getWorld())) {
            return Double.NaN;
        }

        double distance = calculateHorizontalDistance(center, playerLoc);
        return distance - radius; // 正数=在圈外，负数=在圈内
    }

    public List<Player> getPlayersInsideCircle() {
        List<Player> players = center.getWorld().getPlayers();
        List<Player> insidePlayers = new ArrayList<>();

        for (Player player : players) {
            if (!isPlayerOutsideCircle(player)) {
                insidePlayers.add(player);
            }
        }

        return insidePlayers;
    }

    public List<Player> getPlayersOutsideCircle() {
        List<Player> players = center.getWorld().getPlayers();
        List<Player> outsidePlayers = new ArrayList<>();

        for (Player player : players) {
            if (isPlayerOutsideCircle(player)) {
                outsidePlayers.add(player);
            }
        }

        return outsidePlayers;
    }

    public boolean shrinkTo(double targetRadius, double speed) {
        if (radius <= targetRadius) return true;

        radius = Math.max(targetRadius, radius - Math.abs(speed));
        return radius <= targetRadius;
    }

    public double getRadius() {
        return radius;
    }

    public Location getCenter() {
        return center.clone();
    }

    private double calculateHorizontalDistanceSquared(Location loc1, Location loc2) {
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return dx * dx + dz * dz;
    }


    private double calculateHorizontalDistance(Location loc1, Location loc2) {
        return Math.sqrt(calculateHorizontalDistanceSquared(loc1, loc2));
    }


    @Override
    public String toString() {
        String worldName = center.getWorld() != null ? center.getWorld().getName() : "null";
        if (checkYAxis) {
            return String.format("CircleShrinker{world=%s, x=%.1f, y=%.1f, z=%.1f, radius=%.1f, yRange=[%.1f, %.1f]}",
                    worldName, center.getX(), center.getY(), center.getZ(), radius, minY, maxY);
        } else {
            return String.format("CircleShrinker{world=%s, x=%.1f, y=%.1f, z=%.1f, radius=%.1f}",
                    worldName, center.getX(), center.getY(), center.getZ(), radius);
        }
    }


    public String getInfo() {
        int playersInside = getPlayersInsideCircle().size();
        int playersOutside = getPlayersOutsideCircle().size();
        return String.format("半径: %.1f, 圈内玩家: %d, 圈外玩家: %d", radius, playersInside, playersOutside);
    }
}