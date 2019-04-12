package com.kylantraynor.livelyworld.waterV2;

import com.kylantraynor.livelyworld.LivelyWorld;

public class WaterChunkLoaderThread extends Thread {

    public void run(){
        try{
            while (!isInterrupted()) {
                for(WaterWorld world : LivelyWorld.getInstance().getWaterModule().worlds.values()){
                    int loadingChunks = 10;
                    while(world.pendingLoadChunks.peek() != null && loadingChunks > 0){
                        WaterChunkCoords wcc = world.pendingLoadChunks.poll();
                        // Make sure that we only load chunks that are not yet in use.
                        if(!world.loadedChunks.containsKey(wcc)){
                            WaterChunk wc = new WaterChunk(world, wcc);
                            WaterChunkUtils.loadFromFile(wc);// || Utils.fastRandomFloat() < .01f;
                            world.loadedChunks.put(wcc, wc);
                            loadingChunks--;
                        }
                    }
                    int savingChunks = 10;
                    while(world.pendingUnloadChunks.peek() != null && savingChunks > 0){
                        WaterChunk wc = world.pendingUnloadChunks.poll();
                        // Make sure that we only unload chunks that are no longer in use.
                        if(!world.loadedChunks.containsKey(wc.coords)){
                            WaterChunkUtils.save(wc);
                            savingChunks--;
                        }
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
