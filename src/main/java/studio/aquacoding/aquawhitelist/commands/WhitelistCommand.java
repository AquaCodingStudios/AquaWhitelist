package studio.aquacoding.aquawhitelist.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import studio.aquacoding.aquawhitelist.utils.ConfigManager;
import java.util.List;

public class WhitelistCommand implements SimpleCommand {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final ConfigManager config;
    private final Logger logger;

    public WhitelistCommand(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aquawhitelist.admin");
    }

    @Override
    public void execute(Invocation in) {
        try {
            String[] args = in.arguments();
            if (args.length < 2) {
                sendUsage(in);
                return;
            }
            String sub = args[0].toLowerCase();
            String name = args[1];
            switch (sub) {
                case "add" -> {
                    boolean added = config.add(name);
                    if (added) in.source().sendMessage(MM.deserialize("<green>Added <white>" + name + " <green>to the whitelist."));
                    else in.source().sendMessage(MM.deserialize("<yellow>" + name + " is already whitelisted."));
                }
                case "remove", "del", "rm" -> {
                    boolean removed = config.remove(name);
                    if (removed) in.source().sendMessage(MM.deserialize("<red>Removed <white>" + name + " <red>from the whitelist."));
                    else in.source().sendMessage(MM.deserialize("<yellow>" + name + " was not on the whitelist."));
                }
                default -> sendUsage(in);
            }
        } catch (Exception ex) {
            logger.error("[AQUA-WHITELIST] Command error", ex);
            in.source().sendMessage(MM.deserialize("<red>Unexpected error. See console."));
        }
    }

    @Override
    public List<String> suggest(Invocation in) {
        String[] a = in.arguments();
        if (a.length == 1) return List.of("add", "remove");
        if (a.length == 2 && a[0].equalsIgnoreCase("remove")) return config.getWhitelist();
        return List.of();
    }

    private void sendUsage(Invocation in) {
        Component msg = MM.deserialize("""
            <aqua>AquaWhitelist</aqua>
            <white>/aw add <player></white>
            <white>/aw remove <player></white>
            """);
        in.source().sendMessage(msg);
    }
}