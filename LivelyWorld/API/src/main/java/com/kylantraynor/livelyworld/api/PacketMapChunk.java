package com.kylantraynor.livelyworld.api;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface PacketMapChunk {
	/**
     * Sends this packet to a player.
     * <br>You still need to refresh it manually with <code>world.refreshChunk(...)</code>.
     *
     * @param player The player.
     */
	
	public void send(final Player player);
	
    /**
     * Refresh a chunk.
     *
     * @param chunk The chunk.
     */
   
    public static void refreshChunk(final Server server, final Chunk chunk) {
        refreshChunk(server, chunk.getWorld(), chunk.getX(), chunk.getZ());
    }
   
    /**
     * Wrapper for <code>world.refreshChunk(...)</code>
     *
     * @param world The world.
     * @param x The chunk's X.
     * @param z The chunk's Z.
     */
   
    public static void refreshChunk(final Server server, final World world, final int x, final int z) {
        final Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        refreshChunk(server, world, x, z, players.toArray(new Player[players.size()]));
    }
   
    /**
     * Refresh a chunk for the selected players.
     *
     * @param world The chunk's world.
     * @param x The chunk's X.
     * @param z The chunk's Z.
     * @param players The players.
     */
   
    public static void refreshChunk(final Server server, final World world, final int x, final int z, final Player... players) {
    	String packageName = server.getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        PacketMapChunk packet = null;
        try {
            final Class<?> clazz = Class.forName("com.kylantraynor.livelyworld." + version + ".PacketMapChunkHandler");
            if (PacketMapChunk.class.isAssignableFrom(clazz)) {
                packet = (PacketMapChunk) clazz.getConstructor().newInstance();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            server.getLogger().warning("[LivelyWorld] This CraftBukkit version ("+version+") is not supported. Climate biome updates will not work.");
        }
        if(packet == null) return;
        packet.setChunk(world.getChunkAt(x, z));
        for(final Player player : players) {
            packet.send(player);
        }
        world.refreshChunk(x, z);
    }

    /**
     * Sets the chunk of this packet.
     * @param chunk
     */
	public void setChunk(Chunk chunk);
}
