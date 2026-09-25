package de.lmcstudio.fancyeggs;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lang {
    private final String lang;
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static final String GOLD="&#FFD700", GRAY="&#AAAAAA", WHITE="&#FFFFFF",
        GREEN="&#55FF55", RED="&#FF5555", AQUA="&#55FFFF", PINK="&#FF55FF",
        BOLD="&l", RESET="&r";

    public Lang(FileConfiguration c) { this.lang = c.getString("language","de").toLowerCase(); }

    public String get(String k) {
        boolean en = lang.equals("en");
        switch (k) {
            case "menu.title": return GOLD+BOLD+(en?"Eggs | Menu":"Eggs | Menü");

            case "egg.desc": return GRAY+(en?"Description":"Beschreibung");
            case "egg.level": return AQUA+BOLD+"★ "+(en?"LEVEL ":"LEVEL ");
            case "egg.income": return GRAY+(en?"Money / Second: ":"Geld / Sekunde: ")+GREEN+"$";
            case "egg.upgrade_price": return GRAY+(en?"Upgrade Price: ":"Upgrade Preis: ")+RED+"$";
            case "egg.sell_price": return GRAY+(en?"Sell Price: ":"Verkaufspreis: ")+RED+"$";
            case "egg.left_upgrade": return GOLD+BOLD+"➤ "+(en?"LEFT-CLICK to upgrade":"LEFT-KLICK zum Upgraden");
            case "egg.shift_sell": return RED+BOLD+"➤ "+(en?"SHIFT-CLICK to sell":"SHIFT-KLICK zum Verkaufen");
            case "egg.right_remove": return RED+BOLD+"➤ "+(en?"RIGHT-CLICK to store":"RECHTS-KLICK zum Einlagern");
            case "egg.charged_tag": return PINK+BOLD+"[CHARGED] ";
            case "egg.charged_chance": return GRAY+(en?"Charged Chance: ":"Charged Chance: ")+PINK+BOLD+"%chance%%";
            case "egg.charged_bonus": return GRAY+(en?"Charged Bonus: ":"Charged Bonus: ")+PINK+BOLD+"+%bonus%%";

            case "msg.no_permission": return RED+(en?"No permission.":"Keine Rechte.");
            case "msg.give_usage": return RED+(en?"Usage: /fancyeggs give <player> <egg> [forced|normal]":"Nutzung: /fancyeggs give <Spieler> <EggKey> [forced|normal]");
            case "msg.available": return GOLD+BOLD+(en?"Available: ":"Verfügbar: ")+RESET+WHITE;
            case "msg.player_not_found": return RED+(en?"Player not found.":"Spieler nicht gefunden.");
            case "msg.egg_not_found": return RED+(en?"Egg not found.":"Egg nicht gefunden.");
            case "msg.give_sender": return GOLD+BOLD+(en?"Gave %player% a %egg%%charged%.":"Du hast %player% ein %egg%%charged% gegeben.");
            case "msg.give_target": return GOLD+BOLD+(en?"You received a %egg%%charged%!":"Du hast ein %egg%%charged% erhalten!");
            case "msg.charged_suffix": return PINK+BOLD+" (Charged)";
            case "msg.upgraded": return GOLD+BOLD+(en?"Egg upgraded! New Level: ":"Egg upgraded! Neues Level: ")+AQUA;
            case "msg.not_enough": return RED+(en?"Not enough money! Needed: $":"Nicht genug Geld! Benötigt: $");
            case "msg.sold": return GOLD+BOLD+(en?"Sold %egg% for $%price%!":"%egg% für $%price% verkauft!");
            case "msg.only_players_menu": return RED+(en?"Only players.":"Nur Spieler.");
            case "msg.slots_moved": return GOLD+(en?"Your slot limit decreased. %amount% egg(s) moved to storage.":"Dein Slot-Limit wurde verringert. %amount% Egg(s) ins Lager verschoben.");

            case "list.header": return GOLD+BOLD+(en?"========== FancyEggs List ==========":"========== FancyEggs Liste ==========");
            case "list.name": return GOLD+BOLD+"▶ ";
            case "list.base_income": return GRAY+(en?"Base income: ":"Basis-Einkommen: ")+GREEN+"$";
            case "list.upgrade_cost": return GRAY+(en?"Upgrade cost: ":"Upgrade-Kosten: ")+RED+"$";
            case "list.multiplier": return GRAY+(en?"Multiplier: ":"Multiplikator: ")+AQUA+"x";
            case "list.footer": return GOLD+BOLD+"===================================";

            case "autocollect.on_name": return GREEN+BOLD+(en?"AUTO-COLLECT: ON":"AUTO-COLLECT: AN");
            case "autocollect.off_name": return RED+BOLD+(en?"AUTO-COLLECT: OFF":"AUTO-COLLECT: AUS");
            case "autocollect.desc": return GRAY+(en?"Description":"Beschreibung");
            case "autocollect.info": return GOLD+BOLD+(en?"Information:":"Information:");
            case "autocollect.on_text": return WHITE+(en?"Money is auto-collected every second.":"Geld wird jede Sekunde automatisch gesammelt.");
            case "autocollect.off_text": return WHITE+(en?"Money accumulates in the chest.":"Geld sammelt sich in der Kiste an.");
            case "autocollect.click": return GOLD+BOLD+"➤ "+(en?"CLICK to toggle":"KLICK zum Umschalten");

            case "pending.name": return GOLD+BOLD+(en?"PENDING MONEY":"AUSSTEHENDES GELD");
            case "pending.desc": return GRAY+(en?"Description":"Beschreibung");
            case "pending.info": return GOLD+BOLD+(en?"Information:":"Information:");
            case "pending.amount": return GRAY+(en?"Pending: ":"Ausstehend: ");
            case "pending.click": return GOLD+BOLD+"➤ "+(en?"CLICK to collect":"KLICK zum Einsammeln");
            case "pending.collected": return GREEN+BOLD+(en?"Collected $%amount%!":"$%amount% eingesammelt!");
            case "pending.empty": return RED+(en?"Nothing to collect.":"Nichts zum Einsammeln.");

            case "storage.name": return GOLD+BOLD+(en?"EGGS STORAGE":"EGGS LAGER");
            case "storage.desc": return GRAY+(en?"Description":"Beschreibung");
            case "storage.info": return GOLD+BOLD+(en?"Information:":"Information:");
            case "storage.click": return WHITE+(en?"Click to view your storage.":"Klicke hier um dein Lager zu öffnen.");
            case "storage.count": return GOLD+BOLD+(en?"➤ Stored: ":"➤ Gelagert: ");
            case "storage.active": return GOLD+BOLD+(en?"➤ Active: ":"➤ Aktiv: ");
            case "storage.moved_to_storage": return GOLD+BOLD+(en?"%egg% moved to storage.":"%egg% ins Lager verschoben.");
            case "storage.moved_to_active": return GREEN+BOLD+(en?"%egg% equipped!":"%egg% ausgerüstet!");
            case "storage.full_active": return RED+(en?"Max active eggs reached!":"Maximale aktive Eggs erreicht!");
            case "storage.menu.title": return GOLD+BOLD+(en?"Eggs | Storage":"Eggs | Lager");
            case "storage.page.prev": return GOLD+BOLD+"◀ "+(en?"Previous Page":"Vorherige Seite");
            case "storage.page.next": return GOLD+BOLD+(en?"Next Page":"Nächste Seite")+" ▶";
            case "storage.page.info": return GOLD+BOLD+(en?"Page ":"Seite ")+"%page%/%max%";
            case "storage.page.click": return WHITE+(en?"Click to switch page.":"Klicke zum Wechseln.");

            case "console.vault_missing": return en?"Vault not found!":"Vault nicht gefunden!";
            case "console.enabled": return en?"FancyEggs loaded!":"FancyEggs geladen!";
            default: return "&cMissing: "+k;
        }
    }

    public String color(String s) {
        Matcher m = HEX.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) m.appendReplacement(sb, ChatColor.of("#"+m.group(1)).toString());
        m.appendTail(sb);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public String getColored(String k) { return color(get(k)); }
}
