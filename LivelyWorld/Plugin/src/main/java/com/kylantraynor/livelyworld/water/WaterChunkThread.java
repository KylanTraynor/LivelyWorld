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
import com.kylantraynor.livelyworld.Utils.ChunkCoordinates;
import com.kylantraynor.livelyworld.Utils.Enclosed;
import com.kylantraynor.livelyworld.Utils.PrioritizedLock;
import com.kylantraynor.livelyworld.Utils.SmallChunkData;

public class WaterChunkThread extends Thread {
	
	private String name = "WaterChunk Thread";
	final public static Map<String, Map<ChunkCoordinates,SmallChunkData>> loadedChunks = new HashMap<String, Map<ChunkCoordinates, SmallChunkData>>();
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
					lastUpdate = System.currentTimeMillis();
					updateChunks();
					int size = WaterChunk.chunks.size();
					if(size > 0){
						LivelyWorld.getInstance().getLogger().info("Done updating "+ size +" water chunks in " + (System.currentTimeMillis() - lastUpdate) + "ms");
					}
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
	
	public static SmallChunkData getChunkData(WaterChunk wc){
		try {
			mainLocker.lock();
			Map<ChunkCoordinates, SmallChunkData> chunks = loadedChunks.get(wc.getWorld().getName());
			return chunks.get(new ChunkCoordinates(wc.getWorld(), wc.getX(), wc.getZ()));
			/*for(int i = 0; i < chunks.size(); i++){
				SmallChunkData s = chunks.get(i);
				if(s == null) continue;
				if(s.getX() == wc.getX() && s.getZ() == wc.getZ()){
					return s;
				}
			}*/
		} catch (InterruptedException e) {
			LivelyWorld.getInstance().getLogger().warning("Couldn't check Biome of chunk " + wc.getX()+ "," + wc.getZ() + ".");
		} finally {
			mainLocker.unlock();
		}
		return null;
	}
	
	public static Biome getBiomeAt(WaterChunk wc, int x, int z){
		try {
			mainLocker.lock();
			Map<ChunkCoordinates, SmallChunkData> chunks = loadedChunks.get(wc.getWorld().getName());
			SmallChunkData d = chunks.get(new ChunkCoordinates(wc.getWorld(), wc.getX(), wc.getZ()));
			if(d == null) return null;
			return d.getBiome(x, z);
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
			Map<ChunkCoordinates, SmallChunkData> chunks = loadedChunks.get(w.getName());
			return chunks.get(new ChunkCoordinates(w, chunkX, chunkZ)) != null;
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
		Object[] cs = WaterChunk.chunks.values().toArray();
		while(i < cs.length){
			WaterChunk c = (WaterChunk) cs[i];
			if(c == null){
				i++; continue;
			}
			if(!c.isLoaded() && !isChunkLoaded(c.getWorld(), c.getX(), c.getZ())){
				WaterChunk.chunks.remove(new ChunkCoordinates(c.getWorld(), c.getX(), c.getZ()));
				continue;
			}
			i++;
		}
	}

	private void updateChunks() {
		int i = 0;
		Object[] cs = WaterChunk.chunks.values().toArray();
		while(i < cs.length){
			WaterChunk c = (WaterChunk) cs[i];
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
		int count = 30;
		int i = 0;
		Object[] cs = WaterChunk.chunks.values().toArray();
		while(i < cs.length){
			WaterChunk c = (WaterChunk) cs[i];
			if(c == null){
				i++; continue;
			}
			if(c.isLoaded() && !isChunkLoaded(c.getWorld(), c.getX(), c.getZ())){
				c.unload();
				//LivelyWorld.getInstance().getLogger().info("Unloading Chunk at " + c.getX() + ", " + c.getZ() + ", Total: " + WaterChunk.chunks.size());
				WaterChunk.chunks.remove(new ChunkCoordinates(c.getWorld(), c.getX(), c.getZ()));
				count--;
			}
			i++;
		}
	}

	private void loadChunks() {
		int count = 30;
		int i = 0;
		Object[] cs = WaterChunk.chunks.values().toArray();
		while(i < cs.length){
			WaterChunk c = (WaterChunk) cs[i];
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
			Map<ChunkCoordinates, SmallChunkData> cs = loadedChunks.get(c.getWorld().getName());
			if(cs == null){
				cs = new HashMap<ChunkCoordinates, SmallChunkData>();
				cs.put(new ChunkCoordinates(c.getWorld(), c.getX(), c.getZ()), new SmallChunkData(c));
				loadedChunks.put(c.getWorld().getName(), cs);
			} else {
				cs.put(new ChunkCoordinates(c.getWorld(), c.getX(), c.getZ()), new SmallChunkData(c));
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
			Map<ChunkCoordinates, SmallChunkData> cs = loadedChunks.get(c.getWorld().getName());
			if(cs == null){
				return;
			} else {
				cs.remove(new ChunkCoordinates(c.getWorld(), c.getX(), c.getZ()));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			mainLocker.unlock();
		}
		
	}
	
}
