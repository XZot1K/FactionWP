/*
 * Copyright (c) 2019. All rights reserved.
 */

package xzot1k.plugins.fwp.core.hooks;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class WorldGuardHandler {

    private final WorldGuardPlugin worldGuardPlugin;

    public WorldGuardHandler() {
        worldGuardPlugin = WorldGuardPlugin.inst();
    }

    /**
     * Checks if the location is within a region that also has correct flag setup.
     *
     * @param player   the player to check.
     * @param location The location.
     * @return Whether the check passed.
     */
    public boolean passedWorldGuardHook(Player player, Location location) {
        if (worldGuardPlugin == null) return true;
        if (worldGuardPlugin.getDescription().getVersion().startsWith("7") || worldGuardPlugin.getDescription().getVersion().startsWith("8")) {
            com.sk89q.worldguard.protection.regions.RegionQuery query = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location loc = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
            com.sk89q.worldedit.world.World world = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(Objects.requireNonNull(location.getWorld()));
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            if (!com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, world))
                return query.testState(loc, localPlayer, com.sk89q.worldguard.protection.flags.Flags.BUILD);
        } else {
            try {
                Method method = worldGuardPlugin.getClass().getMethod("canBuild", Player.class, Location.class);
                return (boolean) method.invoke(worldGuardPlugin, player, location);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public boolean regionCheckFlags(@NotNull Player player, @NotNull Location location, @NotNull StateFlag... stateFlags) {
        if (worldGuardPlugin == null) return true;
        if (worldGuardPlugin.getDescription().getVersion().startsWith("7") || worldGuardPlugin.getDescription().getVersion().startsWith("8")) {
            RegionQuery query = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location loc = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
            com.sk89q.worldedit.world.World world = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(Objects.requireNonNull(location.getWorld()));
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            if (!com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, world))
                return query.testState(loc, localPlayer, stateFlags);
        } else {
            final WorldGuardPlugin wgPlugin = WorldGuardPlugin.inst();
            try {
                Method rcMethod = wgPlugin.getClass().getDeclaredMethod("getRegionContainer");
                RegionContainer rc = (RegionContainer) rcMethod.invoke(wgPlugin);
                RegionQuery query = rc.createQuery();
                Method tsMethod = query.getClass().getDeclaredMethod("testState", Location.class, Player.class, StateFlag[].class);
                return (boolean) tsMethod.invoke(query, location, player, stateFlags);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

}
