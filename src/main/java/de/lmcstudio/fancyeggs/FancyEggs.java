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
import org.bukkit.configuration.file.FileConfiguration;
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
    private final Map<UUID, List<Egg>> playerEggs = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    private final List<EggType> eggTypes = new ArrayList<>();

    @Override
    public void onEnable() {
        // Config speichern & laden
        saveDefaultConfig();
        reloadConfig();
        loadConfigValues();

        getServer().getScheduler().runTaskLater(this, () -> {
            if (!setupEconomy()) {
                getLogger().severe(lang.get("console.vault_missing"));
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getServer().getPluginManager().registerEvents(this, this);

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        List<Egg> eggs = playerEggs.get(p.getUniqueId());
                        if (eggs == null || eggs.isEmpty()) continue;
                        double totalIncome = 0;
                        for (Egg egg : eggs) totalIncome += egg.getCurrentIncome();
                        if (totalIncome > 0) econ.depositPlayer(p, totalIncome);
                    }
                }
            }.runTaskTimer(this, 20L, 20L);

            getLogger().info(lang.get("console.enabled"));
        }, 1L);

        // bStats
        Metrics metrics = new Metrics(this, 34188);
        metrics.addCustomChart(new SimplePie("language", () -> lang.get("language").equals("en") ? "en" : "de"));
        metrics.addCustomChart(new SimplePie("eggs_owned_chart", () -> {
            int total = 0;
            for (List<Egg> list : playerEggs.values()) total += list.size();
            return String.valueOf(total);
        }));
    }

    private void loadConfigValues() {
        this.lang = new Lang(getConfig());
        this.chargedMultiplier = getConfig().getDouble("charged-multiplier", 1.25);
        eggTypes.clear();

        // Chicken
        eggTypes.add(new EggType(
            "Chicken Egg",
            getConfig().getDouble("eggs.Chicken.base-income", 15.0),
            getConfig().getDouble("eggs.Chicken.base-upgrade-cost", 25000.0),
            getConfig().getDouble("eggs.Chicken.upgrade-multiplier", 1.5),
            Material.CHICKEN_SPAWN_EGG));

        // Parrot
        eggTypes.add(new EggType(
            "Parrot Egg",
            getConfig().getDouble("eggs.Parrot.base-income", 35.0),
            getConfig().getDouble("eggs.Parrot.base-upgrade-cost", 50000.0),
            getConfig().getDouble("eggs.Parrot.upgrade-multiplier", 1.5),
            Material.PARROT_SPAWN_EGG));

        // Blaze
        eggTypes.add(new EggType(
            "Blaze Egg",
            getConfig().getDouble("eggs.Blaze.base-income", 45.0),
            getConfig().getDouble("eggs.Blaze.base-upgrade-cost", 100000.0),
            getConfig().getDouble("eggs.Blaze.upgrade-multiplier", 1.5),
            Material.BLAZE_SPAWN_EGG));

        // Squid
        eggTypes.add(new EggType(
            "Squid Egg",
            getConfig().getDouble("eggs.Squid.base-income", 80.0),
            getConfig().getDouble("eggs.Squid.base-upgrade-cost", 175000.0),
            getConfig().getDouble("eggs.Squid.upgrade-multiplier", 1.6),
            Material.SQUID_SPAWN_EGG));

        // Warden
        eggTypes.add(new EggType(
            "Warden Egg",
            getConfig().getDouble("eggs.Warden.base-income", 150.0),
            getConfig().getDouble("eggs.Warden.base-upgrade-cost", 250000.0),
            getConfig().getDouble("eggs.Warden.upgrade-multiplier", 1.8),
            Material.WARDEN_SPAWN_EGG));
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

        // /fancyeggs give <Spieler> <EggName> [charged]
        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("fancyeggs.admin")) {
                sender.sendMessage(lang.getColored("msg.no_permission"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(lang.getColored("msg.give_usage"));
                sender.sendMessage(lang.getColored("msg.available") + eggTypes.stream()
                        .map(t -> t.name.replace(" ", "_")).collect(Collectors.joining(", ")));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(lang.getColored("msg.player_not_found"));
                return true;
            }
            String eggName = args[2].replace("_", " ");
            EggType type = eggTypes.stream().filter(t -> t.name.equalsIgnoreCase(eggName)).findFirst().orElse(null);
            if (type == null) {
                sender.sendMessage(lang.getColored("msg.egg_not_found"));
                return true;
            }
            boolean isCharged = args.length > 3 && args[3].equalsIgnoreCase("charged");
            playerEggs.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>()).add(new Egg(type, 1, isCharged));

            String chargedSuffix = isCharged ? lang.getColored("msg.charged_suffix") : "";
            sender.sendMessage(lang.color(lang.get("msg.give_sender")
                    .replace("%player%", target.getName())
                    .replace("%egg%", type.name)
                    .replace("%charged%", chargedSuffix)));
            target.sendMessage(lang.color(lang.get("msg.give_target")
                    .replace("%egg%", type.name)
                    .replace("%charged%", chargedSuffix)));
            return true;
        }

        // /fancyeggs list
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(lang.getColored("list.header"));
            for (EggType type : eggTypes) {
                sender.sendMessage(lang.getColored("list.name") + type.name);
                sender.sendMessage(lang.getColored("list.base_income") + df.format(type.baseIncome) + "/s");
                sender.sendMessage(lang.getColored("list.upgrade_cost") + df.format(type.baseUpgradeCost));
                sender.sendMessage(lang.getColored("list.multiplier") + "x" + type.upgradeMultiplier);
            }
            sender.sendMessage(lang.getColored("list.footer"));
            return true;
        }

        // Standard -> Menü
        if (sender instanceof Player) {
            openEggsMenu((Player) sender);
        } else {
            sender.sendMessage(lang.getColored("msg.only_players_menu"));
        }
        return true;
    }

    private void openEggsMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, lang.getColored("menu.title"));

        ItemStack lagerItem = new ItemStack(Material.CHEST);
        ItemMeta lagerMeta = lagerItem.getItemMeta();
        lagerMeta.setDisplayName(lang.getColored("storage.name"));
        lagerMeta.setLore(Arrays.asList(
            lang.getColored("storage.desc"),
            "",
            lang.getColored("storage.info"),
            lang.getColored("storage.click"),
            "",
            lang.getColored("storage.count") + (playerEggs.getOrDefault(p.getUniqueId(), new ArrayList<>()).size())
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

        String charged = egg.isCharged ? lang.getColored("egg.charged_tag") : "";
        meta.setDisplayName(charged + ChatColor.YELLOW + egg.type.name);

        List<String> lore = new ArrayList<>();
        lore.add(lang.getColored("egg.desc"));
        lore.add("");
        lore.add(lang.getColored("egg.level") + egg.level + " ★");
        lore.add("");
        lore.add(lang.getColored("egg.income") + df.format(egg.getCurrentIncome()));
        lore.add(lang.getColored("egg.upgrade_price") + df.format(egg.getUpgradeCost()));
        lore.add("");
        lore.add(lang.getColored("egg.shift_upgrade"));
        lore.add(lang.getColored("egg.left_remove"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        if (e.getView().getTitle().equals(lang.getColored("menu.title"))) {
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
                        p.sendMessage(lang.getColored("msg.upgraded") + targetEgg.level);
                    } else {
                        p.sendMessage(lang.getColored("msg.not_enough") + df.format(cost));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                    p.closeInventory();
                    openEggsMenu(p);
                } else if (e.isLeftClick()) {
                    eggs.remove(targetEgg);
                    p.sendMessage(lang.color(lang.get("msg.removed").replace("%egg%", targetEgg.type.name)));
                    p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
                    p.closeInventory();
                    openEggsMenu(p);
                }
            }
        }

        if (e.getView().getTitle().equals(lang.getColored("menu.storage.title"))) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;

            if (e.getCurrentItem().getType() != Material.AIR
                    && e.getCurrentItem().getType() != Material.GRAY_STAINED_GLASS_PANE) {
                p.sendMessage(lang.getColored("msg.already_storage"));
            }
        }
    }

    private void openInventoryMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, lang.getColored("menu.storage.title"));
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
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        p.openInventory(inv);
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
