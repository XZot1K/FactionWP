/*
 * Copyright (c) 2020 XZot1K, All rights reserved.
 */

package xzot1k.plugins.fwp.core.objects.versions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.fwp.FactionWP;
import xzot1k.plugins.fwp.core.packets.actionbar.BarHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class ABH_Old implements BarHandler {

    private Class<?> icbClass, csClass, packetClass, packetChatClass, craftPlayerClass;

    public ABH_Old() {
        try {
            icbClass = Class.forName("net.minecraft.server."
                    + FactionWP.getPluginInstance().getServerVersion() + ".IChatBaseComponent");

            csClass = Class.forName("net.minecraft.server."
                    + FactionWP.getPluginInstance().getServerVersion() + ".IChatBaseComponent$ChatSerializer");

            packetClass = Class.forName("net.minecraft.server."
                    + FactionWP.getPluginInstance().getServerVersion() + ".Packet");

            packetChatClass = Class.forName("net.minecraft.server."
                    + FactionWP.getPluginInstance().getServerVersion() + ".PacketPlayOutChat");

            craftPlayerClass = Class.forName("org.bukkit.craftbukkit."
                    + FactionWP.getPluginInstance().getServerVersion() + ".entity.CraftPlayer");
        } catch (ClassNotFoundException e) {e.printStackTrace();}
    }

    @Override
    public void sendActionBar(@NotNull Player player, @NotNull String message) {
        try {
            final Method method = csClass.getDeclaredMethod("a", String.class);
            final Object icbc = method.invoke(csClass, ("{\"text\": \""
                    + FactionWP.getPluginInstance().getManager().colorText(message) + "\"}"));

            final Constructor<?> packetConstructor = packetChatClass.getConstructor(icbClass, byte.class);
            final Object packet = packetConstructor.newInstance(icbc, (byte) 2);

            final Object cPlayer = craftPlayerClass.cast(player);
            final Object getHandle = craftPlayerClass.getDeclaredMethod("getHandle").invoke(cPlayer);
            final Object pConnection = getHandle.getClass().getDeclaredField("playerConnection").get(getHandle);
            final Method sendPacket = pConnection.getClass().getDeclaredMethod("sendPacket", packetClass);
            sendPacket.invoke(pConnection, packet);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                 | InstantiationException | NoSuchFieldException e) {e.printStackTrace();}
    }

}
