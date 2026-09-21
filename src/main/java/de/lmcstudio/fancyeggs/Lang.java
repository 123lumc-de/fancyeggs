package de.lmcstudio.fancyeggs;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lang {

    private final String lang;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // --- Farbpalette ---
    public static final String GOLD  = "&#FFD700";
    public static final String GRAY  = "&#AAAAAA";
    public static final String WHITE = "&#FFFFFF";
    public static final String GREEN = "&#55FF55";
    public static final String RED   = "&#FF5555";
    public static final String AQUA  = "&#55FFFF";
    public static final String PINK  = "&#FF55FF";
    public static final String BOLD  = "&l";
    public static final String RESET = "&r";

    public Lang(FileConfiguration config) {
        this.lang = config.getString("language", "de").toLowerCase();
    }

    public String get(String key) {
        boolean en = lang.equals("en");
        switch (key) {
            // ---- Menü Titel ----
            case "menu.title":          return GOLD + BOLD + (en ? "Eggs | Menu"    : "Eggs | Menü");

            // ---- Egg Item ----
            case "egg.desc":            return GRAY + (en ? "Description" : "Beschreibung");
            case "egg.level":           return AQUA + BOLD + "★ " + (en ? "LEVEL " : "LEVEL ");
            case "egg.income":          return GRAY + (en ? "Money / Second: " : "Geld / Sekunde: ") + GREEN + "$";
            case "egg.upgrade_price":   return GRAY + (en ? "Upgrade Price: " : "Upgrade Preis: ") + RED + "$";
            case "egg.sell_price":      return GRAY + (en ? "Sell Price: " : "Verkaufspreis: ") + RED + "$";
            case "egg.left_upgrade":    return GOLD + BOLD + "➤ " + (en ? "LEFT-CLICK to upgrade" : "LEFT-KLICK zum Upgraden");
            case "egg.shift_sell":      return RED + BOLD + "➤ " + (en ? "SHIFT-CLICK to sell" : "SHIFT-KLICK zum Verkaufen");
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
            case "msg.sold":            return GOLD + BOLD + (en ? "Sold %egg% for $%price%!" : "%egg% für $%price% verkauft!");
            case "msg.only_players":    return RED + (en ? "Only players can use this command." : "Nur Spieler können diesen Befehl nutzen.");
            case "msg.only_players_menu": return RED + (en ? "Only players can open the menu." : "Nur Spieler können das Menü öffnen.");

            // ---- /eggs list ----
            case "list.header":         return GOLD + BOLD + (en ? "========== FancyEggs List ==========" : "========== FancyEggs Liste ==========");
            case "list.name":           return GOLD + BOLD + "▶ ";
            case "list.base_income":    return GRAY + (en ? "Base income: " : "Basis-Einkommen: ") + GREEN + "$";
            case "list.upgrade_cost":   return GRAY + (en ? "Upgrade cost: " : "Upgrade-Kosten: ") + RED + "$";
            case "list.multiplier":     return GRAY + (en ? "Multiplier: " : "Multiplikator: ") + AQUA + "x";
            case "list.footer":         return GOLD + BOLD + "===================================";

            // ---- Auto-Collect Button ----
            case "autocollect.on_name":   return GREEN + BOLD + (en ? "AUTO-COLLECT: ON" : "AUTO-COLLECT: AN");
            case "autocollect.off_name":  return RED + BOLD + (en ? "AUTO-COLLECT: OFF" : "AUTO-COLLECT: AUS");
            case "autocollect.desc":      return GRAY + (en ? "Description" : "Beschreibung");
            case "autocollect.info":      return GOLD + BOLD + (en ? "Information:" : "Information:");
            case "autocollect.on_text":   return WHITE + (en ? "Money is auto-collected every second." : "Geld wird jede Sekunde automatisch gesammelt.");
            case "autocollect.off_text":  return WHITE + (en ? "Money accumulates in the chest." : "Geld sammelt sich in der Kiste an.");
            case "autocollect.click":     return GOLD + BOLD + "➤ " + (en ? "CLICK to toggle" : "KLICK zum Umschalten");

            // ---- Pending Chest ----
            case "pending.name":          return GOLD + BOLD + (en ? "PENDING MONEY" : "AUSSTEHENDES GELD");
            case "pending.desc":          return GRAY + (en ? "Description" : "Beschreibung");
            case "pending.info":          return GOLD + BOLD + (en ? "Information:" : "Information:");
            case "pending.amount":        return GRAY + (en ? "Pending: " : "Ausstehend: ");
            case "pending.click":         return GOLD + BOLD + "➤ " + (en ? "CLICK to collect" : "KLICK zum Einsammeln");
            case "pending.collected":     return GREEN + BOLD + (en ? "Collected $%amount%!" : "$%amount% eingesammelt!");
            case "pending.empty":         return RED + (en ? "Nothing to collect." : "Nichts zum Einsammeln.");

            // ---- Console ----
            case "console.vault_missing": return en ? "Vault not found! FancyEggs is disabling." : "Vault nicht gefunden! FancyEggs wird deaktiviert.";
            case "console.enabled":       return en ? "FancyEggs successfully loaded and connected to Vault!" : "FancyEggs erfolgreich geladen und mit Vault verbunden!";

            default: return "&cMissing translation: " + key;
        }
    }

    public String color(String s) {
        Matcher matcher = HEX_PATTERN.matcher(s);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);

        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public String getColored(String key) {
        return color(get(key));
    }
}
