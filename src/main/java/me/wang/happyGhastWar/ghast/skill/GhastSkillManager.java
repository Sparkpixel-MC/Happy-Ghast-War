package me.wang.happyGhastWar.ghast.skill;

import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.ghast.GameGhast;
import me.wang.happyGhastWar.util.SoundUtil;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 乐魂技能管理器
 */
public class GhastSkillManager {

    private final HappyGhastWar ghastWar;
    private final Arena arena;
    private final Map<GameGhast, SkillCooldown> skillCooldowns = new HashMap<>();

    public GhastSkillManager(HappyGhastWar ghastWar, Arena arena) {
        this.ghastWar = ghastWar;
        this.arena = arena;
    }

    /**
     * 技能冷却管理
     */
    private static class SkillCooldown {
        private long lastUsage;
        private final long cooldown;

        public SkillCooldown(long cooldown) {
            this.cooldown = cooldown;
            this.lastUsage = System.currentTimeMillis();
        }

        public boolean isReady() {
            return System.currentTimeMillis() - lastUsage >= cooldown;
        }

        public void use() {
            this.lastUsage = System.currentTimeMillis();
        }

        public long getTimeRemaining() {
            long remaining = cooldown - (System.currentTimeMillis() - lastUsage);
            return Math.max(0, remaining / 1000); // 返回秒数
        }
    }

    /**
     * 注册乐魂技能
     */
    public void registerSkills() {
        // 为每个乐魂设置技能冷却
        for (GameGhast ghast : arena.getGhasts().values()) {
            // 基础技能冷却时间：30秒
            long cooldown = 30000L; // 30秒
            skillCooldowns.put(ghast, new SkillCooldown(cooldown));
        }
    }

    /**
     * 使用技能
     */
    public void useSkill(GameGhast ghast, Player rider) {
        SkillCooldown cooldown = skillCooldowns.get(ghast);
        if (cooldown == null) {
            // 兜底：未注册的乐魂自动注册冷却
            registerSkills();
            cooldown = skillCooldowns.get(ghast);
            if (cooldown == null) {
                Text.send(rider, "<red>该乐魂未注册技能");
                return;
            }
        }
        if (!cooldown.isReady()) {
            long remaining = cooldown.getTimeRemaining();
            Text.send(rider, "<yellow>技能冷却中，还需 " + remaining + " 秒");
            return;
        }

        // 随机选择一个技能
        Random random = new Random();
        int skillType = random.nextInt(4); // 0: 火球爆发, 1: 速度提升, 2: 防护罩, 3: 爆炸冲击

        switch (skillType) {
            case 0:
                fireballBurst(ghast, rider);
                break;
            case 1:
                speedBoost(ghast, rider);
                break;
            case 2:
                protectiveShield(ghast, rider);
                break;
            case 3:
                explosionShock(ghast, rider);
                break;
        }

        // 设置技能冷却
        cooldown.use();
    }

    /**
     * 技能1：火球爆发
     */
    private void fireballBurst(GameGhast ghast, Player rider) {
        HappyGhast entity = ghast.getHappyGhast();
        Location location = entity.getLocation();

        // 发送技能提示
        Text.send(rider, "<red>使用技能：火球爆发！");
        SoundUtil.playAt(location.getWorld(), location, "skill-fireball", Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);

        // 向8个方向发射火球
        for (int i = 0; i < 8; i++) {
            double angle = (i * Math.PI * 2) / 8;
            Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle));

            // World 上没有 launchProjectile（该方法在 ProjectileSource 上），改为直接在乐魂位置生成火球
            Fireball fireball = location.getWorld().spawn(location, Fireball.class);
            fireball.setDirection(direction.multiply(2));
            fireball.setYield(3.0f); // 增加爆炸威力

