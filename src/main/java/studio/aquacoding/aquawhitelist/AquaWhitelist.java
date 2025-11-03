package studio.aquacoding.aquawhitelist;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.spongepowered.configurate.serialize.SerializationException;
import studio.aquacoding.aquawhitelist.commands.WhitelistCommand;
import studio.aquacoding.aquawhitelist.utils.ConfigManager;

import java.nio.file.Path;

@Plugin(
        id = "aquawhitelist",
        name = "AquaWhitelist",
        version = BuildConstants.VERSION,
        description = "Whitelist Plugin for Velocity",
        authors = {"BlackDev"}
)
public class AquaWhitelist {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Logger logger;
    private final Path dataDirectory;
    private final ProxyServer server;
    private ConfigManager config;

    @Inject
    public AquaWhitelist(Logger logger, @DataDirectory Path dataDirectory, ProxyServer server) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) throws SerializationException {
        config = new ConfigManager(dataDirectory, logger);
        config.load();
        var meta = server.getCommandManager().metaBuilder("aquawhitelist").aliases("aw").build();
        server.getCommandManager().register(meta, new WhitelistCommand(config, logger));
        logger.info("[AQUA-WHITELIST] Command registered.");
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        try {
            String name = event.getPlayer().getUsername();
            if (!config.isWhitelisted(name)) {
                event.setResult(LoginEvent.ComponentResult.denied(MM.deserialize(config.getKickMessageRaw())));
            }
        } catch (Exception ex) {
            logger.error("[AQUA-WHITELIST] Login check failed", ex);
            event.setResult(LoginEvent.ComponentResult.denied(MM.deserialize("<red>You are not whitelisted on this proxy.</red>")));
        }
    }
}