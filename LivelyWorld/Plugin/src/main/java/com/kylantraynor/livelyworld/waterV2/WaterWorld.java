package com.kylantraynor.livelyworld.waterV2;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

public class WaterWorld {

    final World bukkitWorld;
    final Queue<WaterChunkCoords> pendingLoadChunks;
    final Queue<WaterChunk> pendingUnloadChunks;
    final Map<WaterChunkCoords, WaterChunk> loadedChunks;

    public WaterWorld(World world){
        bukkitWorld = world;
        pendingLoadChunks = new ConcurrentLinkedQueue<>();
        pendingUnloadChunks = new ConcurrentLinkedQueue<>();
        loadedChunks = new ConcurrentHashMap<>();
    }

    /**
     * Process all loaded {@link WaterChunk} updates.
     */
    public void update(){
        long time = System.currentTimeMillis();
        for(Chunk c : bukkitWorld.getLoadedChunks()){
            if(!isChunkLoaded(c.getX(), c.getZ()) && closestPlayerDistance(c.getX(), c.getZ()) < 6){
                pendingLoadChunks.offer(new WaterChunkCoords(c.getX(), c.getZ()));
            }
        }

        WaterChunk[] chunks = loadedChunks.values().toArray(new WaterChunk[0]);
        int refreshedChunks = 0;
        for(WaterChunk c : chunks){
            int dist = closestPlayerDistance(c.coords.x, c.coords.z);
            if(bukkitWorld.isChunkLoaded(c.coords.x, c.coords.z) && dist < 8){
                if(c.needsRefresh){
                    if(refreshedChunks < 2){
                        c.refreshChunkBlocks(bukkitWorld.getChunkAt(c.coords.x, c.coords.z));
                        refreshedChunks++;
                    }
                } else if(dist < 8 * Utils.fastRandomFloat()) {
                    if(c.lastUpdate + 500 < time){
                        c.updateBukkitChunk(getBukkitChunk(c.coords.x, c.coords.z));
                    }
                }
            } else {
                pendingUnloadChunks.offer(c);
                loadedChunks.remove(new WaterChunkCoords(c.coords.x, c.coords.z));
            }
        }
        int elapsed = (int) (System.currentTimeMillis() - time);
        if(elapsed > 50){
            LivelyWorld.getInstance().getLogger().info("Water Sync Update: " + loadedChunks.size() + " chunks currently loaded! (" + elapsed + "ms for "+ refreshedChunks + " refreshed chunks)");
        }
    }

    public int closestPlayerDistance(int x, int z){
        int closestDistance = 15;
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            if(p.getWorld() == bukkitWorld){
                int d = Utils.manhattanDistance(x,z, p.getLocation().getBlockX() >> 4, p.getLocation().getBlockZ() >> 4);
                closestDistance = d < closestDistance ? d : closestDistance;
            }
        }
        return closestDistance;
    }

    /**
     * Get the {@link WaterChunk} corresponding to the given coordinates, or {@code null}
     * if the real Chunk is not loaded.
     * @param x {@code int} coordinate
     * @param z {@code int} coordinate
     * @return {@link WaterChunk} or {@code null}
     */
    public WaterChunk getChunk(int x, int z){
        //if(!bukkitWorld.isChunkLoaded(x,z)) return null;

        WaterChunkCoords wcc = new WaterChunkCoords(x, z);
        return loadedChunks.get(wcc);
    }

    /**
     * Get the {@link Chunk} at the given coordinates.
     * This function will try to load the chunk if it wasn't loaded.
     * @param x {@code int} coordinate
     * @param z {@code int} coordinate
     * @return {@link Chunk}
     */
    public Chunk getBukkitChunk(int x, int z){
        return bukkitWorld.getChunkAt(x,z);
    }

    /*public void loadChunk(Chunk chunk){
        WaterChunkCoords wcc = new WaterChunkCoords(chunk.getX(),chunk.getZ());
        if(!loadedChunks.containsKey(wcc)){
            WaterChunk wc = new WaterChunk(this, wcc);
            WaterChunkUtils.loadFromFile(wc);
            wc.refreshChunkBlocks(chunk);
            loadedChunks.put(wcc, wc);
        }
    }*/

    /**
     * Unloads all the loaded chunks.
     */
    public void unloadAll(){
        for(WaterChunkCoords wcc : loadedChunks.keySet()){
            unloadChunk(wcc.x, wcc.z);
        }
    }

    /**
     * Unload the {@link WaterChunk} at the given coordinates.
     * @param x {@code int} coordinate
     * @param z {@code int} coordinate
     */
    public void unloadChunk(int x, int z){
        WaterChunkCoords wcc = new WaterChunkCoords(x,z);
        if(loadedChunks.containsKey(wcc)){
            WaterChunk wc = loadedChunks.remove(wcc);
            WaterChunkUtils.save(wc);
        }
    }

    /**
     * Check if the {@link WaterChunk} at the given location is loaded.
     * @param x {@code int} Coordinate
     * @param z {@code int} Coordinate
     * @return {@code true} if it is loaded, {@code false} otherwise
     */
    public boolean isChunkLoaded(int x, int z){
        return loadedChunks.containsKey(new WaterChunkCoords(x,z));
    }

    /**
     * Get the set of entries of all the loaded {@link WaterChunk WaterChunks}.
     * @return {@link Set} of {@link Map.Entry}
     */
    public Set<Map.Entry<WaterChunkCoords, WaterChunk>> entrySet(){
        return loadedChunks.entrySet();
    }


    public void setWaterAt(int x, int y, int z){
        int chunkX = Math.floorDiv(x, WaterChunk.xLength);
        int chunkZ = Math.floorDiv(z, WaterChunk.zLength);
        if(isChunkLoaded(chunkX,chunkZ)){
            getChunk(chunkX,chunkZ).setWaterAt(Math.floorMod(x,WaterChunk.xLength), y, Math.floorMod(z, WaterChunk.zLength), true);
        }
    }
}