            // 设置火球追踪效果
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!fireball.isValid()) {
                        cancel();
                        return;
                    }

                    // 添加火焰粒子
                    fireball.getWorld().spawnParticle(Particle.FLAME, fireball.getLocation(), 5, 0.2, 0.2, 0.2, 0);
                }
            }.runTaskTimer(ghastWar, 0, 5);
        }
    }

    /**
     * 技能2：速度提升
     */
    private void speedBoost(GameGhast ghast, Player rider) {
        HappyGhast entity = ghast.getHappyGhast();

        // 发送技能提示
        Text.send(rider, "<blue>使用技能：速度提升！");

        // 增加速度
        if (entity.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.3); // 正常速度是0.2
        }

        // 添加粒子效果
        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (duration >= 100 || !entity.isValid()) { // 5秒
                    if (entity.isValid() && entity.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                        entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.2);
                    }
                    cancel();
                    return;
                }

                entity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, entity.getLocation(), 3, 0.5, 0.5, 0.5, 0);
                duration += 5;
            }
        }.runTaskTimer(ghastWar, 0, 5);
    }

    /**
     * 技能3：防护罩
     */
    private void protectiveShield(GameGhast ghast, Player rider) {
        HappyGhast entity = ghast.getHappyGhast();
        Location location = entity.getLocation();

        // 发送技能提示
        Text.send(rider, "<green>使用技能：防护罩！");

        // 创建防护罩效果（粒子跟随乐魂当前位置）
        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (duration >= 150 || !entity.isValid()) { // 7.5秒
                    cancel();
                    return;
                }

                Location current = entity.getLocation();
                // 创建旋转的粒子圈
                for (int i = 0; i < 8; i++) {
                    double angle = (duration * 0.1 + i * Math.PI * 2) / 8;
                    double x = Math.cos(angle) * 2;
                    double z = Math.sin(angle) * 2;

                    current.getWorld().spawnParticle(
                        Particle.SPLASH,
                        current.clone().add(x, 1, z),
                        1, 0, 0, 0, 0
                    );
                }

                duration += 5;
            }
        }.runTaskTimer(ghastWar, 0, 5);
    }

    /**
     * 技能4：爆炸冲击
     */
    private void explosionShock(GameGhast ghast, Player rider) {
        HappyGhast entity = ghast.getHappyGhast();
        Location location = entity.getLocation();

        // 发送技能提示
        Text.send(rider, "<dark_purple>使用技能：爆炸冲击！");

        // 在周围生成多个爆炸
        new BukkitRunnable() {
            int explosionCount = 0;
            @Override
            public void run() {
                if (explosionCount >= 5) {
                    cancel();
                    return;
                }

                // 随机生成爆炸位置
                Random random = new Random();
                double x = location.getX() + random.nextDouble() * 10 - 5;
                double z = location.getZ() + random.nextDouble() * 10 - 5;
                Location explosionLoc = new Location(location.getWorld(), x, location.getY(), z);

                // 创建爆炸
                explosionLoc.getWorld().createExplosion(explosionLoc, 2.0f, false, false);

                // 添加粒子效果
                explosionLoc.getWorld().spawnParticle(Particle.EXPLOSION, explosionLoc, 20, 1, 1, 1, 0);

                // 推开附近的敌人
                for (Entity nearby : explosionLoc.getWorld().getNearbyEntities(explosionLoc, 5, 5, 5)) {
                    if (nearby instanceof Player && nearby != rider) {
                        Player target = (Player) nearby;
                        Vector direction = target.getLocation().toVector().subtract(explosionLoc.toVector()).normalize();
                        target.setVelocity(direction.multiply(2));
                        Text.send(target, "<red>被爆炸冲击击飞！");
                    }
                }

                explosionCount++;
            }
        }.runTaskTimer(ghastWar, 0, 20); // 每秒一个爆炸
    }

    /**
     * 获取技能冷却信息
     */
    public String getCooldownInfo(GameGhast ghast) {
        SkillCooldown cooldown = skillCooldowns.get(ghast);
        if (cooldown == null) return "未注册";

        if (cooldown.isReady()) {
            return "就绪";
        } else {
            return "冷却中：" + cooldown.getTimeRemaining() + "秒";
        }
    }

    /**
     * 清理技能数据
     */
    public void cleanup() {
        skillCooldowns.clear();
    }
}