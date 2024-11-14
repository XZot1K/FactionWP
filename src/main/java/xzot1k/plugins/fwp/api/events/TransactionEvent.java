/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import xzot1k.plugins.fwp.api.enums.WPType;

public class TransactionEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private Player player;
    private Location location;
    private WPType wpType;
    private ItemStack itemStack;
    private double economyAmount;

    public TransactionEvent(Player player, Location location, WPType wpType, ItemStack itemStack, double economyAmount)
    {
        setPlayer(player);
        setLocation(location);
        setWPType(wpType);
        setItemStack(itemStack);
        setEconomyAmount(economyAmount);
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

    public Player getPlayer()
    {
        return player;
    }

    private void setPlayer(Player player)
    {
        this.player = player;
    }

    /**
     * Get the interacted block.
     * @return returns the block that interacted with's location.
     */
    public Location getLocation()
    {
        return location;
    }

    private void setLocation(Location location)
    {
        this.location = location;
    }

    public WPType getWPType()
    {
        return wpType;
    }

    private void setWPType(WPType wpType)
    {
        this.wpType = wpType;
    }

    public ItemStack getItemStack()
    {
        return itemStack;
    }

    private void setItemStack(ItemStack itemStack)
    {
        this.itemStack = itemStack;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    /**
     * get the total amount from the interaction.
     * @return total amount being added to the player.
     */
    public double getEconomyAmount() {
        return economyAmount;
    }

    /**
     * set the total amount from the interaction.
     * @return set the total amount being added to the player.
     */
    public void setEconomyAmount(double economyAmount) {
        this.economyAmount = economyAmount;
    }
}
