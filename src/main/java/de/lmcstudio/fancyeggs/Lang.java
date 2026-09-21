package de.lmcstudio.fancyeggs;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lang {

    private final String lang;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // --- Farbpalette ---
    public static final String GOLD  = "&#FFD700"; // Titel, Namen, Werte
    public static final String GRAY  = "&#AAAAAA"; // Labels wie "Beschreibung"
    public static final String WHITE = "&#FFFFFF"; // normale Werte
    public static final String GREEN = "&#55FF55"; // Einkommen
    public static final String RED   = "&#FF5555"; // Kosten, negativ
    public static final String AQUA  = "&#55FFFF"; // Level
    public static final String PINK  = "&#FF55FF"; // Charged
    public static final String BOLD  = "&l";       // Fett
    public static final String RESET = "&r";

    public Lang(FileConfiguration config) {
        this.lang = config.getString("language", "de").toLowerCase();
    }

    public String get(String key) {
        boolean en = lang.equals("en");
        switch (key) {
            // ---- Menü Titel ----
            case "menu.title":          return GOLD + BOLD + (en ? "Eggs | Menu"    : "Eggs | Menü");
            case "menu.storage.title":  return GOLD + BOLD + (en ? "Eggs | Storage" : "Eggs | Lager");

            // ---- Storage Item ----
            case "storage.name":        return GOLD + BOLD + (en ? "EGGS STORAGE" : "EGGS LAGER");
            case "storage.desc":        return GRAY + (en ? "Description"  : "Beschreibung");
            case "storage.info":        return GOLD + BOLD + (en ? "Information:" : "Information:");
            case "storage.click":       return WHITE + (en ? "Click here to equip other eggs." : "Klicke hier um andere Eggs auszurüsten.");
            case "storage.count":       return GOLD + BOLD + (en ? "➤ Your Eggs: " : "➤ Deine Eggs: ");

            // ---- Egg Item ----
            case "egg.desc":            return GRAY + (en ? "Description" : "Beschreibung");
            case "egg.level":           return AQUA + BOLD + "★ " + (en ? "LEVEL " : "LEVEL ");
            case "egg.income":          return GRAY + (en ? "Money / Second: " : "Geld / Sekunde: ") + GREEN + "$";
            case "egg.upgrade_price":   return GRAY + (en ? "Upgrade Price: " : "Upgrade Preis: ") + RED + "$";
            case "egg.shift_upgrade":   return GOLD + BOLD + "➤ " + (en ? "SHIFT-CLICK to upgrade" : "SHIFT-KLICK zum Upgraden");
            case "egg.left_remove":     return RED + BOLD + "➤ " + (en ? "LEFT-CLICK to remove" : "LEFT-KLICK zum Ablegen");
            case "egg.charged_tag":     return PINK + BOLD + "[CHARGED] ";

            // ---- Chat Messages ----
            case "msg.no_permission":   return RED + (en ? "You don't have permission for this." : "Dazu hast du keine Rechte.");
            case "msg.give_usage":      return RED + (en ? "Usage: /fancyeggs give <player> <egg> [charged]" : "Nutzung: /fancyeggs give <Spieler> <EggName> [charged]");
            case "msg.available":       return GOLD + BOLD + (en ? "Available: " : "Verfügbar: ") + RESET + WHITE;
            case "msg.player_not_found":return RED + (en ? "Player not found." : "Spieler nicht gefunden.");
            case "msg.egg_not_found":   return RED + (en ? "Egg type not found." : "Egg-Typ nicht gefunden.");
            case "msg.give_sender":     return GOLD + BOLD + (en ? "You gave %player% a %egg%%charged%." : "Du hast %player% ein %egg%%charged% gegeben.");
            case "msg.give_target":     return GOLD + BOLD + (en ? "You received a %egg%%charged%!" : "Du hast ein %egg%%charged% erhalten!");
            case "msg.charged_suffix":  return PINK + BOLD + " (Charged)";
            case "msg.upgraded":        return GOLD + BOLD + (en ? "Egg upgraded! New Level: " : "Egg upgraded! Neues Level: ") + AQUA;
            case "msg.not_enough":      return RED + (en ? "You don't have enough money! Needed: $" : "Du hast nicht genug Geld! Benötigt: $");
            case "msg.removed":         return RED + (en ? "You removed the %egg%." : "Du hast das %egg% abgelegt.");
            case "msg.already_storage": return GOLD + (en ? "This egg is already in your storage. Use /fancyeggs to manage it." : "Dieses Egg ist bereits in deinem Lager. Nutze /fancyeggs um es zu verwalten.");
            case "msg.only_players":    return RED + (en ? "Only players can use this command." : "Nur Spieler können diesen Befehl nutzen.");
            case "msg.only_players_menu": return RED + (en ? "Only players can open the menu." : "Nur Spieler können das Menü öffnen.");

            // ---- /eggs list ----
            case "list.header":         return GOLD + BOLD + (en ? "========== FancyEggs List ==========" : "========== FancyEggs Liste ==========");
            case "list.name":           return GOLD + BOLD + "▶ ";
            case "list.base_income":    return GRAY + (en ? "Base income: " : "Basis-Einkommen: ") + GREEN + "$";
            case "list.upgrade_cost":   return GRAY + (en ? "Upgrade cost: " : "Upgrade-Kosten: ") + RED + "$";
            case "list.multiplier":     return GRAY + (en ? "Multiplier: " : "Multiplikator: ") + AQUA + "x";
            case "list.footer":         return GOLD + BOLD + "===================================";

            // ---- Console ----
            case "console.vault_missing": return en ? "Vault not found! FancyEggs is disabling." : "Vault nicht gefunden! FancyEggs wird deaktiviert.";
            case "console.enabled":       return en ? "FancyEggs successfully loaded and connected to Vault!" : "FancyEggs erfolgreich geladen und mit Vault verbunden!";

            default: return "&cMissing translation: " + key;
        }
    }

    public String color(String s) {
        // Hex-Codes konvertieren
        Matcher matcher = HEX_PATTERN.matcher(s);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);

        // Klassische Codes (&l, &r, &c etc.) konvertieren
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public String getColored(String key) {
        return color(get(key));
    }
}
