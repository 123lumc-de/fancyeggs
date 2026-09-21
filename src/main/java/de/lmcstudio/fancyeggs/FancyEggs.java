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
    private final Map<UUID, List<Egg>> playerEggs = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    // Egg-Definitionen (Name, Basis-Einkommen, Upgrade-Basis-Kosten, Upgrade-Multiplikator, Icon)
    private final List<EggType> eggTypes = Arrays.asList(
        new EggType("Chicken Egg", 15.0, 25000.0, 1.5, Material.CHICKEN_SPAWN_EGG),
        new EggType("Parrot Egg", 35.0, 50000.0, 1.5, Material.PARROT_SPAWN_EGG),
        new EggType("Blaze Egg", 45.0, 100000.0, 1.5, Material.BLAZE_SPAWN_EGG),
        new EggType("Squid Egg", 80.0, 175000.0, 1.6, Material.SQUID_SPAWN_EGG),
        new EggType("Warden Egg", 150.0, 250000.0, 1.8, Material.WARDEN_SPAWN_EGG)
    );

    @Override
    public void onEnable() {
        // Warte 1 Tick, damit Vault sicher geladen ist
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!setupEconomy()) {
                getLogger().severe("Vault nicht gefunden! FancyEggs wird deaktiviert.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            getServer().getPluginManager().registerEvents(this, this);

            // Passives Einkommen jede Sekunde
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        List<Egg> eggs = playerEggs.get(p.getUniqueId());
                        if (eggs == null || eggs.isEmpty()) continue;
                        double totalIncome = 0;
                        for (Egg egg : eggs) {
                            totalIncome += egg.getCurrentIncome();
                        }
                        if (totalIncome > 0) {
                            econ.depositPlayer(p, totalIncome);
                        }
                    }
                }
            }.runTaskTimer(this, 20L, 20L);

            getLogger().info("FancyEggs erfolgreich geladen und mit Vault verbunden!");
        }, 1L);

        // bStats Setup (Plugin-ID 34188)
        int pluginId = 34188;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("eggs_owned_chart", () -> {
            int total = 0;
            for (List<Egg> list : playerEggs.values()) total += list.size();
            return String.valueOf(total);
        }));
    }

    @Override
    public void onDisable() {
        playerEggs.clear();
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
        // /fancyeggs give <Spieler> <EggName> [charged]  -> Admin
        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("fancyeggs.admin")) {
                sender.sendMessage(ChatColor.RED + "Dazu hast du keine Rechte.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Nutzung: /fancyeggs give <Spieler> <EggName> [charged]");
                sender.sendMessage(ChatColor.YELLOW + "Verfügbar: " + eggTypes.stream()
                        .map(t -> t.name.replace(" ", "_")).collect(Collectors.joining(", ")));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
                return true;
            }
            String eggName = args[2].replace("_", " ");
            EggType type = eggTypes.stream().filter(t -> t.name.equalsIgnoreCase(eggName)).findFirst().orElse(null);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Egg-Typ nicht gefunden.");
                return true;
            }
            boolean isCharged = args.length > 3 && args[3].equalsIgnoreCase("charged");
            playerEggs.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>()).add(new Egg(type, 1, isCharged));
            sender.sendMessage(ChatColor.GREEN + "Du hast " + target.getName() + " ein " + type.name
                    + (isCharged ? " (Charged)" : "") + " gegeben.");
            target.sendMessage(ChatColor.GREEN + "Du hast ein " + type.name
                    + (isCharged ? " (Charged)" : "") + " erhalten!");
            return true;
        }

        // /fancyeggs list -> Alle Egg-Typen anzeigen
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.GOLD + "========== " + ChatColor.YELLOW + "FancyEggs Liste" + ChatColor.GOLD + " ==========");
            for (EggType type : eggTypes) {
                sender.sendMessage(ChatColor.YELLOW + "▶ " + type.name);
                sender.sendMessage(ChatColor.GRAY + "  Basis-Einkommen: " + ChatColor.GREEN + "$" + df.format(type.baseIncome) + "/s");
                sender.sendMessage(ChatColor.GRAY + "  Upgrade-Kosten: " + ChatColor.RED + "$" + df.format(type.baseUpgradeCost));
                sender.sendMessage(ChatColor.GRAY + "  Multiplikator: " + ChatColor.AQUA + "x" + type.upgradeMultiplier);
            }
            sender.sendMessage(ChatColor.GOLD + "===================================");
            return true;
        }

        // Standard /fancyeggs -> Menü öffnen
        if (sender instanceof Player) {
            openEggsMenu((Player) sender);
        } else {
            sender.sendMessage("Nur Spieler können das Menü öffnen.");
        }
        return true;
    }

    private void openEggsMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "Eggs | Menü");

        ItemStack lagerItem = new ItemStack(Material.CHEST);
        ItemMeta lagerMeta = lagerItem.getItemMeta();
        lagerMeta.setDisplayName(ChatColor.GOLD + "EGGS LAGER");
        lagerMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Beschreibung",
            "",
            ChatColor.YELLOW + "Information:",
            ChatColor.WHITE + "Klicke hier um andere Eggs auszurüsten.",
            "",
            ChatColor.GOLD + "➤ Deine Eggs: " + (playerEggs.getOrDefault(p.getUniqueId(), new ArrayList<>()).size())
        ));
        lagerItem.setItemMeta(lagerMeta);
        inv.setItem(49, lagerItem);

        List<Egg> eggs = playerEggs.get(p.getUniqueId());
        if (eggs != null) {
            int slot = 10;
            for (Egg egg : eggs) {
                if (slot > 43) break;
                inv.setItem(slot, createEggItem(egg));
                slot++;
            }
        }
        p.openInventory(inv);
    }

    private ItemStack createEggItem(Egg egg) {
        ItemStack item = new ItemStack(egg.type.icon);
        ItemMeta meta = item.getItemMeta();

        String charged = egg.isCharged ? ChatColor.LIGHT_PURPLE + "[CHARGED] " : "";
        meta.setDisplayName(charged + ChatColor.YELLOW + egg.type.name);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Beschreibung");
        lore.add("");
        lore.add(ChatColor.AQUA + "★ LEVEL " + egg.level + " ★");
        lore.add("");
        lore.add(ChatColor.GREEN + "✿ Geld / Sekunde: $" + df.format(egg.getCurrentIncome()));
        lore.add(ChatColor.RED + "$ Upgrade Preis: $" + df.format(egg.getUpgradeCost()));
        lore.add("");
        lore.add(ChatColor.YELLOW + "➤ SHIFT-KLICK zum Upgraden");
        lore.add(ChatColor.RED + "➤ LEFT-KLICK zum Ablegen");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        if (e.getView().getTitle().equals("Eggs | Menü")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;

            if (e.getSlot() == 49 && e.getCurrentItem().getType() == Material.CHEST) {
                p.closeInventory();
                openInventoryMenu(p);
                return;
            }

            if (e.getCurrentItem().getType() != Material.AIR) {
                List<Egg> eggs = playerEggs.get(p.getUniqueId());
                if (eggs == null || eggs.isEmpty()) return;

                String displayName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                Egg targetEgg = null;
                for (Egg egg : eggs) {
                    if (displayName.contains(egg.type.name)) {
                        targetEgg = egg;
                        break;
                    }
                }
                if (targetEgg == null) return;

                if (e.isShiftClick()) {
                    double cost = targetEgg.getUpgradeCost();
                    if (econ.getBalance(p) >= cost) {
                        econ.withdrawPlayer(p, cost);
                        targetEgg.level++;
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        p.sendMessage(ChatColor.GREEN + "Egg upgraded! Neues Level: " + targetEgg.level);
                    } else {
                        p.sendMessage(ChatColor.RED + "Du hast nicht genug Geld! Benötigt: $" + df.format(cost));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                    p.closeInventory();
                    openEggsMenu(p);
                } else if (e.isLeftClick()) {
                    eggs.remove(targetEgg);
                    p.sendMessage(ChatColor.RED + "Du hast das " + targetEgg.type.name + " abgelegt.");
                    p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
                    p.closeInventory();
                    openEggsMenu(p);
                }
            }
        }

        if (e.getView().getTitle().equals("Eggs | Lager")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;

            if (e.getCurrentItem().getType() != Material.AIR
                    && e.getCurrentItem().getType() != Material.GRAY_STAINED_GLASS_PANE) {
                p.sendMessage(ChatColor.YELLOW + "Dieses Egg ist bereits in deinem Lager. Nutze /fancyeggs um es zu verwalten.");
            }
        }
    }

    private void openInventoryMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "Eggs | Lager");
        List<Egg> eggs = playerEggs.get(p.getUniqueId());
        if (eggs != null) {
            int slot = 0;
            for (Egg egg : eggs) {
                if (slot > 44) break;
                inv.setItem(slot, createEggItem(egg));
                slot++;
            }
        }
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }
        p.openInventory(inv);
    }

    // ============================
    // Egg-Klassen
    // ============================
    public static class Egg {
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
            return isCharged ? base * 1.25 : base;
        }

        public double getUpgradeCost() {
            return type.baseUpgradeCost * Math.pow(type.upgradeMultiplier, level - 1);
        }
    }

    public static class EggType {
        public String name;
        public double baseIncome;
        public double baseUpgradeCost;
        public double upgradeMultiplier;
        public Material icon;

        public EggType(String name, double baseIncome, double baseUpgradeCost, double upgradeMultiplier, Material icon) {
            this.name = name;
            this.baseIncome = baseIncome;
            this.baseUpgradeCost = baseUpgradeCost;
            this.upgradeMultiplier = upgradeMultiplier;
            this.icon = icon;
        }
    }
}
