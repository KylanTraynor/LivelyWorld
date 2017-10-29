package com.kylantraynor.livelyworld.water;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils.Enclosed;
import com.kylantraynor.livelyworld.Utils.PrioritizedLock;
import com.kylantraynor.livelyworld.Utils.SmallChunkData;

public class WaterChunkThread extends Thread {
	
	private String name = "WaterChunk Thread";
	final public static Map<String, List<SmallChunkData>> loadedChunks = new HashMap<String, List<SmallChunkData>>();
	final static Enclosed<Chunk[]> chunksFetcher = new Enclosed<Chunk[]>();
	final static PrioritizedLock mainLocker = new PrioritizedLock(LivelyWorld.getInstance().getMainThreadId());
	
	public void run(){
		try{
			long lastUpdate = 0;
			while (!isInterrupted()) {
				unloadChunks();
				loadChunks();
				if(System.currentTimeMillis() >= lastUpdate + 50){
					//updateListOfLoadedChunks();
					//LivelyWorld.getInstance().getLogger().info("Updating water chunks...");
					updateChunks();
					//LivelyWorld.getInstance().getLogger().info("Done updating water chunks.");
					lastUpdate = System.currentTimeMillis();
				}
			    cleanList();
			}
		} catch (Exception e){
			e.printStackTrace();
			WaterChunk.unloadAll();
		}
	}

	/*private void updateListOfLoadedChunks() {
		BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				for(World w : Bukkit.getWorlds()){
					if(w.getName().equals("world")){
						LivelyWorld.getInstance().getLogger().info("Test1");
						chunksFetcher.set(w.getLoadedChunks());
						LivelyWorld.getInstance().getLogger().info("Test2");
					}
				}
			}
		
		};
		br.runTask(LivelyWorld.getInstance());
		while(chunksFetcher.get() == null){
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		loadedChunks.put("world", chunksFetcher.get());
		chunksFetcher.set(null);
	}*/
	
	public static Biome getBiomeAt(WaterChunk wc, int x, int z){
		try {
			mainLocker.lock();
			List<SmallChunkData> chunks = loadedChunks.get(wc.getWorld().getName());
			for(int i = 0; i < chunks.size(); i++){
				SmallChunkData s = chunks.get(i);
				if(s.getX() == wc.getX() && s.getZ() == wc.getZ()){
					return s.getBiome(x, z);
				}
			}
		} catch (InterruptedException e) {
			LivelyWorld.getInstance().getLogger().warning("Couldn't check Biome of chunk " + wc.getX()+ "," + wc.getZ() + ".");
		} finally {
			mainLocker.unlock();
		}
		return null;
		
	}
	
	public static boolean isChunkLoaded(World w, int chunkX, int chunkZ){
		/*try{
			return w.isChunkLoaded(chunkX, chunkZ);
		} catch (Exception e){
			LivelyWorld.getInstance().getLogger().warning("Couldn't check if chunk " + chunkX + "," + chunkZ + " is loaded.");
		}
		return false;*/
		try {
			mainLocker.lock();
			List<SmallChunkData> chunks = loadedChunks.get(w.getName());
			for(int i = 0; i < chunks.size(); i++){
				SmallChunkData s = chunks.get(i);
				if(s.getX() == chunkX && s.getZ() == chunkZ){
					return true;
				}
			}
		} catch (InterruptedException e) {
			LivelyWorld.getInstance().getLogger().warning("Couldn't check if chunk " + chunkX + "," + chunkZ + " is loaded.");
		} finally {
			mainLocker.unlock();
		}
		return false;
		/*Chunk[] chunks = null;
		chunks = loadedChunks.get(w.getName());
		if(chunks == null) return false;
		for(Chunk c : chunks){
			if(c.getX() == chunkX && c.getZ() == chunkZ) continue;
			return true;
		}
		return false;*/
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

	public void addLoadedChunk(Chunk c) {
		try {
			mainLocker.lock();
			List<SmallChunkData> cs = loadedChunks.get(c.getWorld().getName());
			if(cs == null){
				cs = new ArrayList<SmallChunkData>();
				cs.add(new SmallChunkData(c));
				loadedChunks.put(c.getWorld().getName(), cs);
			} else {
				cs.add(new SmallChunkData(c));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			mainLocker.unlock();
		}
		
	}
	
	public void removeLoadedChunk(Chunk c) {
		try {
			mainLocker.lock();
			List<SmallChunkData> cs = loadedChunks.get(c.getWorld().getName());
			if(cs == null){
				return;
			} else {
				for(int i = 0; i < cs.size(); i++){
					SmallChunkData s = cs.get(i);
					if(s.getX() == c.getX() && s.getZ() == c.getZ()){
						cs.remove(i);
						break;
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			mainLocker.unlock();
		}
		
	}
	
}
