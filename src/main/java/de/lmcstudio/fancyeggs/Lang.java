package de.lmcstudio.fancyeggs;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class Lang {

    private final String lang;

    public Lang(FileConfiguration config) {
        this.lang = config.getString("language", "de").toLowerCase();
    }

    public String get(String key) {
        boolean en = lang.equals("en");
        switch (key) {
            // ---- Menü Titel ----
            case "menu.title":        return en ? "Eggs | Menu"    : "Eggs | Menü";
            case "menu.storage.title": return en ? "Eggs | Storage" : "Eggs | Lager";

            // ---- Storage Item ----
            case "storage.name":       return en ? "&6EGGS STORAGE" : "&6EGGS LAGER";
            case "storage.desc":       return en ? "&7Description"  : "&7Beschreibung";
            case "storage.info":       return en ? "&eInformation:" : "&eInformation:";
            case "storage.click":      return en ? "&fClick here to equip other eggs." : "&fKlicke hier um andere Eggs auszurüsten.";
            case "storage.count":      return en ? "&6➤ Your Eggs: " : "&6➤ Deine Eggs: ";

            // ---- Egg Item ----
            case "egg.desc":           return en ? "&7Description" : "&7Beschreibung";
            case "egg.level":          return en ? "&b★ LEVEL " : "&b★ LEVEL ";
            case "egg.income":         return en ? "&a✿ Money / Second: $" : "&a✿ Geld / Sekunde: $";
            case "egg.upgrade_price":  return en ? "&c$ Upgrade Price: $" : "&c$ Upgrade Preis: $";
            case "egg.shift_upgrade":  return en ? "&e➤ SHIFT-CLICK to upgrade" : "&e➤ SHIFT-KLICK zum Upgraden";
            case "egg.left_remove":    return en ? "&c➤ LEFT-CLICK to remove" : "&c➤ LEFT-KLICK zum Ablegen";
            case "egg.charged_tag":    return en ? "&d[CHARGED] " : "&d[CHARGED] ";

            // ---- Chat Messages ----
            case "msg.no_permission":  return en ? "&cYou don't have permission for this." : "&cDazu hast du keine Rechte.";
            case "msg.give_usage":     return en ? "&cUsage: /fancyeggs give <player> <egg> [charged]" : "&cNutzung: /fancyeggs give <Spieler> <EggName> [charged]";
            case "msg.available":      return en ? "&eAvailable: " : "&eVerfügbar: ";
            case "msg.player_not_found": return en ? "&cPlayer not found." : "&cSpieler nicht gefunden.";
            case "msg.egg_not_found":  return en ? "&cEgg type not found." : "&cEgg-Typ nicht gefunden.";
            case "msg.give_sender":    return en ? "&aYou gave %player% a %egg%%charged%." : "&aDu hast %player% ein %egg%%charged% gegeben.";
            case "msg.give_target":    return en ? "&aYou received a %egg%%charged%!" : "&aDu hast ein %egg%%charged% erhalten!";
            case "msg.charged_suffix": return en ? " (Charged)" : " (Charged)";
            case "msg.upgraded":       return en ? "&aEgg upgraded! New Level: " : "&aEgg upgraded! Neues Level: ";
            case "msg.not_enough":     return en ? "&cYou don't have enough money! Needed: $" : "&cDu hast nicht genug Geld! Benötigt: $";
            case "msg.removed":        return en ? "&cYou removed the %egg%." : "&cDu hast das %egg% abgelegt.";
            case "msg.already_storage":return en ? "&eThis egg is already in your storage. Use /fancyeggs to manage it." : "&eDieses Egg ist bereits in deinem Lager. Nutze /fancyeggs um es zu verwalten.";
            case "msg.only_players":   return en ? "Only players can use this command." : "Nur Spieler können diesen Befehl nutzen.";
            case "msg.only_players_menu": return en ? "Only players can open the menu." : "Nur Spieler können das Menü öffnen.";

            // ---- /eggs list ----
            case "list.header":        return en ? "&6========== &eFancyEggs List &6==========" : "&6========== &eFancyEggs Liste &6==========";
            case "list.name":          return en ? "&e▶ " : "&e▶ ";
            case "list.base_income":   return en ? "&7  Base income: &a$" : "&7  Basis-Einkommen: &a$";
            case "list.upgrade_cost":  return en ? "&7  Upgrade cost: &c$" : "&7  Upgrade-Kosten: &c$";
            case "list.multiplier":    return en ? "&7  Multiplier: &b" : "&7  Multiplikator: &b";
            case "list.footer":        return "&6===================================";

            // ---- Console ----
            case "console.vault_missing": return en ? "Vault not found! FancyEggs is disabling." : "Vault nicht gefunden! FancyEggs wird deaktiviert.";
            case "console.enabled":       return en ? "FancyEggs successfully loaded and connected to Vault!" : "FancyEggs erfolgreich geladen und mit Vault verbunden!";

            default: return "&cMissing translation: " + key;
        }
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public String getColored(String key) {
        return color(get(key));
    }
}
