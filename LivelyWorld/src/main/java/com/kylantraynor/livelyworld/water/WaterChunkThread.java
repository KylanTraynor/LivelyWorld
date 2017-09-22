package com.kylantraynor.livelyworld.water;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;

public class WaterChunkThread extends Thread {
	
	private String name = "WaterChunk Thread";
	final private static ConcurrentMap<World, Chunk[]> loadedChunks = new ConcurrentHashMap<World, Chunk[]>();
	
	public void run(){
		try{
			long lastUpdate = 0;
			while (!isInterrupted()) {
				updateListOfLoadedChunks();
				unloadChunks();
				loadChunks();
				if(System.currentTimeMillis() >= lastUpdate + 50){
					updateChunks();
					lastUpdate = System.currentTimeMillis();
				}
			    cleanList();
			}
		} catch (Exception e){
			e.printStackTrace();
			WaterChunk.unloadAll();
		}
	}

	private void updateListOfLoadedChunks() {
		BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				for(World w : Bukkit.getWorlds()){
					if(w.getName().equals("world")){
						loadedChunks.put(w, w.getLoadedChunks());
					}
				}
			}
		
		};
		br.runTask(LivelyWorld.getInstance());
	}
	
	public static boolean isChunkLoaded(World w, int chunkX, int chunkZ){
		Chunk[] chunks = null;
		chunks = loadedChunks.get(w);
		for(Chunk c : chunks){
			if(c.getX() == chunkX && c.getZ() == chunkZ) continue;
			return true;
		}
		return false;
	}

	private void cleanList() {
		int i = 0;
		while(i < WaterChunk.chunks.size()){
			WaterChunk c = WaterChunk.chunks.get(i);
			if(c == null){
				i++; continue;
			}
			if(!c.isLoaded() && !isChunkLoaded(c.getWorld(), c.getX(), c.getZ())){
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
			if(c.isLoaded() && isChunkLoaded(c.getWorld(), c.getX(), c.getZ())){
				try{
					c.tickAll();
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
			if(c.isLoaded() && !isChunkLoaded(c.getWorld(), c.getX(), c.getZ())){
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
			if(!c.isLoaded() && isChunkLoaded(c.getWorld(), c.getX(), c.getZ())){
				c.load();
				count--;
			}
			i++;
		}
		/*i = 0;
		try{
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
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	
}
