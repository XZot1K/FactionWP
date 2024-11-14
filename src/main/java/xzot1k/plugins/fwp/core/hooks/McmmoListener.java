/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core.hooks;

import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import xzot1k.plugins.fwp.FactionWP;

public class McmmoListener implements Listener {
	private FactionWP pluginInstance;

	public McmmoListener(FactionWP pluginInstance) {
		this.pluginInstance = pluginInstance;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onActivate(McMMOPlayerAbilityActivateEvent e) {
		ItemStack hand = pluginInstance.getManager().getHandItem(e.getPlayer());
		if (hand != null && pluginInstance.getManager().isWPItem(hand)) e.setCancelled(true);
	}

}
