package de.lmcstudio.fancyeggs;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        TopManager top = plugin.getTopManager();

        // === Eigene Werte ===
        if (player != null) {
            if (params.equalsIgnoreCase("total_income")) {
                return NumberFormatter.format(getTotalIncome(player));
            }
            if (params.equalsIgnoreCase("total_income_raw")) {
                return String.valueOf(getTotalIncome(player));
            }
            if (params.equalsIgnoreCase("egg_count")) {
                List<FancyEggs.Egg> eggs = plugin.getPlayerEggs().get(player.getUniqueId());
                return eggs == null ? "0" : String.valueOf(eggs.size());
            }
            if (params.equalsIgnoreCase("rank")) {
                List<Map.Entry<UUID, Double>> sorted = top.getSorted();
                for (int i = 0; i < sorted.size(); i++) {
                    if (sorted.get(i).getKey().equals(player.getUniqueId())) {
                        return String.valueOf(i + 1);
                    }
                }
                return "-";
            }
        }

        // === Top-Liste: %fancyeggs_top_<rank>_<name|value>% ===
        if (params.toLowerCase().startsWith("top_")) {
            String[] parts = params.split("_");
            if (parts.length == 3) {
                try {
                    int rank = Integer.parseInt(parts[1]);
                    String type = parts[2].toLowerCase();

                    List<Map.Entry<UUID, Double>> sorted = top.getSorted();
                    if (rank < 1 || rank > sorted.size()) {
                        return type.equals("name") ? "-" : "0.00";
                    }

                    Map.Entry<UUID, Double> entry = sorted.get(rank - 1);
                    if (type.equals("name")) {
                        return top.getNameByUUID(entry.getKey());
                    } else if (type.equals("value")) {
                        return NumberFormatter.format(entry.getValue());
                    } else if (type.equals("value_raw")) {
                        return String.valueOf(entry.getValue());
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // === Gesamtzahl der Spieler in der Rangliste ===
        if (params.equalsIgnoreCase("top_total")) {
            return String.valueOf(top.size());
        }

        // === Upgrade-Kosten eines Eggs nach Key ===
        if (params.toLowerCase().startsWith("upgrade_cost_")) {
            if (player == null) return "0.00";
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
