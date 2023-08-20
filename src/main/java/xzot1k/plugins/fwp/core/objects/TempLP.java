/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core.objects;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TempLP
{
    private Player player;
    private Location location;

    public TempLP(Player player, Location location)
    {
        setPlayer(player);
        setLocation(location);
    }

    public Location getLocation()
    {
        return location;
    }

    public void setLocation(Location location)
    {
        this.location = location;
    }

    public Player getPlayer()
    {
        return player;
    }

    public void setPlayer(Player player)
    {
        this.player = player;
    }
}
