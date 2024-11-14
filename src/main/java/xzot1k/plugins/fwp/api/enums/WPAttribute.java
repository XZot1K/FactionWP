package xzot1k.plugins.fwp.api.enums;

import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.fwp.FactionWP;

public enum WPAttribute {

    USES("{uses}"), RADIUS("{radius}"), MODIFIER("{modifier}"),
    BLOCK_COUNT("{count}"), ITEMS_SOLD("{count}");

    private final String placeholder;

    WPAttribute(@NotNull String placeholder) {
        this.placeholder = placeholder;
    }

    public String getFormat() {
        if (this == ITEMS_SOLD) return FactionWP.getPluginInstance().getConfig().getString("global-item-section.sell-format");
        else return FactionWP.getPluginInstance().getConfig().getString("global-item-section."
                + name().toLowerCase().replace("_", "-") + "-format");
    }

    public String getPlaceholder() {return placeholder.replace("{", "[{]").replace("}", "[}]");}

}