package com.kylantraynor.livelyworld.utils;

import com.kylantraynor.livelyworld.LivelyWorld;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ChunkUtil {

    /**
     * Gets a chunk from a world without loading or generating it.
     * This method uses the server's main thread and returns a future object.
     *
     * @param world to obtain the chunk from
     * @param x - coordinate of the chunk
     * @param z - coordinate of the chunk
     * @return The chunk, or null if it is not loaded
     */
    public static Future<Chunk> getSyncChunk(World world, final int x, final int z) {
        return Bukkit.getScheduler().callSyncMethod(LivelyWorld.getInstance(), new Callable<Chunk>() {
            @Override
            public Chunk call() throws Exception {
                if (world.isChunkLoaded(x, z)) {
                    return world.getChunkAt(x, z);
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * Gets a chunk from a world without loading or generating it.
     * This method uses the server's main thread and returns a future object.
     *
     * @param world to obtain the chunk from
     * @param x - coordinate of the chunk
     * @param z - coordinate of the chunk
     * @return The chunk, or null if it is not loaded
     */
    public static Future<ChunkSnapshot> getSyncChunkSnapshot(World world, final int x, final int z) {
        return Bukkit.getScheduler().callSyncMethod(LivelyWorld.getInstance(), new Callable<ChunkSnapshot>() {
            @Override
            public ChunkSnapshot call() throws Exception {
                if (world.isChunkLoaded(x, z)) {
                    return world.getChunkAt(x, z).getChunkSnapshot(false, true, false);
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * Gets a chunk from a world without loading or generating it.
     * This method blocks until the main thread returns a chunk or null.
     *
     * @param world to obtain the chunk from
     * @param x - coordinate of the chunk
     * @param z - coordinate of the chunk
     * @return The chunk, or null if it is not loaded
     */
    public static Chunk getChunk(World world, final int x, final int z) {
        if(Thread.currentThread().getId() == LivelyWorld.getInstance().getMainThreadId()){
            if(world.isChunkLoaded(x,z)){
                return world.getChunkAt(x,z);
            } else {
                return null;
            }
        } else {
            Future<Chunk> fc = getSyncChunk(world, x,z);
            try {
                return fc.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Gets a chunk from a world without loading or generating it.
     * This method blocks until the main thread returns a chunk or null.
     *
     * @param world to obtain the chunk from
     * @param x - coordinate of the chunk
     * @param z - coordinate of the chunk
     * @return The chunk, or null if it is not loaded
     */
    public static ChunkSnapshot getChunkSnapshot(World world, final int x, final int z) {
        if(Thread.currentThread().getId() == LivelyWorld.getInstance().getMainThreadId()){
            if(world.isChunkLoaded(x,z)){
                return world.getChunkAt(x,z).getChunkSnapshot(false, true, false);
            } else {
                return null;
            }
        } else {
            Future<ChunkSnapshot> fc = getSyncChunkSnapshot(world, x,z);
            try {
                return fc.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
