package me.wang.happyGhastWar.util;

import me.wang.happyGhastWar.HappyGhastWar;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * 可配置音效：所有游戏音效统一经此类播放，音效本身由 config.yml 的
 * {@code sounds.<key>} 配置，未配置的 key 使用代码内默认值（fallback）。
 * <p>
 * 配置值格式（逗号分隔）：
 * <pre>
 * sounds:
 *   game-start: 'entity.player.levelup'            # 命名空间键（推荐）
 *   countdown: 'UI_BUTTON_CLICK'                   # 旧枚举常量名也可以
 *   airdrop-land: 'block.anvil.land,0.6,1.4'       # 可附加 音量,音调
 * </pre>
 */
public final class SoundUtil {

    /** 解析结果：音效 + 音量 + 音调 */
    private record Play(Sound sound, float volume, float pitch) {
    }

    private SoundUtil() {
    }

    /** 解析某个音效配置（无配置/解析失败时回退到默认音效与默认音量音调） */
    private static Play resolve(String key, Sound fallback, float volume, float pitch) {
        FileConfiguration config = HappyGhastWar.config;
        if (config != null) {
            String raw = config.getString("sounds." + key);
            if (raw != null && !raw.isBlank()) {
                String[] parts = raw.split(",");
                Sound sound = parse(parts[0].trim(), fallback);
                float vol = parts.length > 1 ? parseFloat(parts[1], volume) : volume;
                float pit = parts.length > 2 ? parseFloat(parts[2], pitch) : pitch;
                return new Play(sound, vol, pit);
            }
        }
        return new Play(fallback, volume, pitch);
    }

    /** 只取音效（给自定义播放方式的地方用） */
    public static Sound get(String key, Sound fallback) {
        return resolve(key, fallback, 1.0f, 1.0f).sound();
    }

    /** 向玩家播放音效（在其所在位置） */
    public static void play(Player player, String key, Sound fallback, float volume, float pitch) {
        Play play = resolve(key, fallback, volume, pitch);
        player.playSound(player.getLocation(), play.sound(), play.volume(), play.pitch());
    }

    /** 在指定世界的指定位置播放音效（附近所有玩家可听见） */
    public static void playAt(World world, Location location, String key, Sound fallback, float volume, float pitch) {
        Play play = resolve(key, fallback, volume, pitch);
        world.playSound(location, play.sound(), play.volume(), play.pitch());
    }

    /** 音效名 -> Sound：先按命名空间键查找，再按旧枚举常量名匹配，失败回退默认值 */
    private static Sound parse(String name, Sound fallback) {
        if (name.isEmpty()) {
            return fallback;
        }
        try {
            Sound byKey = Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (byKey != null) {
                return byKey;
            }
        } catch (IllegalArgumentException ignored) {
            // 非法键名字符，走枚举名匹配
        }
        // 兼容旧枚举常量名（如 ENTITY_PLAYER_LEVELUP）：注册表键剥掉 '.'/'_' 后小写比对
        String normalized = name.toLowerCase(Locale.ROOT).replace(".", "").replace("_", "");
        for (Sound sound : Registry.SOUNDS) {
            NamespacedKey nk = Registry.SOUNDS.getKey(sound);
            if (nk == null) {
                continue;
            }
            String key = nk.getKey().replace(".", "").replace("_", "");
            if (key.equals(normalized)) {
                return sound;
            }
        }
        return fallback;
    }

    private static float parseFloat(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
