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

public class ToolUseEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private Player player;
    private Location location;
    private WPType wpType;
    private ItemStack itemStack;

    public ToolUseEvent(Player player, Location location, WPType wpType, ItemStack itemStack)
    {
        setPlayer(player);
        setLocation(location);
        setWPType(wpType);
        setItemStack(itemStack);
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
}
