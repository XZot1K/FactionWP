/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import xzot1k.plugins.fwp.FactionWP;

import java.util.Objects;

public class VaultHandler {

    private final FactionWP pluginInstance;
    private Economy economy;

    public VaultHandler(FactionWP pluginInstance) {this.pluginInstance = pluginInstance;}

    public boolean setupEconomy() {
        if (pluginInstance.getServer().getPluginManager().getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> rsp = pluginInstance.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;

        economy = rsp.getProvider();
        return true;
    }

    public Economy getEconomy() {
        return economy;
    }

}