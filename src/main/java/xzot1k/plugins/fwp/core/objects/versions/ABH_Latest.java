/*
 * Copyright (c) 2020 XZot1K, All rights reserved.
 */

package xzot1k.plugins.fwp.core.objects.versions;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import xzot1k.plugins.fwp.FactionWP;
import xzot1k.plugins.fwp.core.packets.actionbar.BarHandler;

public class ABH_Latest implements BarHandler {
    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(FactionWP.getPluginInstance().getManager().colorText(message)));
    }

}
