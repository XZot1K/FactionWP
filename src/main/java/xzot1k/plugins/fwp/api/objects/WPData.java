package xzot1k.plugins.fwp.api.objects;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.fwp.FactionWP;
import xzot1k.plugins.fwp.api.enums.WPAttribute;
import xzot1k.plugins.fwp.api.enums.WPType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class WPData {
    private final FactionWP INSTANCE;
    private final NBTItem nbtItem;
    private String wpTypeName;
    private long blocksBroken, soldItems;
    private int uses, radius;
    private double modifier;

    public WPData(@NotNull Player player, @NotNull ItemStack itemStack) {
        this.INSTANCE = FactionWP.getPluginInstance();

        this.nbtItem = new NBTItem(itemStack);
        if (nbtItem.hasTag("fwp-type")) {
            this.wpTypeName = nbtItem.getString("fwp-type");
            final WPType wpType = WPType.getType(getWpTypeName());

            if (wpType.hasUses() && nbtItem.hasTag("fwp-uses")) this.uses = nbtItem.getInteger("fwp-uses");
            if (wpType.hasRadius() && nbtItem.hasTag("fwp-radius")) this.radius = nbtItem.getInteger("fwp-radius");
            if (wpType.hasModifier() && nbtItem.hasTag("fwp-modifier")) this.modifier = nbtItem.getInteger("fwp-modifier");
            if (wpType.hasBlockCount() && nbtItem.hasTag("fwp-blocks")) this.blocksBroken = nbtItem.getInteger("fwp-blocks");
            if (wpType.hasSellCount() && nbtItem.hasTag("fwp-sold")) this.soldItems = nbtItem.getInteger("fwp-sold");
        } else {
            this.uses = -1;
            this.radius = 1;
            this.modifier = 1;
            this.blocksBroken = 0;
            this.soldItems = 0;
        }

        loadFromItem(player, itemStack);
    }

    public WPData(@NotNull ItemStack itemStack, @NotNull WPType wpType, int uses, int radius, double modifier, long blocksBroken, long soldItems) {
        this.INSTANCE = FactionWP.getPluginInstance();
        this.uses = uses;
        this.radius = radius;
        this.modifier = modifier;
        this.blocksBroken = blocksBroken;
        this.soldItems = soldItems;

        this.nbtItem = new NBTItem(itemStack);
        this.wpTypeName = wpType.name();
    }

    public void loadFromItem(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() == null) return;

        if (!nbtItem.hasTag("fwp-type")) {
            final WPType wpType = WPType.getType(itemStack);
            if (wpType != null) {
                this.wpTypeName = wpType.name();
                nbtItem.setString("fwp-type", wpType.name());
                nbtItem.applyNBT(itemStack);
                itemStack = nbtItem.getItem();
            }
        }

        loadOldData(player, itemStack);
    }

    private void loadOldData(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() == null || itemStack.getItemMeta().getLore() == null) return;

        ItemMeta itemMeta = itemStack.getItemMeta();
        boolean radiusAreaMode = INSTANCE.getConfig().getBoolean("global-item-section.radius-area-mode");

        List<String> lore = new ArrayList<>(itemMeta.getLore());
        for (int i = -1; ++i < lore.size(); ) {
            final String line = lore.get(i);
            if (line == null || line.isEmpty()) continue;

            final String strippedLine = ChatColor.stripColor(INSTANCE.getManager().colorText(line));

            //WPAttribute foundAttribute = null;
            for (int j = -1; ++j < WPAttribute.values().length; ) {
                final WPAttribute wpAttribute = WPAttribute.values()[j];
                final String format = wpAttribute.getFormat(), strippedFormat = ChatColor.stripColor(INSTANCE.getManager().colorText(format));
                if (strippedFormat == null || strippedFormat.isEmpty()) continue;

                String[] surroundingStrings = strippedFormat.split(wpAttribute.getPlaceholder());
                if (surroundingStrings.length == 0) continue;

                if (wpAttribute == WPAttribute.RADIUS) {
                    if (surroundingStrings.length >= 2 && strippedLine.startsWith(surroundingStrings[0]) && strippedLine.endsWith(surroundingStrings[1])) {
                        if (radiusAreaMode) {
                            String radiusValue = strippedLine.replace(surroundingStrings[0], "").replace(surroundingStrings[1], "");
                            if (radiusValue.contains("x1x")) {
                                this.radius = (Integer.parseInt(radiusValue.split("x1x")[1]));
                                //foundAttribute = WPAttribute.RADIUS;
                                break;
                            } else if (radiusValue.contains("x")) {
                                this.radius = (Integer.parseInt(radiusValue.split("x")[0]) / 2);
                                //foundAttribute = WPAttribute.RADIUS;
                                break;
                            }
                        }

                        this.radius = Integer.parseInt(strippedLine.replace(surroundingStrings[0], "").replace(surroundingStrings[1], ""));
                        //foundAttribute = WPAttribute.RADIUS;
                        break;
                    } else if (surroundingStrings.length == 1 && strippedLine.startsWith(surroundingStrings[0])) {
                        this.radius = Integer.parseInt(strippedLine.replace(surroundingStrings[0], ""));
                        // foundAttribute = WPAttribute.RADIUS;
                        break;
                    }
                }

                boolean continueOn = false;
                String value = "1";

                if (surroundingStrings.length >= 2 && strippedLine.startsWith(surroundingStrings[0]) && strippedLine.endsWith(surroundingStrings[1])) {
                    value = strippedLine.replace(surroundingStrings[0], "").replace(surroundingStrings[1], "");
                    continueOn = true;
                } else if (surroundingStrings.length == 1 && strippedLine.startsWith(surroundingStrings[0])) {
                    value = strippedLine.replace(surroundingStrings[0], "");
                    continueOn = true;
                }

                if (continueOn) {
                    switch (wpAttribute) {
                        case USES: {
                            int newValue = Integer.parseInt(value);
                            if (newValue > getUses()) this.uses = newValue;
                            break;
                        }
                        case MODIFIER: {
                            double newValue = Double.parseDouble(value);
                            if (newValue > getModifier()) this.modifier = newValue;
                            break;
                        }
                        case BLOCK_COUNT: {
                            long newValue = Long.parseLong(value);
                            if (newValue > getBlocksBroken()) this.blocksBroken = newValue;
                            break;
                        }
                        case ITEMS_SOLD: {
                            long newValue = Long.parseLong(value);
                            if (newValue > getSoldItems()) this.soldItems = newValue;
                            break;
                        }
                        default: {break;}
                    }
                }
            }
        }

        nbtItem.setString("fwp-type", getWpTypeName());
        nbtItem.applyNBT(itemStack);

        apply(player, itemStack);
    }

    public ItemStack apply(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (getWpTypeName() == null || getWpTypeName().isEmpty()) return itemStack;

        final WPType wpType = WPType.getType(getWpTypeName());
        if (wpType.hasUses()) nbtItem.setInteger("fwp-uses", getUses());
        if (wpType.hasRadius()) nbtItem.setInteger("fwp-radius", getRadius());
        if (wpType.hasModifier()) nbtItem.setDouble("fwp-modifier", getModifier());
        if (wpType.hasBlockCount()) nbtItem.setLong("fwp-blocks", getBlocksBroken());
        if (wpType.hasSellCount()) nbtItem.setLong("fwp-sold", getSoldItems());
        nbtItem.applyNBT(itemStack);

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            final String itemPath = (wpType.name().toLowerCase().replace("_", "-") + "-section"),
                    usesFormat = INSTANCE.getConfig().getString("global-item-section.uses-format"),
                    radiusFormat = INSTANCE.getConfig().getString("global-item-section.radius-format"),
                    sellFormat = INSTANCE.getConfig().getString("global-item-section.sell-format"),
                    modifierFormat = INSTANCE.getConfig().getString("global-item-section.modifier-format"),
                    blockFormat = INSTANCE.getConfig().getString("global-item-section.block-count-format");
            final boolean areaMode = INSTANCE.getConfig().getBoolean("global-item-section.radius-area-mode"),
                    sellMode = INSTANCE.getConfig().getBoolean("global-item-section.display-sell-count"),
                    blockMode = INSTANCE.getConfig().getBoolean("global-item-section.display-block-count");
            final int rad = ((radius * 2) + 1);

            if (INSTANCE.getConfig().getBoolean("global-item-section.hide-enchantments"))
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            String name = INSTANCE.getConfig().getString(itemPath + ".item.display-name");
            final boolean nameStats = INSTANCE.getConfig().getBoolean("global-item-section.name-use-radius");
            if (nameStats && name != null) {
                String statsFormat = INSTANCE.getConfig().getString("global-item-section.name-use-radius-format");
                if (wpType.hasUses() && statsFormat != null)
                    statsFormat = statsFormat.replace("{uses}", String.valueOf(getUses()));

                if (wpType.hasRadius() && statsFormat != null)
                    statsFormat = statsFormat.replace("{radius}", !areaMode ? String.valueOf(getRadius()) : (rad + "x" + rad + "x" + rad));

                if (wpType.hasSellCount() && sellMode && statsFormat != null)
                    statsFormat = statsFormat.replace("{sell-count}", String.valueOf(getSoldItems()));

                if (wpType.hasBlockCount() && blockMode && statsFormat != null)
                    statsFormat = statsFormat.replace("{block-count}", String.valueOf(getBlocksBroken()));

                if (wpType.hasModifier() && statsFormat != null)
                    statsFormat = statsFormat.replace("{modifier}", String.valueOf(getModifier()));

                name = name + statsFormat;
            }

            if (name != null) itemMeta.setDisplayName(INSTANCE.getManager().colorText(FactionWP.getPluginInstance().isPlaceholderAPIInstalled()
                    ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, name) : name));

            List<String> lore = INSTANCE.getConfig().getStringList(itemPath + ".item.lore");
            ConfigurationSection toolSection = INSTANCE.getConfig().getConfigurationSection(itemPath);
            itemMeta.setLore(new ArrayList<String>() {{
                for (int i = -1; ++i < lore.size(); ) {
                    String line = lore.get(i);
                    if (toolSection != null) {
                        switch (line.toLowerCase()) {
                            case "{uses-line}":
                                if (usesFormat != null && wpType.hasUses() && getUses() != -1)
                                    add(INSTANCE.getManager().colorText(usesFormat.replace("{uses}", String.valueOf(getUses()))));
                                continue;
                            case "{sell-line}":
                                if (sellFormat != null && wpType.hasSellCount() && sellMode)
                                    add(INSTANCE.getManager().colorText(sellFormat.replace("{count}", String.valueOf(getSoldItems()))));
                                continue;
                            case "{modifier-line}":
                                if (modifierFormat != null && wpType.hasModifier())
                                    add(INSTANCE.getManager().colorText(modifierFormat.replace("{modifier}", String.valueOf(getModifier()))));
                                continue;
                            case "{radius-line}":
                                if (radiusFormat != null && wpType.hasRadius()) {
                                    add(INSTANCE.getManager().colorText(radiusFormat.replace("{radius}", !areaMode ? String.valueOf(getRadius())
                                            : (rad + "x" + rad + "x" + rad))));
                                }
                                continue;
                            case "{block-line}":
                                if (blockFormat != null && wpType.hasBlockCount() && blockMode)
                                    add(INSTANCE.getManager().colorText(blockFormat.replace("{count}", String.valueOf(getBlocksBroken()))));
                                continue;
                            default:
                                break;
                        }
                    }

                    add(INSTANCE.getManager().colorText(INSTANCE.isPlaceholderAPIInstalled() ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, line) : line));
                }
            }});
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    public void removeUses(@NotNull Player player, @NotNull ItemStack itemStack, int amount) {
        if (getUses() >= 0) {
            setUses(Math.max(0, (getUses() - amount)));

            if (getUses() == 0) {
                final ItemStack itemDuplicate = itemStack.clone();
                if (itemStack.getAmount() > 1) {
                    itemStack.setAmount(1);
                    itemDuplicate.setAmount(itemDuplicate.getAmount() - amount);
                    if (player.getInventory().firstEmpty() == -1) player.getWorld().dropItemNaturally(player.getLocation(), itemDuplicate);
                    else player.getInventory().addItem(itemDuplicate);
                }

                INSTANCE.getManager().removeItem(player.getInventory(), itemStack, 1);
                player.playSound(player.getLocation(), (!INSTANCE.getServerVersion().startsWith("v1_7") && !INSTANCE.getServerVersion().startsWith("v1_8"))
                        ? Sound.ENTITY_ITEM_BREAK : Sound.valueOf("ITEM_BREAK"), 1, 1);
            }
        }
    }

    private int getOldItemRadius(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String radiusFormat = INSTANCE.getConfig().getString("global-item-section.radius-format");

            if (radiusFormat == null) {
                INSTANCE.log(Level.WARNING, "Your radius format in the configuration is invalid!");
                return 1;
            }

            final String formattedFormat = ChatColor.stripColor(INSTANCE.getManager().colorText(radiusFormat)).replace("{radius}", "%radius%");
            String[] radiusFormatArgs = formattedFormat.split("%radius%");

            int gatheredRadiusCount = 0;
            boolean radiusAreaMode = INSTANCE.getConfig().getBoolean("global-item-section.radius-area-mode");

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
                        } else if (radiusFormatArgs.length == 1) {
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
                        else if (radiusFormatArgs.length == 1)
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

    private double getOldItemModifier(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String modifierFormat = INSTANCE.getConfig().getString("global-item-section.modifier-format");

            if (modifierFormat == null) {
                INSTANCE.log(Level.WARNING, "Your modifier format in the configuration is invalid!");
                return 1;
            }

            final String formattedFormat = ChatColor.stripColor(INSTANCE.getManager().colorText(modifierFormat)).replace("{modifier}", "%modifier%");
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

    private int getOldBlocksBroken(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.hasItemMeta() && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String blockCountFormat = INSTANCE.getConfig().getString("global-item-section.block-count-format");

            if (blockCountFormat == null) {
                INSTANCE.log(Level.WARNING, "Your block count format in the configuration is invalid!");
                return 0;
            }

            final String formattedFormat = ChatColor.stripColor(INSTANCE.getManager().colorText(blockCountFormat)).replace("{count}", "%count%");
            String[] usesFormatArgs = formattedFormat.split("%count%");
            for (int i = -1; ++i < lore.size(); ) {
                String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
                try {
                    if (usesFormatArgs.length >= 2)
                        return Integer.parseInt(
                                formattedLine.replace(usesFormatArgs[0], "").replace(usesFormatArgs[1], ""));
                    else if (usesFormatArgs.length == 1)
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

    public int getOldItemsSold(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = itemMeta.getLore();
            final String sellFormat = INSTANCE.getConfig().getString("global-item-section.sell-format");

            if (sellFormat == null) {
                INSTANCE.log(Level.WARNING, "Your sell format in the configuration is invalid!");
                return 0;
            }

            final String formattedFormat = ChatColor.stripColor(INSTANCE.getManager().colorText(sellFormat)).replace("{count}", "%count%");
            String[] formatArgs = formattedFormat.split("%count%");
            for (int i = -1; ++i < lore.size(); ) {
                String line = lore.get(i), formattedLine = ChatColor.stripColor(line);
                try {
                    if (formatArgs.length >= 2)
                        return Integer.parseInt(formattedLine.replace(formatArgs[0], "").replace(formatArgs[1], ""));
                    else if (formatArgs.length == 1) return Integer.parseInt(formattedLine.replace(formatArgs[0], ""));
                    else return Integer.parseInt(formattedLine);
                } catch (Exception ignored) {
                }
            }
        }

        return 0;
    }

    public int getUses() {return uses;}

    public void setUses(int uses) {this.uses = uses;}

    public int getRadius() {return radius;}

    public void setRadius(int radius) {this.radius = radius;}

    public double getModifier() {return modifier;}

    public void setModifier(double modifier) {this.modifier = modifier;}

    public long getBlocksBroken() {return blocksBroken;}

    public void setBlocksBroken(long blocksBroken) {this.blocksBroken = blocksBroken;}

    public long getSoldItems() {return soldItems;}

    public void setSoldItems(long soldItems) {this.soldItems = soldItems;}

    public String getWpTypeName() {return wpTypeName;}

    public NBTItem getNbtItem() {return nbtItem;}

    @Override
    public String toString() {
        WPType wpType = WPType.getType(getWpTypeName());
        return (getWpTypeName() + " - [" + (wpType.hasUses() ? ("Uses: " + getUses()) : "")
                + (wpType.hasRadius() ? (" | Radius: " + getRadius()) : "")
                + (wpType.hasModifier() ? (" | Modifier: " + getModifier()) : "")
                + (wpType.hasBlockCount() ? (" | Blocks Broken: " + getBlocksBroken()) : "")
                + (wpType.hasSellCount() ? (" | Items Sold: " + getSoldItems()) : "") + "]");
    }
}