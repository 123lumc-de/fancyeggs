package de.lmcstudio.fancyeggs;

import org.bukkit.configuration.ConfigurationSection;
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

    /** Lädt eggs.yml und füllt die playerEggs-Map */
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

        Map<UUID, List<FancyEggs.Egg>> map = plugin.getPlayerEggs();
        map.clear();

        if (!config.isConfigurationSection("players")) return;

        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException ex) { continue; }

            List<FancyEggs.Egg> list = new ArrayList<>();
            List<Map<?, ?>> raw = config.getMapList("players." + uuidStr + ".eggs");

            for (Map<?, ?> entry : raw) {
                String key = String.valueOf(entry.get("type"));
                int level = entry.get("level") instanceof Number ? ((Number) entry.get("level")).intValue() : 1;
                boolean charged = entry.get("charged") instanceof Boolean && (Boolean) entry.get("charged");

                FancyEggs.EggType type = plugin.getEggTypes().stream()
                        .filter(t -> t.key.equalsIgnoreCase(key))
                        .findFirst().orElse(null);
                if (type == null) continue; // Egg-Typ existiert nicht mehr

                list.add(plugin.new Egg(type, level, charged));
            }
            if (!list.isEmpty()) map.put(uuid, list);
        }
        plugin.getLogger().info("EggStorage: " + map.size() + " Spieler geladen.");
    }

    /** Speichert die playerEggs-Map in eggs.yml */
    public void save() {
        if (config == null) return;

        // Alles clearen
        config.set("players", null);

        for (Map.Entry<UUID, List<FancyEggs.Egg>> entry : plugin.getPlayerEggs().entrySet()) {
            String path = "players." + entry.getKey() + ".eggs";
            List<Map<String, Object>> raw = new ArrayList<>();

            for (FancyEggs.Egg egg : entry.getValue()) {
                Map<String, Object> m = new HashMap<>();
                m.put("type", egg.type.key);
                m.put("level", egg.level);
                m.put("charged", egg.isCharged);
                raw.add(m);
            }
            config.set(path, raw);
        }

        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("eggs.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
