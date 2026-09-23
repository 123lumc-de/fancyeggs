package de.lmcstudio.fancyeggs;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopManager {

    private final FancyEggs plugin;
    private final File file;
    private FileConfiguration config;

    // UUID -> PlayerName (damit Offline-Spieler angezeigt werden können)
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    // UUID -> Gesamteinkommen/Sekunde (gespeichert)
    private final Map<UUID, Double> incomes = new ConcurrentHashMap<>();

    public TopManager(FancyEggs plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tops.yml");
        load();
    }

    /** Lädt tops.yml */
    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("tops.yml konnte nicht erstellt werden: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        names.clear();
        incomes.clear();

        if (config.isConfigurationSection("players")) {
            for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String name = config.getString("players." + uuidStr + ".name", "Unknown");
                    double income = config.getDouble("players." + uuidStr + ".income", 0.0);
                    names.put(uuid, name);
                    incomes.put(uuid, income);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        plugin.getLogger().info("TopManager: " + incomes.size() + " Einträge geladen.");
    }

    /** Speichert tops.yml */
    public void save() {
        if (config == null) return;
        for (Map.Entry<UUID, Double> e : incomes.entrySet()) {
            String path = "players." + e.getKey() + ".";
            config.set(path + "name", names.getOrDefault(e.getKey(), "Unknown"));
            config.set(path + "income", e.getValue());
        }
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("tops.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /** Wird jede Sekunde von FancyEggs aufgerufen */
    public void updatePlayer(UUID uuid, String name, double income) {
        names.put(uuid, name);
        incomes.put(uuid, income);
    }

    /** Wird für den "name" Placeholder benutzt */
    public String getNameByUUID(UUID uuid) {
        return names.getOrDefault(uuid, "Unknown");
    }

    /** Wird für den "value" Placeholder benutzt */
    public double getIncomeByUUID(UUID uuid) {
        return incomes.getOrDefault(uuid, 0.0);
    }

    /** Sortierte Rangliste (absteigend nach Einkommen) */
    public List<Map.Entry<UUID, Double>> getSorted() {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>(incomes.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    /** Anzahl Einträge */
    public int size() {
        return incomes.size();
    }
}
