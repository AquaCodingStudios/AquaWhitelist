package studio.aquacoding.aquawhitelist.utils;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    private final Path dataDir;
    private final Logger logger;
    private CommentedConfigurationNode root;
    private YamlConfigurationLoader loader;
    private Path configFile;

    public ConfigManager(Path dataDir, Logger logger) {
        this.dataDir = dataDir;
        this.logger = logger;
    }

    public synchronized void load() {
        try {
            Files.createDirectories(dataDir);
            configFile = dataDir.resolve("config.yml");
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) Files.copy(in, configFile);
                    else Files.writeString(configFile, """
                        settings:
                          kick-message: "<red>You are not whitelisted on this proxy.</red>"
                          enabled: true
                        whitelist: []
                        """);
                }
            }
            loader = YamlConfigurationLoader.builder().path(configFile).build();
            root = loader.load();
            if (root.node("settings", "kick-message").virtual())
                root.node("settings", "kick-message").raw("<red>You are not whitelisted on this proxy.</red>");
            if (root.node("settings", "enabled").virtual())
                root.node("settings", "enabled").raw(true);
            if (root.node("whitelist").virtual())
                root.node("whitelist").raw(new ArrayList<String>());
            logger.info("[AQUA-WHITELIST] Config loaded from {}", configFile.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to load config", e);
            setDefaults();
        }
    }

    private void setDefaults() {
        root = CommentedConfigurationNode.root();
        root.node("settings", "kick-message").raw("<red>You are not whitelisted on this proxy.</red>");
        root.node("settings", "enabled").raw(true);
        root.node("whitelist").raw(new ArrayList<String>());
    }

    public synchronized void save() {
        try {
            if (loader != null && root != null) loader.save(root);
        } catch (IOException e) {
            logger.error("Failed to save config", e);
        }
    }

    public synchronized boolean isWhitelistEnabled() {
        return root.node("settings", "enabled").getBoolean(true);
    }

    public synchronized void setWhitelistEnabled(boolean enabled) {
        root.node("settings", "enabled").raw(enabled);
        save();
    }

    public synchronized String getKickMessageRaw() {
        return root.node("settings", "kick-message").getString("<red>You are not whitelisted on this proxy.</red>");
    }

    public synchronized boolean isWhitelisted(String name) {
        for (String s : getWhitelist()) if (s.equalsIgnoreCase(name)) return true;
        return false;
    }

    public synchronized List<String> getWhitelist() {
        List<String> out = new ArrayList<>();
        try {
            var node = root.node("whitelist");
            if (node.isList()) {
                for (var child : node.childrenList()) {
                    String v = child.getString("");
                    if (!v.isBlank()) out.add(v.trim());
                }
            } else if (node.isMap()) {
                for (var key : node.childrenMap().keySet()) {
                    String v = String.valueOf(key);
                    if (!v.isBlank()) out.add(v.trim());
                }
            } else {
                String scalar = node.getString("");
                if (!scalar.isBlank()) {
                    for (String part : scalar.split("[,\\s]+")) {
                        String s = part.trim();
                        if (!s.isEmpty()) out.add(s);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to read whitelist; returning empty list", e);
        }
        return out;
    }

    public synchronized boolean add(String name) {
        var wl = new LinkedHashSet<>(getWhitelist());
        boolean changed = wl.add(name);
        if (changed) {
            root.node("whitelist").raw(new ArrayList<>(wl));
            save();
        }
        return changed;
    }

    public synchronized boolean remove(String name) {
        var wl = new LinkedHashSet<>(getWhitelist());
        boolean changed = wl.removeIf(s -> s.equalsIgnoreCase(name));
        if (changed) {
            root.node("whitelist").raw(new ArrayList<>(wl));
            save();
        }
        return changed;
    }
}