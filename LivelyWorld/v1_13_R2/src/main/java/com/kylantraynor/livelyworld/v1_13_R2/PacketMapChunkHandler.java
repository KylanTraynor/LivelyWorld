package com.kylantraynor.livelyworld.v1_13_R2;

import com.kylantraynor.livelyworld.api.PacketMapChunk;
import net.minecraft.server.v1_13_R2.PacketPlayOutMapChunk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collection;

public class PacketMapChunkHandler implements PacketMapChunk {
	private net.minecraft.server.v1_13_R2.Chunk chunk;
	   
	/**
     * Creates a PacketMapChunk.
     */
   
    public PacketMapChunkHandler() {
    	
    }
	
    /**
     * Creates a PacketMapChunk.
     *
     * @param world The chunk's world.
     * @param x The chunk's X.
     * @param z The chunk's Z.
     */
   
    public PacketMapChunkHandler(final World world, final int x, final int z) {
        this(world.getChunkAt(x, z));
    }
   
    /**
     * Creates a PacketMapChunk.
     *
     * @param chunk The chunk.
     */
   
    public PacketMapChunkHandler(final Chunk chunk) {
        this.chunk = ((CraftChunk)chunk).getHandle();
    }

    /**
     * Sends this packet to a player.
     * <br>You still need to refresh it manually with <code>world.refreshChunk(...)</code>.
     *
     * @param player The player.
     */
    public final void send(final Player player) {
    	PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, 20);
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
    }

    /**
     * Refresh a chunk.
     *
     * @param chunk The chunk.
     */

    public static final void refreshChunk(final Chunk chunk) {
        refreshChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    /**
     * Wrapper for <code>world.refreshChunk(...)</code>
     *
     * @param world The world.
     * @param x The chunk's X.
     * @param z The chunk's Z.
     */

    public static final void refreshChunk(final World world, final int x, final int z) {
        final Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        refreshChunk(world, x, z, players.toArray(new Player[0]));
    }

    /**
     * Refresh a chunk for the selected players.
     *
     * @param world The chunk's world.
     * @param x The chunk's X.
     * @param z The chunk's Z.
     * @param players The players.
     */

    public static final void refreshChunk(final World world, final int x, final int z, final Player... players) {
        final PacketMapChunk packet = new PacketMapChunkHandler(world.getChunkAt(x, z));
        for(final Player player : players) {
            packet.send(player);
        }
        world.refreshChunk(x, z);
    }

	@Override
	public void setChunk(Chunk chunk) {
		this.chunk = ((CraftChunk)chunk).getHandle();
	}
}
