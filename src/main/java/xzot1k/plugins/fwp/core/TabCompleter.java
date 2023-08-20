/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xzot1k.plugins.fwp.FactionWP;
import xzot1k.plugins.fwp.api.enums.WPType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    private FactionWP pluginInstance;

    public TabCompleter(FactionWP pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("factionwp")) {
            List<String> names = new ArrayList<>();
            if (args.length == 3) {
                for (int i = -1; ++i < WPType.values().length; )
                    names.add(WPType.values()[i].name());
                Collections.sort(names);
                return names;
            }

            for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                names.add(player.getName());
            Collections.sort(names);
            return names;
        }

        return null;
    }

    private FactionWP getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(FactionWP pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}
