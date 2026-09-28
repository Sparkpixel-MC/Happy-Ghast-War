package top.sparkpixel.hgw.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * MiniMessage 统一文本门面。
 * <p>
 * 语言包与代码内文本一律使用 MiniMessage 标签（如 {@code <green>你好}），
 * 旧版 {@code &x} / {@code §x} 色码会在反序列化前自动转换为对应标签，
 * 因此旧语言包无需强制更新也能正常显示。
 * <p>
 * 仅在与只接受 legacy '§' 字符串的旧 API（Bukkit 记分板、TAB、sendTitle 等）
 * 交互时才经 {@link #legacy(String)} 转换。
 */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /** 旧版色码 -> MiniMessage 标签（仅小写，与 Bukkit translateAlternateColorCodes 行为一致） */
    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>")
    );

    private Text() {
    }

    /**
     * 反序列化 MiniMessage 文本为组件。
     * 旧版 {@code &x} / {@code §x} 色码自动转换为标签；空串/null 返回空组件。
     */
    public static Component mm(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return MM.deserialize(legacyToTag(raw));
    }

    /**
     * 反序列化后转为 legacy '§' 字符串。
     * 仅供记分板/TAB/sendTitle 等旧 API 使用，新代码不要用它的结果再 sendMessage。
     */
    public static String legacy(String raw) {
        return LEGACY.serialize(mm(raw));
    }

    /** 以 MiniMessage 组件向玩家/控制台发送消息（自动兼容旧色码） */
    public static void send(CommandSender sender, String raw) {
        if (sender == null || raw == null || raw.isEmpty()) {
            return;
        }
        sender.sendMessage(mm(raw));
    }

    /** 把字符串中的旧版 {@code &x} / {@code §x} 色码替换为 MiniMessage 标签 */
    public static String legacyToTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < raw.length()) {
                String tag = LEGACY_TAGS.get(raw.charAt(i + 1));
                if (tag != null) {
                    sb.append(tag);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
