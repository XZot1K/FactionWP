/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.api;

import com.massivecraft.factions.listeners.FactionsBlockListener;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.fwp.FactionWP;
import xzot1k.plugins.fwp.api.enums.SpawnerPickaxeMode;
import xzot1k.plugins.fwp.api.enums.WPType;
import xzot1k.plugins.fwp.core.objects.versions.ABH_Latest;
import xzot1k.plugins.fwp.core.objects.versions.ABH_Old;
import xzot1k.plugins.fwp.core.packets.actionbar.BarHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;

public class Manager {

    private final FactionWP pluginInstance;
    private Random random;
    private HashMap<UUID, HashMap<WPType, Boolean>> sellModeMap;
    private final HashMap<UUID, SpawnerPickaxeMode> spawnerPickaxeModeMap;
    private HashMap<UUID, Long> globalCooldowns;
    private HashMap<UUID, HashMap<WPType, Long>> wpCooldowns;
    private BarHandler barHandler;

    public Manager(FactionWP pluginInstance) {
        this.pluginInstance = pluginInstance;
        setGlobalCooldowns(new HashMap<>());
        setWPCooldowns(new HashMap<>());
        setSellModeMap(new HashMap<>());
        this.spawnerPickaxeModeMap = new HashMap<>();
        setRandom(new Random());

        boolean succeeded;
        long startTime = System.currentTimeMillis();
        switch (pluginInstance.getServerVersion()) {
            case "v1_9_R1":
            case "v1_8_R3":
            case "v1_8_R2":
            case "v1_8_R1":
                setBarHandler(new ABH_Old());
                break;
            default:
                setBarHandler(new ABH_Latest());
                break;
        }

        succeeded = (getBarHandler() != null);
        if (succeeded)
            pluginInstance.log(Level.INFO, pluginInstance.getServerVersion() + " packets were successfully setup! (Took " + (System.currentTimeMillis() - startTime) + "ms)");
        else
            pluginInstance.log(Level.WARNING, "Your version is not supported by FactionWP's packets. Expect errors when attempting to use anything packet "
                    + "related. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    /**
     * See if a string is NOT a numerical value.
     *
     * @param string The string to check.
     * @return Whether it is numerical or not.
     */
    public boolean isNotNumeric(String string) {
        if (string == null || string.isEmpty()) return true;

        final char[] chars = string.toCharArray();
        if (chars.length == 1 && !Character.isDigit(chars[0])) return true;

        for (int i = -1; ++i < string.length(); ) {
            final char c = chars[i];
            if (!Character.isDigit(c) && c != '.' && !((i == 0 && c == '-'))) return true;
        }

        return false;
    }

    /**
     * Translates all color codes in the message imported then returns.
     *
     * @param message The message to translate all color codes in.
     * @return The translated text outcome.
     */
    public String colorText(String message) {
        String messageCopy = message;
        if (pluginInstance.getServerVersion().startsWith("v1_16") && messageCopy.contains("#")) {
            final List<String> hexToReplace = new ArrayList<>();
            final char[] charArray = messageCopy.toCharArray();

            StringBuilder hexBuilder = new StringBuilder();
            for (int i = -1; ++i < charArray.length; ) {
                final char currentChar = charArray[i];
                if (currentChar == '#') {
                    final int remainingCharLength = (charArray.length - i);
                    if (remainingCharLength < 6) break;
                    else {
                        hexBuilder.append("#");
                        for (int increment = 0; ++increment < 7; )
                            hexBuilder.append(charArray[i + increment]);

                        try {
                            Integer.parseInt(hexBuilder.substring(1));
                            hexToReplace.add(hexBuilder.toString());
                        } catch (NumberFormatException ignored) {
                        }
                        hexBuilder.setLength(0);
                    }
                }
            }

            if (!hexToReplace.isEmpty()) {
                for (String hex : hexToReplace) {
                    messageCopy = messageCopy.replace(hex, net.md_5.bungee.api.ChatColor.of(hex).toString());
                }
            }
        }

        return ChatColor.translateAlternateColorCodes('&', messageCopy);
    }

    /**
     * Sends a message to the passed player from the lang.yml and optional placeholders.
     *
     * @param sender       The player or console sender to send the message to.
     * @param path         The path within the lang.yml.
     * @param placeholders Each placeholder individually. (Format: <placeholder>:<value>)
     */
    public void sendCustomMessage(Object sender, String path, String... placeholders) {
        if (!(sender instanceof CommandSender) || path == null || path.isEmpty()) return;

        String message = pluginInstance.getLangConfig().getString(path);
        if (message == null || message.isEmpty()) return;

        final String prefix = pluginInstance.getLangConfig().getString("prefix");
        if (prefix != null && !message.toLowerCase().startsWith("{bar}")) message = prefix + message;

        if (placeholders != null)
            for (String phLine : placeholders) {
                if (!phLine.contains(":")) continue;
                final String[] args = phLine.split(":");
                message = message.replace(args[0], args[1]);
            }

        if (sender instanceof Player && message.toLowerCase().startsWith("{bar}"))
            pluginInstance.getManager().getBarHandler().sendActionBar((Player) sender, message.substring(5));
        else ((CommandSender) sender).sendMessage(colorText(message));
    }

    /**
     * Gets a BlockFace enum value based on the imported Yaw value.
     *
     * @param Yaw The value to use as Yaw.
     * @return The BlockFace enum value found.
     */
    public BlockFace getDirection(double Yaw) {
        double rotation = (Yaw - 90.0F) % 360.0F;
        if (rotation < 0.0D)
            rotation += 360.0D;
        if ((0.0D <= rotation) && (rotation < 45.0D))
            return BlockFace.WEST;
        else if ((45.0D <= rotation) && (rotation < 135.0D))
            return BlockFace.NORTH;
        else if ((135.0D <= rotation) && (rotation < 225.0D))
            return BlockFace.EAST;
        else if ((225.0D <= rotation) && (rotation < 315.0D))
            return BlockFace.SOUTH;
        else if ((315.0D <= rotation) && (rotation < 360.0D))
            return BlockFace.WEST;
        return null;
    }

    /**
     * Retrieves a random integer between to given integers.
     *
     * @param min The minimum integer.
     * @param max The maximum integer.
     * @return The randomly retrieved value between the min and max.
     */
    public int getRandomInt(int min, int max) {
        return getRandom().nextInt(max - min + 1) + min;
    }

    /**
     * Gets the price of an ItemStack using the Essentials API.
     *
     * @param itemStack The item to check the price for.
     * @return The price found (Will be "0" if invalid).
     */
    public Double getEssentialsSellPrice(ItemStack itemStack) {
        Plugin plugin = pluginInstance.getServer().getPluginManager().getPlugin("Essentials");
        if (plugin != null) {
            com.earth2me.essentials.Essentials essentials = (com.earth2me.essentials.Essentials) plugin;
            double foundPrice = 0.0;
            try {
                Method method = essentials.getWorth().getClass().getMethod("getPrice", com.earth2me.essentials.IEssentials.class, ItemStack.class);
                if (method.getParameterCount() == 2)
                    foundPrice = ((BigDecimal) method.invoke(essentials.getWorth(), essentials, itemStack)).doubleValue();
                else foundPrice = ((BigDecimal) method.invoke(essentials.getWorth(), itemStack)).doubleValue();
            } catch (Exception ignored) {
            }
            return foundPrice;
        }

        return 0.0;
    }

    /**
     * Builds a FactionWP tool with specific stats and returns the ItemStack.
     *
     * @param wpType   The type of tool to build.
     * @param amount   The ItemStack amount.
     * @param uses     The tool use count (can be set to -1 by default for unlimited).
     * @param radius   The radius of the tool (Some types don't use this value).
     * @param modifier The modifier of the tool (Some types don't use this value).
     * @return The tool's ItemStack.
     */
    public ItemStack buildItem(@Nullable Player player, @NotNull WPType wpType, int amount, int uses, int radius, double modifier) {
        String itemPath = wpType.name().toLowerCase().replace("_", "-") + "-section",
                materialName = pluginInstance.getConfig().getString(itemPath + ".item.material");

        if (materialName == null) return null;
        Material material = Material.getMaterial(materialName.toUpperCase().replace(" ", "_").replace("-", "_"));
        if (material == null) return null;

        int durability = pluginInstance.getConfig().getInt(itemPath + ".item.durability");
        ItemStack itemStack = new ItemStack(material, amount, (short) durability);

        List<String> enchantmentList = pluginInstance.getConfig().getStringList(itemPath + ".item.enchantments");
        for (int i = -1; ++i < enchantmentList.size(); ) {
            String enchantmentString = enchantmentList.get(i);
            if (enchantmentString == null || enchantmentString.equalsIgnoreCase("")) continue;

            String[] enchantmentStringArgs = enchantmentString.split(":");
            String enchantName = enchantmentStringArgs[0].toUpperCase().replace(" ", "_").replace("-", "_");

            Enchantment enchant = Enchantment.getByName(enchantName);
            if (enchant != null)
                itemStack.addUnsafeEnchantment(enchant, Integer.parseInt(enchantmentStringArgs[1]));
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            final String usesFormat = pluginInstance.getConfig().getString("global-item-section.uses-format"),
                    radiusFormat = pluginInstance.getConfig().getString("global-item-section.radius-format"),
                    sellFormat = pluginInstance.getConfig().getString("global-item-section.sell-format"),
                    modifierFormat = pluginInstance.getConfig().getString("global-item-section.modifier-format"),
                    blockFormat = pluginInstance.getConfig().getString("global-item-section.block-count-format");
            final boolean areaMode = pluginInstance.getConfig().getBoolean("global-item-section.radius-area-mode"),
                    sellMode = pluginInstance.getConfig().getBoolean("global-item-section.display-sell-count"),
                    blockMode = pluginInstance.getConfig().getBoolean("global-item-section.display-block-count");
            final int rad = ((radius * 2) + 1);

            if (pluginInstance.getConfig().getBoolean("global-item-section.hide-enchantments"))
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            String name = pluginInstance.getConfig().getString(itemPath + ".item.display-name");

            final boolean nameStats = pluginInstance.getConfig().getBoolean("global-item-section.name-use-radius");
            if (nameStats && name != null) {
                String statsFormat = pluginInstance.getConfig().getString("global-item-section.name-use-radius-format");
                if (wpType.hasUses() && statsFormat != null)
                    statsFormat = statsFormat.replace("{uses}", String.valueOf(uses));

                if (wpType.hasRadius() && statsFormat != null)
                    statsFormat = statsFormat.replace("{radius}", !areaMode ? String.valueOf(radius)
                            : (rad + "x" + rad + "x" + rad));

                if (wpType.hasSellCount() && sellMode && statsFormat != null)
                    statsFormat = statsFormat.replace("{sell-count}", String.valueOf(0));

                if (wpType.hasBlockCount() && blockMode && statsFormat != null)
                    statsFormat = statsFormat.replace("{block-count}", String.valueOf(0));

                if (wpType.hasModifier() && statsFormat != null)
                    statsFormat = statsFormat.replace("{modifier}", String.valueOf(modifier));

                name = name + statsFormat;
            }

            if (name != null) itemMeta.setDisplayName(colorText((FactionWP.getPluginInstance().isPlaceholderAPIInstalled() && player != null)
                    ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, name) : name));
            itemMeta.setLore(new ArrayList<String>() {
                private static final long serialVersionUID = 1L;

                {
                    List<String> lore = pluginInstance.getConfig().getStringList(itemPath + ".item.lore");
                    ConfigurationSection toolSection = pluginInstance.getConfig().getConfigurationSection(itemPath);

                    for (int i = -1; ++i < lore.size(); ) {
                        String line = lore.get(i);
                        if (toolSection != null) {
                            switch (line.toLowerCase()) {
                                case "{uses-line}":
                                    if (usesFormat != null && wpType.hasUses() && uses != -1)
                                        add(colorText(usesFormat.replace("{uses}", String.valueOf(uses))));
                                    continue;
                                case "{sell-line}":
                                    if (sellFormat != null && wpType.hasSellCount() && sellMode)
                                        add(colorText(sellFormat.replace("{count}", String.valueOf(0))));
                                    continue;
                                case "{modifier-line}":
                                    if (modifierFormat != null && wpType.hasModifier())
                                        add(colorText(modifierFormat.replace("{modifier}", String.valueOf(modifier))));
                                    continue;
                                case "{radius-line}":
                                    if (radiusFormat != null && wpType.hasRadius()) {
                                        add(colorText(radiusFormat.replace("{radius}", !areaMode ? String.valueOf(radius)
                                                : (rad + "x" + rad + "x" + rad))));
                                    }
                                    continue;
                                case "{block-line}":
                                    if (blockFormat != null && wpType.hasBlockCount() && blockMode)
                                        add(colorText(blockFormat.replace("{count}", String.valueOf(0))));
                                    continue;
                                default:
                                    break;
                            }
                        }

                        add(colorText((FactionWP.getPluginInstance().isPlaceholderAPIInstalled() && player != null)
                                ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, line) : line));
                    }
                }
            });
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    /**
     * Updates the cooldown a player has on a specific tool.
     *
     * @param player The player to put on cooldown.
     * @param wpType The tool type to put the cooldown on.
     */
    public void updateWPCooldown(Player player, WPType wpType) {
        if (!getWpCooldowns().isEmpty() && getWpCooldowns().containsKey(player.getUniqueId())) {
            HashMap<WPType, Long> cooldowns = getWpCooldowns().get(player.getUniqueId());
            if (cooldowns != null)
                cooldowns.put(wpType, System.currentTimeMillis());
            else {
                HashMap<WPType, Long> newCooldowns = new HashMap<>();
                newCooldowns.put(wpType, System.currentTimeMillis());
                getWpCooldowns().put(player.getUniqueId(), newCooldowns);
            }
        } else {
            HashMap<WPType, Long> cooldowns = new HashMap<>();
            cooldowns.put(wpType, System.currentTimeMillis());
            getWpCooldowns().put(player.getUniqueId(), cooldowns);
        }
    }

    /**
     * Gets the cooldown for a particular tool type.
     *
     * @param player   the player to check.
     * @param wpType   the type of tool to check for.
     * @param cooldown the cooldown duration that the cooldown started at.
     * @return the time remaining as a long data type.
     */
    public long getCooldownRemainder(Player player, WPType wpType, int cooldown) {
        if (!wpCooldowns.isEmpty() && wpCooldowns.containsKey(player.getUniqueId())) {
            HashMap<WPType, Long> cooldowns = wpCooldowns.get(player.getUniqueId());
            if (cooldowns != null && !cooldowns.isEmpty() && cooldowns.containsKey(wpType))
                return ((cooldowns.get(wpType) / 1000) + cooldown) - (System.currentTimeMillis() / 1000);
        }

        return 0;
    }

    /**
     * Updates the player's cooldown to the current system time in milliseconds.
     *
     * @param player the player to update.
     */
    public void updateGlobalCooldown(Player player) {
        getGlobalCooldowns().put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Gets the global cooldown of the defined player.
     *
     * @param player   the player to check.
     * @param cooldown the cooldown duration that the cooldown started at.
     * @return the time remaining as a long data type.
     */
    public long getGlobalCooldownRemainder(Player player, int cooldown) {
        if (!getGlobalCooldowns().isEmpty() && getGlobalCooldowns().containsKey(player.getUniqueId()))
            return ((getGlobalCooldowns().get(player.getUniqueId()) / 1000) + cooldown) - (System.currentTimeMillis() / 1000);
        return 0;
    }

    /**
     * Gets how many uses a particular item has left in the lore.
     *
     * @param itemStack the itemstack to check.
     * @return the amount of uses remaining.
     */
    public int getItemUses(ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            final String usesFormat = pluginInstance.getConfig().getString("global-item-section.uses-format");
            if (usesFormat == null) {
                pluginInstance.log(Level.WARNING, "Your uses format in the configuration is invalid!");
                return -1;
            }

            final String formattedFormat = ChatColor.stripColor(colorText(usesFormat)).replace("{uses}", "%uses%");
            String[] usesFormatArgs = formattedFormat.split("%uses%");
            List<String> lore = itemMeta.getLore();
            for (int i = -1; ++i < lore.size(); ) {
                final String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
                if (usesFormatArgs.length >= 2) {
                    if (!formattedLine.startsWith(usesFormatArgs[0]) || !formattedLine.endsWith(usesFormatArgs[1]))
                        continue;
                    return Integer.parseInt(formattedLine.replace(usesFormatArgs[0], "").replace(usesFormatArgs[1], ""));
                } else if (usesFormatArgs.length >= 1) {
                    if (!formattedLine.startsWith(usesFormatArgs[0]))
                        continue;
                    return Integer.parseInt(formattedLine.replace(usesFormatArgs[0], ""));
                } else if (!pluginInstance.getManager().isNotNumeric(formattedLine))
                    return Integer.parseInt(formattedLine);
            }

            return -1;
        }

        return 0;
    }

    /**
     * Removes an amount of uses from an item's total uses.
     *
     * @param player    the player to remove from.
     * @param itemStack the item to remove the uses from.
     * @param uses      the amount of uses.
     */
    public void removeItemUses(Player player, ItemStack itemStack, int uses) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            final int gatheredUses = getItemUses(itemStack);
            ItemStack itemDuplicate = itemStack.clone();

            final String usesFormat = pluginInstance.getConfig().getString("global-item-section.uses-format");
            if (usesFormat == null) {
                pluginInstance.log(Level.WARNING, "Your uses format in the configuration is invalid!");
                return;
            }

            final String formattedFormat = ChatColor.stripColor(colorText(usesFormat)).replace("{uses}", "%uses%");
            String[] usesFormatArgs = formattedFormat.split("%uses%");
            List<String> lore = itemMeta.getLore();
            for (int i = -1; ++i < lore.size(); ) {
                String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
                if (usesFormatArgs.length >= 2) {
                    if (!formattedLine.startsWith(usesFormatArgs[0]) || !formattedLine.endsWith(usesFormatArgs[1]))
                        continue;
                } else if (usesFormatArgs.length >= 1) {
                    if (!formattedLine.startsWith(usesFormatArgs[0]))
                        continue;
                } else if (!pluginInstance.getManager().isNotNumeric(formattedLine))
                    Integer.parseInt(formattedLine);

                final String replace = usesFormat.replace("{uses}", String.valueOf(gatheredUses - uses));
                if (gatheredUses == -1) lore.remove(i);

                lore.set(i, colorText(replace));
                itemMeta.setLore(lore);
                itemStack.setItemMeta(itemMeta);

                if (itemStack.getAmount() > 1) {
                    itemStack.setAmount(1);
                    if (player.getInventory().firstEmpty() == -1) {
                        itemDuplicate.setAmount(itemDuplicate.getAmount() - 1);
                        player.getWorld().dropItemNaturally(player.getLocation(), itemDuplicate);
                    } else {
                        itemDuplicate.setAmount(itemDuplicate.getAmount() - 1);
                        player.getInventory().addItem(itemDuplicate);
                    }
                }

                player.updateInventory();
                if (gatheredUses != -1 && (Math.max(0, gatheredUses) - Math.max(0, uses)) <= 0) {
                    removeItem(player.getInventory(), itemStack);
                    player.playSound(player.getLocation(), (!pluginInstance.getServerVersion().startsWith("v1_7")
                            && !pluginInstance.getServerVersion().startsWith("v1_8")) ? Sound.ENTITY_ITEM_BREAK
                            : Sound.valueOf("ITEM_BREAK"), 1, 1);
                }
            }
        }
    }

