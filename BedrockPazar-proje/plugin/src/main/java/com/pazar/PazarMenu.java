package com.pazar;

import com.pazar.PazarData.Listing;
import com.pazar.PazarData.Order;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Layout: 9x5 grid = 9 top cells (0-8) + 27 listing cells (9-35) + 9 bottom cells (36-44)
 * Cell kinds are detected by the pack from plain text: "~"=empty slot, " "=hidden, icon present=listing, Page/Market/Orders/!/Search=top bar, else bottom button
 */
public class PazarMenu {

    public static final String MARKER = "pazarmenu";
    private static final int PER_PAGE = 27;
    private static final int MAX_LISTING = 30;

    private enum Mode { MARKET, MINE, ORDERS }

    private static class View {
        Mode mode = Mode.MARKET;
        int page = 0, sort = 0, cat = 0;
        String query = "";
    }

    private record Cell(String text, String icon, Runnable click) {}

    private static Cell hidden() { return new Cell(" ", null, null); }
    private static Cell empty() { return new Cell("~", null, null); }
    private static Cell bar(String t, Runnable r) { return new Cell(t, null, r); }

    private final JavaPlugin plugin;
    private final PazarData data;
    private final Economy eco;
    private final Map<UUID, View> views = new HashMap<>();

    public PazarMenu(JavaPlugin plugin, PazarData data, Economy eco) {
        this.plugin = plugin;
        this.data = data;
        this.eco = eco;
    }

    // ---------- helpers ----------
    private void sync(Runnable r) { Bukkit.getScheduler().runTask(plugin, r); }

    private void send(Player p, Form f) { FloodgateApi.getInstance().sendForm(p.getUniqueId(), f); }

    private View view(Player p) { return views.computeIfAbsent(p.getUniqueId(), k -> new View()); }

    /** Removes expired (24h) listings and returns the items to their owners' claims. */
    public void tick() {
        for (Listing l : data.expireDue()) {
            Player s = Bukkit.getPlayer(l.seller);
            if (s != null) s.sendMessage("§eYour listing expired: §f" + describe(l.item) + " §7- collect it from §fClaims§7.");
        }
    }

    static String fmt(double v) {
        String[] s = {"", "K", "M", "B", "T"};
        int i = 0;
        while (v >= 1000 && i < 4) { v /= 1000; i++; }
        String n = (Math.abs(v - Math.rint(v)) < 0.05) ? String.valueOf((long) Math.rint(v)) : String.format(Locale.US, "%.1f", v);
        return "$" + n + s[i];
    }

    static String full(double v) { return "$" + String.format(Locale.US, "%,.0f", v); }

    static double parse(String s) {
        if (s == null) return -1;
        s = s.trim().toLowerCase(Locale.ROOT).replace("$", "").replace(",", ".");
        if (s.isEmpty()) return -1;
        double m = 1;
        char c = s.charAt(s.length() - 1);
        if (c == 'k') m = 1e3; else if (c == 'm') m = 1e6; else if (c == 'b') m = 1e9; else if (c == 't') m = 1e12;
        if (m != 1) s = s.substring(0, s.length() - 1);
        try {
            double d = Double.parseDouble(s) * m;
            return Double.isFinite(d) ? d : -1;
        } catch (Exception e) { return -1; }
    }

