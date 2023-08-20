/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core.utils;

import com.bgsoftware.wildchests.api.WildChestsAPI;
import de.dustplanet.silkspawners.SilkSpawners;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import xzot1k.plugins.fwp.FactionWP;
import xzot1k.plugins.fwp.api.enums.SpawnerPickaxeMode;
import xzot1k.plugins.fwp.api.enums.WPType;
import xzot1k.plugins.fwp.api.events.TransactionEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class FunctionUtil {

    /*
     * This class contains all general tool functions and how they operate for clean separation.
     */

    private final FactionWP pluginInstance;

    public FunctionUtil(FactionWP pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public void performLightningWandMagic(Player player, Creeper creeper) {
        ItemStack handItemStack = pluginInstance.getManager().getHandItem(player);
        if (pluginInstance.getManager().doesWPTypeMatchItem(WPType.LIGHTNING_WAND, handItemStack)) {
            if (!player.hasPermission("factionwp.lightningwand")) {
                pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
                return;
            }

            boolean useGlobalCooldown = pluginInstance.getConfig()
                    .getBoolean("lightning-wand-section" + ".use-global-cooldown");
            int cooldown = pluginInstance.getConfig().getInt("lightning-wand-section.cooldown");
            long cooldownRemainder;
            if (useGlobalCooldown) {
                cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
            } else {
                cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.LIGHTNING_WAND,
                        cooldown);
            }
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }

            creeper.getWorld().strikeLightningEffect(creeper.getLocation());
            creeper.setFireTicks(0);
            creeper.setPowered(true);

            pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
            if (cooldown > -1)
                if (!useGlobalCooldown)
                    pluginInstance.getManager().updateWPCooldown(player, WPType.LIGHTNING_WAND);
                else
                    pluginInstance.getManager().updateGlobalCooldown(player);
        }
    }

    @SuppressWarnings("deprecation")
    public void performSandWandMagic(Player player, Block clickedBlock, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.sandwand")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("sand-wand-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("sand-wand-section.cooldown");
        if (useGlobalCooldown) {
            long cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        } else {
            long cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.SAND_WAND,
                    cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        }

        if (!pluginInstance.getManager().isLocationSafe(clickedBlock.getLocation(), player, false, false, null)) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        int blockRemovalAmount = pluginInstance.getConfig().getInt("sand-wand-section.block-removal-amount"),
                removalSpeed = pluginInstance.getConfig().getInt("sand-wand-section.block-removal-speed");
        boolean onlyRemoveSand = pluginInstance.getConfig().getBoolean("sand-wand-section.only-sand-removal");
        if ((onlyRemoveSand && clickedBlock.getType() == Material.SAND)
                || (!onlyRemoveSand && pluginInstance.getManager().isInList(clickedBlock.getType(),
                clickedBlock.getData(), "sand-wand-section.other-materials"))) {

            clickedBlock.getWorld().playSound(clickedBlock.getLocation(), (pluginInstance.getServerVersion().startsWith("v1_9") || pluginInstance.getServerVersion().startsWith("v1_10")
                    || pluginInstance.getServerVersion().startsWith("v1_11") || pluginInstance.getServerVersion().startsWith("v1_12")
                    || pluginInstance.getServerVersion().startsWith("v1_13") || pluginInstance.getServerVersion().startsWith("v1_14")
                    || pluginInstance.getServerVersion().startsWith("v1_15") || pluginInstance.getServerVersion().startsWith("v1_16"))
                    ? Sound.BLOCK_STONE_BREAK : Sound.valueOf("STEP_STONE"), 1, 1);

            pluginInstance.getManager().logToCoreProtect(player, clickedBlock, true, false);
            clickedBlock.setType(Material.AIR);
            pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
        }

        new BukkitRunnable() {
            int counter = 0;
            final Location location = clickedBlock.getLocation();

            @Override
            public void run() {
                if (!(counter >= blockRemovalAmount)) {
                    location.add(0, -1, 0);
                    if ((onlyRemoveSand && location.getBlock().getType() == Material.SAND)
                            || (!onlyRemoveSand && pluginInstance.getManager().isInList(location.getBlock().getType(),
                            location.getBlock().getData(), "sand-wand-section.other-materials"))) {
                        if (!pluginInstance.getManager().isLocationSafe(location, player, false, false, null))
                            return;

                        clickedBlock.getWorld().playSound(clickedBlock.getLocation(), (pluginInstance.getServerVersion().startsWith("v1_7")
                                || pluginInstance.getServerVersion().startsWith("v1_8"))
                                ? Sound.valueOf("STEP_STONE") : Sound.BLOCK_SAND_BREAK, 1, 1);

                        pluginInstance.getManager().logToCoreProtect(player, clickedBlock, true, false);
                        location.getBlock().setType(Material.AIR);
                    } else {
                        cancel();
                        return;
                    }
                } else {
                    cancel();
                    return;
                }

                counter += 1;
            }
        }.runTaskTimer(pluginInstance, removalSpeed, removalSpeed);

        if (cooldown > -1)
            if (!useGlobalCooldown)
                pluginInstance.getManager().updateWPCooldown(player, WPType.SAND_WAND);
            else
                pluginInstance.getManager().updateGlobalCooldown(player);
    }

    @SuppressWarnings("deprecation")
    public void performCraftWandMagic(Player player, Block clickedBlock, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.craftwand")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("craft-wand-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("craft-wand-section.cooldown");
        if (useGlobalCooldown) {
            long cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        } else {
            long cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.CRAFT_WAND, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        }

        if (!pluginInstance.getManager().isLocationSafe(clickedBlock.getLocation(), player, false, false, null)) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        if (pluginInstance.getManager().isInList(clickedBlock.getType(), clickedBlock.getData(), "craft-wand-section.container-materials") && clickedBlock.getState() instanceof InventoryHolder) {
            InventoryHolder inventoryHolder = (InventoryHolder) clickedBlock.getState();
            HashMap<Material, Integer> materialCountingMap = new HashMap<>(), blockAdditionMap = new HashMap<>();
            int totalBlocksCrafted = 0, totalMaterialsConsumed = 0;

            for (int i = -1; ++i < inventoryHolder.getInventory().getSize(); ) {
                ItemStack itemStack = inventoryHolder.getInventory().getItem(i);
                if (itemStack != null && itemStack.getType() != Material.AIR && (itemStack.getItemMeta() == null || (!itemStack.getItemMeta().hasDisplayName() && !itemStack.getItemMeta().hasLore()))
                        && pluginInstance.getManager().isCraftMaterial(itemStack)) {
                    if (!materialCountingMap.isEmpty() && materialCountingMap.containsKey(itemStack.getType()))
                        materialCountingMap.put(itemStack.getType(), materialCountingMap.get(itemStack.getType()) + itemStack.getAmount());
                    else materialCountingMap.put(itemStack.getType(), itemStack.getAmount());
                }
            }

            List<Material> countingMaterials = new ArrayList<>(materialCountingMap.keySet());
            for (int i = -1; ++i < countingMaterials.size(); ) {
                Material material = countingMaterials.get(i);
                String solutionMaterialString = pluginInstance.getManager().getCraftSolution(material, (short) ((material.name().equalsIgnoreCase("INK_SACK") || material.name().equalsIgnoreCase("INK_SAC")) ? 4 : 0));
                int inventoryStock = materialCountingMap.get(material), craftRequirement = pluginInstance.getManager().getCraftAmount(material, (short) ((material.name().equalsIgnoreCase("INK_SACK")
                        || material.name().equalsIgnoreCase("INK_SAC")) ? 4 : 0)), blocksToAdd = (inventoryStock / craftRequirement), amountToRemove = (craftRequirement * (inventoryStock / craftRequirement));
                if (blocksToAdd <= 0 && amountToRemove <= 0) continue;
                totalBlocksCrafted += blocksToAdd;
                totalMaterialsConsumed += amountToRemove;
                pluginInstance.getManager().removeItem(inventoryHolder.getInventory(), material, (short) ((material.name().equalsIgnoreCase("INK_SACK")
                        || material.name().equalsIgnoreCase("INK_SAC")) ? 4 : 0), amountToRemove);

                if (solutionMaterialString == null || solutionMaterialString.equalsIgnoreCase("")) continue;
                if (!blockAdditionMap.isEmpty() && blockAdditionMap.containsKey(Material.getMaterial(solutionMaterialString)))
                    blockAdditionMap.put(Material.getMaterial(solutionMaterialString), blockAdditionMap.get(Material.getMaterial(solutionMaterialString)) + blocksToAdd);
                else
                    blockAdditionMap.put(Material.getMaterial(solutionMaterialString), blocksToAdd);
            }

            List<Material> blocksAdditionMaterialList = new ArrayList<>(blockAdditionMap.keySet());
            for (int i = -1; ++i < blocksAdditionMaterialList.size(); ) {
                Material material = blocksAdditionMaterialList.get(i);
                if (inventoryHolder.getInventory().firstEmpty() == -1)
                    clickedBlock.getWorld().dropItemNaturally(clickedBlock.getLocation(), new ItemStack(material, blockAdditionMap.get(material)));
                else
                    inventoryHolder.getInventory().addItem(new ItemStack(material, blockAdditionMap.get(material)));
            }

            if (!(totalBlocksCrafted <= 0)) {
                pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
                pluginInstance.getManager().sendCustomMessage(player, "crafted-message", "{total-crafted}:" + totalBlocksCrafted, "{total-consumed}:" + totalMaterialsConsumed);
            } else
                pluginInstance.getManager().sendCustomMessage(player, "not-enough-crafted-message");

            if (cooldown > -1)
                if (!useGlobalCooldown)
                    pluginInstance.getManager().updateWPCooldown(player, WPType.CRAFT_WAND);
                else
                    pluginInstance.getManager().updateGlobalCooldown(player);
        }
    }

    @SuppressWarnings("deprecation")
    public void performSellWandMagic(Player player, Block clickedBlock, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.sellwand")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("sell-wand-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("sell-wand-section.cooldown");
        if (useGlobalCooldown) {
            long cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        } else {
            long cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.SELL_WAND, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        }

        if (!pluginInstance.getManager().isLocationSafe(clickedBlock.getLocation(), player, false, false, null)) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        if (pluginInstance.getManager().isInList(clickedBlock.getType(), clickedBlock.getData(), "sell-wand-section.container-materials")
                && clickedBlock.getState() instanceof InventoryHolder) {
            boolean sellNameItems = pluginInstance.getConfig().getBoolean("general-section.sell-custom-name-items"),
                    sellLoreItems = pluginInstance.getConfig().getBoolean("general-section.sell-custom-lore-items");
            InventoryHolder inventoryHolder = (InventoryHolder) clickedBlock.getState();
            double totalEarnings = 0;
            int totalItems = 0;

            double foundPrice;
            Object chest = ((pluginInstance.getServer().getPluginManager().getPlugin("WildChests") != null)
                    ? WildChestsAPI.getStorageChest(clickedBlock.getLocation()) : null);
            if (chest != null) {
                com.bgsoftware.wildchests.api.objects.chests.StorageChest storageChest = (com.bgsoftware.wildchests.api.objects.chests.StorageChest) chest;
                ItemStack duplicate = storageChest.getItemStack().clone();
                duplicate.setAmount(1);

                if (pluginInstance.getShopGUIPlusHandler() != null)
                    foundPrice = pluginInstance.getShopGUIPlusHandler().getItemStackPriceSell(player, duplicate);
                else if (pluginInstance.isEssentialsInstalled())
                    foundPrice = pluginInstance.getManager().getEssentialsSellPrice(duplicate);
                else
                    foundPrice = pluginInstance.getManager().getMaterialPrice(storageChest.getItemStack(), "sell-wand-section.material-prices");

                if (foundPrice > 0) {
                    totalItems += storageChest.getAmount().intValue();
                    totalEarnings += ((foundPrice * pluginInstance.getManager().getItemModifier(handItemStack)) * storageChest.getAmount().intValue());
                    pluginInstance.getManager().removeItem(inventoryHolder.getInventory(), storageChest.getItemStack().getType(),
                            storageChest.getItemStack().getDurability(), storageChest.getAmount().intValue());
                    storageChest.setAmount(storageChest.getAmount().subtract(storageChest.getAmount()));
                    storageChest.update();
                }
            } else {
                for (int i = -1; ++i < inventoryHolder.getInventory().getSize(); ) {
                    ItemStack itemStack = inventoryHolder.getInventory().getItem(i);
                    if ((itemStack != null && itemStack.getType() != Material.AIR)) {

                        if (itemStack.hasItemMeta() && ((!sellNameItems && itemStack.getItemMeta() != null && itemStack.getItemMeta().hasDisplayName())
                                || (!sellLoreItems && itemStack.getItemMeta() != null && itemStack.getItemMeta().hasLore())))
                            continue;

                        ItemStack duplicate = itemStack.clone();
                        duplicate.setAmount(1);

                        if (pluginInstance.getShopGUIPlusHandler() != null)
                            foundPrice = pluginInstance.getShopGUIPlusHandler().getItemStackPriceSell(player, duplicate);
                        else if (pluginInstance.isEssentialsInstalled())
                            foundPrice = pluginInstance.getManager().getEssentialsSellPrice(duplicate);
                        else
                            foundPrice = pluginInstance.getManager().getMaterialPrice(duplicate, "sell-wand-section.material-prices");

                        if (foundPrice <= 0) continue;
                        totalItems += itemStack.getAmount();
                        totalEarnings += ((foundPrice * pluginInstance.getManager().getItemModifier(handItemStack)) * itemStack.getAmount());
                        pluginInstance.getManager().removeItem(inventoryHolder.getInventory(), itemStack.getType(), itemStack.getDurability(), itemStack.getAmount());
                    }
                }
            }

            if (totalEarnings > 0) {
                if (!pluginInstance.getConfig().getBoolean("sell-wand-section.custom-item-currency")) {
                    if (pluginInstance.getConfig().getBoolean("general-section.use-vault")) {
                        TransactionEvent transactionEvent = new TransactionEvent(player, clickedBlock.getLocation(), WPType.SELL_WAND, handItemStack, totalEarnings);
                        pluginInstance.getServer().getPluginManager().callEvent(transactionEvent);
                        if (!transactionEvent.isCancelled())
                            pluginInstance.getVaultHandler().getEconomy().depositPlayer(player, totalEarnings);
                    } else {
                        pluginInstance.getManager().sendCustomMessage(player, "vault-disabled");
                        return;
                    }
                } else {
                    if (player.getInventory().firstEmpty() == -1)
                        player.getWorld().dropItemNaturally(player.getLocation(), pluginInstance.getManager().getCustomItem((int) totalEarnings, "sell-wand-section.custom-item"));
                    else
                        player.getInventory().addItem(pluginInstance.getManager().getCustomItem((int) totalEarnings, "sell-wand-section.custom-item"));
                }

                pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
                pluginInstance.getManager().addToItemsSold(player, handItemStack, totalItems);
                pluginInstance.getManager().sendCustomMessage(player, "sell-wand-message", "{total-earnings}:" + totalEarnings, "{total-items}:" + totalItems);
            } else pluginInstance.getManager().sendCustomMessage(player, "no-items-to-sell-message");

            if (cooldown > -1)
                if (!useGlobalCooldown)
                    pluginInstance.getManager().updateWPCooldown(player, WPType.SELL_WAND);
                else
                    pluginInstance.getManager().updateGlobalCooldown(player);
        }
    }

    @SuppressWarnings("deprecation")
    public void performSmeltWandMagic(Player player, Block clickedBlock, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.smeltwand")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("smelt-wand-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("smelt-wand-section.cooldown");
        long cooldownRemainder;
        if (useGlobalCooldown) {
            cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
        } else {
            cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.SMELT_WAND, cooldown);
        }
        if (!(cooldown <= -1) && cooldownRemainder > 0) {
            pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
            return;
        }

        if (!pluginInstance.getManager().isLocationSafe(clickedBlock.getLocation(), player, false, false, null)) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        if (pluginInstance.getManager().isInList(clickedBlock.getType(), clickedBlock.getData(), "smelt-wand-section.container-materials")
                && clickedBlock.getState() instanceof InventoryHolder) {
            InventoryHolder inventoryHolder = (InventoryHolder) clickedBlock.getState();
            HashMap<String, Integer> materialCountingMap = new HashMap<>(), itemAdditionMap = new HashMap<>();
            int totalSmelted = 0, totalConsumed = 0;

            for (int i = -1; ++i < inventoryHolder.getInventory().getSize(); ) {
                ItemStack itemStack = inventoryHolder.getInventory().getItem(i);
                if ((itemStack != null && itemStack.getType() != Material.AIR)) {
                    if (!materialCountingMap.isEmpty() && materialCountingMap.containsKey(itemStack.getType() + ":" + itemStack.getDurability()))
                        materialCountingMap.put(itemStack.getType() + ":" + itemStack.getDurability(), materialCountingMap.get(itemStack.getType() + ":" + itemStack.getDurability()) + itemStack.getAmount());
                    else
                        materialCountingMap.put(itemStack.getType() + ":" + itemStack.getDurability(), itemStack.getAmount());
                }
            }

            List<String> smeltableList = pluginInstance.getConfig().getStringList("smelt-wand-section.smeltable-list"),
                    countingMaterials = new ArrayList<>(materialCountingMap.keySet());
            for (int i = -1; ++i < countingMaterials.size(); ) {
                final String materialLine = countingMaterials.get(i);
                if (!materialLine.contains(":")) continue;

                final String[] materialArgs = materialLine.split(":");
                final Material material = Material.getMaterial(materialArgs[0]);
                final int durability = Integer.parseInt(materialArgs[1]);

                int amount = materialCountingMap.get(materialLine), customAmount = 0;
                if (amount <= 0) continue;

                for (int j = -1; ++j < smeltableList.size(); ) {
                    String smeltableString = smeltableList.get(j);
                    if (!smeltableString.contains(":")) continue;

                    final String[] smeltableStringArgs = smeltableString.split(":");
                    final String smeltMaterialString = smeltableStringArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"),
                            resultMaterialString = smeltableStringArgs[1].toUpperCase().replace(" ", "_").replace("-", "_");
                    Material mat1 = Material.getMaterial(smeltMaterialString), mat2 = Material.getMaterial(resultMaterialString);
                    if ((mat1 != null && mat2 != null) && mat1 == material) {
                        customAmount += amount;
                        totalSmelted += customAmount;
                        totalConsumed += amount;
                        if (!itemAdditionMap.isEmpty() && itemAdditionMap.containsKey(mat2.name()))
                            itemAdditionMap.put(mat2.name() + ":0", itemAdditionMap.get(mat2.name() + ":0") + customAmount);
                        else
                            itemAdditionMap.put(mat2.name() + ":0", customAmount);

                        pluginInstance.getManager().removeItem(inventoryHolder.getInventory(), material, (short) 0, materialCountingMap.get(materialLine));
                        break;
                    }
                }
            }

            List<String> additionMaterialList = new ArrayList<>(itemAdditionMap.keySet());
            for (int i = -1; ++i < additionMaterialList.size(); ) {
                final String materialLine = additionMaterialList.get(i);
                if (!materialLine.contains(":")) continue;

                final String[] materialArgs = materialLine.split(":");
                final Material material = Material.getMaterial(materialArgs[0]);
                final int durability = Integer.parseInt(materialArgs[1]);

                if (material != null) {
                    if (inventoryHolder.getInventory().firstEmpty() == -1)
                        clickedBlock.getWorld().dropItemNaturally(clickedBlock.getLocation(), new ItemStack(material, itemAdditionMap.get(materialLine),
                                (short) (material.name().equalsIgnoreCase("INK_SACK_4") ? 4 : 0)));
                    else
                        inventoryHolder.getInventory().addItem(new ItemStack(material, itemAdditionMap.get(materialLine),
                                (short) (material.name().equalsIgnoreCase("INK_SACK_4") ? 4 : 0)));
                }
            }

            if (!(totalSmelted <= 0)) {
                pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
                pluginInstance.getManager().sendCustomMessage(player, "smelted-message", "{total-smelted}:" + totalSmelted, "{total-consumed}:" + totalConsumed);
            } else pluginInstance.getManager().sendCustomMessage(player, "not-enough-smelted-message");

            if (cooldown > -1)
                if (!useGlobalCooldown)
                    pluginInstance.getManager().updateWPCooldown(player, WPType.SMELT_WAND);
                else
                    pluginInstance.getManager().updateGlobalCooldown(player);
        }

    }

    @SuppressWarnings("deprecation")
    public void performTNTWandMagic(Player player, Block clickedBlock, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.tntwand")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        final String factionName = pluginInstance.getManager().getFactionName(player);
        if (factionName == null || factionName.isEmpty()) {
            pluginInstance.getManager().sendCustomMessage(player, "faction-invalid-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("tnt-wand-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("tnt-wand-section.cooldown");
        long cooldownRemainder;
        if (useGlobalCooldown) {
            cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
        } else {
            cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.TNT_WAND, cooldown);
        }
        if (!(cooldown <= -1) && cooldownRemainder > 0) {
            pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
            return;
        }

        if (!pluginInstance.getManager().isLocationSafe(clickedBlock.getLocation(), player, false, false, null)) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        if (pluginInstance.getManager().isInList(clickedBlock.getType(), clickedBlock.getData(), "tnt-wand-section.container-materials")
                && clickedBlock.getState() instanceof InventoryHolder) {
            long totalFoundTnt = 0;
            InventoryHolder inventoryHolder = (InventoryHolder) clickedBlock.getState();
            for (int i = -1; ++i < inventoryHolder.getInventory().getSize(); ) {
                ItemStack itemStack = inventoryHolder.getInventory().getItem(i);
                if ((itemStack != null && itemStack.getType() == Material.TNT)) {
                    totalFoundTnt += itemStack.getAmount();
                    inventoryHolder.getInventory().setItem(i, null);
                }
            }

            if (totalFoundTnt > 0) {
                ConfigurationSection dataSection = pluginInstance.getDataConfig().getConfigurationSection("");
                if (dataSection != null) {
                    if (dataSection.contains("tnt-storage")) {
                        final long foundAmount = dataSection.getLong(factionName.toLowerCase());
                        dataSection.set(("tnt-storage." + factionName.toLowerCase()), (foundAmount + totalFoundTnt));
                    } else dataSection.set(("tnt-storage." + factionName.toLowerCase()), totalFoundTnt);
                }
                pluginInstance.saveDataConfig();

                pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
                pluginInstance.getManager().sendCustomMessage(player, "tnt-wand-message",
                        ("{total}:" + totalFoundTnt));
            } else pluginInstance.getManager().sendCustomMessage(player, "not-enough-tnt-message");

            if (cooldown > -1)
                if (!useGlobalCooldown) pluginInstance.getManager().updateWPCooldown(player, WPType.TNT_WAND);
                else pluginInstance.getManager().updateGlobalCooldown(player);
        }

    }

    public void performIceWandMagic(Player player, Block clickedBlock, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.icewand")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("ice-wand-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("ice-wand-section.cooldown");

        long cooldownRemainder;
        if (useGlobalCooldown) cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
        else cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.ICE_WAND, cooldown);

        if (!(cooldown <= -1) && cooldownRemainder > 0) {
            pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
            return;
        }

        if (!pluginInstance.getManager().isLocationSafe(clickedBlock.getLocation(), player, false, false, null)) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        int itemRadius = pluginInstance.getManager().getItemRadius(handItemStack), meltCounter = 0;
        for (int x = ((-itemRadius) - 1); ++x <= itemRadius; )
            for (int y = ((-itemRadius) - 1); ++y <= itemRadius; )
                for (int z = ((-itemRadius) - 1); ++z <= itemRadius; ) {
                    Block currentBlock = clickedBlock.getRelative(x, y, z);
                    if (pluginInstance.getManager().isIceWandMeltMaterial(currentBlock)
                            && pluginInstance.getManager().isLocationSafe(currentBlock.getLocation(), player, false, false, null)) {
                        pluginInstance.getManager().logToCoreProtect(player, currentBlock, true, false);
                        currentBlock.setType(Material.WATER);
                        currentBlock.getState().update();
                        meltCounter += 1;
                    }
                }

        if (meltCounter > 0) {
            pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
            pluginInstance.getManager().sendCustomMessage(player, "melt-success-message", "{total-melted}:" + meltCounter);
        } else pluginInstance.getManager().sendCustomMessage(player, "melt-fail-message");

        if (cooldown > -1)
            if (!useGlobalCooldown)
                pluginInstance.getManager().updateWPCooldown(player, WPType.ICE_WAND);
            else
                pluginInstance.getManager().updateGlobalCooldown(player);
    }

    public void performProjectileWandMagic(Player player, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.projectilewand")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("projectile-wand-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("projectile-wand-section.cooldown");
        long cooldownRemainder;
        if (useGlobalCooldown) {
            cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
        } else {
            cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.PROJECTILE_WAND, cooldown);
        }
        if (!(cooldown <= -1) && cooldownRemainder > 0) {
            pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
            return;
        }

        if (!pluginInstance.getManager().isLocationSafe(player.getLocation(), player, false, false, null)) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        List<String> entityList = pluginInstance.getConfig().getStringList("projectile-wand-section.projectile-entities");
        for (int i = -1; ++i < entityList.size(); ) {
            String entityName = entityList.get(i);
            EntityType entityType = EntityType.valueOf(entityName.toUpperCase().replace(" ", "_").replace("-", "_"));

            pluginInstance.getServer().getScheduler().runTaskLater(pluginInstance, () -> {
                Entity entity = player.getWorld().spawnEntity(player.getEyeLocation().add(player.getLocation().getDirection()), entityType);
                entity.setCustomNameVisible(false);
                entity.setCustomName("FactionWP/Projectile_Wand-Entity/" + player.getUniqueId());

                if (entity instanceof Projectile) {
                    Projectile projectile = (Projectile) entity;
                    projectile.setShooter(player);
                }

                if (entity instanceof TNTPrimed) {
                    TNTPrimed tntPrimed = (TNTPrimed) entity;
                    tntPrimed.setFuseTicks(20);
                    entity.setVelocity(player.getLocation().getDirection().multiply(2));
                } else if (entity instanceof Arrow)
                    entity.setVelocity(player.getLocation().getDirection().multiply(5));
                else entity.setVelocity(player.getLocation().getDirection().multiply(2));
            }, i * 5L);
        }

        pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
        if (cooldown > -1)
            if (!useGlobalCooldown)
                pluginInstance.getManager().updateWPCooldown(player, WPType.PROJECTILE_WAND);
            else
                pluginInstance.getManager().updateGlobalCooldown(player);
    }

    @SuppressWarnings("deprecation")
    public void performWallWandMagic(Player player, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.wallwand")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("wall-wand-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("wall-wand-section.cooldown");
        long cooldownRemainder;
        if (useGlobalCooldown) {
            cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
        } else {
            cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.WALL_WAND,
                    cooldown);
        }
        if (!(cooldown <= -1) && cooldownRemainder > 0) {
            pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
            return;
        }

        BlockFace blockFace = pluginInstance.getManager().getDirection(player.getLocation().getYaw());
        if (blockFace == null)
            blockFace = BlockFace.NORTH;

        Material material = Material.getMaterial(Objects.requireNonNull(pluginInstance.getConfig().getString("wall-wand-section.build-material"))
                .toUpperCase().replace(" ", "_").replace("-", "_"));
        Block centerBlock = player.getLocation().getBlock().getRelative(blockFace).getRelative(blockFace);
        if (!pluginInstance.getManager().isLocationSafe(centerBlock.getLocation(), player, false, true, material)) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        final int durability = pluginInstance.getConfig().getInt("wall-wand-section.build-durability"), itemRadius = pluginInstance.getManager().getItemRadius(handItemStack),
                wallLength = pluginInstance.getConfig().getInt("wall-wand-section.wall-length");
        for (int i = -1; ++i < itemRadius; )
            for (int j = -1; ++j < wallLength; ) {
                Block currentBlockPositive = (blockFace == BlockFace.EAST || blockFace == BlockFace.WEST) ? centerBlock.getRelative(0, i, j) : centerBlock.getRelative(j, i, 0);
                if (pluginInstance.getManager().isLocationSafe(currentBlockPositive.getLocation(), player, false, true, material)
                        && pluginInstance.getManager().isInList(currentBlockPositive.getType(), currentBlockPositive.getData(), "wall-wand-section.replaceable-materials")) {
                    pluginInstance.getManager().logToCoreProtect(player, currentBlockPositive, true, false);
                    currentBlockPositive.setType(Objects.requireNonNull(material));
                    if (!pluginInstance.getServerVersion().startsWith("v1_13") && !pluginInstance.getServerVersion().startsWith("v1_14") && !pluginInstance.getServerVersion().startsWith("v1_15"))
                        try {
                            Method method = Block.class.getMethod("setData", Byte.class);
                            method.invoke(currentBlockPositive, (byte) durability);
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                        }

                    pluginInstance.getManager().logToCoreProtect(player, currentBlockPositive, false, true);
                }

                Block currentBlockNegative = (blockFace == BlockFace.EAST || blockFace == BlockFace.WEST)
                        ? centerBlock.getRelative(0, i, -j)
                        : centerBlock.getRelative(-j, i, 0);
                if (pluginInstance.getManager().isLocationSafe(currentBlockNegative.getLocation(), player, false, true, material)
                        && pluginInstance.getManager().isInList(currentBlockNegative.getType(),
                        currentBlockNegative.getData(), "wall-wand-section.replaceable-materials")) {
                    pluginInstance.getManager().logToCoreProtect(player, currentBlockNegative, true, false);
                    currentBlockNegative.setType(material);

                    if (!pluginInstance.getServerVersion().startsWith("v1_13") && !pluginInstance.getServerVersion().startsWith("v1_14") && !pluginInstance.getServerVersion().startsWith("v1_15"))
                        try {
                            Method method = Block.class.getMethod("setData", Byte.class);
                            method.invoke(currentBlockNegative, durability);
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                        }

                    pluginInstance.getManager().logToCoreProtect(player, currentBlockNegative, false, true);
                }
            }

        centerBlock.getWorld().playSound(centerBlock.getLocation(), (pluginInstance.getServerVersion().startsWith("v1_7")
                || pluginInstance.getServerVersion().startsWith("v1_8"))
                ? Sound.valueOf("STEP_STONE") : Sound.BLOCK_STONE_BREAK, 1, 1);

        pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
        if (cooldown > -1)
            if (!useGlobalCooldown)
                pluginInstance.getManager().updateWPCooldown(player, WPType.WALL_WAND);
            else
                pluginInstance.getManager().updateGlobalCooldown(player);
    }

    @SuppressWarnings("deprecation")
    public void performPlatformWandMagic(Player player, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.platformwand")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("wall-wand-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("platform-wand-section.cooldown");
        if (useGlobalCooldown) {
            long cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        } else {
            long cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.PLATFORM_WAND,
                    cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        }

        Material material = Material.getMaterial(pluginInstance.getConfig()
                .getString("platform-wand-section.build-material").toUpperCase().replace(" ", "_").replace("-", "_"));
        Block centerBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (!pluginInstance.getManager().isLocationSafe(centerBlock.getLocation(), player, false, true, material)) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        int durability = pluginInstance.getConfig().getInt("platform-wand-section.build-durability"),
                itemRadius = pluginInstance.getManager().getItemRadius(handItemStack);

        for (int x = ((-itemRadius) - 1); ++x <= itemRadius; )
            for (int z = ((-itemRadius) - 1); ++z <= itemRadius; ) {
                Block currentBlock = centerBlock.getRelative(x, 0, z);
                if (pluginInstance.getManager().isLocationSafe(currentBlock.getLocation(), player, false, true, material)
                        && pluginInstance.getManager().isInList(currentBlock.getType(), currentBlock.getData(),
                        "platform-wand-section.replaceable-materials")) {
                    pluginInstance.getManager().logToCoreProtect(player, currentBlock, true, false);
                    currentBlock.setType(material);

                    if (!pluginInstance.getServerVersion().startsWith("v1_13") && !pluginInstance.getServerVersion().startsWith("v1_14") && !pluginInstance.getServerVersion().startsWith("v1_15"))
                        try {
                            Method method = Block.class.getMethod("setData", Byte.class);
                            method.invoke(currentBlock, durability);
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                        }

                    pluginInstance.getManager().logToCoreProtect(player, currentBlock, false, true);
                }
            }

        centerBlock.getWorld().playSound(centerBlock.getLocation(), (pluginInstance.getServerVersion().startsWith("v1_7")
                || pluginInstance.getServerVersion().startsWith("v1_8"))
                ? Sound.valueOf("STEP_STONE") : Sound.BLOCK_STONE_BREAK, 1, 1);

        pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
        if (cooldown > -1)
            if (!useGlobalCooldown)
                pluginInstance.getManager().updateWPCooldown(player, WPType.PLATFORM_WAND);
            else
                pluginInstance.getManager().updateGlobalCooldown(player);
    }

    @SuppressWarnings("deprecation")
    public void performHarvesterHoeMagic(Player player, Block block, ItemStack handItemStack) {

        final Material beforeMaterial = block.getType();

        if (!player.hasPermission("factionwp.harvesterhoe")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("harvester-hoe-section.cooldown");
        int cooldown = pluginInstance.getConfig().getInt("harvester-hoe-section.cooldown");

        long cooldownRemainder;
        if (useGlobalCooldown) cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
        else cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.HARVESTER_HOE, cooldown);

        if (!(cooldown <= -1) && cooldownRemainder > 0) {
            pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
            return;
        }

        if (pluginInstance.getManager().isInList(block.getType(), block.getData(), "harvester-hoe-section.effected-crop-blocks")) {
            if (!pluginInstance.getManager().isLocationSafe(block.getLocation(), player, true, false, block.getType())) {
                pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
                return;
            }
            boolean generateRandomStacks = pluginInstance.getConfig().getBoolean("harvester-hoe-section.generate-random-stacks"),
                    inSellMode = pluginInstance.getManager().isSellModeActive(player, WPType.HARVESTER_HOE),
                    useShopGuiPlus = pluginInstance.getConfig().getBoolean("hooks-section.shop-gui-plus-hook.use-shop-gui-plus"),
                    useEssentials = pluginInstance.getConfig().getBoolean("hooks-section.essentials-hook.use-essentials");
            List<Block> blocksToEffect = new ArrayList<>();

            int totalItems = 0;
            double totalEarnings = 0;

            boolean stopReplant = false;
            if (pluginInstance.getManager().isStackingPlantBlock(block)) {
                int constraintLevel = pluginInstance.getConfig().getInt("harvester-hoe-section.plant-stackable-constraint");

                Block b = block;
                if (b.getRelative(BlockFace.DOWN).getType() == b.getType()) {
                    blocksToEffect.add(block);
                    stopReplant = true;
                }

                for (int i = -1; ++i < constraintLevel; ) {
                    b = b.getRelative(BlockFace.UP);
                    if (!pluginInstance.getManager().isStackingPlantBlock(b)
                            || !pluginInstance.getManager().isLocationSafe(b.getLocation(), player, true, false, block.getType()))
                        break;

                    blocksToEffect.add(b);
                }
            } else blocksToEffect.add(block);

            boolean isOlder = (pluginInstance.getServerVersion().startsWith("v1_7") || pluginInstance.getServerVersion().startsWith("v1_8")
                    || pluginInstance.getServerVersion().startsWith("v1_9") || pluginInstance.getServerVersion().startsWith("v1_10")
                    || pluginInstance.getServerVersion().startsWith("v1_11")),
                    is1_12 = pluginInstance.getServerVersion().startsWith("v1_12");

            Collections.reverse(blocksToEffect);
            for (int i = -1; ++i < blocksToEffect.size(); ) {
                Block currentBlock = blocksToEffect.get(i);
                if (inSellMode) {
                    if (!currentBlock.getType().name().contains("NETHER_WART"))
                        for (ItemStack dropItem : block.getDrops(handItemStack)) {
                            if (generateRandomStacks)
                                dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                        dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));

                            double itemPrice;
                            if (useShopGuiPlus && pluginInstance.getShopGUIPlusHandler() != null)
                                itemPrice = pluginInstance.getShopGUIPlusHandler().getItemStackPriceSell(player, dropItem);
                            else if (useEssentials && pluginInstance.isEssentialsInstalled())
                                itemPrice = pluginInstance.getManager().getEssentialsSellPrice(dropItem);
                            else
                                itemPrice = pluginInstance.getManager().getMaterialPrice(dropItem, "harvester-hoe-section.material-prices");

                            if (!(itemPrice <= 0)) {
                                totalItems += dropItem.getAmount();
                                totalEarnings += (dropItem.getAmount() * (itemPrice * pluginInstance.getManager().getItemModifier(handItemStack)));
                            }
                        }
                    else {
                        ItemStack itemStack = new ItemStack(isOlder ? Objects.requireNonNull(Material.getMaterial("NETHER_STALK"))
                                : (is1_12 ? Objects.requireNonNull(Material.getMaterial("NETHER_WARTS")) : Material.NETHER_WART));
                        if (generateRandomStacks)
                            itemStack.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(itemStack.getType(),
                                    itemStack.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));

                        double itemPrice;
                        if (useShopGuiPlus && pluginInstance.getShopGUIPlusHandler() != null)
                            itemPrice = pluginInstance.getShopGUIPlusHandler().getItemStackPriceSell(player, itemStack);
                        else if (useEssentials && pluginInstance.isEssentialsInstalled())
                            itemPrice = pluginInstance.getManager().getEssentialsSellPrice(itemStack);
                        else
                            itemPrice = pluginInstance.getManager().getMaterialPrice(itemStack, "harvester-hoe-section.material-prices");

                        if (!(itemPrice <= 0)) {
                            totalItems += itemStack.getAmount();
                            totalEarnings += (itemStack.getAmount() * (itemPrice * pluginInstance.getManager().getItemModifier(handItemStack)));
                        }
                    }

                } else {
                    if (!currentBlock.getType().name().contains("NETHER_WART"))
                        for (ItemStack dropItem : block.getDrops(handItemStack)) {
                            if (generateRandomStacks)
                                dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                        dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));

                            if (player.getInventory().firstEmpty() == -1)
                                player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
                            else player.getInventory().addItem(dropItem);
                            totalItems += dropItem.getAmount();
                        }
                    else {
                        ItemStack itemStack = new ItemStack(isOlder ? Objects.requireNonNull(Material.getMaterial("NETHER_STALK"))
                                : (is1_12 ? Objects.requireNonNull(Material.getMaterial("NETHER_WARTS")) : Material.NETHER_WART));
                        if (generateRandomStacks)
                            itemStack.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(itemStack.getType(),
                                    itemStack.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));

                        if (player.getInventory().firstEmpty() == -1)
                            player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                        else player.getInventory().addItem(itemStack);
                        totalItems += itemStack.getAmount();
                    }

                }
                currentBlock.setType(Material.AIR);
                currentBlock.getWorld().playSound(currentBlock.getLocation(), (pluginInstance.getServerVersion().startsWith("v1_7")
                        || pluginInstance.getServerVersion().startsWith("v1_8"))
                        ? Sound.valueOf("STEP_STONE") : Sound.BLOCK_STONE_BREAK, 1, 1);
            }

            if (!stopReplant && pluginInstance.getConfig().getBoolean("harvester-hoe-section.replant")) {
                Block blockUnderInitial = block.getRelative(BlockFace.DOWN);
                if (!blockUnderInitial.getType().name().contains("AIR")) {

                    if (blockUnderInitial.getType().name().equals("SOIL") || blockUnderInitial.getType().name().equals("FARMLAND")) {

                        if (beforeMaterial.name().contains("CROPS") || beforeMaterial.name().contains("WHEAT"))
                            block.setType(!isOlder ? Material.WHEAT : Objects.requireNonNull(Material.getMaterial("CROPS")), true);
                        else if (beforeMaterial.name().contains("CARROT"))
                            block.setType(!isOlder ? Material.CARROTS : Objects.requireNonNull(Material.getMaterial("CARROT")), true);
                        else if (beforeMaterial.name().contains("POTATO"))
                            block.setType(!isOlder ? Material.POTATOES : Objects.requireNonNull(Material.getMaterial("POTATO")), true);
                        else if (beforeMaterial.name().contains("BEETROOT"))
                            block.setType(!isOlder ? Material.BEETROOTS : Objects.requireNonNull(Material.getMaterial("BEETROOT_BLOCK")), true);

                    } else if (blockUnderInitial.getType() == Material.SAND || blockUnderInitial.getType().name().contains("_SAND")) {

                        if (beforeMaterial.name().contains("SUGAR_CANE"))
                            block.setType(!isOlder ? Material.SUGAR_CANE : Objects.requireNonNull(Material.getMaterial("SUGAR_CANE_BLOCK")), true);
                        else if (beforeMaterial.name().contains("NETHER_WART"))
                            block.setType(!isOlder ? Material.NETHER_WART : Objects.requireNonNull(Material.getMaterial("NETHER_WARTS")), true);
                        else if (beforeMaterial.name().contains("CACTUS"))
                            block.setType(Material.CACTUS, true);

                    }
                }
            }

            if (totalItems > 0) {
                if (inSellMode) {
                    if (!pluginInstance.getConfig().getBoolean("harvester-hoe-section.custom-item-currency")) {
                        if (pluginInstance.getConfig().getBoolean("general-section.use-vault")) {
                            TransactionEvent transactionEvent = new TransactionEvent(player, block.getLocation(),
                                    WPType.HARVESTER_HOE, handItemStack, totalEarnings);
                            pluginInstance.getServer().getPluginManager().callEvent(transactionEvent);
                            if (!transactionEvent.isCancelled())
                                pluginInstance.getVaultHandler().getEconomy().depositPlayer(player, totalEarnings);
                        } else {
                            pluginInstance.getManager().sendCustomMessage(player, "vault-disabled");
                            return;
                        }
                    } else {
                        if (player.getInventory().firstEmpty() == -1)
                            player.getWorld().dropItemNaturally(player.getLocation(), pluginInstance.getManager()
                                    .getCustomItem((int) totalEarnings, "harvester-hoe-section.custom-item"));
                        else
                            player.getInventory().addItem(pluginInstance.getManager().getCustomItem((int) totalEarnings,
                                    "harvester-hoe-section.custom-item"));
                    }

                    pluginInstance.getManager().addToItemsSold(player, handItemStack, totalItems);
                    pluginInstance.getManager().sendCustomMessage(player, "sold-message",
                            "{total-earnings}:" + totalEarnings, "{total-items}:" + totalItems);
                } else pluginInstance.getManager().sendCustomMessage(player, "added-message",
                        "{total-earnings}:" + totalEarnings, "{total-items}:" + totalItems);

                pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
            }

            List<String> commandsList = pluginInstance.getConfig().getStringList("harvester-hoe-section.finish-commands");
            for (int i = -1; ++i < commandsList.size(); ) {
                String commandLine = commandsList.get(i);
                if (!commandLine.contains(":")) {
                    pluginInstance.getServer().dispatchCommand(pluginInstance.getServer().getConsoleSender(),
                            commandLine.replace("{total-items}", String.valueOf(totalItems))
                                    .replace("{total-earnings}", String.valueOf(totalEarnings))
                                    .replace("{uuid}", player.getUniqueId().toString())
                                    .replace("{player}", player.getName()));
                    continue;
                }

                String[] commandArgs = commandLine.split(":");
                if (commandArgs[1].equalsIgnoreCase("player"))
                    pluginInstance.getServer().dispatchCommand(player,
                            commandArgs[0].replace("{total-items}", String.valueOf(totalItems))
                                    .replace("{total-earnings}", String.valueOf(totalEarnings))
                                    .replace("{uuid}", player.getUniqueId().toString())
                                    .replace("{player}", player.getName()));
                else
                    pluginInstance.getServer().dispatchCommand(pluginInstance.getServer().getConsoleSender(),
                            commandArgs[0].replace("{total-items}", String.valueOf(totalItems))
                                    .replace("{total-earnings}", String.valueOf(totalEarnings))
                                    .replace("{uuid}", player.getUniqueId().toString())
                                    .replace("{player}", player.getName()));
            }

            if (totalItems > 0 && cooldown > -1)
                if (!useGlobalCooldown) pluginInstance.getManager().updateWPCooldown(player, WPType.HARVESTER_HOE);
                else pluginInstance.getManager().updateGlobalCooldown(player);
        }
    }

    public void performHarvesterAxeMagic(Player player, Block block, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.harvesteraxe")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("harvester-axe-section.cooldown");
        int cooldown = pluginInstance.getConfig().getInt("harvester-axe-section.cooldown");

        if (useGlobalCooldown) {
            long cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        } else {
            long cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.HARVESTER_AXE, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        }

        if (pluginInstance.getManager().isHAMaterial(block)) {
            if (!pluginInstance.getManager().isLocationSafe(block.getLocation(), player, true, false, block.getType())) {
                pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
                return;
            }

            pluginInstance.getManager().logToCoreProtect(player, block, true, false);
            pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
            boolean inSellMode = pluginInstance.getManager().isSellModeActive(player, WPType.HARVESTER_AXE),
                    useShopGuiPlus = pluginInstance.getConfig().getBoolean("hooks-section.shop-gui-plus-hook.use-shop-gui-plus"),
                    useEssentials = pluginInstance.getConfig().getBoolean("hooks-section.essentials-hook.use-essentials");

            int totalItems = 0;
            double totalEarnings = 0;

            Material melonMaterial = Material.getMaterial((pluginInstance.getServerVersion().startsWith("v1_7") || pluginInstance.getServerVersion().startsWith("v1_8")
                    || pluginInstance.getServerVersion().startsWith("v1_9") || pluginInstance.getServerVersion().startsWith("v1_10") || pluginInstance.getServerVersion().startsWith("v1_11")
                    || pluginInstance.getServerVersion().startsWith("v1_12")) ? "MELON_BLOCK" : "MELON");
            if (inSellMode) {
                if (block.getType() == melonMaterial && handItemStack.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
                    double itemPrice = pluginInstance.getManager().getMaterialPrice(new ItemStack(melonMaterial), "harvester-axe-section.material-prices");
                    totalItems += 1;
                    totalEarnings += (1 * (itemPrice * pluginInstance.getManager().getItemModifier(handItemStack)));
                } else {
                    for (ItemStack dropItem : block.getDrops(handItemStack)) {
                        dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));

                        double itemPrice = 0;
                        ItemStack duplicate = dropItem.clone();
                        duplicate.setAmount(1);

                        if (pluginInstance.getShopGUIPlusHandler() != null)
                            itemPrice = pluginInstance.getShopGUIPlusHandler().getItemStackPriceSell(player, duplicate);
                        else if (pluginInstance.isEssentialsInstalled())
                            itemPrice = pluginInstance.getManager().getEssentialsSellPrice(duplicate);
                        else
                            itemPrice = pluginInstance.getManager().getMaterialPrice(duplicate, "harvester-axe-section.material-prices");

                        if (!(itemPrice <= 0)) {
                            totalItems += dropItem.getAmount();
                            totalEarnings += (dropItem.getAmount() * (itemPrice * pluginInstance.getManager().getItemModifier(handItemStack)));
                        }
                    }
                }
            } else {
                if (block.getType() == melonMaterial && handItemStack.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
                    ItemStack melonStack = new ItemStack(melonMaterial);
                    if (player.getInventory().firstEmpty() == -1)
                        player.getWorld().dropItemNaturally(player.getLocation(), melonStack);
                    else player.getInventory().addItem(melonStack);
                    totalItems += 1;
                } else for (ItemStack dropItem : block.getDrops(handItemStack)) {
                    dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                            dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                    if (player.getInventory().firstEmpty() == -1)
                        player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
                    else player.getInventory().addItem(dropItem);
                }
            }

            block.setType(Material.AIR);

            if (inSellMode) {
                if (!pluginInstance.getConfig().getBoolean("harvester-axe-section.custom-item-currency")) {
                    if (pluginInstance.getConfig().getBoolean("general-section.use-vault")) {
                        TransactionEvent transactionEvent = new TransactionEvent(player, block.getLocation(), WPType.HARVESTER_AXE, handItemStack, totalEarnings);
                        pluginInstance.getServer().getPluginManager().callEvent(transactionEvent);
                        if (!transactionEvent.isCancelled())
                            pluginInstance.getVaultHandler().getEconomy().depositPlayer(player, totalEarnings);
                    } else {
                        pluginInstance.getManager().sendCustomMessage(player, "vault-disabled");
                        return;
                    }
                } else {
                    if (player.getInventory().firstEmpty() == -1)
                        player.getWorld().dropItemNaturally(player.getLocation(), pluginInstance.getManager().getCustomItem((int) totalEarnings, "harvester-axe-section.custom-item"));
                    else
                        player.getInventory().addItem(pluginInstance.getManager().getCustomItem((int) totalEarnings, "harvester-axe-section.custom-item"));
                }

                pluginInstance.getManager().addToItemsSold(player, handItemStack, totalItems);
                pluginInstance.getManager().sendCustomMessage(player, "sold-message", "{total-earnings}:" + totalEarnings, "{total-items}:" + totalItems);
            } else {
                pluginInstance.getManager().sendCustomMessage(player, "added-message", "{total-earnings}:" + totalEarnings, "{total-items}:" + totalItems);
            }

            if (cooldown > -1)
                if (!useGlobalCooldown)
                    pluginInstance.getManager().updateWPCooldown(player, WPType.HARVESTER_AXE);
                else
                    pluginInstance.getManager().updateGlobalCooldown(player);
        }
    }

    @SuppressWarnings("deprecation")
    public void performTrayPickaxeMagic(BlockBreakEvent event, Player player, Block block, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.traypickaxe")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        final boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("tray-pickaxe-section.use-global-cooldown");
        final int cooldown = pluginInstance.getConfig().getInt("tray-pickaxe-section.cooldown");
        long cooldownRemainder;
        if (useGlobalCooldown) {
            cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
        } else {
            cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.TRAY_PICKAXE, cooldown);
        }
        if (cooldown > -1 && cooldownRemainder > 0) {
            pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
            return;
        }

        if (pluginInstance.getManager().isInList(block.getType(), block.getData(), "tray-pickaxe-section.trigger-materials")) {
            if (!pluginInstance.getManager().isLocationSafe(block.getLocation(), player, true, false, block.getType())) {
                pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
                return;
            }

            pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
            pluginInstance.getManager().addToBlocksBroken(player, handItemStack, 1);
            int itemRadius = pluginInstance.getManager().getItemRadius(handItemStack), blockCounter = 0;
            boolean keepCenterBlock = pluginInstance.getConfig().getBoolean("tray-pickaxe-section.keep-center-block"),
                    dropItems = pluginInstance.getConfig().getBoolean("tray-pickaxe-section.drop-items"),
                    autoPickup = pluginInstance.getConfig().getBoolean("tray-pickaxe-section.item-auto-pickup");

            if (keepCenterBlock)
                event.setCancelled(true);
            for (int x = ((-itemRadius) - 1); ++x <= itemRadius; )
                for (int z = ((-itemRadius) - 1); ++z <= itemRadius; ) {
                    Block currentBlock = block.getRelative(x, 0, z);
                    if (keepCenterBlock && currentBlock.getX() == block.getX() && currentBlock.getZ() == block.getZ())
                        continue;
                    if (pluginInstance.getManager().isInList(currentBlock.getType(), currentBlock.getData(),
                            "tray-pickaxe-section.removable-materials")
                            && pluginInstance.getManager().isLocationSafe(currentBlock.getLocation(), player, true, false, currentBlock.getType())) {
                        pluginInstance.getManager().logToCoreProtect(player, currentBlock, true, false);
                        if (dropItems && autoPickup) {
                            if (player.getInventory().firstEmpty() == -1) {
                                if (currentBlock.getDrops().size() > 0)
                                    for (ItemStack dropItem : currentBlock.getDrops(handItemStack)) {
                                        dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                                dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                                        player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
                                    }
                            } else {
                                if (currentBlock.getDrops().size() > 0)
                                    for (ItemStack dropItem : currentBlock.getDrops(handItemStack)) {
                                        dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                                dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                                        player.getInventory().addItem(dropItem);
                                    }
                            }

                            currentBlock.setType(Material.AIR);
                            currentBlock.getState().update(true, true);
                        } else if (dropItems) {
                            for (ItemStack dropItem : currentBlock.getDrops(handItemStack)) {
                                dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                        dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                                currentBlock.getWorld().dropItemNaturally(currentBlock.getLocation(), dropItem);
                            }
                            currentBlock.setType(Material.AIR);
                            currentBlock.getState().update(true, true);
                        } else {
                            currentBlock.setType(Material.AIR);
                            currentBlock.getState().update(true, true);
                        }
                        blockCounter += 1;
                    }
                }

            if (blockCounter > 0) {
                pluginInstance.getManager().addToBlocksBroken(player, handItemStack, blockCounter);
                block.getWorld().playSound(block.getLocation(), (pluginInstance.getServerVersion().startsWith("v1_8") || pluginInstance.getServerVersion().startsWith("v1_7"))
                        ? Sound.valueOf("STEP_STONE") : Sound.BLOCK_STONE_BREAK, 1, 1);
            }

            if (cooldown > -1)
                if (!useGlobalCooldown)
                    pluginInstance.getManager().updateWPCooldown(player, WPType.TRAY_PICKAXE);
                else
                    pluginInstance.getManager().updateGlobalCooldown(player);
        }
    }

    @SuppressWarnings("deprecation")
    public void performTrenchPickaxeMagic(Player player, Block block, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.trenchpickaxe")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("trench-pickaxe-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("trench-pickaxe-section.cooldown");
        if (useGlobalCooldown) {
            long cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        } else {
            long cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.TRENCH_PICKAXE, cooldown);
            if (!(cooldown <= -1) && cooldownRemainder > 0) {
                pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
                return;
            }
        }

        if (!pluginInstance.getManager().isInList(block.getType(), block.getData(), "trench-pickaxe-section.non-effected-materials")) {
            if (!pluginInstance.getManager().isLocationSafe(block.getLocation(), player, true, false, block.getType())) {
                pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
                return;
            }

            pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
            int itemRadius = pluginInstance.getManager().getItemRadius(handItemStack), blockCounter = 0;
            boolean dropItems = pluginInstance.getConfig().getBoolean("trench-pickaxe-section.drop-items"),
                    autoPickup = pluginInstance.getConfig().getBoolean("trench-pickaxe-section.item-auto-pickup");

            block.getWorld().playSound(block.getLocation(), (pluginInstance.getServerVersion().startsWith("v1_8") || pluginInstance.getServerVersion().startsWith("v1_7"))
                    ? Sound.valueOf("STEP_STONE") : Sound.BLOCK_STONE_BREAK, 1, 1);

            for (int y = ((-itemRadius) - 1); ++y <= itemRadius; )
                for (int x = ((-itemRadius) - 1); ++x <= itemRadius; )
                    for (int z = ((-itemRadius) - 1); ++z <= itemRadius; ) {
                        Block currentBlock = block.getRelative(x, y, z);
                        if (!pluginInstance.getManager().isInList(currentBlock.getType(), currentBlock.getData(), "trench-pickaxe-section.non-effected-materials")
                                && pluginInstance.getManager().isLocationSafe(currentBlock.getLocation(), player, true, false, currentBlock.getType())) {
                            pluginInstance.getManager().logToCoreProtect(player, currentBlock, true, false);
                            if (dropItems && autoPickup) {
                                List<ItemStack> dropList = new ArrayList<>(currentBlock.getDrops());
                                if (player.getInventory().firstEmpty() == -1) {
                                    if (currentBlock.getDrops().size() > 0)
                                        for (ItemStack dropItem : currentBlock.getDrops(handItemStack)) {
                                            dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                                    dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                                            player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
                                        }

                                } else {
                                    if (currentBlock.getDrops().size() > 0)
                                        for (ItemStack dropItem : currentBlock.getDrops(handItemStack)) {
                                            dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                                    dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                                            player.getInventory().addItem(dropItem);
                                        }
                                }


                                currentBlock.setType(Material.AIR);
                                currentBlock.getState().update(true, true);
                            } else if (dropItems) {
                                for (ItemStack dropItem : currentBlock.getDrops(handItemStack)) {
                                    dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                            dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                                    currentBlock.getWorld().dropItemNaturally(currentBlock.getLocation(), dropItem);
                                }
                                currentBlock.setType(Material.AIR);
                                currentBlock.getState().update(true, true);
                            } else {
                                currentBlock.setType(Material.AIR);
                                currentBlock.getState().update(true, true);
                            }
                            blockCounter += 1;
                        }
                    }

            if (blockCounter > 0)
                pluginInstance.getManager().addToBlocksBroken(player, handItemStack, blockCounter + 1);
            if (cooldown > -1)
                if (!useGlobalCooldown)
                    pluginInstance.getManager().updateWPCooldown(player, WPType.TRENCH_PICKAXE);
                else
                    pluginInstance.getManager().updateGlobalCooldown(player);
        }
    }

    @SuppressWarnings("deprecation")
    public void performTrenchShovelMagic(Player player, Block block, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.trenchshovel")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("trench-spade-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("trench-spade-section.cooldown");
        long cooldownRemainder;
        if (useGlobalCooldown) cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
        else cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.TRENCH_SHOVEL, cooldown);
        if (!(cooldown <= -1) && cooldownRemainder > 0) {
            pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
            return;
        }

        if (!pluginInstance.getManager().isInList(block.getType(), block.getData(),
                "trench-shovel-section.non-effected-materials")) {
            if (!pluginInstance.getManager().isLocationSafe(block.getLocation(), player, true, false, block.getType())) {
                pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
                return;
            }

            pluginInstance.getManager().removeItemUses(player, handItemStack, 1);
            pluginInstance.getManager().addToBlocksBroken(player, handItemStack, 1);
            int itemRadius = pluginInstance.getManager().getItemRadius(handItemStack), blockCounter = 0;
            boolean dropItems = pluginInstance.getConfig().getBoolean("trench-shovel-section.drop-items"),
                    autoPickup = pluginInstance.getConfig().getBoolean("trench-shovel-section.item-auto-pickup");

            block.getWorld().playSound(block.getLocation(), (!pluginInstance.getServerVersion().startsWith("v1_8")) ? Sound.BLOCK_STONE_BREAK : Sound.valueOf("STEP_STONE"), 1, 1);

            for (int y = ((-itemRadius) - 1); ++y <= itemRadius; )
                for (int x = ((-itemRadius) - 1); ++x <= itemRadius; )
                    for (int z = ((-itemRadius) - 1); ++z <= itemRadius; ) {
                        Block currentBlock = block.getRelative(x, y, z);
                        if (!pluginInstance.getManager().isInList(currentBlock.getType(), currentBlock.getData(), "trench-shovel-section.non-effected-materials")
                                && pluginInstance.getManager().isLocationSafe(currentBlock.getLocation(), player, true, false, currentBlock.getType())) {
                            pluginInstance.getManager().logToCoreProtect(player, currentBlock, true, false);
                            if (dropItems && autoPickup) {
                                List<ItemStack> dropList = new ArrayList<>(currentBlock.getDrops());
                                if (player.getInventory().firstEmpty() == -1) {
                                    if (currentBlock.getDrops().size() > 0)
                                        for (ItemStack dropItem : currentBlock.getDrops(handItemStack)) {
                                            dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                                    dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                                            player.getInventory().addItem(dropItem);
                                        }

                                } else {
                                    if (currentBlock.getDrops().size() > 0)
                                        for (ItemStack dropItem : currentBlock.getDrops(handItemStack)) {
                                            dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                                    dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                                            player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
                                        }
                                }

                                currentBlock.setType(Material.AIR);
                                currentBlock.getState().update(true, true);
                            } else if (dropItems) {
                                for (ItemStack dropItem : currentBlock.getDrops(handItemStack)) {
                                    dropItem.setAmount(pluginInstance.getManager().getSimulatedFortuneAmount(dropItem.getType(),
                                            dropItem.getDurability(), handItemStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)));
                                    currentBlock.getWorld().dropItemNaturally(currentBlock.getLocation(), dropItem);
                                }
                                currentBlock.setType(Material.AIR);
                                currentBlock.getState().update(true, true);
                            } else {
                                currentBlock.setType(Material.AIR);
                                currentBlock.getState().update(true, true);
                            }
                            blockCounter += 1;
                        }
                    }

            if (blockCounter > 0) pluginInstance.getManager().addToBlocksBroken(player, handItemStack, blockCounter);
            if (cooldown > -1)
                if (!useGlobalCooldown) pluginInstance.getManager().updateWPCooldown(player, WPType.TRENCH_SHOVEL);
                else pluginInstance.getManager().updateGlobalCooldown(player);
        }
    }

    public void performSpawnerPickaxeMagic(Player player, Block block, ItemStack handItemStack) {
        if (!player.hasPermission("factionwp.spawnerpickaxe")) {
            pluginInstance.getManager().sendCustomMessage(player, "no-permission-message");
            return;
        }

        boolean useGlobalCooldown = pluginInstance.getConfig().getBoolean("spawner-pickaxe-section.use-global-cooldown");
        int cooldown = pluginInstance.getConfig().getInt("spawner-pickaxe-section.cooldown");
        long cooldownRemainder;

        if (useGlobalCooldown) cooldownRemainder = pluginInstance.getManager().getGlobalCooldownRemainder(player, cooldown);
        else cooldownRemainder = pluginInstance.getManager().getCooldownRemainder(player, WPType.TRENCH_PICKAXE, cooldown);

        if (!(cooldown <= -1) && cooldownRemainder > 0) {
            pluginInstance.getManager().sendCustomMessage(player, "on-cooldown-message", "{time-left}:" + cooldownRemainder);
            return;
        }

        if (!block.getType().name().contains("SPAWNER")) {
            pluginInstance.getManager().sendCustomMessage(player, "not-spawner-message");
            return;
        }

        if (!pluginInstance.getManager().isLocationSafe(block.getLocation(), player, true, false, block.getType())) {
            pluginInstance.getManager().sendCustomMessage(player, "hook-fail-message");
            return;
        }

        final SpawnerPickaxeMode mode = pluginInstance.getManager().getSpawnerPickaxeModeMap().getOrDefault(player.getUniqueId(), SpawnerPickaxeMode.NATURAL);

        boolean isPlaced;
        if (pluginInstance.getServerVersion().startsWith("v1_8") || pluginInstance.getServerVersion().startsWith("v1_9")
                || pluginInstance.getServerVersion().startsWith("v1_10") || pluginInstance.getServerVersion().startsWith("v1_11")
                || pluginInstance.getServerVersion().startsWith("v1_12") || pluginInstance.getServerVersion().startsWith("v1_13")
                || pluginInstance.getServerVersion().startsWith("v1_14") || pluginInstance.getServerVersion().startsWith("v1_15")) {
            isPlaced = (block.getState().getData().getData() == 1);
        } else {
            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            isPlaced = spawner.isPlaced();
        }

        if (isPlaced && mode == SpawnerPickaxeMode.NATURAL) { // mode supposed to be placed

            pluginInstance.getManager().sendCustomMessage(player, "spawner-unbreakable-message",
                    ("{mode}:" + pluginInstance.getConfig().getString("spawner-pickaxe-section.modes." + SpawnerPickaxeMode.PLACED.name().toLowerCase())));
            return;

        } else if(!isPlaced && mode == SpawnerPickaxeMode.PLACED) { // mode supposed to be natural

            pluginInstance.getManager().sendCustomMessage(player, "spawner-unbreakable-message",
                    ("{mode}:" + pluginInstance.getConfig().getString("spawner-pickaxe-section.modes." + SpawnerPickaxeMode.NATURAL.name().toLowerCase())));
            return;
        }

        String entityId = null;
        ItemStack spawnerItem;
        if (pluginInstance.getSilkSpawnersHandler() != null) {
            entityId = pluginInstance.getSilkSpawnersHandler().getSilkUtil().getSpawnerEntityID(block);

            final SilkSpawners silkSpawners = pluginInstance.getSilkSpawnersHandler().getSilkSpawners();

            int amount;
            if (silkSpawners.mobs.contains("creatures." + entityId + ".dropAmount"))
                amount = silkSpawners.mobs.getInt("creatures." + entityId + ".dropAmount", 1);
            else amount = silkSpawners.config.getInt("dropAmount", 1);

            spawnerItem = pluginInstance.getSilkSpawnersHandler().getSilkUtil().newSpawnerItem(entityId,
                    pluginInstance.getSilkSpawnersHandler().getSilkUtil().getCustomSpawnerName(entityId), amount, false);
        } else {
            Material material = Material.getMaterial("SPAWNER");
            spawnerItem = new ItemStack(material == null ? Objects.requireNonNull(Material.getMaterial("MOB_SPAWNER")) : material);
        }

        pluginInstance.getManager().removeItemUses(player, handItemStack, 1);

        int blockCounter = 0;
        boolean dropItems = pluginInstance.getConfig().getBoolean("spawner-pickaxe-section.drop-items"),
                autoPickup = pluginInstance.getConfig().getBoolean("spawner-pickaxe-section.item-auto-pickup");

        block.getWorld().playSound(block.getLocation(), (pluginInstance.getServerVersion().startsWith("v1_8") || pluginInstance.getServerVersion().startsWith("v1_7"))
                ? Sound.valueOf("STEP_STONE") : Sound.BLOCK_STONE_BREAK, 1, 1);

        pluginInstance.getManager().logToCoreProtect(player, block, true, false);
        if (dropItems && autoPickup) {
            if (player.getInventory().firstEmpty() == -1) block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
            else player.getInventory().addItem(spawnerItem);

            block.setType(Material.AIR);
            block.getState().update(true, true);
        } else if (dropItems) {
            block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
            block.setType(Material.AIR);
            block.getState().update(true, true);
        } else {
            block.setType(Material.AIR);
            block.getState().update(true, true);
        }

        if (pluginInstance.getSilkSpawnersHandler() != null)
            pluginInstance.getSilkSpawnersHandler().getSilkSpawners().informPlayer(player, ChatColor.translateAlternateColorCodes('\u0026',
                            Objects.requireNonNull(pluginInstance.getSilkSpawnersHandler().getSilkSpawners().localization.getString("spawnerBroken")))
                    .replace("%creature%", pluginInstance.getSilkSpawnersHandler().getSilkUtil().getCreatureName(entityId)));

        pluginInstance.getManager().addToBlocksBroken(player, handItemStack, (blockCounter + 1));
        if (cooldown > -1)
            if (!useGlobalCooldown) pluginInstance.getManager().updateWPCooldown(player, WPType.SPAWNER_PICKAXE);
            else pluginInstance.getManager().updateGlobalCooldown(player);
    }

}