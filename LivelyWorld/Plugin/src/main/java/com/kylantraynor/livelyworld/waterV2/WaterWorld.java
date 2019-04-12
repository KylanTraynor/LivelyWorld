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

    public void loadChunksAroundPlayers(){
        for(Player p : bukkitWorld.getPlayers()){
            int loaded = 0;
            List<WaterChunkCoords> checked = new ArrayList<>();
            LinkedList<WaterChunkCoords> queue = new LinkedList<>();
            queue.add(new WaterChunkCoords(p.getLocation().getChunk().getX(), p.getLocation().getChunk().getZ()));
            while(queue.size() > 0 && loaded < 10){
                WaterChunkCoords wcc = queue.pollFirst();
                if(checked.contains(wcc)) continue;
                checked.add(wcc);
                if(Utils.euclidianDistanceSquared(p.getLocation().getChunk().getX(), p.getLocation().getChunk().getZ(), wcc.x, wcc.z) >= 6*6) continue;
                queue.add(new WaterChunkCoords(wcc.x - 1, wcc.z));
                queue.add(new WaterChunkCoords(wcc.x + 1, wcc.z));
                queue.add(new WaterChunkCoords(wcc.x, wcc.z - 1));
                queue.add(new WaterChunkCoords(wcc.x, wcc.z + 1));
                if(loadedChunks.containsKey(wcc) || pendingLoadChunks.contains(wcc)) continue;
                pendingLoadChunks.add(wcc);
                loaded++;
            }
        }
    }

    /**
     * Process all loaded {@link WaterChunk} updates.
     */
    public void update(){
        long time = System.currentTimeMillis();
        loadChunksAroundPlayers();
        /*for(Chunk c : bukkitWorld.getLoadedChunks()){
            if(!isChunkLoaded(c.getX(), c.getZ()) && closestPlayerDistance(c.getX(), c.getZ()) < 6){
                pendingLoadChunks.offer(new WaterChunkCoords(c.getX(), c.getZ()));
            }
        }*/

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
                        if(Utils.fastRandomFloat() < 0.005){
                            c.updateFullBukkitChunk(getBukkitChunk(c.coords.x, c.coords.z));
                        } else {
                            c.updateBukkitChunk(getBukkitChunk(c.coords.x, c.coords.z));
                        }
                    }
                }
            } else {
                pendingUnloadChunks.offer(
                        loadedChunks.remove(new WaterChunkCoords(c.coords.x, c.coords.z))
                );
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

    /**
     * Adds the given amount of water at the given world location.
     * @param blockX {@code int} World coordinate
     * @param blockY {@code int} World coordinate
     * @param blockZ {@code int} World coordinate
     * @param amount {@code int} Amount of water to add
     * @return {@code int} Amount of water that could not be added
     */
    public int addWaterAt(int blockX, int blockY, int blockZ, int amount){
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;

        int x = Utils.floorMod2(blockX, 4);
        int z = Utils.floorMod2(blockZ, 4);

        if(isChunkLoaded(chunkX, chunkZ)){
            return getChunk(chunkX, chunkZ).addWaterIn(new BlockLocation(x, blockY, z), amount);
        }
        return amount;
    }

    public int removeWaterAt(int blockX, int blockY, int blockZ, int amount){
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;

        int x = Utils.floorMod2(blockX, 4);
        int z = Utils.floorMod2(blockZ, 4);

        if(isChunkLoaded(chunkX, chunkZ)){
            return getChunk(chunkX, chunkZ).removeWaterIn(new BlockLocation(x, blockY, z), amount);
        }
        return amount;
    }

    public void setWaterAt(int x, int y, int z){
        int chunkX = Math.floorDiv(x, WaterChunk.xLength);
        int chunkZ = Math.floorDiv(z, WaterChunk.zLength);
        if(isChunkLoaded(chunkX,chunkZ)){
            getChunk(chunkX,chunkZ).setWaterAt(Math.floorMod(x,WaterChunk.xLength), y, Math.floorMod(z, WaterChunk.zLength), true);
        }
    }
}
