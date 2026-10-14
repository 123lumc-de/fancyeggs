package de.lmcstudio.fancyeggs;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final FancyEggs plugin;
    private final String type;      // "h2" oder "mysql"
    private Connection connection;

    // MySQL
    private String host, database, username, password, tablePrefix;
    private int port;

    // H2
    private String h2File;

    public DatabaseManager(FancyEggs plugin) {
        this.plugin = plugin;
        this.type = plugin.getConfig().getString("storage.type", "h2").toLowerCase();

        if (type.equals("mysql")) {
            this.host = plugin.getConfig().getString("storage.mysql.host", "localhost");
            this.port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            this.database = plugin.getConfig().getString("storage.mysql.database", "fancyeggs");
            this.username = plugin.getConfig().getString("storage.mysql.username", "root");
            this.password = plugin.getConfig().getString("storage.mysql.password", "");
            this.tablePrefix = plugin.getConfig().getString("storage.mysql.table-prefix", "fancyeggs_");
        } else {
            this.h2File = plugin.getConfig().getString("storage.h2.file", "data");
        }
    }

    /** Gibt "h2" oder "mysql" zurück */
    public String getType() {
        return type;
    }

    /** Tabellen-Präfix (nur bei MySQL relevant, bei H2 leer) */
    public String getTablePrefix() {
        return type.equals("mysql") ? tablePrefix : "";
    }

    /** Baut die Datenbank-Verbindung auf und legt Tabellen an. */
    public boolean connect() {
        try {
            if (type.equals("mysql")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8";
                connection = DriverManager.getConnection(url, username, password);
            } else {
                Class.forName("org.h2.Driver");
                File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists()) dataFolder.mkdirs();
                String url = "jdbc:h2:" + new File(dataFolder, h2File).getAbsolutePath()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
                connection = DriverManager.getConnection(url, "sa", "");
            }

            createTables();
            plugin.getLogger().info("Datenbank (" + type + ") verbunden.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Datenbank-Verbindung fehlgeschlagen: " + e.getMessage());
            return false;
        }
    }

    /** Gibt die aktuelle Connection zurück (versucht Reconnect, falls geschlossen). */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) connect();
        } catch (SQLException e) {
            plugin.getLogger().warning("Reconnect fehlgeschlagen: " + e.getMessage());
        }
        return connection;
    }

    /** Schließt die Verbindung. */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Disconnect-Fehler: " + e.getMessage());
        }
    }

    /** Legt alle Tabellen an, falls sie noch nicht existieren. */
    private void createTables() throws SQLException {
        String p = getTablePrefix();
        try (Statement st = connection.createStatement()) {

            // Aktive Eggs (pro Spieler, mit Slot-Index für die Reihenfolge)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + p + "eggs_active ("
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "egg_key VARCHAR(64) NOT NULL, "
                    + "level INT NOT NULL DEFAULT 1, "
                    + "charged TINYINT NOT NULL DEFAULT 0, "
                    + "slot_index INT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (uuid, egg_key, slot_index)"
                    + ")");

            // Lager-Eggs (pro Spieler, ID für Reihenfolge)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + p + "eggs_storage ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "egg_key VARCHAR(64) NOT NULL, "
                    + "level INT NOT NULL DEFAULT 1, "
                    + "charged TINYINT NOT NULL DEFAULT 0"
                    + ")");

            // Top-Rangliste (offline Spieler, name + income)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + p + "tops ("
                    + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "name VARCHAR(64) NOT NULL, "
                    + "income DOUBLE NOT NULL DEFAULT 0"
                    + ")");
        }
    }
}
