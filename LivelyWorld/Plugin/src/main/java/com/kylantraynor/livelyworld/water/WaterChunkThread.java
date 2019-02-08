package com.kylantraynor.livelyworld.water;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils.ChunkCoordinates;
import com.kylantraynor.livelyworld.Utils.Enclosed;
import com.kylantraynor.livelyworld.Utils.SmallChunkData;

public class WaterChunkThread extends Thread {
	/*
	private String name = "WaterChunk Thread";
	final public static Map<String, Map<String, int[]>> playerCoordinates = new ConcurrentHashMap<String, Map<String, int[]>>();
	final public static Map<String, Map<ChunkCoordinates,SmallChunkData>> loadedChunks = new ConcurrentHashMap<String, Map<ChunkCoordinates, SmallChunkData>>();
	final static Enclosed<Chunk[]> chunksFetcher = new Enclosed<Chunk[]>();
	//final static PrioritizedLock mainLocker = new PrioritizedLock(LivelyWorld.getInstance().getMainThreadId());
	
	public void run(){
		try{
			long lastUpdate = 0;
			int lastDelay = 0;
			long lastSave = 0;
			while (!isInterrupted()) {
				unloadChunks();
				loadChunks();
				if(System.currentTimeMillis() >= lastUpdate + (500 - lastDelay)){
					//updateListOfLoadedChunks();
					//LivelyWorld.getInstance().getLogger().info("Updating water chunks...");
					lastUpdate = System.currentTimeMillis();
					updateChunks();
					int size = WaterChunk.chunks.size();
					int time = (int) (System.currentTimeMillis() - lastUpdate);
					if(size > 0 && time > 2000){
						LivelyWorld.getInstance().getLogger().info("Slow water system: Done updating "+ size +" water chunks in " + time + "ms");
					}
					lastDelay = time - 500;
					if(lastDelay < 0) lastDelay = 0;
					lastUpdate = System.currentTimeMillis();
				}
				if(System.currentTimeMillis() >= lastSave + (1000 * 60 * 10)){
					saveChunks();
					lastSave = System.currentTimeMillis();
				}
			    cleanList();
			}
		} catch (Exception e){
			e.printStackTrace();
			WaterChunk.unloadAll();
		}
	}
	
	public static SmallChunkData getChunkData(WaterChunk wc){
			Map<ChunkCoordinates, SmallChunkData> chunks = loadedChunks.get(wc.getWorld().getName());
			if(chunks == null) return null; 
			return chunks.get(new ChunkCoordinates(wc.getWorld(), wc.getX(), wc.getZ()));
	}
	
	public static Biome getBiomeAt(WaterChunk wc, int x, int z){

			Map<ChunkCoordinates, SmallChunkData> chunks = loadedChunks.get(wc.getWorld().getName());
			if(chunks == null) return null;
			SmallChunkData d = chunks.get(new ChunkCoordinates(wc.getWorld(), wc.getX(), wc.getZ()));
			if(d == null) return null;
			return d.getBiome(x, z);
		
	}
	
	
	public static SmallChunkData getChunkData(World w, int chunkX, int chunkZ){
		Map<ChunkCoordinates, SmallChunkData> chunks = loadedChunks.get(w.getName());
		if(chunks == null) return null;
		return chunks.get(new ChunkCoordinates(w, chunkX, chunkZ));
	}
	
	public static boolean isChunkLoaded(World w, int chunkX, int chunkZ){
			Map<ChunkCoordinates, SmallChunkData> chunks = loadedChunks.get(w.getName());
			if(chunks == null) return false;
			return chunks.get(new ChunkCoordinates(w, chunkX, chunkZ)) != null;
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
				i++;
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
					c.update();
				} catch (Exception e){
					LivelyWorld.getInstance().getLogger().severe("Exception while ticking water chunk at " + c.getX() + "," + c.getZ()+ ".");
					e.printStackTrace();
				}
			}
			i++;
		}
	}
	
	private void saveChunks() {
		int i = 0;
		Object[] cs = WaterChunk.chunks.values().toArray();
		while(i < cs.length){
			WaterChunk c = (WaterChunk) cs[i];
			if(c == null){
				i++; continue;
			}
			if(c.isLoaded()){
				c.save();
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
		int i = 0;
		Object[] cs = WaterChunk.chunks.values().toArray();
		while(i < cs.length){
			WaterChunk c = (WaterChunk) cs[i];
			if(c == null){
				i++; continue;
			}
			if(!c.isLoaded() && isChunkLoaded(c.getWorld(), c.getX(), c.getZ())){
				c.load();
				//count--;
			}
			i++;
		}

	}

	public SmallChunkData addLoadedChunk(Chunk c) {
			Map<ChunkCoordinates, SmallChunkData> cs = loadedChunks.get(c.getWorld().getName());
			if(cs == null){
				cs = new ConcurrentHashMap<ChunkCoordinates, SmallChunkData>();
				SmallChunkData scd = new SmallChunkData(c);
				cs.put(new ChunkCoordinates(c.getWorld(), c.getX(), c.getZ()), scd);
				loadedChunks.put(c.getWorld().getName(), cs);
				return scd;
			} else {
				SmallChunkData scd = new SmallChunkData(c);
				cs.put(new ChunkCoordinates(c.getWorld(), c.getX(), c.getZ()), scd);
				return scd;
			}

	}

	public void removeLoadedChunk(Chunk c) {
		Map<ChunkCoordinates, SmallChunkData> cs = loadedChunks.get(c.getWorld().getName());
		if(cs == null){
			return;
		} else {
			cs.remove(new ChunkCoordinates(c.getWorld(), c.getX(), c.getZ()));
		}
	}
	
	public void removeOnlinePlayer(Player p) {
		Map<String, int[]> cs = playerCoordinates.get(p.getWorld().getName());
		if(cs == null){
			return;
		} else {
			cs.remove(p.getUniqueId().toString());
		}
	}
	
	public void updateOnlinePlayer(Player p) {
		Map<String, int[]> cs = playerCoordinates.get(p.getWorld().getName());
		if(cs == null){
			cs = new ConcurrentHashMap<String, int[]>();
			cs.put(p.getUniqueId().toString(), new int[] {p.getLocation().getChunk().getX(), p.getLocation().getChunk().getZ()});
			playerCoordinates.put(p.getWorld().getName(), cs);
		} else {
			cs.put(p.getUniqueId().toString(), new int[] {p.getLocation().getChunk().getX(), p.getLocation().getChunk().getZ()});
		}
	}
	
	public static List<int[]> getPlayerCoordinates(World w){
		Map<String, int[]> cs = playerCoordinates.get(w.getName());
		if(cs == null){
			return null;
		} else {
			List<int[]> result = new ArrayList<int[]>();
			for(int[] coords : cs.values()){
				result.add(coords);
			}
			return result;
		}
	}
	*/
}