    static String pretty(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String x : m.name().toLowerCase(Locale.ROOT).split("_"))
            sb.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1)).append(' ');
        return sb.toString().trim();
    }

    static String describe(ItemStack i) {
        String s = pretty(i.getType()) + " x" + i.getAmount();
        if (i.hasItemMeta() && !i.getItemMeta().getEnchants().isEmpty()) s += " §d(enchanted)";
        return s;
    }

    private static final Map<Material, String> ICON = Map.of(
            Material.TOTEM_OF_UNDYING, "textures/items/totem",
            Material.GOLDEN_APPLE, "textures/items/apple_golden",
            Material.ENCHANTED_GOLDEN_APPLE, "textures/items/apple_golden",
            Material.APPLE, "textures/items/apple");

    static String icon(Material m) {
        String o = ICON.get(m);
        if (o != null) return o;
        return (m.isBlock() ? "textures/blocks/" : "textures/items/") + m.name().toLowerCase(Locale.ROOT);
    }

    /** 1 Minerals, 2 Blocks, 3 Armor, 4 Food, 5 Tools & Weapons, 6 Other */
    static int cat(Material m) {
        String n = m.name();
        if (n.endsWith("_ORE") || n.endsWith("_INGOT") || n.endsWith("_NUGGET") || n.startsWith("RAW_") || n.equals("DIAMOND")
                || n.equals("EMERALD") || n.equals("COAL") || n.equals("CHARCOAL") || n.equals("REDSTONE") || n.equals("LAPIS_LAZULI")
                || n.equals("QUARTZ") || n.equals("AMETHYST_SHARD") || n.equals("NETHERITE_SCRAP")) return 1;
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")
                || n.equals("SHIELD") || n.equals("ELYTRA")) return 3;
        if (m.isEdible()) return 4;
        if (n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL")
                || n.endsWith("_HOE") || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT") || n.equals("MACE")) return 5;
        if (m.isBlock()) return 2;
        return 6;
    }

    private void give(Player p, ItemStack it) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(it);
        if (!left.isEmpty()) {
            left.values().forEach(i -> data.addClaim(p.getUniqueId(), i));
            p.sendMessage("§eInventory full! The rest is waiting in §fClaims§e.");
        }
    }

    private void giveMaterial(UUID owner, Material m, int amount) {
        while (amount > 0) {
            int n = Math.min(amount, m.getMaxStackSize());
            data.addClaim(owner, new ItemStack(m, n));
            amount -= n;
        }
    }

    // ---------- main menu ----------
    public void ac(Player p) { open(p); }

    /** /ah sell <price> : lists the whole stack in your hand. */
    public void sellHand(Player p, String price) {
        doList(p, p.getInventory().getHeldItemSlot(), null, price);
    }

    private void open(Player p) {
        FloodgateApi api = FloodgateApi.getInstance();
        if (!api.isFloodgatePlayer(p.getUniqueId())) {
            p.sendMessage("This menu is for Bedrock players only.");
            return;
        }
        tick();
        View v = view(p);
        List<Cell> mid = new ArrayList<>();
        int pages;

        if (v.mode == Mode.ORDERS) {
            List<Order> l = data.orders.stream().sorted(Comparator.comparingLong((Order o) -> o.time).reversed()).collect(Collectors.toList());
            pages = Math.max(1, (l.size() + PER_PAGE - 1) / PER_PAGE);
            v.page = Math.max(0, Math.min(v.page, pages - 1));
            for (int i = v.page * PER_PAGE; i < Math.min(l.size(), (v.page + 1) * PER_PAGE); i++) {
                Order o = l.get(i);
                mid.add(new Cell(""+ fmt(o.each), icon(o.mat), () -> orderClick(p, o)));
            }
        } else {
            List<Listing> l = filtered(p, v);
            pages = Math.max(1, (l.size() + PER_PAGE - 1) / PER_PAGE);
            v.page = Math.max(0, Math.min(v.page, pages - 1));
            for (int i = v.page * PER_PAGE; i < Math.min(l.size(), (v.page + 1) * PER_PAGE); i++) {
                Listing it = l.get(i);
                mid.add(new Cell(""+ fmt(it.price), icon(it.item.getType()),
                        () -> { if (v.mode == Mode.MINE) cancelListing(p, it); else confirmBuy(p, it); }));
            }
        }
        while (mid.size() < PER_PAGE) mid.add(empty());

        boolean hasNext = v.page < pages - 1;
        Cell[] all = new Cell[45];
        Arrays.fill(all, hidden());
        all[0] = bar("Page (" + (v.page + 1) + "/" + pages + ")", null);
        if (v.mode == Mode.MARKET) all[2] = new Cell("Search", "textures/items/spyglass", () -> searchForm(p));
        all[3] = bar("§e§l!", () -> info(p));
        all[7] = bar("" + (v.mode == Mode.ORDERS ? "§7" : "§f") + "Market", () -> { v.mode = Mode.MARKET; v.page = 0; open(p); });
        all[8] = bar("" + (v.mode == Mode.ORDERS ? "§f" : "§7") + "Orders", () -> { v.mode = Mode.ORDERS; v.page = 0; open(p); });
        for (int k = 0; k < PER_PAGE; k++) all[9 + k] = mid.get(k);

        List<Cell> btn = new ArrayList<>();
        Cell claims = bar("Claims", () -> claimsForm(p));
        Cell list = bar("List\nItem", () -> listForm(p));
        if (v.mode == Mode.ORDERS) {
            btn.add(bar("Place\nOrder", () -> orderCreateForm(p)));
            btn.add(claims);
        } else if (v.mode == Mode.MINE) {
            btn.add(bar("All\nListings", () -> { v.mode = Mode.MARKET; v.page = 0; open(p); }));
            btn.add(claims);
            btn.add(list);
        } else {
            btn.add(bar("Your\nListings", () -> { v.mode = Mode.MINE; v.page = 0; open(p); }));
            btn.add(claims);
            btn.add(bar("Filters", () -> filterForm(p)));
            btn.add(list);
        }
        for (int j = 0; j < btn.size(); j++) all[38 + j] = btn.get(j);
        if (v.page > 0) all[36] = bar("<--", () -> { v.page--; open(p); });
        if (hasNext) all[44] = bar("-->", () -> { v.page++; open(p); });

        SimpleForm.Builder fb = SimpleForm.builder().title(MARKER + "AUCTION HOUSE").content("");
        for (Cell c : all) {
            if (c.icon() != null) fb.button(c.text(), FormImage.Type.PATH, c.icon());
            else fb.button(c.text());
        }
        fb.validResultHandler(r -> {
            Cell c = all[r.clickedButtonId()];
            sync(c.click() != null ? c.click() : () -> open(p));
        });
        send(p, fb.build());
    }

    private List<Listing> filtered(Player p, View v) {
        String q = v.query.toLowerCase(Locale.ROOT);
        List<Listing> l = data.listings.stream()
                .filter(x -> v.mode != Mode.MINE || x.seller.equals(p.getUniqueId()))
                .filter(x -> v.cat == 0 || cat(x.item.getType()) == v.cat)
                .filter(x -> q.isEmpty() || x.item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ').contains(q)
                        || x.item.getType().name().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());
        Comparator<Listing> c;
        if (v.sort == 1) c = Comparator.comparingDouble((Listing x) -> x.price);
        else if (v.sort == 2) c = Comparator.comparingDouble((Listing x) -> x.price).reversed();
        else c = Comparator.comparingLong((Listing x) -> x.time).reversed();
        l.sort(c);
        return l;
    }

    private void info(Player p) {
        p.sendMessage("§9§lAuction House §7- Tap a listing to buy it. §fList Item§7 puts an item up for 24 hours, "
                + "§fOrders§7 lets you place/fill buy orders, §fClaims§7 holds your returned items. §f/ah sell <price>§7 lists your hand.");
        open(p);
    }

    // ---------- buying ----------
    private void confirmBuy(Player p, Listing l) {
        if (!data.listings.contains(l)) { p.sendMessage("§cThis listing no longer exists."); open(p); return; }
        if (l.seller.equals(p.getUniqueId())) { p.sendMessage("§cYou can't buy your own listing. Remove it from 'Your Listings'."); open(p); return; }
        ModalForm.Builder b = ModalForm.builder().title("Buy Item")
                .content(describe(l.item) + "\n§7Seller: §f" + l.sellerName + "\n§7Price: §a" + full(l.price)
                        + "\n§7Balance: §f" + full(eco.getBalance(p)))
                .button1("Buy").button2("Back");
        b.validResultHandler(r -> sync(() -> { if (r.clickedFirst()) buy(p, l); else open(p); }));
        send(p, b.build());
    }

    private void buy(Player p, Listing l) {
        if (!data.listings.contains(l)) { p.sendMessage("§cThis listing no longer exists."); open(p); return; }
        if (!eco.has(p, l.price)) { p.sendMessage("§cYou don't have enough money."); open(p); return; }
        if (!eco.withdrawPlayer(p, l.price).transactionSuccess()) { p.sendMessage("§cPayment failed."); open(p); return; }
        eco.depositPlayer(Bukkit.getOfflinePlayer(l.seller), l.price);
        data.listings.remove(l);
        give(p, l.item.clone());
        data.save();
        p.sendMessage("§aPurchased: §f" + describe(l.item) + " §7(" + full(l.price) + ")");
        Player s = Bukkit.getPlayer(l.seller);
        if (s != null) s.sendMessage("§a" + p.getName() + " bought your listing: §f" + describe(l.item) + " §7+" + full(l.price));
        open(p);
    }

    private void cancelListing(Player p, Listing l) {
        long hrs = Math.max(0, (l.expires - System.currentTimeMillis()) / 3600_000L);
        ModalForm.Builder b = ModalForm.builder().title("Remove Listing")
                .content(describe(l.item) + "\n§7Price: §a" + full(l.price) + "\n§7Expires in: §f~" + hrs + "h\n\nRemove this listing and get the item back?")
                .button1("Remove").button2("Back");
        b.validResultHandler(r -> sync(() -> {
            if (r.clickedFirst() && data.listings.remove(l)) {
                give(p, l.item.clone());
                data.save();
                p.sendMessage("§eListing removed.");
            }
            open(p);
        }));
        send(p, b.build());
    }

    // ---------- listing an item ----------
    private void listForm(Player p) {
        ItemStack[] c = p.getInventory().getStorageContents();
        List<Integer> slots = new ArrayList<>();
        SimpleForm.Builder fb = SimpleForm.builder().title("List Item").content("Select an item from your inventory. Listings last 24 hours.");
        for (int i = 0; i < c.length; i++) {
            if (c[i] == null || c[i].getType() == Material.AIR) continue;
            slots.add(i);
            fb.button(describe(c[i]), FormImage.Type.PATH, icon(c[i].getType()));
        }
        if (slots.isEmpty()) { p.sendMessage("§cYour inventory is empty."); open(p); return; }
        fb.validResultHandler(r -> sync(() -> priceForm(p, slots.get(r.clickedButtonId()))));
        fb.closedOrInvalidResultHandler(() -> sync(() -> open(p)));
        send(p, fb.build());
    }

    private void priceForm(Player p, int slot) {
        ItemStack st = p.getInventory().getItem(slot);
        if (st == null || st.getType() == Material.AIR) { p.sendMessage("§cItem not found."); open(p); return; }
        CustomForm.Builder b = CustomForm.builder().title("List " + pretty(st.getType()))
                .input("Total price (e.g. 20k, 1.5m, 2b)", "20k", "")
                .input("Amount (max " + st.getAmount() + ", empty = all) - listed for 24 hours", String.valueOf(st.getAmount()), "");
        b.validResultHandler(r -> {
            String pr = r.asInput();
            String a = r.asInput();
            sync(() -> doList(p, slot, a, pr));
        });
        b.closedOrInvalidResultHandler(() -> sync(() -> open(p)));
        send(p, b.build());
    }

    private void doList(Player p, int slot, String a, String pr) {
        ItemStack st = p.getInventory().getItem(slot);
        if (st == null || st.getType() == Material.AIR) { p.sendMessage("§cItem not found."); open(p); return; }
        int amt = st.getAmount();
        if (a != null && !a.isBlank()) {
            try { amt = Integer.parseInt(a.trim()); } catch (Exception e) { p.sendMessage("§cInvalid amount."); open(p); return; }
        }
        if (amt < 1 || amt > st.getAmount()) { p.sendMessage("§cAmount must be between 1 and " + st.getAmount() + "."); open(p); return; }
        double price = parse(pr);
        if (price <= 0 || price > 1e13) { p.sendMessage("§cInvalid price."); open(p); return; }
        long mine = data.listings.stream().filter(x -> x.seller.equals(p.getUniqueId())).count();
        if (mine >= MAX_LISTING) { p.sendMessage("§cYou can have at most " + MAX_LISTING + " listings."); open(p); return; }
        Listing l = new Listing();
        l.seller = p.getUniqueId();
        l.sellerName = p.getName();
        l.item = st.clone();
        l.item.setAmount(amt);
        l.price = price;
        if (amt == st.getAmount()) p.getInventory().setItem(slot, null); else st.setAmount(st.getAmount() - amt);
        data.listings.add(l);
        data.save();
        p.sendMessage("§aListed for 24 hours: §f" + describe(l.item) + " §7- " + full(price));
        open(p);
    }

    // ---------- search / filters ----------
    private void searchForm(Player p) {
        View v = view(p);
        CustomForm.Builder b = CustomForm.builder().title("Search").input("Item name (empty = clear)", "diamond", v.query);
        b.validResultHandler(r -> {
            String q = r.asInput();
            sync(() -> { v.query = q == null ? "" : q.trim(); v.page = 0; open(p); });
        });
        b.closedOrInvalidResultHandler(() -> sync(() -> open(p)));
        send(p, b.build());
    }

    private void filterForm(Player p) {
        View v = view(p);
        String[] names = {"All Items", "Minerals", "Blocks", "Armor", "Food", "Tools & Weapons"};
        String[] icons = {"textures/items/ender_eye", "textures/items/diamond", "textures/blocks/stone",
                "textures/items/diamond_chestplate", "textures/items/apple", "textures/items/diamond_sword"};
        SimpleForm.Builder fb = SimpleForm.builder().title("Filters").content("Pick a category or a sort order.");
        for (int i = 0; i < names.length; i++) fb.button(names[i], FormImage.Type.PATH, icons[i]);
        fb.button("Sort: Newest");
        fb.button("Sort: Price low to high");
        fb.button("Sort: Price high to low");
        fb.validResultHandler(r -> {
            int id = r.clickedButtonId();
            sync(() -> {
                if (id < names.length) v.cat = id; else v.sort = id - names.length;
                v.page = 0;
                open(p);
            });
        });
        fb.closedOrInvalidResultHandler(() -> sync(() -> open(p)));
        send(p, fb.build());
    }

    // ---------- claims ----------
    private void claimsForm(Player p) {
        List<ItemStack> list = data.claims.get(p.getUniqueId());
        if (list == null || list.isEmpty()) { p.sendMessage("§7You have nothing to claim."); open(p); return; }
        int n = list.stream().mapToInt(ItemStack::getAmount).sum();
        ModalForm.Builder b = ModalForm.builder().title("Claims")
                .content(list.size() + " stacks (" + n + " items) are waiting.\nClaim them all?")
                .button1("Claim All").button2("Back");
        b.validResultHandler(r -> sync(() -> {
            if (r.clickedFirst()) {
                List<ItemStack> left = new ArrayList<>();
                for (ItemStack i : list) left.addAll(p.getInventory().addItem(i).values());
                if (left.isEmpty()) data.claims.remove(p.getUniqueId()); else data.claims.put(p.getUniqueId(), left);
                data.save();
                p.sendMessage(left.isEmpty() ? "§aItems claimed." : "§eInventory full, the rest stays in Claims.");
            }
            open(p);
        }));
        send(p, b.build());
    }

    // ---------- orders ----------
    private void orderCreateForm(Player p) {
        CustomForm.Builder b = CustomForm.builder().title("Place Order")
                .input("Item name (e.g. diamond)", "diamond", "")
                .input("Amount", "64", "")
                .input("Price each (e.g. 1k)", "1k", "");
        b.validResultHandler(r -> {
            String n = r.asInput(), a = r.asInput(), e = r.asInput();
            sync(() -> doOrder(p, n, a, e));
        });
        b.closedOrInvalidResultHandler(() -> sync(() -> open(p)));
        send(p, b.build());
    }

    private void doOrder(Player p, String name, String a, String e) {
        Material m = name == null ? null : Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        if (m == null || !m.isItem() || m == Material.AIR) { p.sendMessage("§cInvalid item name."); open(p); return; }
        int amt;
        try { amt = Integer.parseInt(a.trim()); } catch (Exception ex) { p.sendMessage("§cInvalid amount."); open(p); return; }
        double each = parse(e);
        if (amt < 1 || amt > 100000 || each <= 0) { p.sendMessage("§cInvalid amount or price."); open(p); return; }
        double total = each * amt;
        if (!eco.has(p, total)) { p.sendMessage("§cNot enough money (need " + full(total) + ")."); open(p); return; }
        eco.withdrawPlayer(p, total);
        Order o = new Order();
        o.owner = p.getUniqueId();
        o.ownerName = p.getName();
        o.mat = m;
        o.left = amt;
        o.each = each;
        data.orders.add(o);
        data.save();
        p.sendMessage("§aOrder placed: §f" + amt + "x " + pretty(m) + " §7(" + full(total) + " held)");
        open(p);
    }

    private void orderClick(Player p, Order o) {
        if (!data.orders.contains(o)) { p.sendMessage("§cThis order no longer exists."); open(p); return; }
        if (o.owner.equals(p.getUniqueId())) {
            ModalForm.Builder b = ModalForm.builder().title("Cancel Order")
                    .content(o.left + "x " + pretty(o.mat) + " - " + full(o.each) + " each\nRefund " + full(o.left * o.each) + "?")
                    .button1("Cancel Order").button2("Back");
            b.validResultHandler(r -> sync(() -> {
                if (r.clickedFirst() && data.orders.remove(o)) {
                    eco.depositPlayer(p, o.left * o.each);
                    data.save();
                    p.sendMessage("§eOrder cancelled and refunded.");
                }
                open(p);
            }));
            send(p, b.build());
            return;
        }
        int have = count(p, o.mat);
        int max = Math.min(have, o.left);
        if (max < 1) { p.sendMessage("§cYou don't have any " + pretty(o.mat) + "."); open(p); return; }
        CustomForm.Builder b = CustomForm.builder().title("Fill Order")
                .input(pretty(o.mat) + " - " + full(o.each) + " each (wanted: " + o.left + ", you have: " + have + ")\nHow many to deliver?",
                        String.valueOf(max), String.valueOf(max));
        b.validResultHandler(r -> {
            String a = r.asInput();
            sync(() -> fulfill(p, o, a));
        });
        b.closedOrInvalidResultHandler(() -> sync(() -> open(p)));
        send(p, b.build());
    }

    private int count(Player p, Material m) {
        int n = 0;
        for (ItemStack i : p.getInventory().getStorageContents()) if (i != null && i.getType() == m) n += i.getAmount();
        return n;
    }

    private void fulfill(Player p, Order o, String a) {
        if (!data.orders.contains(o)) { p.sendMessage("§cThis order no longer exists."); open(p); return; }
        int n;
        try { n = Integer.parseInt(a.trim()); } catch (Exception ex) { p.sendMessage("§cInvalid amount."); open(p); return; }
        n = Math.min(n, Math.min(o.left, count(p, o.mat)));
        if (n < 1) { p.sendMessage("§cNothing to deliver."); open(p); return; }
        int rest = n;
        ItemStack[] c = p.getInventory().getStorageContents();
        for (int i = 0; i < c.length && rest > 0; i++) {
            if (c[i] == null || c[i].getType() != o.mat) continue;
            int take = Math.min(rest, c[i].getAmount());
            rest -= take;
            if (take == c[i].getAmount()) c[i] = null; else c[i].setAmount(c[i].getAmount() - take);
        }
        p.getInventory().setStorageContents(c);
        double pay = n * o.each;
        eco.depositPlayer(p, pay);
        giveMaterial(o.owner, o.mat, n);
        o.left -= n;
        if (o.left <= 0) data.orders.remove(o);
        data.save();
        p.sendMessage("§aDelivered " + n + "x " + pretty(o.mat) + ". §7+" + full(pay));
        Player ow = Bukkit.getPlayer(o.owner);
        if (ow != null) ow.sendMessage("§a" + n + "x " + pretty(o.mat) + " was delivered to your order. §7Collect it from Claims.");
        open(p);
    }
}
