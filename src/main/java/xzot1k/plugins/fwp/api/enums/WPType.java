/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.api.enums;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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