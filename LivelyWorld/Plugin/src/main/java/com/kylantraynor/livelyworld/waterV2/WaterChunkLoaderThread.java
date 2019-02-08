package com.kylantraynor.livelyworld.waterV2;

import com.kylantraynor.livelyworld.LivelyWorld;
import org.apache.commons.math3.linear.SymmLQ;

public class WaterChunkLoaderThread extends Thread {

    public void run(){
        try{
            while (!isInterrupted()) {
                for(WaterWorld world : LivelyWorld.getInstance().getWaterModule().worlds.values()){
                    int loadingChunks = 5;
                    while(world.pendingLoadChunks.peek() != null && loadingChunks > 0){
                        WaterChunkCoords wcc = world.pendingLoadChunks.poll();
                        if(!world.loadedChunks.containsKey(wcc)){
                            WaterChunk wc = new WaterChunk(world, wcc);
                            WaterChunkUtils.loadFromFile(wc);
                            world.loadedChunks.put(wcc, wc);
                            loadingChunks--;
                        }
                    }
                    int savingChunks = 5;
                    while(world.pendingUnloadChunks.peek() != null && savingChunks > 0){
                        WaterChunk wc = world.pendingUnloadChunks.poll();
                        WaterChunkUtils.save(wc);
                        savingChunks--;
                    }
                    long time = System.currentTimeMillis();
                    int updatedCells = 0;
                    for(WaterChunk chunk : world.loadedChunks.values()){
                        updatedCells += chunk.update();
                    }
                    int elapsed = (int)(System.currentTimeMillis() - time);
                    if(elapsed > 1000){
                        LivelyWorld.getInstance().getLogger().info("Water Async Update: " + world.loadedChunks.size() + " chunks currently loaded! (" + elapsed + "ms for "+updatedCells+" updated cells)");
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            LivelyWorld.getInstance().getWaterModule().unloadAll();
        }
    }

}
