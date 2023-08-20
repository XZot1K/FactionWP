/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xzot1k.plugins.fwp.api.Manager;
import xzot1k.plugins.fwp.core.Commands;
import xzot1k.plugins.fwp.core.Listeners;
import xzot1k.plugins.fwp.core.TabCompleter;
import xzot1k.plugins.fwp.core.hooks.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.logging.Level;

public class FactionWP extends JavaPlugin {

    private static FactionWP pluginInstance;
    private String serverVersion;
    private Manager manager;
    private boolean prismaInstalled, essentialsInstalled, commandPanelsInstalled;

    private FileConfiguration langConfig, dataConfig;
    private File langFile, dataFile;

    private VaultHandler vaultHandler;
    private WorldGuardHandler worldGuardHandler;
    private ShopGUIPlusHandler shopGUIPlusHandler;
    private SilkSpawnersHandler silkSpawnersHandler;

    @Override
    public void onEnable() {
        pluginInstance = this;
        setServerVersion(getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3]);
        setPrismaInstalled(getServer().getPluginManager().getPlugin("Prisma") != null);
        this.essentialsInstalled = (getServer().getPluginManager().getPlugin("Essentials") != null);

        File file = new File(getDataFolder(), "/config.yml");
        if (file.exists()) {
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection cs = yaml.getConfigurationSection("");
            if (cs != null && cs.contains("language-section"))
                file.renameTo(new File(getDataFolder(), "/old-config.yml"));
        }

        saveDefaultConfigs();
        updateConfigs();
        manager = new Manager(this);

        log(Level.INFO, "Setting up the plugin's internal requisites...");
        if (getConfig().getBoolean("general-section.use-vault")) {
            setVaultHandler(new VaultHandler(this));
            if (!getVaultHandler().setupEconomy()) {
                getServer().getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        PluginCommand command = getCommand("factionwp");
        if (command != null) {
            command.setExecutor(new Commands(getPluginInstance()));
            command.setTabCompleter(new TabCompleter(getPluginInstance()));
        }

        getServer().getPluginManager().registerEvents(new Listeners(this), this);

        Plugin worldGuard = getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuard != null) setWorldGuardHandler(new WorldGuardHandler());

        if (getServer().getPluginManager().getPlugin("ShopGUIPlus") != null)
            shopGUIPlusHandler = new ShopGUIPlusHandler(this);

        if (getServer().getPluginManager().getPlugin("mcMMO") != null && getConfig().getBoolean("general-section.support-mcmmo"))
            getServer().getPluginManager().registerEvents(new McmmoListener(this), this);

        Plugin silkSpawners = getServer().getPluginManager().getPlugin("SilkSpawners");
        if (silkSpawners != null && silkSpawners.getDescription().getMain().equals("de.dustplanet.silkspawners.SilkSpawners"))
            silkSpawnersHandler = new SilkSpawnersHandler(this, silkSpawners);

        this.commandPanelsInstalled = (getServer().getPluginManager().getPlugin("CommandPanels") != null);
        // loadSpawnerLocations();

        log(Level.INFO, "Version " + getDescription().getVersion() + " has been successfully loaded!");

        if (isOutdated())
            log(Level.INFO, "HEY YOU! A new version of the plugin is on the page (" + getLatestVersion() + ")!");
        else log(Level.INFO, "Everything seems to be up to date!");
    }

