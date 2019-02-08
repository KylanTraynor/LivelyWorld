package com.kylantraynor.livelyworld.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class AsyncChunk {
    Future<ChunkSnapshot> chunkFuture;

    public AsyncChunk(Future<ChunkSnapshot> chunkFuture){
        this.chunkFuture = chunkFuture;
    }

    public boolean isReady(){
        return chunkFuture.isDone();
    }

    public boolean isValid() {
        try {
            return isReady() && chunkFuture.get() != null;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Biome getBiome(int x, int z){
        try {
            return chunkFuture.get() != null ? chunkFuture.get().getBiome(x,z) : null;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Material getType(int x, int y, int z){
        try {
            return chunkFuture.get() != null ? chunkFuture.get().getBlockType(x,y,z) : null;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BlockData getBlockData(int x, int y, int z){
        try {
            return chunkFuture.get() != null ? chunkFuture.get().getBlockData(x,y,z) : null;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static AsyncChunk getAt(World world, int chunkX, int chunkZ){
        return new AsyncChunk(ChunkUtil.getSyncChunkSnapshot(world, chunkX, chunkZ));
    }
}
