package com.aurasmp.ruin.data;

import com.aurasmp.ruin.RuinPlugin;
import com.aurasmp.ruin.ability.Ability;
import com.aurasmp.ruin.card.Card;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Loads/saves {@link PlayerData} to {@code data.yml} in the plugin folder. */
public final class DataStore {

    private final RuinPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataStore(RuinPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    /** Returns cached data, loading from disk on first access. */
    public PlayerData get(UUID id) {
        return cache.computeIfAbsent(id, this::load);
    }

    public void unload(UUID id) {
        PlayerData data = cache.remove(id);
        if (data != null) save(id, data);
    }

    private PlayerData load(UUID id) {
        PlayerData data = new PlayerData();
        if (!file.exists()) return data;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String base = "players." + id;
        if (!cfg.contains(base)) return data;

        data.setLevel(Math.max(1, cfg.getInt(base + ".level", 1)));
        data.setXp(Math.max(0, cfg.getInt(base + ".xp", 0)));

        for (String name : cfg.getStringList(base + ".cards")) {
            try {
                data.cards().add(Card.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Card was renamed/removed — skip silently.
            }
        }
        for (String name : cfg.getStringList(base + ".abilities")) {
            try {
                Ability ability = Ability.valueOf(name);
                if (!data.abilities().contains(ability)) data.abilities().add(ability);
            } catch (IllegalArgumentException ignored) {
                // Ability was renamed/removed — skip silently.
            }
        }
        return data;
    }

    public void save(UUID id, PlayerData data) {
        FileConfiguration cfg = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        String base = "players." + id;
        cfg.set(base + ".level", data.level());
        cfg.set(base + ".xp", data.xp());
        cfg.set(base + ".cards", data.cards().stream().map(Enum::name).toList());
        cfg.set(base + ".abilities", data.abilities().stream().map(Enum::name).toList());
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save Ruin data for " + id, e);
        }
    }

    /** Persist every loaded player (used on disable). */
    public void saveAll() {
        cache.forEach(this::save);
    }
}