    // update checker methods
    private boolean isOutdated() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=73499").openConnection();
            c.setRequestMethod("GET");
            String oldVersion = getDescription().getVersion(),
                    newVersion = new BufferedReader(new InputStreamReader(c.getInputStream())).readLine();
            if (!newVersion.equalsIgnoreCase(oldVersion))
                return true;
        } catch (Exception ignored) {}
        return false;
    }

    private String getLatestVersion() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=73499").openConnection();
            c.setRequestMethod("GET");
            return new BufferedReader(new InputStreamReader(c.getInputStream())).readLine();
        } catch (Exception ex) {return getDescription().getVersion();}
    }

    public void log(Level level, String text) {getServer().getLogger().log(level, "[" + getDescription().getName() + "] " + text);}

    /**
     * Reloads all configs.
     */
    public void reloadConfigs() {
        reloadConfig();

        if (langFile == null) langFile = new File(getDataFolder(), "lang.yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream inputStream = this.getResource("lang.yml");
        if (inputStream == null) return;

        Reader defConfigStream = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
        langConfig.setDefaults(defConfig);

        try {
            defConfigStream.close();
        } catch (IOException e) {log(Level.WARNING, e.getMessage());}

        if (dataFile == null) dataFile = new File(getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        inputStream = this.getResource("data.yml");
        if (inputStream == null) return;

        defConfigStream = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
        langConfig.setDefaults(defConfig);

        try {
            defConfigStream.close();
        } catch (IOException e) {log(Level.WARNING, e.getMessage());}
    }

    /**
     * Gets the language file configuration.
     *
     * @return The FileConfiguration found.
     */
    public FileConfiguration getLangConfig() {
        if (langConfig == null) reloadConfigs();
        return langConfig;
    }

    /**
     * Saves the default configuration files (Doesn't replace existing).
     */
    public void saveDefaultConfigs() {
        saveDefaultConfig();
        if (langFile == null) langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) saveResource("lang.yml", false);
        if (dataConfig == null) dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) saveResource("data.yml", false);
        reloadConfigs();
    }

    public void saveLangConfig() {
        if (langConfig == null || langFile == null) return;
        try {
            getLangConfig().save(langFile);
        } catch (IOException e) {
            log(Level.WARNING, e.getMessage());
        }
    }

    public FileConfiguration getDataConfig() {
        if (dataConfig == null) reloadConfigs();
        return dataConfig;
    }

    public void saveDataConfig() {
        if (dataConfig == null || dataFile == null) return;
        try {
            getDataConfig().save(dataFile);
        } catch (IOException e) {
            log(Level.WARNING, e.getMessage());
        }
    }

    private void updateConfigs() {
        long startTime = System.currentTimeMillis();
        int totalUpdates = 0;

        String[] configNames = {"config", "lang"};
        for (int i = -1; ++i < configNames.length; ) {
            String name = configNames[i];

            InputStream inputStream = getClass().getResourceAsStream("/" + name + ".yml");
            if (inputStream == null) continue;

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
            int updateCount = updateKeys(yaml, name.equalsIgnoreCase("config") ? getConfig() : getLangConfig());

            if (name.equalsIgnoreCase("config")) {
                String itemRemoveSound = getConfig().getString("immersion-section.item-removal-sound");
                if (itemRemoveSound != null && itemRemoveSound.equalsIgnoreCase("ENTITY_ITEM_BREAK") && getServerVersion().startsWith("v1_8")) {
                    getConfig().set("immersion-section.item-removal-sound", "ITEM_BREAK");
                    updateCount++;
                } else if (itemRemoveSound != null && itemRemoveSound.equalsIgnoreCase("ITEM_BREAK") && !getServerVersion().startsWith("v1_8")) {
                    getConfig().set("immersion-section.item-removal-sound", "ENTITY_ITEM_BREAK");
                    updateCount++;
                }

                String blockDestroySound = getConfig().getString("immersion-section.block-destroy-sound");
                if (blockDestroySound != null && blockDestroySound.equalsIgnoreCase("BLOCK_STONE_BREAK") && getServerVersion().startsWith("v1_8")) {
                    getConfig().set("immersion-section.block-destroy-sound", "STEP_STONE");
                    updateCount++;
                } else if (blockDestroySound != null && blockDestroySound.equalsIgnoreCase("STEP_STONE") && !getServerVersion().startsWith("v1_8")) {
                    getConfig().set("immersion-section.block-destroy-sound", "BLOCK_STONE_BREAK");
                    updateCount++;
                }

                String trenchShovelMaterial = getConfig().getString("trench-shovel-section.item.material");
                if (trenchShovelMaterial != null && trenchShovelMaterial.toUpperCase().endsWith("_SPADE")
                        && !(getServerVersion().startsWith("v1_8") || getServerVersion().startsWith("v1_9") || getServerVersion().startsWith("v1_10")
                        || getServerVersion().startsWith("v1_11") || getServerVersion().startsWith("v1_12"))) {
                    getConfig().set("trench-shovel-section.item.material", trenchShovelMaterial.toUpperCase().replace("_SPADE", "_SHOVEL"));
                    updateCount++;
                } else if (trenchShovelMaterial != null && trenchShovelMaterial.toUpperCase().endsWith("_SHOVEL")
                        && (getServerVersion().startsWith("v1_8") || getServerVersion().startsWith("v1_9") || getServerVersion().startsWith("v1_10")
                        || getServerVersion().startsWith("v1_11") || getServerVersion().startsWith("v1_12"))) {
                    getConfig().set("trench-shovel-section.shovel-item.material", trenchShovelMaterial.toUpperCase().replace("_SHOVEL", "_SPADE"));
                    updateCount++;
                }
            }

            try {
                inputStream.close();
                reader.close();
            } catch (IOException e) {
                log(Level.WARNING, e.getMessage());
            }

            if (updateCount > 0)
                switch (name) {
                    case "config":
                        saveConfig();
                        break;
                    case "lang":
                        saveLangConfig();
                        break;
                    default:
                        break;
                }

            if (updateCount > 0) {
                totalUpdates += updateCount;
                log(Level.INFO,
                        updateCount + " things were fixed, updated, or removed in the '" + name + ".yml' configuration file. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
            }
        }

        if (totalUpdates > 0) {
            reloadConfigs();
            log(Level.INFO,
                    "A total of " + totalUpdates + " thing(s) were fixed, updated, or removed from all the configuration together. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
            log(Level.WARNING, "Please go checkout the configuration files as they are no longer the same as their default counterparts.");
        } else
            log(Level.INFO, "Everything inside the configuration seems to be up to date. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    private int updateKeys(FileConfiguration jarYaml, FileConfiguration currentYaml) {
        int updateCount = 0;
        ConfigurationSection currentConfigurationSection = currentYaml.getConfigurationSection(""),
                latestConfigurationSection = jarYaml.getConfigurationSection("");
        if (currentConfigurationSection != null && latestConfigurationSection != null) {
            Set<String> newKeys = latestConfigurationSection.getKeys(true),
                    currentKeys = currentConfigurationSection.getKeys(true);
            for (String updatedKey : newKeys)
                if (!currentKeys.contains(updatedKey)) {
                    currentYaml.set(updatedKey, jarYaml.get(updatedKey));
                    updateCount++;
                }

            for (String currentKey : currentKeys)
                if (!newKeys.contains(currentKey)) {
                    currentYaml.set(currentKey, null);
                    updateCount++;
                }
        }

        return updateCount;
    }

  /*  public void loadSpawnerLocations() {
        String jsonString = "";

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("spawner-locations.json"))) {
            jsonString = bufferedReader.readLine();
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException)) e.printStackTrace();
            log(Level.INFO, "Unable to read from the \"spawner-locations.json\" file.");
        }

        spawnerLocation = new JSONObject(jsonString);
    }

    private boolean containsEntry(@NotNull JSONArray jsonArray, @NotNull String entry) {
        for (int i = -1; ++i < jsonArray.length(); ) {
            final String jsonEntry = jsonArray.getString(i);
            if (jsonEntry.equals(entry)) return true;
        }
        return false;
    }

    private int containsEntryIndex(@NotNull JSONArray jsonArray, @NotNull String entry) {
        for (int i = -1; ++i < jsonArray.length(); ) {
            final String jsonEntry = jsonArray.getString(i);
            if (jsonEntry.equals(entry)) return i;
        }
        return -1;
    }

    public void addSpawnerLocation(@NotNull Location location) {

        final int chunkX = (int) Math.floor((double) location.getBlockX() / 16),
                chunkZ = (int) Math.floor((double) location.getBlockZ() / 16);
        final String worldName = Objects.requireNonNull(location.getWorld()).getName(),
                chunkName = (chunkX + ":" + chunkZ),
                locationName = (location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());

        if (!getSpawnerLocation().has(worldName)) {

            // new world object and json array
            JSONObject worldObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(locationName);
            worldObject.put(chunkName, jsonArray);
            getSpawnerLocation().put(worldName, worldObject);
            saveSpawnerLocations(true);

        } else {
            JSONObject worldObject = getSpawnerLocation().getJSONObject(worldName);

            if (worldObject.has(chunkName)) {
                JSONArray jsonArray = worldObject.getJSONArray(chunkName);
                if (containsEntry(jsonArray, locationName)) return;

                jsonArray.put(locationName);
                worldObject.put(chunkName, jsonArray);
                getSpawnerLocation().put(worldName, worldObject);
                saveSpawnerLocations(true);
            } else {
                // new json array w/ new location added
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(locationName);
                worldObject.put(chunkName, jsonArray);
                getSpawnerLocation().put(worldName, worldObject);
                saveSpawnerLocations(true);
            }
        }
    }

    public void removeSpawnerLocation(@NotNull Location location) {

        final int chunkX = (int) Math.floor((double) location.getBlockX() / 16),
                chunkZ = (int) Math.floor((double) location.getBlockZ() / 16);
        final String worldName = Objects.requireNonNull(location.getWorld()).getName(),
                chunkName = (chunkX + ":" + chunkZ),
                locationName = (location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());

        if (!getSpawnerLocation().has(worldName)) return;

        JSONObject worldObject = getSpawnerLocation().getJSONObject(worldName);
        if (!worldObject.has(chunkName)) return;

        int index;
        JSONArray jsonArray = worldObject.getJSONArray(chunkName);
        if ((index = containsEntryIndex(jsonArray, locationName)) != -1) return;

        jsonArray.remove(index);
        worldObject.put(chunkName, jsonArray);
        getSpawnerLocation().put(worldName, worldObject);

        saveSpawnerLocations(true);
    }

    public void saveSpawnerLocations(boolean async) {
        if (async) getServer().getScheduler().runTaskAsynchronously(this, () -> saveSpawnerLocations());
        else saveSpawnerLocations();
    }

    private void saveSpawnerLocations() {
        try (FileWriter file = new FileWriter("spawner-locations.json")) {
            file.write(getSpawnerLocation().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    // getters and setters
    public Manager getManager() {return manager;}

    public static FactionWP getPluginInstance() {return pluginInstance;}

    public String getServerVersion() {return serverVersion;}

    private void setServerVersion(String serverVersion) {this.serverVersion = serverVersion;}

    public VaultHandler getVaultHandler() {return vaultHandler;}

    private void setVaultHandler(VaultHandler vaultHandler) {this.vaultHandler = vaultHandler;}

    public WorldGuardHandler getWorldGuardHandler() {return worldGuardHandler;}

    public void setWorldGuardHandler(WorldGuardHandler worldGuardHandler) {this.worldGuardHandler = worldGuardHandler;}

    public boolean isPrismaInstalled() {return prismaInstalled;}

    private void setPrismaInstalled(boolean prismaInstalled) {this.prismaInstalled = prismaInstalled;}

    public ShopGUIPlusHandler getShopGUIPlusHandler() {return shopGUIPlusHandler;}

    public boolean isEssentialsInstalled() {return essentialsInstalled;}

    public SilkSpawnersHandler getSilkSpawnersHandler() {return silkSpawnersHandler;}

    public boolean isCommandPanelsInstalled() {return commandPanelsInstalled;}

}