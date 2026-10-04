package com.pazar;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PazarData {

    public static class Listing {
        public UUID id = UUID.randomUUID();
        public UUID seller;
        public String sellerName;
        public ItemStack item;
        public double price;
        public long time = System.currentTimeMillis();
        public long expires = time + 24L * 3600_000L;
    }

    public static class Order {
        public UUID id = UUID.randomUUID();
        public UUID owner;
        public String ownerName;
        public Material mat;
        public int left;
        public double each;
        public long time = System.currentTimeMillis();
    }

    public final List<Listing> listings = new ArrayList<>();
    public final List<Order> orders = new ArrayList<>();
    public final Map<UUID, List<ItemStack>> claims = new HashMap<>();
    private final File file;

    public PazarData(File dir) {
        dir.mkdirs();
        file = new File(dir, "data.yml");
        load();
    }

    private void load() {
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection ls = c.getConfigurationSection("listings");
        if (ls != null) for (String k : ls.getKeys(false)) {
            ConfigurationSection s = ls.getConfigurationSection(k);
            if (s == null || s.getItemStack("item") == null) continue;
            Listing l = new Listing();
            l.id = UUID.fromString(k);
            l.seller = UUID.fromString(s.getString("seller"));
            l.sellerName = s.getString("sellerName", "?");
            l.item = s.getItemStack("item");
            l.price = s.getDouble("price");
            l.time = s.getLong("time");
            l.expires = s.getLong("expires", l.time + 24L * 3600_000L);
            listings.add(l);
        }
        ConfigurationSection os = c.getConfigurationSection("orders");
        if (os != null) for (String k : os.getKeys(false)) {
            ConfigurationSection s = os.getConfigurationSection(k);
            if (s == null) continue;
            Material m = Material.matchMaterial(s.getString("mat", ""));
            if (m == null) continue;
            Order o = new Order();
            o.id = UUID.fromString(k);
            o.owner = UUID.fromString(s.getString("owner"));
            o.ownerName = s.getString("ownerName", "?");
            o.mat = m;
            o.left = s.getInt("left");
            o.each = s.getDouble("each");
            o.time = s.getLong("time");
            orders.add(o);
        }
        ConfigurationSection cs = c.getConfigurationSection("claims");
        if (cs != null) for (String k : cs.getKeys(false)) {
            List<ItemStack> list = new ArrayList<>();
            for (Object o : cs.getList(k, new ArrayList<>())) if (o instanceof ItemStack i) list.add(i);
            if (!list.isEmpty()) claims.put(UUID.fromString(k), list);
        }
    }

    public void save() {
        YamlConfiguration c = new YamlConfiguration();
        for (Listing l : listings) {
            String p = "listings." + l.id + ".";
            c.set(p + "seller", l.seller.toString());
            c.set(p + "sellerName", l.sellerName);
            c.set(p + "item", l.item);
            c.set(p + "price", l.price);
            c.set(p + "time", l.time);
            c.set(p + "expires", l.expires);
        }
        for (Order o : orders) {
            String p = "orders." + o.id + ".";
            c.set(p + "owner", o.owner.toString());
            c.set(p + "ownerName", o.ownerName);
            c.set(p + "mat", o.mat.name());
            c.set(p + "left", o.left);
            c.set(p + "each", o.each);
            c.set(p + "time", o.time);
        }
        for (Map.Entry<UUID, List<ItemStack>> e : claims.entrySet())
            if (!e.getValue().isEmpty()) c.set("claims." + e.getKey(), e.getValue());
        try { c.save(file); } catch (IOException ex) { ex.printStackTrace(); }
    }

    /** Süresi dolan ilanları iddialara taşır, taşınanları döndürür. */
    public List<Listing> expireDue() {
        long now = System.currentTimeMillis();
        List<Listing> out = new ArrayList<>();
        for (Iterator<Listing> it = listings.iterator(); it.hasNext(); ) {
            Listing l = it.next();
            if (l.expires <= now) { it.remove(); addClaim(l.seller, l.item); out.add(l); }
        }
        if (!out.isEmpty()) save();
        return out;
    }

    public void addClaim(UUID u, ItemStack i) {
        claims.computeIfAbsent(u, k -> new ArrayList<>()).add(i);
    }
}
