package de.lmcstudio.fancyeggs;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;

public class FancyEggsExpansion extends PlaceholderExpansion {

    private final FancyEggs plugin;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

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

        if (params.equalsIgnoreCase("total_income")) {
            return String.valueOf(getTotalIncome(player));
        }

        if (params.equalsIgnoreCase("total_income_formatted")) {
            return df.format(getTotalIncome(player));
        }

        if (params.equalsIgnoreCase("egg_count")) {
            List<FancyEggs.Egg> eggs = plugin.getPlayerEggs().get(player.getUniqueId());
            return eggs == null ? "0" : String.valueOf(eggs.size());
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
