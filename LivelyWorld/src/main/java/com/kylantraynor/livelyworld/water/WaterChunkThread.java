package com.kylantraynor.livelyworld.water;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;

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
		int i = 0;
		while(i < WaterChunk.chunks.size()){
			WaterChunk c = WaterChunk.chunks.get(i);
			if(c == null){
				i++; continue;
			}
			if(c.isLoaded() && c.getWorld().isChunkLoaded(c.getX(), c.getZ())){
				try{
					c.randomTick();
				} catch (Exception e){
					LivelyWorld.getInstance().getLogger().severe("Exception while ticking water chunk at " + c.getX() + "," + c.getZ()+ ".");
					e.printStackTrace();
				}
			}
			i++;
		}
	}

	private void unloadChunks() {
		int count = 5;
		int i = 0;
		while(i < WaterChunk.chunks.size() && count > 0){
			WaterChunk c = WaterChunk.chunks.get(i);
			if(c == null){
				i++; continue;
			}
			if(c.isLoaded() && !c.getWorld().isChunkLoaded(c.getX(), c.getZ())){
				c.unload();
				//LivelyWorld.getInstance().getLogger().info("Unloading Chunk at " + c.getX() + ", " + c.getZ() + ", Total: " + WaterChunk.chunks.size());
				WaterChunk.chunks.remove(i);
				count--;
			}
			i++;
		}
	}

	private void loadChunks() {
		int count = 5;
		int i = 0;
		while(i < WaterChunk.chunks.size() && count > 0){
			WaterChunk c = WaterChunk.chunks.get(i);
			if(c == null){
				i++; continue;
			}
			if(!c.isLoaded() && c.getWorld().isChunkLoaded(c.getX(), c.getZ())){
				c.load();
				count--;
			}
			i++;
		}
		i = 0;
		Chunk[] loadedChunks = Bukkit.getServer().getWorld("world").getLoadedChunks();
		while(i < loadedChunks.length && count > 0){
			Chunk c = loadedChunks[i];
			WaterChunk wc = WaterChunk.get(c.getWorld(), c.getX(), c.getZ());
			if(!wc.isLoaded() && c.isLoaded()){
				wc.load();
				count--;
			}
			i++;
		}
	}
	
}