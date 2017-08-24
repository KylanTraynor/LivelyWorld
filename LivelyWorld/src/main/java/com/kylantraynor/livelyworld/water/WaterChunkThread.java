package com.kylantraynor.livelyworld.water;

import com.kylantraynor.livelyworld.LivelyWorld;

public class WaterChunkThread extends Thread {
	
	private String name = "WaterChunk Thread";
	
	public void run(){
		try{
			while (!isInterrupted()) {
				unloadChunks();
			    loadChunks();
			    updateChunks();
			    cleanList();
			}
		} catch (Exception e){
			e.printStackTrace();
			WaterChunk.unloadAll();
		}
	}

	private void cleanList() {
		int i = 0;
		while(i < WaterChunk.chunks.size()){
			WaterChunk c = WaterChunk.chunks.get(i);
			if(c == null){
				i++; continue;
			}
			if(!c.isLoaded() && !c.getWorld().isChunkLoaded(c.getX(), c.getZ())){
				WaterChunk.chunks.remove(i);
				continue;
			}
			i++;
		}
	}

	private void updateChunks() {
		
	}

	private void unloadChunks() {
		int i = 0;
		while(i < WaterChunk.chunks.size()){
			WaterChunk c = WaterChunk.chunks.get(i);
			if(c == null){
				i++; continue;
			}
			if(c.isLoaded() && !c.getWorld().isChunkLoaded(c.getX(), c.getZ())){
				c.unload();
				//LivelyWorld.getInstance().getLogger().info("Unloading Chunk at " + c.getX() + ", " + c.getZ() + ", Total: " + WaterChunk.chunks.size());
				WaterChunk.chunks.remove(i);
				return;
			}
			i++;
		}
	}

	private void loadChunks() {
		int i = 0;
		while(i < WaterChunk.chunks.size()){
			WaterChunk c = WaterChunk.chunks.get(i);
			if(c == null){
				i++; continue;
			}
			if(!c.isLoaded() && c.getWorld().isChunkLoaded(c.getX(), c.getZ())){
				c.load();
				return;
			}
			i++;
		}
	}
	
}
