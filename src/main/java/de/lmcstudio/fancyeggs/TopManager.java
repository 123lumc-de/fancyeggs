package de.lmcstudio.fancyeggs;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopManager {

    private final FancyEggs plugin;
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<UUID, Double> incomes = new ConcurrentHashMap<>();

    public TopManager(FancyEggs plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        names.clear();
        incomes.clear();
        String p = plugin.getDatabaseManager().getTablePrefix();
        try (PreparedStatement ps = plugin.getDatabaseManager().getConnection()
                .prepareStatement("SELECT uuid, name, income FROM " + p + "tops")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                names.put(uuid, rs.getString("name"));
                incomes.put(uuid, rs.getDouble("income"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("TopManager laden fehlgeschlagen: " + e.getMessage());
        }
        plugin.getLogger().info("TopManager (DB): " + incomes.size() + " Einträge geladen.");
    }

    public void save() {
        String p = plugin.getDatabaseManager().getTablePrefix();
        try (Connection con = plugin.getDatabaseManager().getConnection()) {
            try (PreparedStatement ins = con.prepareStatement(
                    "REPLACE INTO " + p + "tops (uuid, name, income) VALUES (?, ?, ?)")) {
                for (Map.Entry<UUID, Double> e : incomes.entrySet()) {
                    ins.setString(1, e.getKey().toString());
                    ins.setString(2, names.getOrDefault(e.getKey(), "Unknown"));
                    ins.setDouble(3, e.getValue());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("TopManager speichern fehlgeschlagen: " + e.getMessage());
        }
    }

    public void updatePlayer(UUID uuid, String name, double income) {
        names.put(uuid, name);
        incomes.put(uuid, income);
    }

    public String getNameByUUID(UUID uuid) { return names.getOrDefault(uuid, "Unknown"); }
    public double getIncomeByUUID(UUID uuid) { return incomes.getOrDefault(uuid, 0.0); }

    public List<Map.Entry<UUID, Double>> getSorted() {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>(incomes.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    public int size() { return incomes.size(); }
}
