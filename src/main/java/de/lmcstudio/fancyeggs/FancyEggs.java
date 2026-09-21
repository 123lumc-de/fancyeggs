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

    private Economy econ;
    private Lang lang;
    private double chargedMultiplier = 1.25;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    private final Map<UUID, List<Egg>> playerEggs = new HashMap<>();
    private final Map<UUID, Boolean> autoCollect = new HashMap<>();
    private final Map<UUID, Double> pendingMoney = new HashMap<>();
    private final List<EggType> eggTypes = new ArrayList<>();

    private static final int[] BORDER_SLOTS = {
        0,1,2,3,4,5,6,7,8, 9,17, 18,26, 27,35, 36,44, 45,46,47,48,49,50,51,52,53
    };
    private static final int SLOT_AUTOCOLLECT = 49;
    private static final int SLOT_INFO = 4;
    private static final int[] EGG_SLOTS = {
        10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        getServer().getScheduler().runTaskLater(this, () -> {
            if (!setupEconomy()) {
                getLogger().severe(lang != null ? lang.get("console.vault_missing") : "Vault missing!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getServer().getPluginManager().registerEvents(this, this);
            loadConfigValues();

            new BukkitRunnable() {
                @Override public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        List<Egg> eggs = playerEggs.get(p.getUniqueId());
                        if (eggs == null || eggs.isEmpty()) continue;
                        double income = 0;
                        for (Egg e : eggs) income += e.getCurrentIncome();
                        if (income <= 0) continue;
                        if (autoCollect.getOrDefault(p.getUniqueId(), true)) econ.depositPlayer(p, income);
                        else pendingMoney.merge(p.getUniqueId(), income, Double::sum);
                    }
                }
            }.runTaskTimer(this, 20L, 20L);

            getLogger().info(lang.get("console.enabled"));
        }, 1L);

        try {
            Metrics m = new Metrics(this, 34188);
            m.addCustomChart(new SimplePie("language", () -> getConfig().getString("language", "de")));
        } catch (Throwable t) {
            getLogger().warning("bStats: " + t.getMessage());
        }
    }

    private void loadConfigValues() {
        this.lang = new Lang(getConfig());
        this.chargedMultiplier = getConfig().getDouble("charged-multiplier", 1.25);
        eggTypes.clear();

        if (!getConfig().isConfigurationSection("eggs")) return;

        for (String key : getConfig().getConfigurationSection("eggs").getKeys(false)) {
            String p = "eggs." + key + ".";
            String dn = getConfig().getString(p + "display-name", key);
            String ic = getConfig().getString(p + "icon", "EGG");
            double bi = getConfig().getDouble(p + "base-income", 10.0);
            double bu = getConfig().getDouble(p + "base-upgrade-cost", 10000.0);
            double um = getConfig().getDouble(p + "upgrade-multiplier", 1.5);
            double sm = getConfig().getDouble(p + "sell-multiplier", 4.0);
            Material icon;
            try { icon = Material.valueOf(ic.toUpperCase()); } catch (Exception ex) { icon = Material.EGG; }
            eggTypes.add(new EggType(key, dn, bi, bu, um, sm, icon));
        }
    }

    @Override
    public void onDisable() {
        for (Map.Entry<UUID, Double> e : pendingMoney.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && econ != null) econ.depositPlayer(p, e.getValue());
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            if (!s.hasPermission("fancyeggs.admin")) { s.sendMessage(lang.getColored("msg.no_permission")); return true; }
            reloadConfig(); loadConfigValues();
            s.sendMessage(lang.color(Lang.GOLD+Lang.BOLD+"Reloaded. "+eggTypes.size()+" Eggs aktiv."));
            return true;
        }

        if (a.length > 0 && a[0].equalsIgnoreCase("give")) {
            if (!s.hasPermission("fancyeggs.admin")) { s.sendMessage(lang.getColored("msg.no_permission")); return true; }
            if (a.length < 3) {
                s.sendMessage(lang.getColored("msg.give_usage"));
                s.sendMessage(lang.getColored("msg.available") + eggTypes.stream().map(t -> t.key).collect(Collectors.joining(", ")));
                return true;
            }
            Player t = Bukkit.getPlayer(a[1]);
            if (t == null) { s.sendMessage(lang.getColored("msg.player_not_found")); return true; }
            EggType type = eggTypes.stream().filter(e -> e.key.equalsIgnoreCase(a[2])).findFirst().orElse(null);
            if (type == null) { s.sendMessage(lang.getColored("msg.egg_not_found")); return true; }
            boolean ch = a.length > 3 && a[3].equalsIgnoreCase("charged");
            playerEggs.computeIfAbsent(t.getUniqueId(), k -> new ArrayList<>()).add(new Egg(type, 1, ch));
            String sfx = ch ? lang.getColored("msg.charged_suffix") : "";
            s.sendMessage(lang.color(lang.get("msg.give_sender").replace("%player%", t.getName()).replace("%egg%", type.name).replace("%charged%", sfx)));
            t.sendMessage(lang.color(lang.get("msg.give_target").replace("%egg%", type.name).replace("%charged%", sfx)));
            return true;
        }

        if (a.length > 0 && a[0].equalsIgnoreCase("list")) {
            s.sendMessage(lang.getColored("list.header"));
            for (EggType t : eggTypes) {
                s.sendMessage(lang.getColored("list.name") + lang.color(Lang.GOLD+Lang.BOLD+t.name+" ("+t.key+")"));
                s.sendMessage(lang.getColored("list.base_income") + lang.color(Lang.GOLD+Lang.BOLD+df.format(t.baseIncome)));
                s.sendMessage(lang.getColored("list.upgrade_cost") + lang.color(Lang.GOLD+Lang.BOLD+df.format(t.baseUpgradeCost)));
                s.sendMessage(lang.getColored("list.multiplier") + lang.color(Lang.GOLD+Lang.BOLD+t.upgradeMultiplier));
            }
            s.sendMessage(lang.getColored("list.footer"));
            return true;
        }

        if (s instanceof Player) openEggsMenu((Player) s);
        else s.sendMessage(lang.getColored("msg.only_players_menu"));
        return true;
    }

    private void openEggsMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, lang.getColored("menu.title"));
        ItemStack border = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta(); bm.setDisplayName(" "); border.setItemMeta(bm);
        for (int slot : BORDER_SLOTS) inv.setItem(slot, border);

        List<Egg> eggs = playerEggs.get(p.getUniqueId());
        if (eggs != null) {
            int i = 0;
            for (Egg e : eggs) { if (i >= EGG_SLOTS.length) break; inv.setItem(EGG_SLOTS[i++], createEggItem(e)); }
        }

        boolean auto = autoCollect.getOrDefault(p.getUniqueId(), true);
        ItemStack toggle = new ItemStack(auto ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta tm = toggle.getItemMeta();
        tm.setDisplayName(lang.getColored(auto ? "autocollect.on_name" : "autocollect.off_name"));
        tm.setLore(Arrays.asList(lang.getColored("autocollect.desc"), "", lang.getColored("autocollect.info"),
                lang.getColored(auto ? "autocollect.on_text" : "autocollect.off_text"), "", lang.getColored("autocollect.click")));
        toggle.setItemMeta(tm);
        inv.setItem(SLOT_AUTOCOLLECT, toggle);

        if (!auto) {
            double pending = pendingMoney.getOrDefault(p.getUniqueId(), 0.0);
            ItemStack ch = new ItemStack(Material.CHEST);
            ItemMeta cm = ch.getItemMeta();
            cm.setDisplayName(lang.getColored("pending.name"));
            cm.setLore(Arrays.asList(lang.getColored("pending.desc"), "", lang.getColored("pending.info"),
                    lang.getColored("pending.amount")+lang.color(Lang.GOLD+Lang.BOLD+"$"+df.format(pending)), "",
                    lang.getColored("pending.click")));
            ch.setItemMeta(cm);
            inv.setItem(SLOT_INFO, ch);
        }
        p.openInventory(inv);
    }

    private ItemStack createEggItem(Egg egg) {
        ItemStack item = new ItemStack(egg.type.icon);
        ItemMeta meta = item.getItemMeta();
        String charged = egg.isCharged ? lang.getColored("egg.charged_tag") : "";
        meta.setDisplayName(charged + lang.color(Lang.GOLD+Lang.BOLD+egg.type.name));

        List<String> lore = new ArrayList<>();
        lore.add(lang.getColored("egg.desc"));
        lore.add("");
        lore.add(lang.getColored("egg.level") + lang.color(Lang.GOLD+Lang.BOLD+egg.level+" ★"));
        lore.add("");
        lore.add(lang.getColored("egg.income") + lang.color(Lang.GOLD+Lang.BOLD+df.format(egg.getCurrentIncome())));
        lore.add(lang.getColored("egg.upgrade_price") + lang.color(Lang.GOLD+Lang.BOLD+df.format(egg.getUpgradeCost())));
        lore.add(lang.getColored("egg.sell_price") + lang.color(Lang.GOLD+Lang.BOLD+df.format(egg.getSellPrice())));
        lore.add("");
        lore.add(lang.getColored("egg.left_upgrade"));
        lore.add(lang.getColored("egg.shift_sell"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!e.getView().getTitle().equals(lang.getColored("menu.title"))) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        if (e.getSlot() == SLOT_AUTOCOLLECT) {
            boolean ns = !autoCollect.getOrDefault(p.getUniqueId(), true);
            autoCollect.put(p.getUniqueId(), ns);
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, ns ? 1.2f : 0.8f);
            p.closeInventory();
            Bukkit.getScheduler().runTaskLater(this, () -> openEggsMenu(p), 1L);
            return;
        }

        if (e.getSlot() == SLOT_INFO && e.getCurrentItem().getType() == Material.CHEST) {
            double pending = pendingMoney.getOrDefault(p.getUniqueId(), 0.0);
            if (pending > 0) {
                econ.depositPlayer(p, pending);
                pendingMoney.put(p.getUniqueId(), 0.0);
                p.sendMessage(lang.color(lang.get("pending.collected").replace("%amount%", df.format(pending))));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            } else {
                p.sendMessage(lang.getColored("pending.empty"));
            }
            p.closeInventory();
            Bukkit.getScheduler().runTaskLater(this, () -> openEggsMenu(p), 1L);
            return;
        }

        Material t = e.getCurrentItem().getType();
        if (t == Material.LIGHT_GRAY_STAINED_GLASS_PANE || t == Material.LIME_STAINED_GLASS_PANE
                || t == Material.RED_STAINED_GLASS_PANE || t == Material.CHEST) return;

        List<Egg> eggs = playerEggs.get(p.getUniqueId());
        if (eggs == null) return;
        String dn = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
        Egg target = null;
        for (Egg egg : eggs) if (dn.contains(egg.type.name)) { target = egg; break; }
        if (target == null) return;

        if (e.isShiftClick()) {
            double sp = target.getSellPrice();
            econ.depositPlayer(p, sp);
            eggs.remove(target);
            p.sendMessage(lang.color(lang.get("msg.sold").replace("%egg%", target.type.name).replace("%price%", df.format(sp))));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
        } else if (e.isLeftClick()) {
            double cost = target.getUpgradeCost();
            if (econ.getBalance(p) >= cost) {
                econ.withdrawPlayer(p, cost);
                target.level++;
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                p.sendMessage(lang.getColored("msg.upgraded") + target.level);
            } else {
                p.sendMessage(lang.getColored("msg.not_enough") + df.format(cost));
            }
        }
        p.closeInventory();
        Bukkit.getScheduler().runTaskLater(this, () -> openEggsMenu(p), 1L);
    }

    public class Egg {
        public EggType type; public int level; public boolean isCharged;
        public Egg(EggType t, int l, boolean c) { type=t; level=l; isCharged=c; }
        public double getCurrentIncome() {
            double b = type.baseIncome * Math.pow(type.upgradeMultiplier, level-1);
            return isCharged ? b * chargedMultiplier : b;
        }
        public double getUpgradeCost() { return type.baseUpgradeCost * Math.pow(type.upgradeMultiplier, level-1); }
        public double getSellPrice() { return getUpgradeCost() * type.sellMultiplier; }
    }

    public static class EggType {
        public String key, name; public double baseIncome, baseUpgradeCost, upgradeMultiplier, sellMultiplier;
        public Material icon;
        public EggType(String k, String n, double bi, double bu, double um, double sm, Material ic) {
            key=k; name=n; baseIncome=bi; baseUpgradeCost=bu; upgradeMultiplier=um; sellMultiplier=sm; icon=ic;
        }
    }
}
