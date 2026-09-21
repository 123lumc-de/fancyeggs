package de.lmcstudio.fancyeggs;

import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class FancyEggs extends JavaPlugin implements Listener {

    private Economy econ = null;
    private Lang lang;
    private double chargedMultiplier = 1.25;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    // Aktive Eggs (im Menü sichtbar)
    private final Map<UUID, List<Egg>> playerEggs = new HashMap<>();
    // Auto-Collect Status
    private final Map<UUID, Boolean> autoCollect = new HashMap<>();
    // Aufgesammeltes Geld (wenn Auto-Collect aus)
    private final Map<UUID, Double> pendingMoney = new HashMap<>();

    private final List<EggType> eggTypes = new ArrayList<>();

    // ==== GUI Slots ====
    private static final int[] BORDER_SLOTS = {
        0,1,2,3,4,5,6,7,8,
        9,17,
        18,26,
        27,35,
        36,44,
        45,46,47,48,49,50,51,52,53
    };
    private static final int SLOT_AUTOCOLLECT = 49;
    private static final int SLOT_INFO = 4;
    private static final int[] EGG_SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        getServer().getScheduler().runTaskLater(this, () -> {
            if (!setupEconomy()) {
                getLogger().severe("Vault nicht gefunden! FancyEggs wird deaktiviert.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getServer().getPluginManager().registerEvents(this, this);
            loadConfigValues();

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        List<Egg> eggs = playerEggs.get(p.getUniqueId());
                        if (eggs == null || eggs.isEmpty()) continue;
                        double totalIncome = 0;
                        for (Egg egg : eggs) totalIncome += egg.getCurrentIncome();
                        if (totalIncome <= 0) continue;

                        boolean auto = autoCollect.getOrDefault(p.getUniqueId(), true);
                        if (auto) {
                            econ.depositPlayer(p, totalIncome);
                        } else {
                            pendingMoney.merge(p.getUniqueId(), totalIncome, Double::sum);
                        }
                    }
                }
            }.runTaskTimer(this, 20L, 20L);

            getLogger().info(lang.get("console.enabled"));
        }, 1L);

        // bStats Setup
        try {
            Metrics metrics = new Metrics(this, 34188);
            metrics.addCustomChart(new SimplePie("language", () -> getConfig().getString("language", "de")));
            metrics.addCustomChart(new SimplePie("eggs_owned_chart", () -> {
                int total = 0;
                for (List<Egg> list : playerEggs.values()) total += list.size();
                return String.valueOf(total);
            }));
            getLogger().info("bStats erfolgreich aktiviert (ID: 34188).");
        } catch (Throwable t) {
            getLogger().warning("bStats konnte nicht geladen werden: " + t.getMessage());
        }
    }

    private void loadConfigValues() {
        this.lang = new Lang(getConfig());
        this.chargedMultiplier = getConfig().getDouble("charged-multiplier", 1.25);
        eggTypes.clear();

        if (!getConfig().isConfigurationSection("eggs")) {
            getLogger().warning("Keine Eggs in der config.yml gefunden!");
            return;
        }

        for (String key : getConfig().getConfigurationSection("eggs").getKeys(false)) {
            String path = "eggs." + key + ".";
            String displayName = getConfig().getString(path + "display-name", key);
            String iconName = getConfig().getString(path + "icon", "EGG");
            double baseIncome = getConfig().getDouble(path + "base-income", 10.0);
            double baseUpgrade = getConfig().getDouble(path + "base-upgrade-cost", 10000.0);
            double multiplier = getConfig().getDouble(path + "upgrade-multiplier", 1.5);
            double sellMultiplier = getConfig().getDouble(path + "sell-multiplier", 4.0);

            Material icon;
            try {
                icon = Material.valueOf(iconName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Ungültiges Icon '" + iconName + "' für Egg '" + key + "'. Benutze EGG als Fallback.");
                icon = Material.EGG;
            }

            eggTypes.add(new EggType(key, displayName, baseIncome, baseUpgrade, multiplier, sellMultiplier, icon));
        }

        getLogger().info(eggTypes.size() + " Eggs aus der Config geladen.");
    }

    @Override
    public void onDisable() {
        // Pending-Money noch auszahlen
        for (Map.Entry<UUID, Double> entry : pendingMoney.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && econ != null) econ.depositPlayer(p, entry.getValue());
        }
        playerEggs.clear();
        pendingMoney.clear();
        autoCollect.clear();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /fancyeggs reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("fancyeggs.admin")) {
                sender.sendMessage(lang.getColored("msg.no_permission"));
                return true;
            }
            reloadConfig();
            loadConfigValues();
            sender.sendMessage(lang.color(Lang.GOLD + Lang.BOLD + "FancyEggs config reloaded. " + eggTypes.size() + " Eggs aktiv."));
            return true;
        }

        // /fancyeggs give <Spieler> <EggKey> [charged]
        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("fancyeggs.admin")) {
                sender.sendMessage(lang.getColored("msg.no_permission"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(lang.getColored("msg.give_usage"));
                sender.sendMessage(lang.getColored("msg.available") + eggTypes.stream()
                        .map(t -> t.key).collect(Collectors.joining(", ")));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(lang.getColored("msg.player_not_found"));
                return true;
            }
            String eggKey = args[2];
            EggType type = eggTypes.stream()
                    .filter(t -> t.key.equalsIgnoreCase(eggKey) || t.name.equalsIgnoreCase(eggKey.replace("_", " ")))
                    .findFirst().orElse(null);
            if (type == null) {
                sender.sendMessage(lang.getColored("msg.egg_not_found"));
                return true;
            }
            boolean isCharged = args.length > 3 && args[3].equalsIgnoreCase("charged");
            playerEggs.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>()).add(new Egg(type, 1, isCharged));

            String suffix = isCharged ? lang.getColored("msg.charged_suffix") : "";
            sender.sendMessage(lang.color(lang.get("msg.give_sender")
                    .replace("%player%", target.getName()).replace("%egg%", type.name).replace("%charged%", suffix)));
            target.sendMessage(lang.color(lang.get("msg.give_target")
                    .replace("%egg%", type.name).replace("%charged%", suffix)));
            return true;
        }

        // /fancyeggs list
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(lang.getColored("list.header"));
            for (EggType type : eggTypes) {
                sender.sendMessage(lang.getColored("list.name") + lang.color(Lang.GOLD + Lang.BOLD + type.name + " (" + type.key + ")"));
                sender.sendMessage(lang.getColored("list.base_income") + lang.color(Lang.GOLD + Lang.BOLD + df.format(type.baseIncome)) + lang.color(Lang.WHITE) + "/s");
                sender.sendMessage(lang.getColored("list.upgrade_cost") + lang.color(Lang.GOLD + Lang.BOLD + df.format(type.baseUpgradeCost)));
                sender.sendMessage(lang.getColored("list.multiplier") + lang.color(Lang.GOLD + Lang.BOLD + type.upgradeMultiplier));
            }
            sender.sendMessage(lang.getColored("list.footer"));
            return true;
        }

        if (sender instanceof Player) {
            openEggsMenu((Player) sender);
        } else {
            sender.sendMessage(lang.getColored("msg.only_players_menu"));
        }
        return true;
    }

    // ============================
    // HAUPT MENÜ
    // ============================
    private void openEggsMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, lang.getColored("menu.title"));

        // ---- Rahmen mit hellgrauen Scheiben ----
        ItemStack border = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        for (int slot : BORDER_SLOTS) inv.setItem(slot, border);

        // ---- Eggs anzeigen ----
        List<Egg> eggs = playerEggs.get(p.getUniqueId());
        if (eggs != null) {
            int i = 0;
            for (Egg egg : eggs) {
                if (i >= EGG_SLOTS.length) break;
                inv.setItem(EGG_SLOTS[i], createEggItem(egg));
                i++;
            }
        }

        // ---- Auto-Collect Toggle ----
        boolean auto = autoCollect.getOrDefault(p.getUniqueId(), true);
        ItemStack toggle = new ItemStack(auto ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta toggleMeta = toggle.getItemMeta();
        toggleMeta.setDisplayName(lang.getColored(auto ? "autocollect.on_name" : "autocollect.off_name"));
        toggleMeta.setLore(Arrays.asList(
            lang.getColored("autocollect.desc"),
            "",
            lang.getColored("autocollect.info"),
            lang.getColored(auto ? "autocollect.on_text" : "autocollect.off_text"),
            "",
            lang.getColored("autocollect.click")
        ));
        toggle.setItemMeta(toggleMeta);
        inv.setItem(SLOT_AUTOCOLLECT, toggle);

        // ---- Info Chest (nur wenn Auto-Collect aus) ----
        if (!auto) {
            double pending = pendingMoney.getOrDefault(p.getUniqueId(), 0.0);
            ItemStack chest = new ItemStack(Material.CHEST);
            ItemMeta chestMeta = chest.getItemMeta();
            chestMeta.setDisplayName(lang.getColored("pending.name"));
            chestMeta.setLore(Arrays.asList(
                lang.getColored("pending.desc"),
                "",
                lang.getColored("pending.info"),
                lang.getColored("pending.amount") + lang.color(Lang.GOLD + Lang.BOLD + "$" + df.format(pending)),
                "",
                lang.getColored("pending.click")
            ));
            chest.setItemMeta(chestMeta);
            inv.setItem(SLOT_INFO, chest);
        }

        p.openInventory(inv);
    }

    private ItemStack createEggItem(Egg egg) {
        ItemStack item = new ItemStack(egg.type.icon);
        ItemMeta meta = item.getItemMeta();

        String charged = egg.isCharged ? lang.getColored("egg.charged_tag") : "";
        meta.setDisplayName(charged + lang.color(Lang.GOLD + Lang.BOLD + egg.type.name));

        List<String> lore = new ArrayList<>();
        lore.add(lang.getColored("egg.desc"));
        lore.add("");
        lore.add(lang.getColored("egg.level") + lang.color(Lang.GOLD + Lang.BOLD + egg.level + " ★"));
        lore.add("");
        lore.add(lang.getColored("egg.income") + lang.color(Lang.GOLD + Lang.BOLD + df.format(egg.getCurrentIncome())));
        lore.add(lang.getColored("egg.upgrade_price") + lang.color(Lang.GOLD + Lang.BOLD + df.format(egg.getUpgradeCost())));
        lore.add(lang.getColored("egg.sell_price") + lang.color(Lang.GOLD + Lang.BOLD + df.format(egg.getSellPrice())));
        lore.add("");
        lore.add(lang.getColored("egg.left_upgrade"));
        lore.add(lang.getColored("egg.shift_sell"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        String title = e.getView().getTitle();

        if (title.equals(lang.getColored("menu.title"))) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            // ---- Auto-Collect Toggle ----
            if (e.getSlot() == SLOT_AUTOCOLLECT) {
                boolean newState = !autoCollect.getOrDefault(p.getUniqueId(), true);
                autoCollect.put(p.getUniqueId(), newState);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, newState ? 1.2f : 0.8f);
                p.closeInventory();
                Bukkit.getScheduler().runTaskLater(this, () -> openEggsMenu(p), 1L);
                return;
            }

            // ---- Info Chest (Geld einsammeln) ----
            if (e.getSlot() == SLOT_INFO && e.getCurrentItem().getType() == Material.CHEST) {
                double pending = pendingMoney.getOrDefault(p.getUniqueId(), 0.0);
                if (pending > 0) {
                    econ.depositPlayer(p, pending);
                    pendingMoney.put(p.getUniqueId(), 0.0);
                    p.sendMessage(lang.color(lang.get("pending.collected").replace("%amount%", df.format(pending))));
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                } else {
                    p.sendMessage(lang.getColored("pending.empty"));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                p.closeInventory();
                Bukkit.getScheduler().runTaskLater(this, () -> openEggsMenu(p), 1L);
                return;
            }

            // ---- Egg angeklickt ----
            if (e.getCurrentItem().getType() != Material.AIR
                    && e.getCurrentItem().getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE
                    && e.getCurrentItem().getType() != Material.LIME_STAINED_GLASS_PANE
                    && e.getCurrentItem().getType() != Material.RED_STAINED_GLASS_PANE
                    && e.getCurrentItem().getType() != Material.CHEST) {

                List<Egg> eggs = playerEggs.get(p.getUniqueId());
                if (eggs == null || eggs.isEmpty()) return;

                String displayName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                Egg targetEgg = null;
                for (Egg egg : eggs) {
                    if (displayName.contains(egg.type.name)) { targetEgg = egg; break; }
                }
                if (targetEgg == null) return;

                if (e.isShiftClick()) {
                    // ============ VERKAUFEN ============
                    double sellPrice = targetEgg.getSellPrice();
                    econ.depositPlayer(p, sellPrice);
                    eggs.remove(targetEgg);
                    p.sendMessage(lang.color(lang.get("msg.sold")
                            .replace("%egg%", targetEgg.type.name)
                            .replace("%price%", df.format(sellPrice))));
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
                    p.closeInventory();
                    Bukkit.getScheduler().runTaskLater(this, () -> openEggsMenu(p), 1L);

                } else if (e.isLeftClick()) {
                    // ============ UPGRADEN ============
                    double cost = targetEgg.getUpgradeCost();
                    if (econ.getBalance(p) >= cost) {
                        econ.withdrawPlayer(p, cost);
                        targetEgg.level++;
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        p.sendMessage(lang.getColored("msg.upgraded") + targetEgg.level);
                    } else {
                        p.sendMessage(lang.getColored("msg.not_enough") + df.format(cost));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                    p.closeInventory();
                    Bukkit.getScheduler().runTaskLater(this, () -> openEggsMenu(p), 1L);
                }
            }
        }
    }

    // ============================
    // Egg-Klassen
    // ============================
    public class Egg {
        public EggType type;
        public int level;
        public boolean isCharged;

        public Egg(EggType type, int level, boolean isCharged) {
            this.type = type;
            this.level = level;
            this.isCharged = isCharged;
        }

        public double getCurrentIncome() {
            double base = type.baseIncome * Math.pow(type.upgradeMultiplier, level - 1);
            return isCharged ? base * chargedMultiplier : base;
        }

        public double getUpgradeCost() {
            return type.baseUpgradeCost * Math.pow(type.upgradeMultiplier, level - 1);
        }

        // Verkaufspreis = sellMultiplier * Upgrade-Preis (Standard: 4.0)
        public double getSellPrice() {
            return getUpgradeCost() * type.sellMultiplier;
        }
    }

    public static class EggType {
        public String key;              // Config-Key (z.B. "Chicken_Egg")
        public String name;             // Anzeigename (z.B. "Chicken Egg")
        public double baseIncome;
        public double baseUpgradeCost;
        public double upgradeMultiplier;
        public double sellMultiplier;
        public Material icon;

        public EggType(String key, String name, double baseIncome, double baseUpgradeCost,
                       double upgradeMultiplier, double sellMultiplier, Material icon) {
            this.key = key;
            this.name = name;
            this.baseIncome = baseIncome;
            this.baseUpgradeCost = baseUpgradeCost;
            this.upgradeMultiplier = upgradeMultiplier;
            this.sellMultiplier = sellMultiplier;
            this.icon = icon;
        }
    }
}
