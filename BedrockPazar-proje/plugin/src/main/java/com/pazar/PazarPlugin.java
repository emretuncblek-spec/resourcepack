package com.pazar;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PazarPlugin extends JavaPlugin {

    private PazarMenu menu;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("Vault economy not found! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        menu = new PazarMenu(this, new PazarData(getDataFolder()), rsp.getProvider());
        getServer().getScheduler().runTaskTimer(this, menu::tick, 1200L, 1200L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            menu.sellHand(p, args[1]);
            return true;
        }
        menu.ac(p);
        return true;
    }
}
