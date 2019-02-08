package com.kylantraynor.livelyworld.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class WorldUtil {
    public static boolean isLoaded(Location location) {
        return isLoaded(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static boolean isLoaded(Block block) {
        return isLoaded(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public static boolean isLoaded(org.bukkit.World world, double x, double y, double z) {
        return isLoaded(world, MathUtil.toChunk(x), MathUtil.toChunk(z));
    }

    public static boolean isLoaded(org.bukkit.World world, int x, int y, int z) {
        return isLoaded(world, x >> 4, z >> 4);
    }

    public static boolean isLoaded(org.bukkit.World world, int chunkX, int chunkZ) {
        if (world == null) {
            return false;
        }
        return world.isChunkLoaded(chunkX, chunkZ);
    }
}
