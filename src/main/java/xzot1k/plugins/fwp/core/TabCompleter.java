/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.fwp.FactionWP;
import xzot1k.plugins.fwp.api.enums.WPType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    private FactionWP pluginInstance;

    public TabCompleter(FactionWP pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, Command command, @NotNull String label, String[] args) {

        if (command.getName().equalsIgnoreCase("factionwp")) {
            List<String> names = new ArrayList<>();

            if (args.length == 1) {
                if ("give".startsWith(args[0].toLowerCase()) && commandSender.hasPermission("factionwp.give")) names.add("give");
                if ("reload".startsWith(args[0].toLowerCase()) && commandSender.hasPermission("factionwp.reload")) names.add("reload");
                if ("list".startsWith(args[0].toLowerCase()) && commandSender.hasPermission("factionwp.list")) names.add("list");
                if ("info".startsWith(args[0].toLowerCase()) && commandSender.hasPermission("factionwp.info")) names.add("info");
            } else if (args.length > 1 && args[0].equalsIgnoreCase("give") && commandSender.hasPermission("factionwp.give")) {
                if (args.length == 2) {
                    getPluginInstance().getServer().getOnlinePlayers().parallelStream().forEach(player -> {
                        if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) names.add(player.getName());
                    });
                } else if (args.length == 3) {
                    Arrays.stream(WPType.values()).parallel().forEach(wpType -> {
                        if ((wpType.name().toLowerCase().startsWith(args[2].toLowerCase()))) names.add(wpType.name());
                    });
                } else if (args.length == 4) names.add("1");
                else if (args.length == 5) {
                    final WPType wpType = WPType.getType(args[2]);
                    if (wpType != null && wpType.hasUses()) names.add("-1");
                } else if (args.length == 6) {
                    final WPType wpType = WPType.getType(args[2]);
                    if (wpType != null && (wpType.hasRadius() || wpType.hasModifier())) names.add("1");
                }
            }

            if (!names.isEmpty()) Collections.sort(names);
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
