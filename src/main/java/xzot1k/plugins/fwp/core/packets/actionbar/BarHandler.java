/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core.packets.actionbar;

import org.bukkit.entity.Player;

public interface BarHandler {
    void sendActionBar(Player player, String message);
}
