/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import xzot1k.plugins.fwp.FactionWP;
import xzot1k.plugins.fwp.api.enums.WPType;
import xzot1k.plugins.fwp.api.events.MultiToolSwapEvent;
import xzot1k.plugins.fwp.api.events.ToolUseEvent;
import xzot1k.plugins.fwp.core.utils.FunctionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class Listeners implements Listener {

    private final FactionWP pluginInstance;
    private final FunctionUtil functionUtil;

    public Listeners(FactionWP pluginInstance) {
        this.pluginInstance = pluginInstance;
        functionUtil = new FunctionUtil(pluginInstance);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractWithEntity(PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getType() == EntityType.CREEPER) {
            Creeper creeper = (Creeper) e.getRightClicked();
            if (!creeper.isPowered()) {

                ItemStack itemStack = pluginInstance.getManager().getHandItem(e.getPlayer());
                if (itemStack == null) return;

                final WPType wpType = pluginInstance.getManager().getWPItemType(itemStack);
                if (wpType != WPType.LIGHTNING_WAND) return;

                checkSpartan(e.getPlayer());
                ToolUseEvent toolUseEvent = new ToolUseEvent(e.getPlayer(), creeper.getLocation(), WPType.LIGHTNING_WAND, pluginInstance.getManager().getHandItem(e.getPlayer()));
                pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                if (!toolUseEvent.isCancelled()) getFunctionUtil().performLightningWandMagic(e.getPlayer(), creeper);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {

        if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getItem() != null && e.getClickedBlock() != null) {
            final WPType wpType = pluginInstance.getManager().getWPItemType(e.getItem());
            if (wpType != WPType.MULTI_TOOL) return;

            final Material newMaterial = pluginInstance.getManager().getMultiToolSwapMaterial(e.getItem(), e.getClickedBlock());
            if (newMaterial == null || newMaterial == e.getItem().getType()) return;

            checkSpartan(e.getPlayer());

            MultiToolSwapEvent swapEvent = new MultiToolSwapEvent(e.getPlayer(), e.getClickedBlock().getLocation(),
                    pluginInstance.getManager().getHandItem(e.getPlayer()));
            pluginInstance.getServer().getPluginManager().callEvent(swapEvent);
            if (!swapEvent.isCancelled()) {
                e.getItem().setType(newMaterial);
                e.getPlayer().updateInventory();

                if (!pluginInstance.getConfig().getBoolean("multi-tool-section.alternative-durability-usage"))
                    pluginInstance.getManager().removeItemUses(e.getPlayer(), e.getItem(), 1);
            }

        } else if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && e.getItem() != null) {
            final WPType wpType = pluginInstance.getManager().getWPItemType(e.getItem());
            if (wpType == null) return;

            checkSpartan(e.getPlayer());

            List<String> commandList = pluginInstance.getConfig().getStringList(wpType.name().toLowerCase().replace("_", "-") + "-section.commands");
            if (!commandList.isEmpty()) for (String commandLine : commandList) {
                if (commandLine.contains(":")) {
                    String[] args = commandLine.split(":");
                    if (args.length > 1) {
                        switch (args[1].toLowerCase()) {
                            case "player": {
                                pluginInstance.getServer().dispatchCommand(e.getPlayer(), commandLine.replace("{player}", e.getPlayer().getName())
                                        .replace("{uuid}", e.getPlayer().getUniqueId().toString()));
                                continue;
                            }
                            case "chat": {
                                e.getPlayer().chat(commandLine.replace("{player}", e.getPlayer().getName())
                                        .replace("{uuid}", e.getPlayer().getUniqueId().toString()));
                                continue;
                            }
                            default: {break;}
                        }
                    }
                }

                pluginInstance.getServer().dispatchCommand(pluginInstance.getServer().getConsoleSender(),
                        commandLine.replace("{player}", e.getPlayer().getName()).replace("{uuid}", e.getPlayer().getUniqueId().toString()));
            }

            ToolUseEvent toolUseEvent;
            switch (wpType) {

                case SPAWNER_PICKAXE: {
                    e.setCancelled(true);
                    pluginInstance.getManager().toggleSpawnerPickaxeMode(e.getPlayer());
                    break;
                }

                case HARVESTER_HOE:
                case HARVESTER_AXE:

                    if ((wpType == WPType.HARVESTER_HOE && e.getClickedBlock() != null && (e.getClickedBlock().getType().name().contains("GRASS")
                            || e.getClickedBlock().getType().name().contains("DIRT")))
                            || (wpType == WPType.HARVESTER_AXE && e.getClickedBlock() != null && e.getClickedBlock().getType().name().contains("LOG")))
                        return;

                    e.setCancelled(true);

                    if (!pluginInstance.getConfig().getBoolean("harvester-" + (wpType == WPType.HARVESTER_HOE ? "hoe" : "axe")
                            + "-section.custom-item-currency") && !pluginInstance.getConfig().getBoolean("general-section.use-vault")) {
                        pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "mode-switch-fail");
                        return;
                    }

                    pluginInstance.getManager().toggleSellMode(e.getPlayer(), wpType);
                    break;

                case WALL_WAND:
                    e.setCancelled(true);
                    getFunctionUtil().performWallWandMagic(e.getPlayer(), e.getItem());
                    break;

                case PLATFORM_WAND:
                    e.setCancelled(true);
                    getFunctionUtil().performPlatformWandMagic(e.getPlayer(), e.getItem());
                    break;

                case PROJECTILE_WAND:
                    e.setCancelled(true);
                    getFunctionUtil().performProjectileWandMagic(e.getPlayer(), e.getItem());
                    break;

                case SAND_WAND:
                    if (e.getClickedBlock() != null) {
                        e.setCancelled(true);
                        toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getClickedBlock().getLocation(), WPType.SAND_WAND,
                                pluginInstance.getManager().getHandItem(e.getPlayer()));
                        pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                        if (!toolUseEvent.isCancelled())
                            getFunctionUtil().performSandWandMagic(e.getPlayer(), e.getClickedBlock(), e.getItem());
                    }
                    break;

                case CRAFT_WAND:
                    if (e.getClickedBlock() != null) {
                        e.setCancelled(true);
                        toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getClickedBlock().getLocation(), WPType.CRAFT_WAND,
                                pluginInstance.getManager().getHandItem(e.getPlayer()));
                        pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                        if (!toolUseEvent.isCancelled())
                            getFunctionUtil().performCraftWandMagic(e.getPlayer(), e.getClickedBlock(), e.getItem());
                    }
                    break;

                case SELL_WAND:
                    if (e.getClickedBlock() != null) {
                        e.setCancelled(true);
                        toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getClickedBlock().getLocation(), WPType.SELL_WAND,
                                pluginInstance.getManager().getHandItem(e.getPlayer()));
                        pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                        if (!toolUseEvent.isCancelled()) {
                            if (!pluginInstance.getConfig().getBoolean("sell-wand-section.custom-item-currency")
                                    && !pluginInstance.getConfig().getBoolean("general-section.use-vault")) {
                                pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "mode-switch-fail");
                                return;
                            }

                            getFunctionUtil().performSellWandMagic(e.getPlayer(), e.getClickedBlock(), e.getItem());
                        }
                    }
                    break;

                case SMELT_WAND:
                    if (e.getClickedBlock() != null) {
                        e.setCancelled(true);
                        toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getClickedBlock().getLocation(), WPType.SMELT_WAND,
                                pluginInstance.getManager().getHandItem(e.getPlayer()));
                        pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                        if (!toolUseEvent.isCancelled())
                            getFunctionUtil().performSmeltWandMagic(e.getPlayer(), e.getClickedBlock(), e.getItem());
                    }
                    break;

                case ICE_WAND:
                    if (e.getClickedBlock() != null) {
                        e.setCancelled(true);
                        toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getClickedBlock().getLocation(), WPType.ICE_WAND,
                                pluginInstance.getManager().getHandItem(e.getPlayer()));
                        pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                        if (!toolUseEvent.isCancelled())
                            getFunctionUtil().performIceWandMagic(e.getPlayer(), e.getClickedBlock(), e.getItem());
                    }
                    break;

                case TNT_WAND:
                    if (e.getClickedBlock() != null) {
                        e.setCancelled(true);
                        toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getClickedBlock().getLocation(), WPType.TNT_WAND,
                                pluginInstance.getManager().getHandItem(e.getPlayer()));
                        pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                        if (!toolUseEvent.isCancelled())
                            getFunctionUtil().performTNTWandMagic(e.getPlayer(), e.getClickedBlock(), e.getItem());
                    }
                    break;

                default:
                    break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        FactionWP.getPluginInstance().getManager().getSellModeMap().remove(e.getPlayer().getUniqueId());
        FactionWP.getPluginInstance().getManager().getSpawnerPickaxeModeMap().remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageMulti(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            Player damager = (Player) e.getDamager();
            final ItemStack handItem = pluginInstance.getManager().getHandItem(damager);
            final WPType wpType = pluginInstance.getManager().getWPItemType(handItem);
            if (wpType == WPType.MULTI_TOOL) {
                checkSpartan(damager);
                pluginInstance.getManager().removeItemUses(damager, handItem, 1);
                final Material newMaterial = Material.getMaterial(handItem.getType().name().split("_")[0] + "_SWORD");
                if (newMaterial != null) {
                    MultiToolSwapEvent swapEvent = new MultiToolSwapEvent(damager, e.getEntity().getLocation(),
                            pluginInstance.getManager().getHandItem(damager));
                    pluginInstance.getServer().getPluginManager().callEvent(swapEvent);
                    if (!swapEvent.isCancelled()) {
                        handItem.setType(newMaterial);
                        damager.updateInventory();
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageProj(EntityDamageByEntityEvent e) {
        if (e.getDamager().getCustomName() != null && e.getDamager().getCustomName().startsWith("FactionWP/Projectile_Wand-Entity/")) {

            if (pluginInstance.getWorldGuardHandler() != null && e.getEntity() instanceof Player) {
                final Player player = (Player) e.getEntity();
                if (!pluginInstance.getWorldGuardHandler().regionCheckFlags(player, player.getLocation(),
                        new com.sk89q.worldguard.protection.flags.StateFlag("INVINCIBLE", true))) e.setCancelled(true);
            }

            if (!pluginInstance.getConfig().getBoolean("projectile-wand-section.deal-damage")
                    || e.getDamager().getCustomName().endsWith(e.getEntity().getUniqueId().toString())) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHit(ProjectileHitEvent e) {
        if (((pluginInstance.getServerVersion().startsWith("v1_12") || pluginInstance.getServerVersion().startsWith("v1_13")
                || pluginInstance.getServerVersion().startsWith("v1_14") || pluginInstance.getServerVersion().startsWith("v1_15"))
                ? e.getHitBlock() != null : e.getEntity().isOnGround()) && e.getEntity().getCustomName() != null
                && e.getEntity().getCustomName().startsWith("FactionWP/Projectile_Wand-Entity/"))
            e.getEntity().remove();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplode(EntityExplodeEvent e) {
        if (e.getEntity().getCustomName() != null && e.getEntity().getCustomName().startsWith("FactionWP/Projectile_Wand-Entity/")) {
            e.setYield(0);
            e.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void durabilityChange(PlayerItemDamageEvent e) {
        final WPType wpType = pluginInstance.getManager().getWPItemType(e.getItem());
        if (wpType == null) return;

        e.setCancelled(true);
        e.getItem().setDurability((short) 0);
    }

   /* @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if(FactionWP.getPluginInstance().getSilkSpawnersHandler() == null) {

        }
    }*/

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        ItemStack handItemStack = pluginInstance.getManager().getHandItem(e.getPlayer());
        if (handItemStack == null) return;

        final WPType wpType = pluginInstance.getManager().getWPItemType(handItemStack);
        if (wpType == null) return;
        else if (wpType == WPType.MULTI_TOOL) {
            if (pluginInstance.getConfig().getBoolean("multi-tool-section.alternative-durability-usage"))
                pluginInstance.getManager().removeItemUses(e.getPlayer(), pluginInstance.getManager().getHandItem(e.getPlayer()), 1);
            return;
        }

        e.setCancelled(true);
        handItemStack.setDurability((short) 0);
        e.getPlayer().updateInventory();

        ToolUseEvent toolUseEvent;
        switch (wpType) {

            case SPAWNER_PICKAXE:
                toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getBlock().getLocation(), WPType.SPAWNER_PICKAXE, pluginInstance.getManager().getHandItem(e.getPlayer()));
                pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                if (!toolUseEvent.isCancelled())
                    getFunctionUtil().performSpawnerPickaxeMagic(e.getPlayer(), e.getBlock(), handItemStack);
                break;

            case TRENCH_PICKAXE:
                toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getBlock().getLocation(), WPType.TRENCH_PICKAXE, pluginInstance.getManager().getHandItem(e.getPlayer()));
                pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                if (!toolUseEvent.isCancelled())
                    getFunctionUtil().performTrenchPickaxeMagic(e.getPlayer(), e.getBlock(), handItemStack);
                break;

            case TRENCH_SHOVEL:
                toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getBlock().getLocation(), WPType.TRENCH_SHOVEL, pluginInstance.getManager().getHandItem(e.getPlayer()));
                pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                if (!toolUseEvent.isCancelled())
                    getFunctionUtil().performTrenchShovelMagic(e.getPlayer(), e.getBlock(), handItemStack);
                break;

            case TRAY_PICKAXE:
                toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getBlock().getLocation(), WPType.TRAY_PICKAXE,
                        pluginInstance.getManager().getHandItem(e.getPlayer()));
                pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                if (!toolUseEvent.isCancelled())
                    getFunctionUtil().performTrayPickaxeMagic(e, e.getPlayer(), e.getBlock(), handItemStack);
                break;

            case HARVESTER_HOE:
                toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getBlock().getLocation(), WPType.HARVESTER_HOE, pluginInstance.getManager().getHandItem(e.getPlayer()));
                pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                if (!toolUseEvent.isCancelled())
                    getFunctionUtil().performHarvesterHoeMagic(e.getPlayer(), e.getBlock(), handItemStack);
                break;

            case HARVESTER_AXE:
                toolUseEvent = new ToolUseEvent(e.getPlayer(), e.getBlock().getLocation(), WPType.HARVESTER_AXE, pluginInstance.getManager().getHandItem(e.getPlayer()));
                pluginInstance.getServer().getPluginManager().callEvent(toolUseEvent);
                if (!toolUseEvent.isCancelled())
                    getFunctionUtil().performHarvesterAxeMagic(e.getPlayer(), e.getBlock(), handItemStack);
                break;

            default:
                break;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnchant(EnchantItemEvent e) {
        if (pluginInstance.getManager().isWPItem(e.getItem()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAnvilEnchant(InventoryClickEvent e) {
        if (e.getInventory() instanceof AnvilInventory) {
            if (e.getRawSlot() == 2 && (pluginInstance.getManager().isWPItem(e.getInventory().getItem(0))
                    || pluginInstance.getManager().isWPItem(e.getInventory().getItem(1))))
                e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        final String commandPrefix = pluginInstance.getConfig().getString("tnt-wand-section.withdraw-command"),
                storageCommandPrefix = pluginInstance.getConfig().getString("tnt-wand-section.storage-command");
        if (commandPrefix != null && !commandPrefix.isEmpty() && e.getMessage().toLowerCase().startsWith(commandPrefix.toLowerCase())) {
            e.setCancelled(true);

            final String factionName = pluginInstance.getManager().getFactionName(e.getPlayer());
            if (factionName == null || factionName.isEmpty()) {
                pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "faction-invalid-message");
                return;
            }

            ConfigurationSection dataSection = pluginInstance.getDataConfig().getConfigurationSection("");
            if (dataSection == null || (!dataSection.contains("tnt-storage") || !dataSection.contains("tnt-storage." + factionName.toLowerCase()))) {
                pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "tnt-insufficient-message");
                return;
            }

            String value = e.getMessage().toLowerCase().split(commandPrefix.toLowerCase() + " ")[1];

            final long foundValue = Long.parseLong(value), storage = dataSection.getLong("tnt-storage." + factionName.toLowerCase());
            if (foundValue <= 0) {
                pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "tnt-invalid-message",
                        ("{format}:" + commandPrefix.toLowerCase()));
                return;
            } else if (storage <= 0 || foundValue > storage) {
                pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "tnt-insufficient-message");
                return;
            }

            dataSection.set(("tnt-storage." + factionName.toLowerCase()), (storage - foundValue));
            pluginInstance.saveDataConfig();

            if (e.getPlayer().getInventory().firstEmpty() == -1)
                e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(),
                        new ItemStack(Material.TNT, Math.toIntExact(foundValue)));
            else e.getPlayer().getInventory().addItem(new ItemStack(Material.TNT, Math.toIntExact(foundValue)));
            pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "tnt-withdraw-message",
                    ("{amount}:" + foundValue));
        } else if (storageCommandPrefix != null && !storageCommandPrefix.isEmpty() && e.getMessage().toLowerCase().startsWith(storageCommandPrefix.toLowerCase())) {
            e.setCancelled(true);

            final String factionName = pluginInstance.getManager().getFactionName(e.getPlayer());
            if (factionName == null || factionName.isEmpty()) {
                pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "faction-invalid-message");
                return;
            }

            ConfigurationSection dataSection = pluginInstance.getDataConfig().getConfigurationSection("");
            if (dataSection == null || (!dataSection.contains("tnt-storage") || !dataSection.contains("tnt-storage." + factionName.toLowerCase()))) {
                pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "tnt-insufficient-message");
                return;
            }

            String value = e.getMessage().toLowerCase().split(storageCommandPrefix.toLowerCase() + " ")[1];
            pluginInstance.getManager().sendCustomMessage(e.getPlayer(), "tnt-balance-message",
                    ("{amount}:" + value));
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryClickEvent e) {
        if (e.getClick() == ClickType.DOUBLE_CLICK || !pluginInstance.getConfig().getBoolean("general-section.tool-merging")
                || !(e.getWhoClicked() instanceof Player)) return;

        if (e.getCursor() != null && e.getCurrentItem() != null && pluginInstance.getManager().isWPItem(e.getCursor())
                && pluginInstance.getManager().isWPItem(e.getCurrentItem())) {

            final WPType wpType1 = pluginInstance.getManager().getWPItemType(e.getCursor()), wpType2 = pluginInstance.getManager().getWPItemType(e.getCurrentItem());
            if (wpType2 == null || wpType1 != wpType2) return;

            if (pluginInstance.getManager().getItemRadius(e.getCurrentItem()) != pluginInstance.getManager().getItemRadius(e.getCursor())
                    || pluginInstance.getManager().getItemModifier(e.getCurrentItem()) != pluginInstance.getManager().getItemModifier(e.getCursor()))
                return;

            final int useCount1 = pluginInstance.getManager().getItemUses(e.getCursor()),
                    useCount2 = pluginInstance.getManager().getItemUses(e.getCurrentItem());
            if (useCount1 == -1 || useCount2 == -1) return;

            e.setCancelled(true);
            if (e.getCursor().getAmount() > 1) e.getCursor().setAmount(e.getCursor().getAmount() - 1);
            else e.setCursor(null);

            pluginInstance.getManager().addItemUses(e.getCurrentItem(), useCount1);
            ((Player) e.getWhoClicked()).updateInventory();
        }
    }

    // helping methods
    private void checkSpartan(Player player) {
        if (!pluginInstance.getConfig().getBoolean("general-section.cancel-spartan-click")) {
            Plugin plugin1 = pluginInstance.getServer().getPluginManager().getPlugin("SpartanAPI");
            if (plugin1 != null) {
                try {
                    Class<?> apiClass = Class.forName("me.vagdedes.spartan.api.API"),
                            hackTypeClass = Class.forName("me.vagdedes.spartan.system.Enums$HackType");

                    Method cancelCheckMethod = apiClass.getDeclaredMethod("cancelCheck", Player.class, hackTypeClass, Integer.class);
                    Field enumTypeField = hackTypeClass.getField("FastClicks");

                    cancelCheckMethod.invoke(null, player, enumTypeField, 40);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // getters & setters
    private FunctionUtil getFunctionUtil() {
        return functionUtil;
    }
}