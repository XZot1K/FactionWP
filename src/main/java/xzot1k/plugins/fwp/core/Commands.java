/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core;

import org.apache.commons.lang.WordUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.fwp.FactionWP;
import xzot1k.plugins.fwp.api.enums.WPType;

import java.util.Arrays;

public class Commands implements CommandExecutor {

    private final FactionWP pluginInstance;

    public Commands(FactionWP pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, Command command, @NotNull String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("factionwp")) {
            switch (args.length) {
                case 1: {
                    if (args[0].equalsIgnoreCase("reload")) {
                        runReloadCommand(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("list")) {
                        runListCommand(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("info")) {
                        runInfoCommand(commandSender);
                        return true;
                    }

                    sendUsageMessage(commandSender);
                    break;
                }
                case 4: {
                    if (args[0].equalsIgnoreCase("give")) {
                        runGiveCommand(commandSender, args[1], args[2], args[3]); // amount
                        return true;
                    }

                    sendUsageMessage(commandSender);
                    break;
                }
                case 5: {
                    if (args[0].equalsIgnoreCase("give")) {
                        runGiveCommand(commandSender, args[1], args[2], args[3], args[4]); // amount + uses
                        return true;
                    }

                    sendUsageMessage(commandSender);
                    break;
                }
                case 6: {
                    if (args[0].equalsIgnoreCase("give")) {
                        runGiveCommand(commandSender, args[1], args[2], args[3], args[4], args[5]); // amount + uses + <radius or modifier>
                        return true;
                    }

                    sendUsageMessage(commandSender);
                    break;
                }
                case 7: {
                    if (args[0].equalsIgnoreCase("give")) {
                        runGiveCommand(commandSender, args[1], args[2], args[3], args[4], args[5], args[6]); // amount + uses + radius + modifier
                        return true;
                    }

                    sendUsageMessage(commandSender);
                    break;
                }
                default: {
                    sendUsageMessage(commandSender);
                    break;
                }
            }

            return true;
        }

        return false;
    }

    private void runInfoCommand(CommandSender commandSender) {
        if (!commandSender.hasPermission("factionwp.info")) {
            pluginInstance.getManager().sendCustomMessage(commandSender, "no-permission-message");
            return;
        }

        commandSender.sendMessage(pluginInstance.getManager().colorText("&0&m-----------------------------"));
        commandSender.sendMessage("");
        commandSender.sendMessage(pluginInstance.getManager().colorText(" &7&lPlugin Name:&r &bFactionWP"));
        commandSender.sendMessage(pluginInstance.getManager().colorText(" &7&lAuthor(s):&r &dXZot1K"));
        commandSender.sendMessage(pluginInstance.getManager().colorText(" &7&lPlugin Version:&r &a" + pluginInstance.getDescription().getVersion()));
        commandSender.sendMessage("");
        commandSender.sendMessage(pluginInstance.getManager().colorText("&0&m-----------------------------"));
    }

    private void runListCommand(CommandSender commandSender) {
        if (!commandSender.hasPermission("factionwp.list")) {
            pluginInstance.getManager().sendCustomMessage(commandSender, "no-permission-message");
            return;
        }

        pluginInstance.getManager().sendCustomMessage(commandSender, "list-message", "{types}:" + Arrays.toString(WPType.values()));
    }

    private void runGiveCommand(CommandSender commandSender, String... parameters) {
        if (!commandSender.hasPermission("factionwp.give")) {
            pluginInstance.getManager().sendCustomMessage(commandSender, "no-permission-message");
            return;
        }

        Player player = pluginInstance.getServer().getPlayer(parameters[0]);
        if (player != null) {
            final String entry = parameters[1].replace(" ", "_").replace("-", "_");
            final WPType wpType = WPType.getType(entry);
            if (wpType == null) {
                pluginInstance.getManager().sendCustomMessage(commandSender, "invalid-type-message", "{types}:" + Arrays.toString(WPType.values()));
                return;
            }

            int amount = 1;
            if (parameters.length >= 3) {
                if (pluginInstance.getManager().isNotNumeric(parameters[2])) {
                    pluginInstance.getManager().sendCustomMessage(commandSender, "invalid-amount-message", "{types}:" + Arrays.toString(WPType.values()));
                    return;
                } else amount = Integer.parseInt(parameters[2]);
            }

            int uses = -1;
            if (parameters.length >= 4) {
                if (parameters[3] != null && !parameters[3].isEmpty()) {
                    if (!wpType.hasUses()) {
                        pluginInstance.getManager().sendCustomMessage(commandSender, "type-no-uses-message");
                        return;
                    }

                    if (pluginInstance.getManager().isNotNumeric(parameters[3])) {
                        pluginInstance.getManager().sendCustomMessage(commandSender, "invalid-uses-message");
                        return;
                    } else {
                        uses = Integer.parseInt(parameters[3]);
                        if (uses < -1) uses = -1;
                    }
                } else if (wpType.hasUses()) uses = pluginInstance.getConfig().getInt("global-item-section.predefined-uses");
            }

            int radius = -1;
            double modifier = 1;

            if (parameters.length >= 5) {
                if (parameters[4] != null && !parameters[4].isEmpty()) {
                    if (!wpType.hasRadius() && !wpType.hasModifier()) {
                        pluginInstance.getManager().sendCustomMessage(commandSender, "type-no-radius-message");
                        return;
                    }

                    if (!wpType.hasModifier()) {
                        if (pluginInstance.getManager().isNotNumeric(parameters[4])) {
                            pluginInstance.getManager().sendCustomMessage(commandSender, "invalid-radius-message");
                            return;
                        } else {
                            radius = Integer.parseInt(parameters[4]);
                            if (radius <= 0) {
                                pluginInstance.getManager().sendCustomMessage(commandSender, "invalid-radius-message");
                                return;
                            }
                        }
                    } else {
                        if (pluginInstance.getManager().isNotNumeric(parameters[4])) {
                            pluginInstance.getManager().sendCustomMessage(commandSender, "invalid-modifier-message");
                            return;
                        } else {
                            modifier = Double.parseDouble(parameters[4]);
                            if (modifier < -1) modifier = 1;
                        }
                    }
                } else if (wpType.hasRadius()) {
                    pluginInstance.getManager().sendCustomMessage(commandSender, "type-requires-radius-message");
                    return;
                }
            }

            if (parameters.length >= 6) {
                if (parameters[5] != null && parameters[5].isEmpty()) {
                    if (wpType.hasModifier()) {
                        if (pluginInstance.getManager().isNotNumeric(parameters[5])) {
                            pluginInstance.getManager().sendCustomMessage(commandSender, "invalid-modifier-message");
                            return;
                        } else {
                            modifier = Double.parseDouble(parameters[5]);
                            if (modifier < -1) modifier = 1;
                        }
                    } else {
                        pluginInstance.getManager().sendCustomMessage(commandSender, "type-no-modifier-message");
                        return;
                    }
                }
            }

            ItemStack itemStack = pluginInstance.getManager().buildItem(player, wpType, amount, uses, radius, modifier);
            if (player.getInventory().firstEmpty() == -1) player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            else player.getInventory().addItem(itemStack);

            String formattedTypeName = WordUtils.capitalize(wpType.name().toLowerCase().replace("_", " "));
            pluginInstance.getManager().sendCustomMessage(commandSender, "given-message", "{player}:" + player.getName(), "{amount}:" + amount, "{type}:" + formattedTypeName);
            pluginInstance.getManager().sendCustomMessage(player, "received-message", "{amount}:" + amount, "{type}:" + formattedTypeName);
        } else pluginInstance.getManager().sendCustomMessage(commandSender, "player-invalid-message");
    }

    private void runReloadCommand(CommandSender commandSender) {
        final boolean hasPerm = commandSender.hasPermission("factionwp.reload");
        if (hasPerm) pluginInstance.reloadConfigs();
        pluginInstance.getManager().sendCustomMessage(commandSender, (hasPerm ? "reload" : "usage") + "-message");
    }

    private void sendUsageMessage(CommandSender commandSender) {
        final boolean hasPerm = commandSender.hasPermission("factionwp.reload");
        pluginInstance.getManager().sendCustomMessage(commandSender, (!hasPerm ? "no-permission" : "usage") + "-message");
    }

}