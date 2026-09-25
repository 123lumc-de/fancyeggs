package de.lmcstudio.fancyeggs;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;

public class Currency {

    private final String symbol;
    private final String format;
    private final boolean useCustom;

    /**
     * @param econ   Die Vault-Economy-Instanz (kann null sein)
     * @param config Die config.yml (für optionale Overrides)
     */
    public Currency(Economy econ, FileConfiguration config) {
        // Wenn in der config.yml eine Währung definiert ist, nutze diese
        String custom = config.getString("currency.symbol", "");
        this.useCustom = !custom.isEmpty();

        if (useCustom) {
            this.symbol = custom;
        } else if (econ != null) {
            // Vault-Währung nutzen (z.B. "$", "€", "Coins")
            String s = econ.currencyNameSingular();
            if (s == null || s.isEmpty()) s = econ.currencyNamePlural();
            if (s == null || s.isEmpty()) s = "$";
            this.symbol = s;
        } else {
            this.symbol = "$";
        }

        // Format: "{symbol}{amount}" oder "{amount} {symbol}" oder "{symbol} {amount}"
        this.format = config.getString("currency.format", "{symbol}{amount}");
    }

    /**
     * Formatiert einen Betrag mit der Währung.
     * Beispiel: 1500.0 -> "$1.50K" oder "1.50K Coins"
     */
    public String format(double amount) {
        String number = NumberFormatter.format(amount);
        return format
                .replace("{symbol}", symbol)
                .replace("{amount}", number)
                .replace("{space}", " ")
                .replace("{s}", " ")
                .trim();
    }

    /** Für Fälle, wo du nur das Symbol brauchst. */
    public String getSymbol() {
        return symbol;
    }
}
