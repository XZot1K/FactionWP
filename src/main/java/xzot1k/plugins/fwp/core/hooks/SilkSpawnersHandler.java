package xzot1k.plugins.fwp.core.hooks;

import de.dustplanet.silkspawners.SilkSpawners;
import de.dustplanet.util.SilkUtil;
import org.bukkit.plugin.Plugin;
import xzot1k.plugins.fwp.FactionWP;

public class SilkSpawnersHandler {

    private final FactionWP instance;
    private SilkUtil silkUtil;
    private SilkSpawners silkSpawners;

    public SilkSpawnersHandler(FactionWP instance, Plugin silkSpawners) {
        this.instance = instance;
        SilkSpawners plugin = (SilkSpawners) silkSpawners;
        if (plugin != null) {
            this.silkSpawners = plugin;
            silkUtil = new SilkUtil(plugin);
        }
    }

    public SilkUtil getSilkUtil() {return silkUtil;}

    public SilkSpawners getSilkSpawners() {return silkSpawners;}

}