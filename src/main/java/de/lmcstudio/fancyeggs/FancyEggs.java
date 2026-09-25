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

import java.util.*;
import java.util.stream.Collectors;

public class FancyEggs extends JavaPlugin implements Listener {

    private Economy econ;
    private Lang lang;
    private TopManager topManager;
    private EggStorage eggStorage;
    private double chargedMultiplier = 1.25;
    private int chargedChance = 20;
    private int defaultSlots = 1;
    private final Random random = new Random();

    private final Map<UUID, List<Egg>> playerEggs = new HashMap<>();
    private final Map<UUID, List<Egg>> playerStorage = new HashMap<>();
    private final Map<UUID, Boolean> autoCollect = new HashMap<>();
    private final Map<UUID, Double> pendingMoney = new HashMap<>();
    private final Map<UUID, Integer> storagePage = new HashMap<>();
    private final List<EggType> eggTypes = new ArrayList<>();

    private static final int[] BORDER_SLOTS = {
        0,1,2,3,4,5,6,7,8, 9,17, 18,26, 27,35, 36,44, 45,46,47,48,49,50,51,52,53
    };
    private static final int SLOT_AUTOCOLLECT = 49;
    private static final int SLOT_INFO = 4;
    private static final int SLOT_PENDING = 50;
    private static final int SLOT_STORAGE_PREV = 48;
    private static final int SLOT_STORAGE_NEXT = 50;
    private static final int SLOT_STORAGE_BACK = 53;

