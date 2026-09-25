package de.lmcstudio.fancyeggs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EggStorage {

    private final FancyEggs plugin;
    private final File file;
    private FileConfiguration config;

    public EggStorage(FancyEggs plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "eggs.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("eggs.yml konnte nicht erstellt werden: " + e.getMessage());
                return;
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        Map<UUID, List<FancyEggs.Egg>> active = plugin.getPlayerEggs();
        Map<UUID, List<FancyEggs.Egg>> storage = plugin.getPlayerStorage();
        active.clear();
        storage.clear();

        // Aktive Eggs
        if (config.isConfigurationSection("players")) {
            for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                UUID uuid;
                try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException ex) { continue; }
                List<FancyEggs.Egg> list = parseList(config.getMapList("players." + uuidStr + ".eggs"));
                if (!list.isEmpty()) active.put(uuid, list);
            }
        }

        // Storage Eggs
        if (config.isConfigurationSection("storage")) {
            for (String uuidStr : config.getConfigurationSection("storage").getKeys(false)) {
                UUID uuid;
                try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException ex) { continue; }
                List<FancyEggs.Egg> list = parseList(config.getMapList("storage." + uuidStr + ".eggs"));
                if (!list.isEmpty()) storage.put(uuid, list);
            }
        }

        plugin.getLogger().info("EggStorage: " + active.size() + " aktive Spieler, " + storage.size() + " Lager-Spieler geladen.");
    }

    private List<FancyEggs.Egg> parseList(List<Map<?, ?>> raw) {
        List<FancyEggs.Egg> list = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            String key = String.valueOf(entry.get("type"));
            int level = entry.get("level") instanceof Number ? ((Number) entry.get("level")).intValue() : 1;
            boolean charged = entry.get("charged") instanceof Boolean && (Boolean) entry.get("charged");

            FancyEggs.EggType type = plugin.getEggTypes().stream()
                    .filter(t -> t.key.equalsIgnoreCase(key))
                    .findFirst().orElse(null);
            if (type == null) continue;

            list.add(plugin.new Egg(type, level, charged));
        }
        return list;
    }

    public void save() {
        if (config == null) return;
        config.set("players", null);
        config.set("storage", null);

        for (Map.Entry<UUID, List<FancyEggs.Egg>> entry : plugin.getPlayerEggs().entrySet()) {
            config.set("players." + entry.getKey() + ".eggs", serialize(entry.getValue()));
        }
        for (Map.Entry<UUID, List<FancyEggs.Egg>> entry : plugin.getPlayerStorage().entrySet()) {
            config.set("storage." + entry.getKey() + ".eggs", serialize(entry.getValue()));
        }

        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("eggs.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private List<Map<String, Object>> serialize(List<FancyEggs.Egg> eggs) {
        List<Map<String, Object>> raw = new ArrayList<>();
        for (FancyEggs.Egg egg : eggs) {
            Map<String, Object> m = new HashMap<>();
            m.put("type", egg.type.key);
            m.put("level", egg.level);
            m.put("charged", egg.isCharged);
            raw.add(m);
        }
        return raw;
    }
}
