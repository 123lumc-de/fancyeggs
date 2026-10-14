package de.lmcstudio.fancyeggs;

import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;

public class EggStorage {

    private final FancyEggs plugin;

    public EggStorage(FancyEggs plugin) {
        this.plugin = plugin;
    }

    /** Lädt alle Eggs aus der DB in die In-Memory-Maps. */
    public void load() {
        Map<UUID, List<FancyEggs.Egg>> active = plugin.getPlayerEggs();
        Map<UUID, List<FancyEggs.Egg>> storage = plugin.getPlayerStorage();
        active.clear();
        storage.clear();

        String p = plugin.getDatabaseManager().getTablePrefix();

        // Aktive Eggs
        try (PreparedStatement ps = plugin.getDatabaseManager().getConnection()
                .prepareStatement("SELECT uuid, egg_key, level, charged, slot_index FROM " + p
                        + "eggs_active ORDER BY uuid, slot_index ASC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                FancyEggs.EggType type = findType(rs.getString("egg_key"));
                if (type == null) continue;
                active.computeIfAbsent(uuid, k -> new ArrayList<>())
                        .add(plugin.new Egg(type, rs.getInt("level"), rs.getInt("charged") == 1));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Aktive Eggs laden fehlgeschlagen: " + e.getMessage());
        }

        // Storage Eggs
        try (PreparedStatement ps = plugin.getDatabaseManager().getConnection()
                .prepareStatement("SELECT uuid, egg_key, level, charged FROM " + p
                        + "eggs_storage ORDER BY uuid, id ASC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                FancyEggs.EggType type = findType(rs.getString("egg_key"));
                if (type == null) continue;
                storage.computeIfAbsent(uuid, k -> new ArrayList<>())
                        .add(plugin.new Egg(type, rs.getInt("level"), rs.getInt("charged") == 1));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Storage-Eggs laden fehlgeschlagen: " + e.getMessage());
        }

        plugin.getLogger().info("EggStorage (DB): " + active.size() + " aktive Spieler, "
                + storage.size() + " Lager-Spieler geladen.");
    }

    private FancyEggs.EggType findType(String key) {
        return plugin.getEggTypes().stream()
                .filter(t -> t.key.equalsIgnoreCase(key))
                .findFirst().orElse(null);
    }

    /**
     * Speichert die Eggs eines einzelnen Spielers (nur seine Daten!).
     * Das ist der Schlüssel: wir löschen und schreiben nicht mehr die ganze Datei,
     * sondern nur die Daten dieses Spielers.
     */
    public void savePlayer(UUID uuid) {
        String p = plugin.getDatabaseManager().getTablePrefix();
        try (Connection con = plugin.getDatabaseManager().getConnection()) {
            // Aktive Eggs löschen + neu schreiben
            try (PreparedStatement del = con.prepareStatement(
                    "DELETE FROM " + p + "eggs_active WHERE uuid = ?")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }

            List<FancyEggs.Egg> active = plugin.getPlayerEggs().get(uuid);
            if (active != null && !active.isEmpty()) {
                try (PreparedStatement ins = con.prepareStatement(
                        "INSERT INTO " + p + "eggs_active (uuid, egg_key, level, charged, slot_index) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    int idx = 0;
                    for (FancyEggs.Egg egg : active) {
                        ins.setString(1, uuid.toString());
                        ins.setString(2, egg.type.key);
                        ins.setInt(3, egg.level);
                        ins.setInt(4, egg.isCharged ? 1 : 0);
                        ins.setInt(5, idx++);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }

            // Storage Eggs löschen + neu schreiben
            try (PreparedStatement del = con.prepareStatement(
                    "DELETE FROM " + p + "eggs_storage WHERE uuid = ?")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }

            List<FancyEggs.Egg> stored = plugin.getPlayerStorage().get(uuid);
            if (stored != null && !stored.isEmpty()) {
                try (PreparedStatement ins = con.prepareStatement(
                        "INSERT INTO " + p + "eggs_storage (uuid, egg_key, level, charged) "
                                + "VALUES (?, ?, ?, ?)")) {
                    for (FancyEggs.Egg egg : stored) {
                        ins.setString(1, uuid.toString());
                        ins.setString(2, egg.type.key);
                        ins.setInt(3, egg.level);
                        ins.setInt(4, egg.isCharged ? 1 : 0);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("savePlayer(" + uuid + ") fehlgeschlagen: " + e.getMessage());
        }
    }

    /** Speichert alle Spieler (z.B. beim Shutdown). */
    public void saveAll() {
        Set<UUID> all = new HashSet<>();
        all.addAll(plugin.getPlayerEggs().keySet());
        all.addAll(plugin.getPlayerStorage().keySet());

        // Auch Offline-Spieler, die gerade keine aktiven Eggs haben, müssen nicht gespeichert werden.
        for (UUID uuid : all) savePlayer(uuid);
    }
}
