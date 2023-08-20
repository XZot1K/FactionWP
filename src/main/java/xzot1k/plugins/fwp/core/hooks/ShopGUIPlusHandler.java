/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core.hooks;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.exception.player.PlayerDataNotLoadedException;
import net.brcdev.shopgui.modifier.PriceModifierActionType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xzot1k.plugins.fwp.FactionWP;

import java.util.logging.Level;

public class ShopGUIPlusHandler {

    private final FactionWP pluginInstance;

    public ShopGUIPlusHandler(FactionWP pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public double getItemStackPriceSell(Player player, ItemStack itemStack) {
        try {
            final double price = ShopGuiPlusApi.getItemStackPriceSell(player, itemStack);
            return (price * ShopGuiPlusApi.getPriceModifier(player, PriceModifierActionType.SELL).getModifier());
        } catch (PlayerDataNotLoadedException e) {
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING, "There was an issue obtaining sell values from the ShopGUIPlus plugin.");
        }

        return 0;
    }

    private FactionWP getPluginInstance() {
        return pluginInstance;
    }
}
