/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.api.enums;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.fwp.FactionWP;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum WPType {
    SAND_WAND(true, false, false, false, false), CRAFT_WAND(true, false, false, false, false),
    SELL_WAND(true, false, true, false, true), LIGHTNING_WAND(true, false, false, false, false),
    SMELT_WAND(true, false, false, false, false), HARVESTER_HOE(true, false, true, false, true),
    HARVESTER_AXE(true, false, true, true, true), ICE_WAND(true, true, false, false, false),
    TRAY_PICKAXE(true, true, false, true, false), TRENCH_PICKAXE(true, true, false, true, false),
    TRENCH_SHOVEL(true, true, false, true, false), WALL_WAND(true, true, false, false, false),
    PLATFORM_WAND(true, true, false, false, false), PROJECTILE_WAND(true, false, false, false, false),
    MULTI_TOOL(true, false, false, false, false), TNT_WAND(true, false, false, false, false),
    SPAWNER_PICKAXE(true, false, false, true, false);

    private final boolean uses, radius, modifier, blockCount, sellCount;

    WPType(boolean uses, boolean radius, boolean modifier, boolean blockCount, boolean sellCount) {
        this.uses = uses;
        this.radius = radius;
        this.modifier = modifier;
        this.blockCount = blockCount;
        this.sellCount = sellCount;
    }

    public static WPType getType(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() && itemStack.getItemMeta().hasLore()) {
            for (WPType wpType : WPType.values()) {
                String typeIdString = wpType.name().toLowerCase().replace("_", "-");

                String displayName = FactionWP.getPluginInstance().getManager().colorText(FactionWP.getPluginInstance()
                        .getConfig().getString(typeIdString + "-section.item.display-name"));
                Material material = Material.getMaterial(Objects.requireNonNull(FactionWP.getPluginInstance()
                                .getConfig().getString(typeIdString + "-section.item.material"))
                        .toUpperCase().replace(" ", "_").replace("-", "_"));
                int durability = FactionWP.getPluginInstance().getConfig().getInt(typeIdString + "-section.item.durability");

                if (((wpType == WPType.MULTI_TOOL || itemStack.getType() == material) && (itemStack.getDurability() == durability || durability == -1))
                        && (displayName.startsWith(itemStack.getItemMeta().getDisplayName()) || displayName.contains(itemStack.getItemMeta().getDisplayName())
                        || displayName.equals(itemStack.getItemMeta().getDisplayName()))
                        && FactionWP.getPluginInstance().getManager().enchantmentsMatch(itemStack, FactionWP.getPluginInstance().getConfig()
                        .getStringList(typeIdString + "-section.item.enchantments"))) return wpType;
            }
        }
        return null;
    }

    public static WPType getType(@NotNull String type) {
        String typeFormatted = type.toUpperCase().replace(" ", "_").replace("-", "_");
        Optional<WPType> wpTypeOptional = Arrays.stream(values()).parallel().filter(wpType -> typeFormatted.equals(wpType.name())).findFirst();
        return wpTypeOptional.orElse(null);
    }

    public boolean hasUses() {
        return uses;
    }

    public boolean hasRadius() {
        return radius;
    }

    public boolean hasModifier() {
        return modifier;
    }

    public boolean hasBlockCount() {
        return blockCount;
    }

    public boolean hasSellCount() {
        return sellCount;
    }
}