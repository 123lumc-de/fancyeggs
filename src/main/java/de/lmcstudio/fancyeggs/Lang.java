package de.lmcstudio.fancyeggs;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lang {

    private final FancyEggs plugin;
    private final FileConfiguration messages;
    private final String languageCode;
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // Farb-Shortcuts (für Nutzung im Code)
    public static final String ORANGE = "&#F69D28";
    public static final String GRAY   = "&#AAAAAA";
    public static final String WHITE  = "&#FFFFFF";
    public static final String GREEN  = "&#55FF55";
    public static final String RED    = "&#FF5555";
    public static final String AQUA   = "&#55FFFF";
    public static final String PINK   = "&#FF55FF";
    public static final String BOLD   = "&l";
    public static final String RESET  = "&r";

    public Lang(FancyEggs plugin, String languageCode) {
        this.plugin = plugin;
        this.languageCode = languageCode;

        // Datei-Existenz prüfen
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) langDir.mkdirs();

        File langFile = new File(langDir, languageCode + ".yml");

        // Falls die Datei fehlt → aus JAR extrahieren oder leer anlegen
        if (!langFile.exists()) {
            String resource = "languages/" + languageCode + ".yml";
            if (plugin.getResource(resource) != null) {
                plugin.saveResource(resource, false);
            } else {
                // Fallback: en_US aus JAR holen
                plugin.getLogger().warning("Sprachdatei '" + languageCode
                        + ".yml' nicht gefunden. Verwende en_US als Fallback.");
                if (plugin.getResource("languages/en_US.yml") != null) {
                    plugin.saveResource("languages/en_US.yml", false);
                }
            }
        }

        this.messages = YamlConfiguration.loadConfiguration(langFile);

        // Standardwerte aus JAR laden (damit fehlende Keys auffallen)
        String resource = "languages/" + languageCode + ".yml";
        InputStream stream = plugin.getResource(resource);
        if (stream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
        }
    }

    public String get(String key) {
        String value = messages.getString(key);
        if (value == null) return "&cMissing: " + key;
        return value;
    }

    public String color(String s) {
        if (s == null) return "";
        Matcher m = HEX.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) m.appendReplacement(sb, ChatColor.of("#" + m.group(1)).toString());
        m.appendTail(sb);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public String getColored(String key) {
        return color(get(key));
    }

    /** Liefert den verwendeten Sprachcode, z.B. "de_DE". */
    public String getLanguageCode() {
        return languageCode;
    }

    /** Für /fancyeggs reload */
    public void reload() {
        File langFile = new File(plugin.getDataFolder(), "languages/" + languageCode + ".yml");
        if (langFile.exists()) {
            FileConfiguration newConf = YamlConfiguration.loadConfiguration(langFile);
            // In dasselbe Objekt kopieren
            for (String key : newConf.getKeys(true)) {
                messages.set(key, newConf.get(key));
            }
        }
    }
}
