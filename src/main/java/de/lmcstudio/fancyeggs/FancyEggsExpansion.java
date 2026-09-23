package de.lmcstudio.fancyeggs;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FancyEggsExpansion extends PlaceholderExpansion {

    private final FancyEggs plugin;

    public FancyEggsExpansion(FancyEggs plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "fancyeggs"; }

    @Override
    public @NotNull String getAuthor() { return "LMCStudio"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public boolean canRegister() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        // %fancyeggs_total_income% -> z.B. "1.50M"
        if (params.equalsIgnoreCase("total_income")) {
            return NumberFormatter.format(getTotalIncome(player));
        }

        // %fancyeggs_total_income_raw% -> z.B. "1500000.0"
        if (params.equalsIgnoreCase("total_income_raw")) {
            return String.valueOf(getTotalIncome(player));
        }

        // %fancyeggs_egg_count% -> z.B. "7"
        if (params.equalsIgnoreCase("egg_count")) {
            List<FancyEggs.Egg> eggs = plugin.getPlayerEggs().get(player.getUniqueId());
            return eggs == null ? "0" : String.valueOf(eggs.size());
        }

        // %fancyeggs_upgrade_cost_<key>% -> z.B. "%fancyeggs_upgrade_cost_Chicken_Egg%"
        if (params.toLowerCase().startsWith("upgrade_cost_")) {
            String key = params.substring("upgrade_cost_".length());
            List<FancyEggs.Egg> eggs = plugin.getPlayerEggs().get(player.getUniqueId());
            if (eggs != null) {
                for (FancyEggs.Egg egg : eggs) {
                    if (egg.type.key.equalsIgnoreCase(key)) {
                        return NumberFormatter.format(egg.getUpgradeCost());
                    }
                }
            }
            return "0.00";
        }

        return null;
    }

    private double getTotalIncome(OfflinePlayer player) {
        List<FancyEggs.Egg> eggs = plugin.getPlayerEggs().get(player.getUniqueId());
        if (eggs == null || eggs.isEmpty()) return 0.0;
        double total = 0;
        for (FancyEggs.Egg egg : eggs) total += egg.getCurrentIncome();
        return total;
    }
}