    private static final int[] ALL_EGG_SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    };

    private static final int[] STORAGE_CONTENT_SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34,
        37,38,39,40,41,42,43
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        loadConfigValues();
        this.topManager = new TopManager(this);
        this.eggStorage = new EggStorage(this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new FancyEggsExpansion(this).register();
                getLogger().info("PlaceholderAPI-Expansion registriert.");
            } catch (Throwable t) {
                getLogger().warning("PlaceholderAPI-Expansion fehlgeschlagen: " + t.getMessage());
            }
        }

        getServer().getScheduler().runTaskLater(this, () -> {
            if (!setupEconomy()) {
                getLogger().severe("Vault nicht gefunden! FancyEggs wird deaktiviert.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getServer().getPluginManager().registerEvents(this, this);

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        checkSlotLimit(p);

                        List<Egg> eggs = playerEggs.get(p.getUniqueId());
                        if (eggs == null || eggs.isEmpty()) {
                            topManager.updatePlayer(p.getUniqueId(), p.getName(), 0.0);
                            continue;
                        }
                        double income = 0;
                        for (Egg e : eggs) income += e.getCurrentIncome();
                        topManager.updatePlayer(p.getUniqueId(), p.getName(), income);
                        if (income <= 0) continue;
                        if (autoCollect.getOrDefault(p.getUniqueId(), true)) econ.depositPlayer(p, income);
                        else pendingMoney.merge(p.getUniqueId(), income, Double::sum);
                    }
                }
            }.runTaskTimer(this, 20L, 20L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    topManager.save();
                    eggStorage.save();
                }
            }.runTaskTimer(this, 6000L, 6000L);

            getLogger().info(lang.get("console.enabled"));
        }, 1L);

        try {
            Metrics m = new Metrics(this, 34188);
            m.addCustomChart(new SimplePie("language", () -> getConfig().getString("language", "de")));
            m.addCustomChart(new SimplePie("charged_chance", () -> String.valueOf(chargedChance)));
            getLogger().info("bStats aktiviert (ID: 34188).");
        } catch (Throwable t) {
            getLogger().warning("bStats: " + t.getMessage());
        }
    }

    public List<EggType> getEggTypes() { return eggTypes; }
    public Map<UUID, List<Egg>> getPlayerEggs() { return playerEggs; }
    public Map<UUID, List<Egg>> getPlayerStorage() { return playerStorage; }
    public TopManager getTopManager() { return topManager; }
    public int getChargedChance() { return chargedChance; }
    public double getChargedMultiplier() { return chargedMultiplier; }

    private void loadConfigValues() {
        this.lang = new Lang(getConfig());
        this.chargedMultiplier = getConfig().getDouble("charged-multiplier", 1.25);
        this.chargedChance = getConfig().getInt("charged-chance", 20);
        this.defaultSlots = getConfig().getInt("default-slots", 1);
        eggTypes.clear();

        if (!getConfig().isConfigurationSection("eggs")) {
            getLogger().warning("Keine Eggs in der config.yml gefunden!");
            return;
        }

        for (String key : getConfig().getConfigurationSection("eggs").getKeys(false)) {
            String p = "eggs." + key + ".";
            String dn = getConfig().getString(p + "display-name", key);
            String ic = getConfig().getString(p + "icon", "EGG");
            double bi = getConfig().getDouble(p + "base-income", 10.0);
            double bu = getConfig().getDouble(p + "base-upgrade-cost", 10000.0);
            double um = getConfig().getDouble(p + "upgrade-multiplier", 1.5);
            double sm = getConfig().getDouble(p + "sell-multiplier", 4.0);

            Material icon;
            try { icon = Material.valueOf(ic.toUpperCase()); }
            catch (Exception ex) {
                getLogger().warning("Ungültiges Icon '" + ic + "' für Egg '" + key + "'. Nutze EGG.");
                icon = Material.EGG;
            }
            eggTypes.add(new EggType(key, dn, bi, bu, um, sm, icon));
        }
        getLogger().info(eggTypes.size() + " Eggs geladen.");
    }

    @Override
    public void onDisable() {
        for (Map.Entry<UUID, Double> e : pendingMoney.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && econ != null) econ.depositPlayer(p, e.getValue());
        }
        if (topManager != null) topManager.save();
        if (eggStorage != null) eggStorage.save();
        playerEggs.clear();
        playerStorage.clear();
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

    // ==================================================
    //   PERMISSION-SLOTS
    // ==================================================
    public int getMaxSlots(Player p) {
        for (int i = 27; i >= 1; i--) {
            if (p.hasPermission("fancyeggs.slots." + i)) return i;
        }
        return defaultSlots;
    }

    private void checkSlotLimit(Player p) {
        List<Egg> active = playerEggs.get(p.getUniqueId());
        if (active == null || active.isEmpty()) return;

        int max = getMaxSlots(p);
        if (active.size() <= max) return;

        List<Egg> storage = playerStorage.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>());
        int toMove = active.size() - max;
        int moved = 0;
        for (int i = 0; i < toMove; i++) {
            Egg removed = active.remove(active.size() - 1);
            storage.add(removed);
            moved++;
        }
        if (moved > 0) {
            p.sendMessage(lang.color(lang.get("msg.slots_moved").replace("%amount%", String.valueOf(moved))));
            eggStorage.save();
        }
    }

    private int[] getActiveEggSlots(Player p) {
        int max = getMaxSlots(p);
        int count = Math.min(max, ALL_EGG_SLOTS.length);
        int[] result = new int[count];
        System.arraycopy(ALL_EGG_SLOTS, 0, result, 0, count);
        return result;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            if (!s.hasPermission("fancyeggs.admin")) { s.sendMessage(lang.getColored("msg.no_permission")); return true; }
            reloadConfig(); loadConfigValues();
            s.sendMessage(lang.color(Lang.ORANGE+Lang.BOLD+"Reloaded. "+eggTypes.size()+" Eggs aktiv."));
            return true;
        }

        if (a.length > 0 && a[0].equalsIgnoreCase("save")) {
            if (!s.hasPermission("fancyeggs.admin")) { s.sendMessage(lang.getColored("msg.no_permission")); return true; }
            eggStorage.save(); topManager.save();
            s.sendMessage(lang.color(Lang.ORANGE+Lang.BOLD+"Gespeichert."));
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
            EggType type = eggTypes.stream()
                    .filter(e -> e.key.equalsIgnoreCase(a[2]) || e.name.equalsIgnoreCase(a[2].replace("_", " ")))
                    .findFirst().orElse(null);
            if (type == null) { s.sendMessage(lang.getColored("msg.egg_not_found")); return true; }

            boolean ch;
            if (a.length > 3 && a[3].equalsIgnoreCase("forced")) ch = true;
            else if (a.length > 3 && a[3].equalsIgnoreCase("normal")) ch = false;
            else ch = random.nextInt(100) < chargedChance;

            playerEggs.computeIfAbsent(t.getUniqueId(), k -> new ArrayList<>()).add(new Egg(type, 1, ch));
            eggStorage.save();

            String sfx = ch ? lang.getColored("msg.charged_suffix") : "";
            s.sendMessage(lang.color(lang.get("msg.give_sender")
                    .replace("%player%", t.getName()).replace("%egg%", type.name).replace("%charged%", sfx)));
            t.sendMessage(lang.color(lang.get("msg.give_target")
                    .replace("%egg%", type.name).replace("%charged%", sfx)));
            return true;
        }

        if (a.length > 0 && a[0].equalsIgnoreCase("list")) {
            s.sendMessage(lang.getColored("list.header"));
            for (EggType t : eggTypes) {
                s.sendMessage(lang.getColored("list.name") + lang.color(Lang.ORANGE+Lang.BOLD+t.name+" ("+t.key+")"));
                s.sendMessage(lang.getColored("list.base_income") + lang.color(Lang.ORANGE+Lang.BOLD+NumberFormatter.format(t.baseIncome)) + lang.color(Lang.WHITE) + "/s");
                s.sendMessage(lang.getColored("list.upgrade_cost") + lang.color(Lang.ORANGE+Lang.BOLD+NumberFormatter.format(t.baseUpgradeCost)));
                s.sendMessage(lang.getColored("list.multiplier") + lang.color(Lang.ORANGE+Lang.BOLD+t.upgradeMultiplier));
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

        List<Egg> active = playerEggs.get(p.getUniqueId());
        int[] slots = getActiveEggSlots(p);
        if (active != null) {
            int i = 0;
            for (Egg e : active) {
                if (i >= slots.length) break;
                inv.setItem(slots[i++], createActiveEggItem(e));
            }
        }

        int storedCount = playerStorage.getOrDefault(p.getUniqueId(), new ArrayList<>()).size();
        ItemStack storageItem = new ItemStack(Material.CHEST);
        ItemMeta sm = storageItem.getItemMeta();
        sm.setDisplayName(lang.getColored("storage.name"));
        sm.setLore(Arrays.asList(
                lang.getColored("storage.desc"), "",
                lang.getColored("storage.info"),
                lang.getColored("storage.click"),
                "",
                lang.getColored("storage.active") + (active == null ? 0 : active.size()) + "/" + getMaxSlots(p),
                lang.getColored("storage.count") + storedCount
        ));
        storageItem.setItemMeta(sm);
        inv.setItem(SLOT_INFO, storageItem);

        boolean auto = autoCollect.getOrDefault(p.getUniqueId(), true);
        ItemStack toggle = new ItemStack(auto ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta tm = toggle.getItemMeta();
        tm.setDisplayName(lang.getColored(auto ? "autocollect.on_name" : "autocollect.off_name"));
        tm.setLore(Arrays.asList(
                lang.getColored("autocollect.desc"), "",
                lang.getColored("autocollect.info"),
                lang.getColored(auto ? "autocollect.on_text" : "autocollect.off_text"),
                "", lang.getColored("autocollect.click")));
        toggle.setItemMeta(tm);
        inv.setItem(SLOT_AUTOCOLLECT, toggle);

        if (!auto) {
            double pending = pendingMoney.getOrDefault(p.getUniqueId(), 0.0);
            ItemStack chest = new ItemStack(Material.CHEST);
            ItemMeta cm = chest.getItemMeta();
            cm.setDisplayName(lang.getColored("pending.name"));
            cm.setLore(Arrays.asList(
                    lang.getColored("pending.desc"), "",
                    lang.getColored("pending.info"),
                    lang.getColored("pending.amount") + lang.color(Lang.ORANGE+Lang.BOLD+"$"+NumberFormatter.format(pending)),
                    "", lang.getColored("pending.click")));
            chest.setItemMeta(cm);
            inv.setItem(SLOT_PENDING, chest);
        }

        p.openInventory(inv);
    }

    /** Aktives Egg im Menü: nur Upgrade + Einlagern, KEIN Verkauf */
    private ItemStack createActiveEggItem(Egg egg) {
        ItemStack item = new ItemStack(egg.type.icon);
        ItemMeta meta = item.getItemMeta();
        String charged = egg.isCharged ? lang.getColored("egg.charged_tag") : "";
        meta.setDisplayName(charged + lang.color(Lang.ORANGE+Lang.BOLD+egg.type.name));

        List<String> lore = new ArrayList<>();
        lore.add(lang.getColored("egg.desc"));
        lore.add("");
        lore.add(lang.getColored("egg.level") + lang.color(Lang.ORANGE+Lang.BOLD+egg.level+" ★"));
        lore.add("");
        lore.add(lang.getColored("egg.income") + lang.color(Lang.ORANGE+Lang.BOLD+NumberFormatter.format(egg.getCurrentIncome())));
        lore.add(lang.getColored("egg.upgrade_price") + lang.color(Lang.ORANGE+Lang.BOLD+NumberFormatter.format(egg.getUpgradeCost())));
        lore.add("");
        lore.add(lang.color(lang.get("egg.charged_chance").replace("%chance%", String.valueOf(chargedChance))));
        int bonusPercent = (int) Math.round((chargedMultiplier - 1.0) * 100);
        lore.add(lang.color(lang.get("egg.charged_bonus").replace("%bonus%", String.valueOf(bonusPercent))));
        lore.add("");
        lore.add(lang.getColored("egg.left_upgrade"));
        lore.add(lang.getColored("egg.right_remove"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Egg im Lager: Linksklick = Verkaufen, Rechtsklick = Ausrüsten */
    private ItemStack createStorageEggItem(Egg egg) {
        ItemStack item = new ItemStack(egg.type.icon);
        ItemMeta meta = item.getItemMeta();
        String charged = egg.isCharged ? lang.getColored("egg.charged_tag") : "";
        meta.setDisplayName(charged + lang.color(Lang.ORANGE+Lang.BOLD+egg.type.name));

        List<String> lore = new ArrayList<>();
        lore.add(lang.getColored("egg.desc"));
        lore.add("");
        lore.add(lang.getColored("egg.level") + lang.color(Lang.ORANGE+Lang.BOLD+egg.level+" ★"));
        lore.add("");
        lore.add(lang.getColored("egg.income") + lang.color(Lang.ORANGE+Lang.BOLD+NumberFormatter.format(egg.getCurrentIncome())));
        lore.add(lang.getColored("egg.sell_price") + lang.color(Lang.ORANGE+Lang.BOLD+NumberFormatter.format(egg.getSellPrice())));
        lore.add("");
        lore.add(lang.getColored("storage.egg.left_sell"));
        lore.add(lang.getColored("storage.egg.right_equip"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        // ========== HAUPT-MENÜ ==========
        if (title.equals(lang.getColored("menu.title"))) {
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
                p.closeInventory();
                storagePage.put(p.getUniqueId(), 0);
                Bukkit.getScheduler().runTaskLater(this, () -> openStorageMenu(p), 1L);
                return;
            }

            if (!autoCollect.getOrDefault(p.getUniqueId(), true)
                    && e.getSlot() == SLOT_PENDING
                    && e.getCurrentItem().getType() == Material.CHEST) {
                double pending = pendingMoney.getOrDefault(p.getUniqueId(), 0.0);
                if (pending > 0) {
                    econ.depositPlayer(p, pending);
                    pendingMoney.put(p.getUniqueId(), 0.0);
                    p.sendMessage(lang.color(lang.get("pending.collected").replace("%amount%", NumberFormatter.format(pending))));
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                } else {
                    p.sendMessage(lang.getColored("pending.empty"));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                p.closeInventory();
                Bukkit.getScheduler().runTaskLater(this, () -> openEggsMenu(p), 1L);
                return;
            }

            Material t = e.getCurrentItem().getType();
            if (t == Material.LIGHT_GRAY_STAINED_GLASS_PANE || t == Material.LIME_STAINED_GLASS_PANE
                    || t == Material.RED_STAINED_GLASS_PANE || t == Material.CHEST) return;

            List<Egg> eggs = playerEggs.get(p.getUniqueId());
            if (eggs == null || eggs.isEmpty()) return;

            String dn = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            Egg target = null;
            for (Egg egg : eggs) {
                if (dn.contains(egg.type.name)) { target = egg; break; }
            }
            if (target == null) return;

            if (e.isRightClick()) {
                // Ins Lager verschieben
                eggs.remove(target);
                playerStorage.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(target);
                p.sendMessage(lang.color(lang.get("storage.moved_to_storage").replace("%egg%", target.type.name)));
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 0.8f);
            } else if (e.isLeftClick()) {
                // Upgraden
                double cost = target.getUpgradeCost();
                if (econ.getBalance(p) >= cost) {
                    econ.withdrawPlayer(p, cost);
                    target.level++;
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    p.sendMessage(lang.getColored("msg.upgraded") + target.level);
                } else {
                    p.sendMessage(lang.getColored("msg.not_enough") + NumberFormatter.format(cost));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
            eggStorage.save();
            p.closeInventory();
            Bukkit.getScheduler().runTaskLater(this, () -> openEggsMenu(p), 1L);
            return;
        }

        // ========== LAGER-MENÜ ==========
        if (title.equals(lang.getColored("storage.menu.title"))) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType()