    /**
     * Adds an amount of uses to an item's total uses.
     *
     * @param itemStack the item to add the uses to.
     * @param uses      the amount of uses.
     */
    public void addItemUses(ItemStack itemStack, int uses) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            final int gatheredUses = getItemUses(itemStack);

            final String usesFormat = pluginInstance.getConfig().getString("global-item-section.uses-format");
            if (usesFormat == null) {
                pluginInstance.log(Level.WARNING, "Your uses format in the configuration is invalid!");
                return;
            }

            final String formattedFormat = ChatColor.stripColor(colorText(usesFormat)).replace("{uses}", "%uses%");
            String[] usesFormatArgs = formattedFormat.split("%uses%");
            List<String> lore = itemMeta.getLore();
            for (int i = -1; ++i < lore.size(); ) {
                final String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
                if (usesFormatArgs.length >= 2 && !formattedLine.startsWith(usesFormatArgs[0])
                        || !formattedLine.endsWith(usesFormatArgs[1])) continue;
                else if (!formattedLine.startsWith(usesFormatArgs[0])) continue;

                final String replace = usesFormat.replace("{uses}", String.valueOf(gatheredUses + uses));

                if (gatheredUses == -1) lore.remove(i);
                lore.set(i, colorText(replace));
            }

            itemMeta.setLore(lore);
            itemStack.setItemMeta(itemMeta);
        }
    }

    /**
     * Gets the amount of blocks broken from an item.
     *
     * @param itemStack the item to get block break count from.
     * @return the amount of blocks broken with this item.
     */
    public int getBlocksBroken(ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.hasItemMeta() && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String blockCountFormat = pluginInstance.getConfig().getString("global-item-section.block-count-format");

            if (blockCountFormat == null) {
                pluginInstance.log(Level.WARNING, "Your block count format in the configuration is invalid!");
                return 0;
            }

            final String formattedFormat = ChatColor.stripColor(colorText(blockCountFormat)).replace("{count}", "%count%");
            String[] usesFormatArgs = formattedFormat.split("%count%");
            for (int i = -1; ++i < lore.size(); ) {
                String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
                try {
                    if (usesFormatArgs.length >= 2)
                        return Integer.parseInt(
                                formattedLine.replace(usesFormatArgs[0], "").replace(usesFormatArgs[1], ""));
                    else if (usesFormatArgs.length >= 1)
                        return Integer.parseInt(formattedLine.replace(usesFormatArgs[0], ""));
                    else {
                        try {
                            return Integer.parseInt(formattedLine);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return 0;
    }

    /**
     * Adds to the amount of blocks broken on an item.
     *
     * @param player     the player to add to.
     * @param itemStack  the item to add the amount of blocks broken count.
     * @param blockCount the amount of broken blocks to add.
     */
    public void addToBlocksBroken(Player player, ItemStack itemStack, int blockCount) {
        if (!pluginInstance.getConfig().getBoolean("global-item-section.display-block-count"))
            return;

        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String blockCountFormat = pluginInstance.getConfig().getString("global-item-section.block-count-format");

            if (blockCountFormat == null) {
                pluginInstance.log(Level.WARNING, "Your block count format in the configuration is invalid!");
                return;
            }

            final String formattedFormat = ChatColor.stripColor(colorText(blockCountFormat)).replace("{count}", "%count%");
            int brokenCount = getBlocksBroken(itemStack);
            counterHelper(player, itemStack, blockCount, itemMeta, lore, blockCountFormat, formattedFormat, brokenCount);
        }
    }

    /**
     * Gets the amount of items sold with an item.
     *
     * @param itemStack the item to get the amount of items sold.
     * @return the amount of items sold.
     */
    public int getItemsSold(ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String sellFormat = pluginInstance.getConfig().getString("global-item-section.sell-format");

            if (sellFormat == null) {
                pluginInstance.log(Level.WARNING, "Your sell format in the configuration is invalid!");
                return 0;
            }

            final String formattedFormat = ChatColor.stripColor(colorText(sellFormat)).replace("{count}", "%count%");
            String[] formatArgs = formattedFormat.split("%count%");
            for (int i = -1; ++i < lore.size(); ) {
                String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
                try {
                    if (formatArgs.length >= 2)
                        return Integer.parseInt(formattedLine.replace(formatArgs[0], "").replace(formatArgs[1], ""));
                    else if (formatArgs.length >= 1) return Integer.parseInt(formattedLine.replace(formatArgs[0], ""));
                    else return Integer.parseInt(formattedLine);
                } catch (Exception ignored) {
                }
            }
        }

        return 0;
    }

    /**
     * Adds to the amount of items sold to an item.
     *
     * @param player    the player to add too.
     * @param itemStack the item to get the amount from.
     * @param sellCount the amount of items sold to add.
     */
    public void addToItemsSold(Player player, ItemStack itemStack, int sellCount) {
        if (!pluginInstance.getConfig().getBoolean("global-item-section.display-sell-count"))
            return;

        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String sellFormat = pluginInstance.getConfig().getString("global-item-section.sell-format");

            if (sellFormat == null) {
                pluginInstance.log(Level.WARNING, "Your sell format in the configuration is invalid!");
                return;
            }

            final String formattedFormat = ChatColor.stripColor(colorText(sellFormat)).replace("{count}", "%count%");
            int sellCounted = getItemsSold(itemStack);
            counterHelper(player, itemStack, sellCount, itemMeta, lore, sellFormat, formattedFormat, sellCounted);
        }
    }

    private void counterHelper(Player player, ItemStack itemStack, int sellCount, ItemMeta itemMeta,
                               List<String> lore, String sellFormat, String formattedFormat, int sellCounted) {
        ItemStack itemDuplicate = itemStack.clone();
        String[] usesFormatArgs = formattedFormat.split("%count%");
        for (int i = -1; ++i < lore.size(); ) {
            String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
            if (usesFormatArgs.length >= 2) {
                if (!formattedLine.startsWith(usesFormatArgs[0]) || !formattedLine.endsWith(usesFormatArgs[1]))
                    continue;
            } else if (usesFormatArgs.length >= 1) {
                if (!formattedLine.startsWith(usesFormatArgs[0]))
                    continue;
            } else {
                try {
                    Integer.parseInt(formattedLine);
                } catch (Exception ignored) {
                    continue;
                }
            }

            lore.set(i, colorText(sellFormat.replace("{count}", String.valueOf(sellCounted + sellCount))));
            itemMeta.setLore(lore);
            itemStack.setItemMeta(itemMeta);
            player.updateInventory();

            if (itemStack.getAmount() > 1) {
                itemStack.setAmount(1);
                if (player.getInventory().firstEmpty() == -1) {
                    itemDuplicate.setAmount(itemDuplicate.getAmount() - 1);
                    player.getWorld().dropItemNaturally(player.getLocation(), itemDuplicate);
                } else {
                    itemDuplicate.setAmount(itemDuplicate.getAmount() - 1);
                    player.getInventory().addItem(itemDuplicate);
                }
            }

            break;
        }
    }

    private boolean isSimilar(ItemStack itemOne, ItemStack itemTwo) {
        ItemStack newItem = itemOne.clone();
        newItem.setAmount(itemTwo.getAmount());
        return newItem.toString().equals(itemTwo.toString());
    }

    /**
     * Removes an item from an inventory.
     *
     * @param inventory the inventory to remove from.
     * @param itemStack the item to remove (not including item amount).
     */
    private void removeItem(Inventory inventory, ItemStack itemStack) {
        int left = 1;

        for (int i = -1; ++i < inventory.getSize(); ) {
            ItemStack is = inventory.getItem(i);
            if (is != null && isSimilar(is, itemStack)) {
                if (left >= is.getAmount()) {
                    inventory.clear(i);
                    left -= is.getAmount();
                } else {
                    if (left <= 0) break;
                    is.setAmount(is.getAmount() - left);
                    left = 0;
                }
            }
        }

        if (inventory.getHolder() instanceof Player)
            ((Player) inventory.getHolder()).updateInventory();
    }

    /**
     * Attempts to calculate the vanilla fortune amount for the item.
     *
     * @param dropMaterial The material dropped.
     * @param fortuneLevel The fortune level.
     * @param durability   The durability of the material dropped.
     * @return The generated amount.
     */
    public int getSimulatedFortuneAmount(Material dropMaterial, int durability, int fortuneLevel) {
        int amountToReturn = 1;

        if (dropMaterial.name().contains("AMETHYST")) amountToReturn = 4;
        else if (dropMaterial.name().contains("GOLD_NUGGET")) amountToReturn = getRandomInt(2, 6);
        else if (dropMaterial.name().contains("LAPIS_LAZULI") || (dropMaterial.name().contains("INK") && durability == 4))
            amountToReturn = getRandomInt(4, 9);
        else if (dropMaterial.name().contains("REDSTONE")) amountToReturn = getRandomInt(4, 5);
        else if (dropMaterial.name().contains("GLOWSTONE")) {
            amountToReturn = getRandomInt(2, 4);
            return Math.min(4, calculateFortune(fortuneLevel, amountToReturn));
        } else if (dropMaterial.name().contains("NETHER_WART") || dropMaterial.name().contains("NETHER_STALK")) {
            amountToReturn = getRandomInt(2, 4);
            return Math.min(4, calculateFortune(fortuneLevel, amountToReturn));
        } else if (dropMaterial.name().contains("MELON")) {
            amountToReturn = getRandomInt(3, 7);
            return Math.min(9, calculateFortune(fortuneLevel, amountToReturn));
        } else if (dropMaterial.name().contains("PRISMARINE_CRYSTALS")) {
            amountToReturn = getRandomInt(2, 3);
            return Math.min(5, calculateFortune(fortuneLevel, amountToReturn));
        } else if (dropMaterial.name().contains("SWEET_BERRIES")) {
            amountToReturn = getRandomInt(2, 3);
            return calculateFortune(fortuneLevel, amountToReturn);
        } else if (dropMaterial.name().contains("FLINT") || dropMaterial.name().contains("SEED") || dropMaterial.name().contains("COAL"))
            return calculateFortune(fortuneLevel, amountToReturn);

        return amountToReturn;
    }

    private int calculateFortune(int fortuneLevel, int amountToReturn) {
        double chance = noDropsChance(fortuneLevel);
        if (Math.random() < chance) return amountToReturn;

        int dropAmount = getRandomInt(2, (fortuneLevel + 1));
        for (int i = 1; ++i < (fortuneLevel + 1); ) {
            if (Math.random() >= (chance + (i * (chance / 2))))
                dropAmount = getRandomInt(2, (fortuneLevel + 1));
        }

        return (dropAmount * amountToReturn);
    }

    private float noDropsChance(int fortuneLevel) {
        return (float) (2 / (fortuneLevel + 2));
    }

    /**
     * This method checks all hooks to make sure a location is safe.
     *
     * @param location        the location to check.
     * @param player          the player to check.
     * @param isBreakCheck    whether this check is about breaking a block.
     * @param isBuildCheck    whether this check is about placing or building a block.
     * @param gpMaterialCheck this is the material of the block a location being checked for the GriefPrevention hook.
     * @return true or false.
     */
    public boolean isLocationSafe(Location location, Player player, boolean isBreakCheck, boolean isBuildCheck, Material gpMaterialCheck) {
        if (location == null || location.getWorld() == null) return false;

        WorldBorder border = location.getWorld().getWorldBorder();
        double size = border.getSize() / 2;
        Location center = border.getCenter();
        double x = location.getX() - center.getX(), z = location.getZ() - center.getZ();
        if (((x > size || (-x) > size) || (z > size || (-z) > size))) return false;

        if (getWPItemType(getHandItem(player)) != WPType.SPAWNER_PICKAXE) {
            List<String> materialList = pluginInstance.getConfig().getStringList("hooks-section.blocked-materials");
            for (int i = -1; ++i < materialList.size(); ) {
                String materialName = materialList.get(i);
                if (materialName != null && materialName.replace(" ", "_").replace("-", "_")
                        .equalsIgnoreCase(location.getBlock().getType().name()))
                    return false;
            }
        }

        if (pluginInstance.getWorldGuardHandler() != null && !pluginInstance.getWorldGuardHandler().passedWorldGuardHook(player, location))
            return false;

        Plugin griefPrevention = pluginInstance.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefPrevention != null) {
            me.ryanhamshire.GriefPrevention.Claim claimAtLocation = me.ryanhamshire.GriefPrevention.GriefPrevention
                    .instance.dataStore.getClaimAt(location, true, null);
            if (claimAtLocation != null && !claimAtLocation.getOwnerID().toString().equals(player.getUniqueId().toString())
                    && !claimAtLocation.hasExplicitPermission(player, ClaimPermission.Build))
                return false;
        }

        Plugin supSkyBlock = pluginInstance.getServer().getPluginManager().getPlugin("SuperiorSkyblock2");
        if (supSkyBlock != null) {
            com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer superiorPlayer = com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
            final com.bgsoftware.superiorskyblock.api.island.Island island = com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI.getIslandAt(location);
            if (island != null && !island.isMember(superiorPlayer) && !island.getOwner().getUniqueId().toString().equals(player.getUniqueId().toString()))
                return false;
        }

        Plugin aSkyBlock = pluginInstance.getServer().getPluginManager().getPlugin("ASkyBlock");
        if (aSkyBlock != null) {
            com.wasteofplastic.askyblock.Island island = com.wasteofplastic.askyblock.ASkyBlockAPI.getInstance().getIslandAt(location);
            if (island != null && (!island.getOwner().toString().equals(player.getUniqueId().toString()) && !island.getMembers().contains(player.getUniqueId())))
                return false;
        }

       /* Plugin iSkyBlock = pluginInstance.getServer().getPluginManager().getPlugin("IridiumSkyblock");
        if (iSkyBlock != null) {
            @NotNull Optional<com.iridium.iridiumskyblock.database.Island> island =
                    com.iridium.iridiumskyblock.IridiumSkyblock.getInstance().getIslandManager().getIslandViaLocation(location);
            com.iridium.iridiumskyblock.database.User user = com.iridium.iridiumskyblock.IridiumSkyblock.getInstance().getUserManager().getUser(player);
            if (island.isPresent() && user.getPlayer() != null) {
                com.iridium.iridiumskyblock.database.Island is = island.get();
                if (!is.getOwner().getUuid().toString().equals(user.getPlayer().getUniqueId().toString())
                        && !is.getMembers().contains(user)) return false;
            }
        }*/

        Plugin residence = pluginInstance.getServer().getPluginManager().getPlugin("Residence");
        if (residence != null) {
            com.bekvon.bukkit.residence.protection.ClaimedResidence res = com.bekvon.bukkit.residence.Residence.getInstance().getResidenceManager().getByLoc(location);
            if (res != null && !res.getOwnerUUID().toString().equals(player.getUniqueId().toString())) return false;
        }

        /*Plugin skyBlock = pluginInstance.getServer().getPluginManager().getPlugin("FabledSkyBlock");
        if (skyBlock == null)
            skyBlock = pluginInstance.getServer().getPluginManager().getPlugin("SkyBlock");

        if (skyBlock != null) {
            com.songoda.skyblock.api.island.Island island = com.songoda.skyblock.api.SkyBlockAPI.getIslandManager().getIslandAtLocation(location);
            if (island != null && !island.getOwnerUUID().toString().equals(player.getUniqueId().toString()) && !island.getCoopPlayers().containsKey(player.getUniqueId())
                    && island.getRole(player) != com.songoda.skyblock.api.island.IslandRole.MEMBER && island.getRole(player) != com.songoda.skyblock.api.island.IslandRole.COOP
                    && island.getRole(player) != com.songoda.skyblock.api.island.IslandRole.OPERATOR) return false;
        }

        Plugin factionsX = pluginInstance.getServer().getPluginManager().getPlugin("FactionsX");
        if (factionsX != null) {
            net.prosavage.factionsx.manager.GridManager gridManager = net.prosavage.factionsx.manager.GridManager.INSTANCE;
            net.prosavage.factionsx.core.Faction faction = gridManager.getFactionAt(location.getChunk()),
                    playerFaction = PlayerManager.INSTANCE.getFPlayer(player).getFaction();
            if (faction.isWarzone() || faction.getId() != playerFaction.getId()) return false;
        }*/

        Plugin factions = pluginInstance.getServer().getPluginManager().getPlugin("Factions");
        if (factions != null)
            if (!factions.getDescription().getDepend().contains("MassiveCore")) {
                com.massivecraft.factions.FLocation fLocation = new com.massivecraft.factions.FLocation(location);
                com.massivecraft.factions.Faction factionAtLocation = com.massivecraft.factions.Board.getInstance().getFactionAt(fLocation);
                com.massivecraft.factions.FPlayer fPlayer = com.massivecraft.factions.FPlayers.getInstance().getByPlayer(player);
                return (factionAtLocation.isWilderness()
                        || fPlayer.getFaction().getComparisonTag().equalsIgnoreCase(factionAtLocation.getComparisonTag()));
            } else {

                try {
                    final Method method = FactionsBlockListener.class.getDeclaredMethod("playerCanBuildDestroyBlock",
                            Player.class, Location.class, String.class, Boolean.class);
                    if (!((boolean) method.invoke(FactionsBlockListener.class, player, location, "destroy", false)))
                        return false;
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
                }

                com.massivecraft.factions.entity.Faction factionAtLocation = com.massivecraft.factions.entity.BoardColl.get()
                        .getFactionAt(com.massivecraft.massivecore.ps.PS.valueOf(location));
                com.massivecraft.factions.entity.MPlayer mPlayer = com.massivecraft.factions.entity.MPlayer.get(player);
                return (factionAtLocation.getId().equalsIgnoreCase(com.massivecraft.factions.entity.FactionColl.get().getNone().getId())
                        || factionAtLocation.getId().equalsIgnoreCase(mPlayer.getFaction().getId()));
            }

        return true;
    }

    /**
     * Gets the player faction name, if possible.
     *
     * @param player The player name.
     * @return The faction name in all lowercase.
     */
    public String getFactionName(Player player) {
        String factionName = null;
        Plugin factions = pluginInstance.getServer().getPluginManager().getPlugin("Factions");
        if (factions != null)
            if (!factions.getDescription().getDepend().contains("MassiveCore")) {
                com.massivecraft.factions.FPlayer fPlayer = com.massivecraft.factions.FPlayers.getInstance().getByPlayer(player);
                if (fPlayer.getFaction() != null) factionName = fPlayer.getFaction().getComparisonTag();
            } else {
                com.massivecraft.factions.entity.MPlayer mPlayer = com.massivecraft.factions.entity.MPlayer.get(player);
                if (mPlayer.getFaction() != null) factionName = mPlayer.getFaction().getName();
            }
      /*  else {
            Plugin factionsX = pluginInstance.getServer().getPluginManager().getPlugin("FactionsX");
            if (factionsX != null) {
                net.prosavage.factionsx.manager.PlayerManager playerManager = net.prosavage.factionsx.manager.PlayerManager.INSTANCE;
                net.prosavage.factionsx.core.Faction faction = playerManager.getFPlayer(player).getFaction();
                if (faction != null) factionName = factionsX.getName();
            }
        }*/

        return (factionName != null ? factionName.toLowerCase() : null);
    }

    @SuppressWarnings("deprecation")
    public void logToCoreProtect(Player player, Block block, boolean isBrokenBlock, boolean isPlacedBlock) {
        if (!pluginInstance.getConfig().getBoolean("general-section.log-core-protect")) return;

        Plugin plugin2 = pluginInstance.getServer().getPluginManager().getPlugin("CoreProtect");
        if (plugin2 instanceof net.coreprotect.CoreProtect) {
            net.coreprotect.CoreProtectAPI coreProtect = ((net.coreprotect.CoreProtect) plugin2).getAPI();
            if (coreProtect.isEnabled()) {
                if (pluginInstance.getServerVersion().startsWith("v1_7") || pluginInstance.getServerVersion().startsWith("v1_8")) {
                    if (isBrokenBlock)
                        coreProtect.logRemoval(player.getName(), block.getLocation(), block.getType(), block.getData());
                    if (isPlacedBlock)
                        coreProtect.logPlacement(player.getName(), block.getLocation(), block.getType(), block.getData());
                    return;
                }

                if (isBrokenBlock)
                    coreProtect.logRemoval(player.getName(), block.getLocation(), block.getType(), block.getBlockData());
                if (isPlacedBlock)
                    coreProtect.logPlacement(player.getName(), block.getLocation(), block.getType(), block.getBlockData());
            }
        }
    }

    /**
     * This is used to retrieve how many rows the given trench pickaxe itemstack can
     * mine.
     *
     * @param itemStack The trench pickaxe itemstack
     * @return the amount of rows the pickaxe can mine.
     */
    public int getItemRadius(ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String radiusFormat = pluginInstance.getConfig().getString("global-item-section.radius-format");

            if (radiusFormat == null) {
                pluginInstance.log(Level.WARNING, "Your radius format in the configuration is invalid!");
                return 1;
            }

            final String formattedFormat = ChatColor.stripColor(colorText(radiusFormat)).replace("{radius}", "%radius%");
            String[] radiusFormatArgs = formattedFormat.split("%radius%");

            int gatheredRadiusCount = 0;
            boolean radiusAreaMode = pluginInstance.getConfig().getBoolean("global-item-section.radius-area-mode");

            for (int i = -1; ++i < lore.size(); ) {
                String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
                try {
                    if (radiusAreaMode) {
                        if (radiusFormatArgs.length >= 2) {
                            String radiusValue = formattedLine.replace(radiusFormatArgs[0], "").replace(radiusFormatArgs[1], "");
                            if (radiusValue.contains("x1x"))
                                gatheredRadiusCount = (Integer.parseInt(radiusValue.split("x1x")[1]));
                            else if (radiusValue.contains("x"))
                                gatheredRadiusCount = (Integer.parseInt(radiusValue.split("x")[0]) / 2);
                            else
                                gatheredRadiusCount = Integer.parseInt(formattedLine.replace(radiusFormatArgs[0], "").replace(radiusFormatArgs[1], ""));
                        } else if (radiusFormatArgs.length >= 1) {
                            String radiusValue = formattedLine.replace(radiusFormatArgs[0], "");
                            if (radiusValue.contains("x1x"))
                                gatheredRadiusCount = (Integer.parseInt(radiusValue.split("x1x")[1]));
                            else if (radiusValue.contains("x"))
                                gatheredRadiusCount = (Integer.parseInt(radiusValue.split("x")[0]) / 2);
                            else
                                gatheredRadiusCount = Integer.parseInt(formattedLine.replace(radiusFormatArgs[0], ""));
                        } else {
                            if (formattedLine.contains("x1x"))
                                gatheredRadiusCount = (Integer.parseInt(formattedLine.split("x1x")[1]));
                            else if (formattedLine.contains("x"))
                                gatheredRadiusCount = (Integer.parseInt(formattedLine.split("x")[0]) / 2);
                            else gatheredRadiusCount = Integer.parseInt(formattedLine);
                        }
                    } else {
                        if (radiusFormatArgs.length >= 2)
                            gatheredRadiusCount = Integer.parseInt(formattedLine.replace(radiusFormatArgs[0], "").replace(radiusFormatArgs[1], ""));
                        else if (radiusFormatArgs.length >= 1)
                            gatheredRadiusCount = Integer.parseInt(formattedLine.replace(radiusFormatArgs[0], ""));
                        else gatheredRadiusCount = Integer.parseInt(formattedLine);
                    }
                } catch (Exception ignored) {
                }
            }

            return gatheredRadiusCount;
        }

        return 1;
    }

    public double getItemModifier(ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String modifierFormat = pluginInstance.getConfig().getString("global-item-section.modifier-format");

            if (modifierFormat == null) {
                pluginInstance.log(Level.WARNING, "Your modifier format in the configuration is invalid!");
                return 1;
            }

            final String formattedFormat = ChatColor.stripColor(colorText(modifierFormat)).replace("{modifier}", "%modifier%");
            String[] modifierFormatArgs = formattedFormat.split("%modifier%");

            double gatheredCount = 1;
            for (int i = -1; ++i < lore.size(); ) {
                String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
                try {
                    if (modifierFormatArgs.length == 1)
                        gatheredCount = Double.parseDouble(formattedLine.replace(modifierFormatArgs[0], ""));
                    else if (modifierFormatArgs.length >= 2)
                        gatheredCount = Double.parseDouble(formattedLine.replace(modifierFormatArgs[0], "")
                                .replace(modifierFormatArgs[1], ""));
                    else gatheredCount = Double.parseDouble(formattedLine);
                } catch (Exception ignored) {
                }
            }

            return gatheredCount;
        }

        return 1;
    }

    /**
     * Gets if the item is a FactionWP tool or not.
     *
     * @param itemStack the item to check.
     * @return whether it is a FactionWP tool or not.
     */
    public boolean isWPItem(ItemStack itemStack) {
        for (int i = -1; ++i < WPType.values().length; )
            if (doesWPTypeMatchItem(WPType.values()[i], itemStack))
                return true;
        return false;
    }

    /**
     * Gets if item matches a particular FactionWP tool.
     *
     * @param wpType    the type of tool to match.
     * @param itemStack the itemstack to check.
     * @return whether it is or not.
     */
    public boolean doesWPTypeMatchItem(WPType wpType, ItemStack itemStack) {
        if (itemStack != null && (itemStack.getItemMeta() != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() && itemStack.getItemMeta().hasLore())) {
            final String path = (wpType.name().toLowerCase().replace("_", "-") + "-section.item."),
                    displayName = pluginInstance.getManager().colorText(pluginInstance.getConfig().getString(path + "display-name"));
            final Material material = Material.getMaterial(Objects.requireNonNull(pluginInstance.getConfig().getString(path + "material"))
                    .toUpperCase().replace(" ", "_").replace("-", "_"));
            int durability = pluginInstance.getConfig().getInt(path + "durability");

            return (wpType == WPType.MULTI_TOOL || itemStack.getType() == material && (itemStack.getDurability() == durability || durability == -1))
                    && (displayName.startsWith(itemStack.getItemMeta().getDisplayName()) || displayName.contains(itemStack.getItemMeta().getDisplayName()) || displayName.equals(itemStack.getItemMeta().getDisplayName()))
                    && enchantmentsMatch(itemStack, pluginInstance.getConfig().getStringList("sand-wand-section.item.enchantments"));
        }

        return false;
    }

    /**
     * Gets the FactionWP tool type from an item.
     *
     * @param itemStack the item to get from.
     * @return the tool type.
     */
    public WPType getWPItemType(ItemStack itemStack) {
        if (itemStack != null && (itemStack.getItemMeta() != null && itemStack.hasItemMeta()
                && itemStack.getItemMeta().hasDisplayName() && itemStack.getItemMeta().hasLore())) {
            for (WPType wpType : WPType.values()) {
                String typeIdString = wpType.name().toLowerCase().replace("_", "-");

                String displayName = pluginInstance.getManager().colorText(pluginInstance.getConfig().getString(typeIdString + "-section.item.display-name"));
                Material material = Material.getMaterial(Objects.requireNonNull(pluginInstance.getConfig().getString(typeIdString + "-section.item.material")).toUpperCase()
                        .replace(" ", "_").replace("-", "_"));
                int durability = pluginInstance.getConfig().getInt(typeIdString + "-section.item.durability");

                if (((wpType == WPType.MULTI_TOOL || itemStack.getType() == material) && (itemStack.getDurability() == durability || durability == -1))
                        && (displayName.startsWith(itemStack.getItemMeta().getDisplayName()) || displayName.contains(itemStack.getItemMeta().getDisplayName()) || displayName.equals(itemStack.getItemMeta().getDisplayName()))
                        && enchantmentsMatch(itemStack, pluginInstance.getConfig().getStringList(typeIdString + "-section.item.enchantments")))
                    return wpType;
            }
        }

        return null;
    }

    /**
     * Obtains a multi-tool's next form.
     *
     * @param handItem The multi-tool.
     * @param block    The block to change based on.
     * @return The material to swap to.
     */
    public Material getMultiToolSwapMaterial(ItemStack handItem, Block block) {
        final List<String> pickaxeMaterials = pluginInstance.getConfig().getStringList("multi-tool-section.pickaxe-swap-materials"),
                axeMaterials = pluginInstance.getConfig().getStringList("multi-tool-section.axe-swap-materials"),
                shovelMaterials = pluginInstance.getConfig().getStringList("multi-tool-section.shovel-swap-materials"),
                hoeMaterials = pluginInstance.getConfig().getStringList("multi-tool-section.hoe-swap-materials"),
                swordMaterials = pluginInstance.getConfig().getStringList("multi-tool-section.sword-swap-materials");

        Material mat = handItem.getType();
        String type = (handItem.getType().name().contains("_") ? (handItem.getType().name().split("_")[0] + "_") : null);
        if (type != null) {
            if (!pickaxeMaterials.isEmpty())
                for (String materialLine : pickaxeMaterials) {
                    if (materialLine.contains(":")) {
                        String[] args = materialLine.split(":");
                        if (block.getType().name().contains(args[0].toUpperCase().replace(" ", "_").replace("-", "_"))
                                && (Integer.parseInt(args[1]) < 0 || block.getData() == Integer.parseInt(args[1])))
                            mat = Material.getMaterial(type + "PICKAXE");
                    }
                }

            if (!axeMaterials.isEmpty())
                for (String materialLine : axeMaterials) {
                    if (materialLine.contains(":")) {
                        String[] args = materialLine.split(":");
                        if (block.getType().name().contains(args[0].toUpperCase().replace(" ", "_").replace("-", "_"))
                                && (Integer.parseInt(args[1]) < 0 || block.getData() == Integer.parseInt(args[1])))
                            mat = Material.getMaterial(type + "AXE");
                    }
                }

            if (!shovelMaterials.isEmpty()) {
                final boolean isNewMaterialVersion = (!pluginInstance.getServerVersion().startsWith("v1_8") && !pluginInstance.getServerVersion().startsWith("v1_9")
                        && !pluginInstance.getServerVersion().startsWith("v1_10") && !pluginInstance.getServerVersion().startsWith("v1_11")
                        && !pluginInstance.getServerVersion().startsWith("v1_12"));
                for (String materialLine : shovelMaterials) {
                    if (materialLine.contains(":")) {
                        String[] args = materialLine.split(":");
                        if (block.getType().name().contains(args[0].toUpperCase().replace(" ", "_").replace("-", "_"))
                                && (Integer.parseInt(args[1]) < 0 || block.getData() == Integer.parseInt(args[1])))
                            mat = Material.getMaterial(type + (isNewMaterialVersion ? "SHOVEL" : "SPADE"));
                    }
                }
            }

            if (!hoeMaterials.isEmpty())
                for (String materialLine : hoeMaterials) {
                    if (materialLine.contains(":")) {
                        String[] args = materialLine.split(":");
                        if (block.getType().name().contains(args[0].toUpperCase().replace(" ", "_").replace("-", "_"))
                                && (Integer.parseInt(args[1]) < 0 || block.getData() == Integer.parseInt(args[1])))
                            mat = Material.getMaterial(type + "HOE");
                    }
                }

            if (!swordMaterials.isEmpty())
                for (String materialLine : swordMaterials) {
                    if (materialLine.contains(":")) {
                        String[] args = materialLine.split(":");
                        if (block.getType().name().contains(args[0].toUpperCase().replace(" ", "_").replace("-", "_"))
                                && (Integer.parseInt(args[1]) < 0 || block.getData() == Integer.parseInt(args[1])))
                            mat = Material.getMaterial(type + "SWORD");
                    }
                }
        }

        return mat;
    }

    /**
     * Checks if an item has the enchantments from a string list.
     *
     * @param itemStack       the item to check.
     * @param enchantmentList the list of enchants (normally from configurations).
     * @return whether the item has all of the or not (check includes levels).
     */
    private boolean enchantmentsMatch(ItemStack itemStack, List<String> enchantmentList) {
        if (enchantmentList.isEmpty() && itemStack.getEnchantments().isEmpty())
            return true;

        for (int i = -1; ++i < enchantmentList.size(); ) {
            String enchantmentString = enchantmentList.get(i);
            if (enchantmentString.contains(":")) {
                String[] enchantmentStringArgs = enchantmentString.split(":");
                Enchantment enchantment = Enchantment.getByName(enchantmentStringArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
                if (enchantment == null || itemStack.getEnchantmentLevel(enchantment) != Integer.parseInt(enchantmentStringArgs[1]))
                    return false;
            } else {
                Enchantment enchantment = Enchantment.getByName(enchantmentString.toUpperCase().replace(" ", "_").replace("-", "_"));
                if (itemStack.getEnchantments().isEmpty() || (!itemStack.getEnchantments().isEmpty()
                        && itemStack.getEnchantments().containsKey(enchantment)))
                    return false;
            }
        }

        return true;
    }

    /**
     * Removes a specific amount of a single item from an inventory.
     *
     * @param inventory  the inventory to remove from.
     * @param material   the item material to remove.
     * @param durability the item durability.
     * @param amount     the amount of the item to remove.
     */
    public void removeItem(Inventory inventory, Material material, short durability, int amount) {
        int left = amount;

        for (int i = -1; ++i < inventory.getSize(); ) {
            ItemStack is = inventory.getItem(i);
            if (is != null && is.getType() == material && is.getDurability() == durability) {
                if (left >= is.getAmount()) {
                    inventory.clear(i);
                    left -= is.getAmount();
                } else {
                    if (left <= 0) break;
                    is.setAmount(is.getAmount() - left);
                    left = 0;
                }
            }
        }

        if (inventory.getHolder() instanceof Player)
            ((Player) inventory.getHolder()).updateInventory();
    }

    @SuppressWarnings("deprecation")
    public ItemStack getHandItem(Player player) {
        if (pluginInstance.getServerVersion().toLowerCase().startsWith("v1_14")
                || pluginInstance.getServerVersion().toLowerCase().startsWith("v1_13")
                || pluginInstance.getServerVersion().toLowerCase().startsWith("v1_12")
                || pluginInstance.getServerVersion().toLowerCase().startsWith("v1_11")
                || pluginInstance.getServerVersion().toLowerCase().startsWith("v1_10")
                || pluginInstance.getServerVersion().toLowerCase().startsWith("v1_9"))
            return player.getInventory().getItemInMainHand();
        return player.getItemInHand();
    }

    public boolean isSellModeActive(Player player, WPType wpType) {
        if (!getSellModeMap().isEmpty() && getSellModeMap().containsKey(player.getUniqueId())) {
            HashMap<WPType, Boolean> innerMap = getSellModeMap().get(player.getUniqueId());
            if (innerMap != null && !innerMap.isEmpty() && innerMap.containsKey(wpType))
                return innerMap.get(wpType);
        }

        return false;
    }

    public void toggleSellMode(Player player, WPType wpType) {
        boolean enabled;
        if (!getSellModeMap().isEmpty() && getSellModeMap().containsKey(player.getUniqueId())) {
            HashMap<WPType, Boolean> innerMap = getSellModeMap().get(player.getUniqueId());
            if (innerMap != null && !innerMap.isEmpty() && innerMap.containsKey(wpType)) {
                boolean reversedStatus = !innerMap.get(wpType);
                innerMap.put(wpType, reversedStatus);
                enabled = reversedStatus;
            } else {
                HashMap<WPType, Boolean> newMap = new HashMap<>();
                newMap.put(wpType, true);
                getSellModeMap().put(player.getUniqueId(), newMap);
                enabled = true;
            }
        } else {
            HashMap<WPType, Boolean> newMap = new HashMap<>();
            newMap.put(wpType, true);
            getSellModeMap().put(player.getUniqueId(), newMap);
            enabled = true;
        }

        sendCustomMessage(player, "sell-toggle-message", "{status}:"
                + (enabled ? pluginInstance.getConfig().getString("global-item-section.enable-placeholder")
                : pluginInstance.getConfig().getString("global-item-section.disabled-placeholder")));
    }

    public void toggleSpawnerPickaxeMode(Player player) {
        SpawnerPickaxeMode currentMode = getSpawnerPickaxeModeMap().getOrDefault(player.getUniqueId(), SpawnerPickaxeMode.NATURAL), nextMode = currentMode;
        if (currentMode == SpawnerPickaxeMode.ALL) {
            if (player.hasPermission("factionwp.spawnerpickaxe.natural"))
                nextMode = SpawnerPickaxeMode.NATURAL;
            else if (player.hasPermission("factionwp.spawnerpickaxe.placed"))
                nextMode = SpawnerPickaxeMode.PLACED;
        } else if (currentMode == SpawnerPickaxeMode.NATURAL) {
            if (player.hasPermission("factionwp.spawnerpickaxe.placed"))
                nextMode = SpawnerPickaxeMode.PLACED;
            else if (player.hasPermission("factionwp.spawnerpickaxe.all"))
                nextMode = SpawnerPickaxeMode.ALL;
        } else if (currentMode == SpawnerPickaxeMode.PLACED) {
            if (player.hasPermission("factionwp.spawnerpickaxe.all"))
                nextMode = SpawnerPickaxeMode.ALL;
            else if (player.hasPermission("factionwp.spawnerpickaxe.natural"))
                nextMode = SpawnerPickaxeMode.NATURAL;
        }

        if (currentMode == nextMode) return;
        getSpawnerPickaxeModeMap().put(player.getUniqueId(), nextMode);
        sendCustomMessage(player, "spawner-pickaxe-cycle-message",
                ("{mode}:" + pluginInstance.getConfig().getString("spawner-pickaxe-section.modes." + nextMode.name().toLowerCase())));
    }

    public boolean isInList(Material material, int durability, String configurationPath, boolean blacklist, boolean looseCheck) {
        List<String> otherMaterials = pluginInstance.getConfig().getStringList(configurationPath);
        for (int i = -1; ++i < otherMaterials.size(); ) {
            String materialLine = otherMaterials.get(i);

            if (materialLine.contains(":")) {
                String[] materialLineArgs = materialLine.split(":");
                String formattedMat = materialLineArgs[0].toUpperCase().replace("-", "_").replace(" ", "_");
                int lineDurability = Integer.parseInt(materialLineArgs[1]);

                if ((looseCheck ? material.name().contains(formattedMat) : material.name().equals(formattedMat))
                        && (lineDurability == -1 || durability == Integer.parseInt(materialLineArgs[1]))) return blacklist;
            }

            String formattedMat = materialLine.toUpperCase().replace("-", "_").replace(" ", "_");
            if (looseCheck ? material.name().contains(formattedMat) : material.name().equals(formattedMat))
                return blacklist;
        }
        return !blacklist;
    }

    public double getMaterialPrice(ItemStack itemStack, String configurationPath) {
        List<String> priceStringList = pluginInstance.getConfig().getStringList(configurationPath);
        for (int i = -1; ++i < priceStringList.size(); ) {
            try {
                String priceString = priceStringList.get(i);
                String[] priceStringArgs = priceString.split(":");
                String materialName = priceStringArgs[0].toUpperCase().replace(" ", "_").replace("-", "_");
                Material material = Material.getMaterial(materialName);
                int durability = Integer.parseInt(priceStringArgs[1]);
                double price = Double.parseDouble(priceStringArgs[2]);

                if (itemStack.getType() == material && (durability == -1 || itemStack.getDurability() == durability))
                    return price;
            } catch (Exception e) {
                e.printStackTrace();
                pluginInstance.log(Level.WARNING, "There seems to have been a issue with one of "
                        + "your material prices inside the list at '" + configurationPath + "'.");
            }
        }

        return -1;
    }

    public boolean isStackingPlantBlock(Block block) {
        String[] materials = {"CHORUS", "CACTUS", "SUGAR", "CANE", "BAMBOO"};
        for (int i = -1; ++i < materials.length; ) {
            try {
                if (block.getType().name().contains(materials[i]))
                    return true;
            } catch (Exception e) {
                e.printStackTrace();
                pluginInstance.log(Level.WARNING, "There was a issue with your Stackable Plant Blocks list.");
            }
        }
        return false;
    }

    public boolean isHAMaterial(Block block) {
        String[] materials = {"MELON", "PUMPKIN"};
        for (int i = -1; ++i < materials.length; )
            if (block.getType().name().contains(materials[i]))
                return true;
        return false;
    }

    public boolean isCraftMaterial(ItemStack itemStack) {
        String[] materials = {"COAL:0", "INGOT", "NUGGET", "DIAMOND", "EMERALD", "QUARTZ", "REDSTONE",
                "LAPIS", "INK:4", "INK_SACK:4"};
        for (int i = -1; ++i < materials.length; ) {
            String materialString = materials[i];
            if (materialString.contains(":")) {
                String[] materialArgs = materialString.split(":");
                String material = materialArgs[0];
                int durability = Integer.parseInt(materialArgs[1]);
                if (itemStack.getType().name().contains(material) && (durability <= -1 || itemStack.getDurability() == durability)
                        && (itemStack.getItemMeta() == null || (!itemStack.getItemMeta().hasDisplayName() && !itemStack.getItemMeta().hasLore())))
                    return true;
            } else if (itemStack.getType().name().contains(materials[i]))
                return true;
        }

        return false;
    }

    public String getCraftSolution(Material material, int durability) {
        if (material.name().equalsIgnoreCase("COAL"))
            return "COAL_BLOCK";
        else if (material.name().equalsIgnoreCase("GOLD_INGOT"))
            return "GOLD_BLOCK";
        else if (material.name().equalsIgnoreCase("GOLD_NUGGET"))
            return "GOLD_BLOCK";
        else if (material.name().equalsIgnoreCase("IRON_INGOT"))
            return "IRON_BLOCK";
        else if (material.name().equalsIgnoreCase("IRON_NUGGET"))
            return "IRON_BLOCK";
        else if (material.name().equalsIgnoreCase("DIAMOND"))
            return "DIAMOND_BLOCK";
        else if (material.name().equalsIgnoreCase("EMERALD"))
            return "EMERALD_BLOCK";
        else if (material.name().equalsIgnoreCase("QUARTZ"))
            return "QUARTZ_BLOCK";
        else if (material.name().equalsIgnoreCase("REDSTONE"))
            return "REDSTONE_BLOCK";
        else if (material.name().equalsIgnoreCase("LAPIS_LAZULI")
                || ((material.name().equalsIgnoreCase("INK_SACK")
                || material.name().equalsIgnoreCase("INK_SAC")) && durability == 4))
            return "LAPIS_BLOCK";
        else
            return null;
    }

    public int getCraftAmount(Material material, int durability) {
        if (material.name().equalsIgnoreCase("COAL"))
            return 9;
        else if (material.name().equalsIgnoreCase("GOLD_INGOT"))
            return 9;
        else if (material.name().equalsIgnoreCase("GOLD_NUGGET"))
            return 81;
        else if (material.name().equalsIgnoreCase("IRON_INGOT"))
            return 9;
        else if (material.name().equalsIgnoreCase("IRON_NUGGET"))
            return 81;
        else if (material.name().equalsIgnoreCase("DIAMOND"))
            return 9;
        else if (material.name().equalsIgnoreCase("EMERALD"))
            return 9;
        else if (material.name().equalsIgnoreCase("QUARTZ"))
            return 9;
        else if (material.name().equalsIgnoreCase("REDSTONE"))
            return 9;
        else if (material.name().equalsIgnoreCase("LAPIS_LAZULI")
                || (material.name().equalsIgnoreCase("INK_SACK") && durability == 4))
            return 9;
        else
            return 0;
    }

    public ItemStack getCustomItem(int amount, String configurationPath) {
        Material material = Material.getMaterial(Objects.requireNonNull(pluginInstance.getConfig().getString(configurationPath + ".material"))
                .toUpperCase().replace(" ", "_").replace("-", "_"));
        int durability = pluginInstance.getConfig().getInt(configurationPath + ".durability");

        if (material == null) return null;
        ItemStack itemStack = new ItemStack(material, amount, (short) durability);

        List<String> enchantmentList = pluginInstance.getConfig().getStringList(configurationPath + ".enchantments");
        for (int i = -1; ++i < enchantmentList.size(); ) {
            String enchantmentString = enchantmentList.get(i);
            String[] enchantmentStringArgs = enchantmentString.split(":");

            Enchantment enchantment = Enchantment.getByName(enchantmentStringArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
            if (enchantment == null) continue;

            itemStack.addUnsafeEnchantment(enchantment, Integer.parseInt(enchantmentStringArgs[1]));
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(colorText(pluginInstance.getConfig().getString(configurationPath + ".display-name")));
            itemMeta.setLore(new ArrayList<String>() {
                private static final long serialVersionUID = 1L;

                {
                    List<String> lore = pluginInstance.getConfig().getStringList(configurationPath + ".lore");
                    for (int i = -1; ++i < lore.size(); )
                        add(colorText(lore.get(i)));
                }
            });
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    public boolean isIceWandMeltMaterial(Block block) {
        return block.getType().name().toUpperCase().contains("ICE");
    }

    public int generateRandomDropAmount(Material material, int durability) {
        List<String> dropTable = pluginInstance.getConfig().getStringList("global-item-section.material-drop-table");
        for (int i = -1; ++i < dropTable.size(); ) {
            String dropLine = dropTable.get(i);
            try {
                String[] lineArgs = dropLine.split(":");
                Material lineMaterial = Material
                        .getMaterial(lineArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
                int lineDurability = Integer.parseInt(lineArgs[1]);
                if (lineMaterial == material && (lineDurability == -1 || durability == lineDurability)) {
                    String[] rangeArgs = lineArgs[2].split(",");
                    return getRandomInt(Integer.parseInt(rangeArgs[0]), Integer.parseInt(rangeArgs[1]));
                }
            } catch (Exception e) {
                e.printStackTrace();
                pluginInstance.log(Level.WARNING,
                        "There was a issue located within the drop table list. Please check it.");
            }
        }

        return 1;
    }

    // getters & setters
    public HashMap<UUID, Long> getGlobalCooldowns() {
        return globalCooldowns;
    }

    private void setGlobalCooldowns(HashMap<UUID, Long> globalCooldowns) {
        this.globalCooldowns = globalCooldowns;
    }

    public Random getRandom() {
        return random;
    }

    private void setRandom(Random random) {
        this.random = random;
    }

    public HashMap<UUID, HashMap<WPType, Long>> getWpCooldowns() {
        return wpCooldowns;
    }

    private void setWPCooldowns(HashMap<UUID, HashMap<WPType, Long>> wpCooldowns) {
        this.wpCooldowns = wpCooldowns;
    }

    public HashMap<UUID, HashMap<WPType, Boolean>> getSellModeMap() {
        return sellModeMap;
    }

    private void setSellModeMap(HashMap<UUID, HashMap<WPType, Boolean>> sellModeMap) {
        this.sellModeMap = sellModeMap;
    }

    public BarHandler getBarHandler() {
        return barHandler;
    }

    private void setBarHandler(BarHandler barHandler) {
        this.barHandler = barHandler;
    }

    public HashMap<UUID, SpawnerPickaxeMode> getSpawnerPickaxeModeMap() {
        return spawnerPickaxeModeMap;
    }

}