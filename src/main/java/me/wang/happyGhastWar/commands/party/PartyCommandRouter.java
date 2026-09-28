package me.wang.happyGhastWar.commands.party;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.wang.happyGhastWar.HappyGhastWar;
import me.wang.happyGhastWar.arena.Arena;
import me.wang.happyGhastWar.commands.party.impl.*;
import me.wang.happyGhastWar.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;

public class PartyCommandRouter implements CommandExecutor, TabExecutor {
    private static final List<PartyCommand> COMMANDS = ImmutableList.of(new Accept(), new Create(), new Decline(), new Disband(), new Info(), new Invite(), new Kick(), new Leave(), new Transfer(), new Chat());
    private final HappyGhastWar plugin;
    private final Map<String, PartyCommand> commands;

    public PartyCommandRouter(HappyGhastWar plugin){
        this.plugin = plugin;
        ImmutableMap.Builder<String, PartyCommand> commands = ImmutableMap.builder();

        for(PartyCommand command : COMMANDS) {
            command.getCommands().forEach((label) -> commands.put(label.toString(), command));
        }

        this.commands = commands.build();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 0) {
            for (String help : plugin.getLanguage(sender instanceof Player ? (Player) sender : null).getTranslatedList("commands.party-list")){
                sender.sendMessage(help);
            }
            return true;
        } else {

            String search = args[0].toLowerCase(Locale.ROOT);
            PartyCommand target = (PartyCommand) this.commands.get(search);
            if (target == null) {
                Text.send(sender, "<red>Unknown command <gray>" + search);
                return true;
            } else {
                String permission = target.getPermission();
                if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
                    Text.send(sender, "<red>You do not have permission to do this!");
                    return true;
                } else {
                    if (sender instanceof Player){
                        target.evaluate(this.plugin, (Player) sender, search, Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
                        return true;
                    }else {
                        if (target.isPlayerSender()){
                            sender.sendMessage("该命令只能由玩家执行");
                            return true;
                        }
                        target.evaluate(this.plugin, sender, search, Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
                        return true;
                    }
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        List<String> suggestions = new ArrayList();
        if (args.length > 1) {
            PartyCommand target = (PartyCommand) this.commands.get(args[0].toLowerCase(Locale.ROOT));
            if (target != null) {
                target.complete(this.plugin, sender, args[0].toLowerCase(Locale.ROOT), Arrays.asList(Arrays.copyOfRange(args, 1, args.length)), suggestions);
            }

            return suggestions;
        } else {
            Stream<String> targets = PartyCommand.filterByPermission(sender, this.commands.values().stream()).map(PartyCommand::getCommand).map(Object::toString);
            PartyCommand.suggestByParameter(targets, suggestions, args.length == 0 ? null : args[0]);
            return suggestions;
        }
    }
}



