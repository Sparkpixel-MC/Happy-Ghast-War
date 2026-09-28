package top.sparkpixel.hgw.commands.gw;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import top.sparkpixel.hgw.HappyGhastWar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public abstract class GWCommand {
    private final String command;
    private final Set<String> alias;
    private String permission;
    private final boolean playerSender;

    public GWCommand(String command, boolean playerSender, String... alias){
        this.command = command;
        this.playerSender = playerSender;
        this.alias = Sets.newHashSet(alias);
        this.setPermission("gw." + command);
    }

    public GWCommand(String command, String... alias){
        this.command = command;
        this.playerSender = false;
        this.alias = Sets.newHashSet(alias);
        this.setPermission("gw." + command);
    }

    public boolean isPlayerSender() {
        return playerSender;
    }

    public String getCommand() {
        return command;
    }

    public final String getPermission() {
        return this.permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public static Stream<GWCommand> filterByPermission(CommandSender sender, Stream<GWCommand> commands) {
        if (! (sender instanceof  Player)){
            return commands.filter((target) -> (target.getPermission() == null || sender.hasPermission(target.getPermission())) && !target.isPlayerSender());
        }
        return commands.filter((target) -> target.getPermission() == null || sender.hasPermission(target.getPermission()));
    }

    public static void suggestByParameter(Stream<String> possible, List<String> suggestions, String parameter) {
        if (parameter == null) {
            possible.forEach(suggestions::add);
        } else {
            possible.filter((suggestion) -> suggestion.toLowerCase(Locale.ROOT).startsWith(parameter.toLowerCase(Locale.ROOT))).forEach(suggestions::add);
        }

    }


    public final ImmutableSet<String> getCommands() {
        return ImmutableSet.<String>builder().add(this.command).addAll(this.alias).build();
    }

    public void evaluate(CommandSender commandSender, String s, List<String> params){}

    public void evaluate(HappyGhastWar happyGhastWar, Player commandSender, String s, List<String> params) throws IOException {}

    public void complete(HappyGhastWar happyGhastWar, CommandSender sender, String alias, List<String> params, List<String> suggestions){}
}
