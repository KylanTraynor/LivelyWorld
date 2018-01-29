package com.kylantraynor.livelyworld.water;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.Utils.ChunkCoordinates;
import com.kylantraynor.livelyworld.Utils.SmallChunkData;

public class WaterChunk {
	final static Map<ChunkCoordinates, WaterChunk> chunks = new ConcurrentHashMap<ChunkCoordinates, WaterChunk>(); 
	final static int sectorLength = 4096;
	final static int currentVersion = 1;
	static boolean disabled = false;
	
	double tickRandom = Utils.fastRandomDouble();
	
	//final byte[] data = new byte[16 * 16 * 256 * 4];
	final WaterData[][][] data = new WaterData[256][16][16];
	final int dataLength = data.length * data[0].length * data[0][0].length;
	final int dataByteLength = dataLength * 4;
	private boolean isLoaded = false;
	private final int x;
	private final int z;
	private final World world;
	private boolean needsUpdate = false;
	private boolean wasGenerated = false;
	private static Utils.Lock fileLock = new Utils.Lock();
	private long lastUpdate = System.currentTimeMillis();
	
	public WaterChunk(World w, int x, int z){
		this.world = w;
		this.x = x;
		this.z = z;
		for(int y = 255; y >= 0; y--){
			for(int x1 = 0; x1 < 16; x1++){
				for(int z1 = 0; z1 < 16; z1++){
					if(data[y][x1][z1] == null){
						data[y][x1][z1] = new WaterData(0, x1, y, z1);
					}
				}
			}
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof WaterChunk)) return false;
		if(!this.world.equals(((WaterChunk)o).getWorld())) return false;
		if(this.getX() != ((WaterChunk)o).getX()) return false;
		if(this.getZ() != ((WaterChunk)o).getZ()) return false;
		return true;
	}
	
	@Override
	public int hashCode(){
		int result = 11;
		result *= 13 * world.hashCode();
		result *= 17 * Integer.hashCode(x);
		result *= 29 * Integer.hashCode(z);
		return result;
	}
	
	public void load(){
		try {
			fileLock.lock();
			try{
				//LivelyWorld.getInstance().getLogger().info("Attempting to load chunk " + getX() + "_" + getZ());
				if(isLoaded) return;
				//LivelyWorld.getInstance().getLogger().info("Loading chunk " + getX() + "_" + getZ() + " from file.");
				loadFromFile();
				setLoaded(true);
				//LivelyWorld.getInstance().getLogger().info("Chunk " + getX() + "_" + getZ() + " is loaded. (" + isLoaded + ")");
			} finally {
				fileLock.unlock();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public void save(){
		try {
			fileLock.lock();
			try{
				//LivelyWorld.getInstance().getLogger().info("Attempting to save chunk " + getX() + "_" + getZ());
				if(!isLoaded) return;
				//LivelyWorld.getInstance().getLogger().info("Saving chunk " + getX() + "_" + getZ() + " to file.");
				saveToFile();
			} finally {
				fileLock.unlock();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void unload(){
		if(getWorld().isChunkLoaded(getX(), getZ())) return;
		try {
			fileLock.lock();
			try{
				//LivelyWorld.getInstance().getLogger().info("Attempting to unload chunk " + getX() + "_" + getZ() + " ("+isLoaded+")");
				if(!isLoaded) return;
				save();
				setLoaded(false);
				//LivelyWorld.getInstance().getLogger().info("Chunk " + getX() + "_" + getZ() + " is unloaded.");
			} finally {
				fileLock.unlock();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public int getX(){ return x; }
	
	public int getZ(){ return z; }
	
	public WaterChunk getRelative(int x, int z){
		return WaterChunk.get(world, this.x + x, this.z + z);
	}
	
	public WaterChunk getRelativeOrNull(int x, int z){
		if(WaterChunkThread.isChunkLoaded(world, this.x + x, this.z + z)){
			return WaterChunk.get(world, this.x + x, this.z + z);
		}
		return null;
	}
	
	public WaterData getAt(int x, int y, int z){
		if(!isLoaded) load();
		return data[y][x][z];
	}
	
	/*void setDataUnchecked(int data, int x, int y, int z){
		if(!isLoaded) load();
		byte[] b = Utils.toByteArray(data);
		int index = getIndex(x,y,z);
		this.data[index    ] = b[0];
		this.data[index + 1] = b[1];
		this.data[index + 2] = b[2];
		this.data[index + 3] = b[3];
	}*/
	
	/*void setData(int data, int x, int y, int z){
		if(!isLoaded) load();
		byte[] b = Utils.toByteArray(data);
		int index = getIndex(x,y,z);
		synchronized(this.data){
			this.data[index    ] = b[0];
			this.data[index + 1] = b[1];
			this.data[index + 2] = b[2];
			this.data[index + 3] = b[3];
		}
	}*/
	
	private int getIndex(int x, int y, int z){
		return (y << 10) + (x << 6) + (z << 2);
	}
	
	/*int getData(int x, int y, int z) {
		if(!isLoaded) load();
		int index = getIndex(x, y, z);
		return Utils.toInt(data[index], data[index + 1], data[index + 2], data[index + 3]);
	}*/
	
	public World getWorld() {
		return world;
	}
	
	public static WaterChunk get(World world, int x, int z){
		
		/*int i = 0;
		while(i < chunks.size()){
			WaterChunk c = chunks.get(i);
			if(c.getWorld().equals(world) && c.getX() == x && c.getZ() == z){
				return c;
			}
			i++;
		}*/
		WaterChunk wc = chunks.get(new ChunkCoordinates(world, x, z));
		if(wc != null){
			return wc;
		}
		wc = new WaterChunk(world,x,z);
		chunks.put(new ChunkCoordinates(world, x, z), wc);
		return wc;
	}
	
	public static void unloadAll(){
		if(disabled) return;
		disabled = true;
		ChunkCoordinates[] keys = chunks.keySet().toArray(new ChunkCoordinates[chunks.size()]);
		for(ChunkCoordinates cc : keys){
			WaterChunk c = chunks.get(cc);
			if(c == null) continue;
			c.unload();
			chunks.remove(cc);
			LivelyWorld.getInstance().getLogger().info("" + chunks.size() + " remaining.");
		}
	}
	
	public File getFile(){
		File dir1 = new File(LivelyWorld.getInstance().getDataFolder(), "BlockData");
		if(!dir1.exists()){
			dir1.mkdir();
		}
		File dir2 = new File(dir1, getWorld().getName());
		if(!dir2.exists()){
			dir2.mkdir();
		}
		File f = new File(dir2, "" + (getX() >> 5) + "_" + (getZ() >> 5) + ".rbd");
		if(!f.exists()){
			try {
				LivelyWorld.getInstance().getLogger().info("Creating region file " + f.getName() +".");
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return f;
	}
	
	public void saveToFile(){
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DeflaterOutputStream dos = new DeflaterOutputStream(baos);
		try {
			dos.write(Utils.toByteArray(currentVersion));
			dos.write(Utils.toByteArray(getChunkStateCode()));
			synchronized(this.data){
				for(int y = 0; y < data.length; y++){
					for(int x = 0; x < data[y].length; x++){
						for(int z = 0; z < data[y][x].length; z++){
							dos.write(data[y][x][z].getByteArray());
						}
					}
				}
				//dos.write(data);
			}
			dos.flush();
		} catch (IOException e2) {
			e2.printStackTrace();
		} finally{
			try {
				dos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		RandomAccessFile f = null;
		int locationIndex = (Math.floorMod(getX(),32) * 32 * 4) + (Math.floorMod(getZ(),32) * 4);
		int sizeIndex = ((4096) + (Math.floorMod(getX(),32) * 32 * 4) + (Math.floorMod(getZ(),32) * 4));
		try {
			f = new RandomAccessFile(getFile(), "rw");
			if(f.length() % sectorLength != 0){
				LivelyWorld.getInstance().getLogger().warning(getFile().getName()+": Unexpected file size (" + f.length() + "). Wiping data before rewrite.");
				f.setLength(0);
			}
			if(f.length() < 8192){
				f.seek(locationIndex);
				f.writeInt(2);
				f.seek(locationIndex);
				f.seek(sizeIndex);
				f.writeInt(baos.size());
				f.seek(sizeIndex);
				f.seek(8192);
				int padding = sectorLength - Math.floorMod(baos.size(), sectorLength);
				f.write(baos.toByteArray());
				f.write(new byte[padding]);
				return;
			}
			f.seek(locationIndex);
			int location = f.readInt();
			int size = 0;
			if(location < 2){
				location = Math.floorDiv((Math.max((int)f.length(), 8192)), sectorLength);
				f.seek(locationIndex);
				f.writeInt(location);
			} else {
				f.seek(sizeIndex);
				size = f.readInt();
			}
			f.seek(sizeIndex);
			f.writeInt(baos.size());
			f.seek(location * sectorLength);
			if(location * sectorLength >= f.length()){
				f.write(baos.toByteArray());
				f.write(new byte[sectorLength - Math.floorMod(baos.size(), sectorLength)]);
			} else {
				int remainingPadding = sectorLength - Math.floorMod(size, sectorLength);
				int finalPadding = sectorLength - Math.floorMod(baos.size(), sectorLength);
				int newSectors = (baos.size() + finalPadding - (size + remainingPadding)) / sectorLength;
				if(newSectors != 0){
					//LivelyWorld.getInstance().getLogger().info(getFile().getName()+": Rewriting. Initial file size: " + f.length() + ". Expected final size: " + (f.length() + newSectors*sectorLength));
					int nextChunkIndex = location*sectorLength + size + remainingPadding;
					byte[] nextChunks = new byte[0];
					if(f.length() - nextChunkIndex > 0){
						nextChunks = new byte[(int) (f.length() - nextChunkIndex)];
					}
					f.seek(nextChunkIndex);
					f.readFully(nextChunks);
					f.seek(location * sectorLength);
					LivelyWorld.getInstance().getLogger().info(getFile().getName()+": Rewriting at location: " + location + " (" + location*sectorLength + ") with a size of " + baos.size() + " with padding: " + finalPadding + " and " + newSectors + " sectors" + ". Moving " + nextChunks.length + " bytes.");
					// Write Chunk Data
					f.setLength(location * sectorLength);
					f.write(baos.toByteArray());
					f.write(new byte[finalPadding]);
					// Write moved chunks
					f.write(nextChunks);
					//LivelyWorld.getInstance().getLogger().info(getFile().getName()+": Rewriting. Final file size: " + f.length());
					// Update chunk locations
					f.seek(0);
					while(f.getFilePointer() < 4096){
						int pos = (int)f.getFilePointer();
						int loc = f.readInt();
						if(loc > location){
							f.seek(pos);
							f.writeInt(loc + newSectors);
						}
					}
				} else {
					//LivelyWorld.getInstance().getLogger().info("Rewriting at location: " + location + " (" + location*sectorLength + ") with a size of " + baos.size() + " with padding: " + finalPadding);
					f.write(baos.toByteArray());
					f.write(new byte[finalPadding]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(f!=null){
					f.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private int getChunkStateCode() {
		int result = 0;
		result = needsUpdate? (result | (1)) : result;
		result = wasGenerated? (result | (2)) : result;
		return result;
	}

	public void loadFromFile(){
		int compressedSize = 0;
		byte[] compressedData = null;
		RandomAccessFile f = null;
		try {
			f = new RandomAccessFile(getFile(), "r");
			if(f.length() < 8192) return;
			if(f.length() % sectorLength != 0) {
				LivelyWorld.getInstance().getLogger().warning(getFile().getName()+": Unexpected file size (" + f.length() + "). Chunk won't be loaded.");
				return;
			}
			int locationIndex = (Math.floorMod(getX(),32) * 32 * 4) + (Math.floorMod(getZ(),32) * 4);
			int sizeIndex = ((4096) + (Math.floorMod(getX(),32) * 32 * 4) + (Math.floorMod(getZ(),32) * 4));
			
			f.seek(locationIndex);
			int location = f.readInt();
			if(location < 8192 / sectorLength) return;
			f.seek(sizeIndex);
			int size = f.readInt();
			
			compressedData = new byte[compressedSize];
			f.seek(location * sectorLength);
			
			//LivelyWorld.getInstance().getLogger().info("Reading from location: " + location + " (" + location*sectorLength + ") with a size of " + size);
			
			ByteArrayOutputStream baos = null;
			InflaterOutputStream ios = null;
			
			try{
				baos = new ByteArrayOutputStream();
				ios = new InflaterOutputStream(baos);
				
				byte[] buf = new byte[4096];
				int rlen = -1;
				while((rlen = f.read(buf)) >= 0){
					ios.write(buf, 0, rlen);
				}
				
				ios.flush();
				if(baos.size() > 8){
					byte[] array = baos.toByteArray();
					int version = Utils.toInt(array[0], array[1], array[2], array[3]);
					if(version == currentVersion && baos.size() == dataByteLength + 8){
						int chunkData = Utils.toInt(array[4], array[5], array[6], array[7]);
						synchronized(data){
							for(int i = 0; i < dataLength; i++){
								int index = (i * 4) + 8;
								int y = (i >> 8) & (data.length - 1);
								int x = (i >> 4) & (data[0].length - 1);
								int z = i & (data[0][0].length - 1);
								byte[] b = new byte[]{array[index], array[index + 1], array[index + 2], array[index + 3]};
								//int d = Utils.toInt(array[index], array[index + 1], array[index + 2], array[index + 3]);
								data[y][x][z] = new WaterData(b, x, y, z);
								//data[i - 8] = array[i];
							}
						}
						needsUpdate = ((chunkData & 1) == 1);
						wasGenerated = ((chunkData & 2) == 2);
					} else if(baos.size() == dataByteLength) {
						synchronized(data){
							for(int i = 0; i < dataLength; i++){
								int index = (i * 4);
								int y = (i >> 8) & (data.length - 1);
								int x = (i >> 4) & (data[0].length - 1);
								int z = i & (data[0][0].length - 1);
								byte[] b = new byte[]{array[index], array[index + 1], array[index + 2], array[index + 3]};
								//int d = Utils.toInt(array[index], array[index + 1], array[index + 2], array[index + 3]);
								data[y][x][z] = new WaterData(b, x, y, z);
							}
						}
					}
				}
			} finally {
				if(ios != null){
					ios.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(f != null){
					f.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		int corruptedCount = 0;
		for(int y = 255; y >= 0; y--){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					if(data[y][x][z] == null){
						data[y][x][z] = new WaterData(0, x, y, z);
						corruptedCount++;
					}
				}
			}
		}
		if(corruptedCount > 0){
			LivelyWorld.getInstance().getLogger().info("Found " + corruptedCount + " missing data in chunk " + x + ", " + z +".");
		}
		
		/*if(compressedData == null) return;
		
		Inflater inflater = new Inflater();
		inflater.setInput(compressedData);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(compressedData.length);
		byte[] buf = new byte[512];
		while(!inflater.finished()){
			int count = 0;
			try {
				count = inflater.inflate(buf);
			} catch (DataFormatException e) {
				e.printStackTrace();
			}
			baos.write(buf, 0, count);
		}
		try {
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] bao = baos.toByteArray();
		if(bao.length == data.length){
			synchronized(data){
				for(int i = 0; i < data.length; i++){
					data[i] = bao[i];
				}
			}
		}*/
		/*InflaterInputStream iis = new InflaterInputStream(bais);
		try {
			byte[] buf = new byte[512];
	        int rlen = -1;
	        int i = 0;
	        while ((rlen = iis.read(buf)) != -1) {
	        	for(int b = 0; b < rlen; b++){
	        		synchronized(data){
	        			data[i] = buf[b];
	        		}
	        		i++;
	        	}
	        }
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				iis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}*/
		/*
		synchronized(data){
			byte[] buffer = baos.toByteArray();
			for(int i = 0; i < buffer.length; i++){
				data[i] = buffer[i];
			}
		}*/
		
		/*try {
			fileLock.lock();
			try{
				try {
					if(getFile().length() <= 0) return;
					InflaterInputStream s = new InflaterInputStream(new FileInputStream(getFile()));
					int length =16*16*256*4;
					int startIndex = (Math.floorMod(getX(), 32) * 32 * length) + (Math.floorMod(getZ(), 32) * length);
					byte[] bucket = new byte[length];
					ByteArrayOutputStream o = null;
					try{
						o = new ByteArrayOutputStream(bucket.length);
						int bytesToSkip = startIndex - 1;
						while(bytesToSkip > 0){
							bytesToSkip -= s.skip(bytesToSkip);
						}
						int totalBytesRead = 0;
						int bytesRead = 0;
						while(bytesRead >= 0 && totalBytesRead < length){
							bytesRead = s.read(bucket);
							if(bytesRead > 0){
								totalBytesRead += bytesRead;
								o.write(bucket, 0, bytesRead);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							s.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if(o.size() == length){
						byte[] ob = o.toByteArray();
						synchronized(this.data){
							for(int i = 0; i < ob.length; i++){
								this.data[i] = ob[i];
							}
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}finally{
				fileLock.unlock();
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}*/
	}

	public boolean isLoaded() {
		return this.isLoaded;
	}
	
	private void setLoaded(boolean b){
		//LivelyWorld.getInstance().getLogger().info("Setting chunk " + getX() + "_" + getZ() + " to loaded = " + b + " Previous = " + isLoaded);
		this.isLoaded = b;
	}
	
	private int distanceSquaredFromNearestPlayer(){
		int result = 200;
		List<int[]> coords = WaterChunkThread.getPlayerCoordinates(this.world);
		if(coords == null) return 200;
		for(int[] c : coords){
			result = Math.min((c[0] - x) * (c[0] - x) + (c[1] - z) * (c[1] - z), result);
		}
		return result;
	}
	
	private double getPlayerCountSquared(){
		List<int[]> coords = WaterChunkThread.getPlayerCoordinates(this.world);
		if(coords == null) return 0;
		return coords.size() * coords.size();
	}
	
	public void processWaterMove(int x, int y, int z){
		
		WaterData d = data[y][x][z];
		
		if(d.getLevel() == 0) return;
		if(d.isSolid){
			if(Utils.fastRandomInt(256) <= d.getResistance()){
				return;
			}
		}
		
		WaterData up = null;
		if(y < 255) up = data[y + 1][x][z];
		WaterData down = null;
		if(y > 0) down = data[y - 1][x][z];
		WaterData west = null;
		if(x > 0){
			west = data[y][x - 1][z];
		} else if(this.isRelativeLoaded(-1, 0)){
			west = getRelative(-1,0).data[y][15][z];
		}
		WaterData east = null;
		if(x < 15){
			east = data[y][x + 1][z];
		} else if(this.isRelativeLoaded(1, 0)){
			east = getRelative(1,0).data[y][0][z];
		}
		WaterData north = null;
		if(z > 0){
			north = data[y][x][z - 1];
		} else if(this.isRelativeLoaded(0, -1)) {
			north = getRelative(0,-1).data[y][x][15];
		}
		WaterData south = null;
		if(z < 15){
			south = data[y][x][z + 1];
		} else if(this.isRelativeLoaded(0, 1)){
			south = getRelative(0,1).data[y][x][0];
		}
		
		boolean stable = false;
		while(d.getLevel() > 0 && !stable){
			if(down != null){
				int max = down.getMaxQuantityRDM();
				if(down.getLevel() < max
						&& down.pressure < d.pressure + max && Utils.fastRandomInt(256) > down.getResistance()){
					down.level++;
					if(down.isSolid){
						down.pressure++;
					}
					d.level--;
					d.pressure--;
					stable = false;
					d.lastDirection = 0;
					continue;
				}
			}
			if(up != null){
				int max = d.getMaxQuantityRDM();
				if(up.pressure < d.pressure - max){
					if(up.getLevel() < up.getMaxQuantityRDM()){
						up.level++;
						up.pressure++;
						d.level--;
						if(d.isSolid){
							d.pressure--;
						}
						stable = false;
						d.lastDirection = 0;
						continue;
					}
				}
			}
			WaterData min = getMinPressure(north, east, west, south);
			if(min == null) {
				stable = true;
				d.lastDirection = 0;
			} else if(min.pressure < d.pressure){
				if(min.getLevel() < min.getMaxQuantityRDM()){
					min.level++;
					min.pressure++;
					d.level--;
					d.pressure--;
					stable = false;
					if(min == north) d.lastDirection = 1;
					if(min == east) d.lastDirection = 2;
					if(min == west) d.lastDirection = 3;
					if(min == south) d.lastDirection = 4;
				} else {
					stable = true;
				}
			} else {
				stable = true;
				d.lastDirection = 0;
			}
		}
		
	}
	
	public WaterData getMinPressure(WaterData... data){
		WaterData min = data[0];
		for(int i = 1; i < data.length; i++){
			if(data[i] == null) continue;
			if(data[i].isSolid && Utils.fastRandomInt(256) >= data[i].getResistance()) continue;
			if(min == null) {
				min = data[i];
			} else if(data[i].pressure < min.pressure){
				min = data[i];
			}
		}
		return min;
	}

	public void updatePressure(int x, int y, int z){
		WaterData up = null;
		if(y < 255) up = data[y + 1][x][z];
		WaterData d = data[y][x][z];
		d.pressure = d.getLevel();
		if(d.isSolid){
			d.pressure += d.getResistance();
		} else if(up != null && !up.isSolid && d.getLevel() == 0xFF){
			d.pressure += up.pressure;
		}
	}
	
	void update(){
		if(!isLoaded) return;
		
		// If the chunk was not generate, generate it.
		if(!wasGenerated){
			this.saturate();
			wasGenerated = true;
		}
		
		// Get a snapshot of the actual chunk.
		ChunkSnapshot c = null;
		if(WaterChunkThread.isChunkLoaded(world, x, z)){
			c = world.getChunkAt(x, z).getChunkSnapshot(false, true, false);
		} else {
			return;
		}
		
		// Saturate Oceans.
		Biome biome = null;
		for(int x = 0; x < 16; x++){
			for(int z = 0; z < 16; z++){
				biome = c.getBiome(x, z);
				if(biome != null){
					if((Utils.isOcean(biome) || biome == Biome.RIVER)){
						int y = 48;
						Material m = Material.getMaterial(c.getBlockTypeId(x, y, z));
						while(y > 0 && (Utils.isWater(m) || m == Material.AIR)){
							data[y][x][z].level = (byte) 0xFF;
							y--;
						}
					}
				}
			}
		}
		
		// Update Pressure.
		for(int y = 255; y >= 0; y--){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					Material m = Material.getMaterial(c.getBlockTypeId(x, y, z));
					data[y][x][z].resistance = (byte) getResistanceFor(m);
					data[y][x][z].isSolid = isSolid(m);
					updatePressure(x, y, z);
				}
			}
		}
		
		// Update Level.
		int xStep = Utils.fastRandomInt(2) < 1 ? -1 : 1;
		int zStep = Utils.fastRandomInt(2) < 1 ? -1 : 1;
		
		for(int y = 0; y < 256; y++){
			for(int x = xStep == 1 ? 0 : 15; xStep == 1 ? x < 16 : x >= 0; x += xStep){
				for(int z = zStep == 1 ? 0 : 15; zStep == 1 ? z < 16 : z >= 0; z += zStep){
					processWaterMove(x, y, z);
				}
			}
		}
		
		if(System.currentTimeMillis() - lastUpdate < 1000) return;
		if(this.distanceSquaredFromNearestPlayer() > 100) return;
		if(this.distanceSquaredFromNearestPlayer() > 2 && Utils.fastRandomDouble() > 0.01) return;
		
		for(int y = 0; y < 256; y++){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					Material m = Material.getMaterial(c.getBlockTypeId(x, y, z));
					byte d = (byte) c.getBlockData(x, y, z);
					
					if(canReplace(m) && Utils.getWaterHeight(m, d) != toWaterLevel(data[y][x][z].getLevel())){
						updateVisually(x,y,z, toWaterLevel(data[y][x][z].getLevel()));
					}
				}
			}
		}
	}
	
	public static int getResistanceFor(Material material){
		/*if(material == null){
			if(!chunk.isLoaded() || !WaterChunkThread.isChunkLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ()))
				return 0;
			int id = chunk.getWorld().getBlockTypeIdAt(getX(), getY(), getZ());
			material = Material.getMaterial(id);
		}*/
		if(material == null) return 0;
		switch (material){
		case WATER: case STATIONARY_WATER: case AIR:
			return 1;
		case WOOD_BUTTON: case STONE_BUTTON:
		case LADDER: case RAILS: case TORCH: case IRON_FENCE: case WALL_SIGN: case SIGN_POST:
		case WOOD_PLATE: case STONE_PLATE: case IRON_PLATE: case GOLD_PLATE:
			return 2;
		case TRAP_DOOR:
		case WOODEN_DOOR: case WOOD_DOOR:
		case ACACIA_DOOR: case SPRUCE_DOOR: case DARK_OAK_DOOR: case JUNGLE_DOOR: case BIRCH_DOOR:
		case FENCE_GATE: case ACACIA_FENCE_GATE: case  SPRUCE_FENCE_GATE: case DARK_OAK_FENCE_GATE: case JUNGLE_FENCE_GATE: case BIRCH_FENCE_GATE:
		case FENCE: case ACACIA_FENCE: case SPRUCE_FENCE: case DARK_OAK_FENCE: case JUNGLE_FENCE: case BIRCH_FENCE: case NETHER_FENCE:
			return 20;
		case LONG_GRASS: case DOUBLE_PLANT: case RED_ROSE: case YELLOW_FLOWER:
		case LEAVES: case LEAVES_2:
			return 30;
		case ANVIL:
			return 100;
		case SAND: case GRAVEL: case SNOW: case SNOW_BLOCK:
			return 200;
		case DIRT: case GRASS_PATH: case GRASS: case SOIL: case CLAY:
			return 215;
		case COBBLESTONE: case COBBLE_WALL:
			return 225;
		default:
			return 255;
		}
	}
	
	public static boolean isSolid(Material mat){
		if(mat == null) return true;
		switch (mat){
		case WATER: case STATIONARY_WATER: case AIR:
		case WOOD_BUTTON: case STONE_BUTTON:
		case LADDER: case RAILS: case TORCH: case IRON_FENCE: case WALL_SIGN: case SIGN_POST:
		case WOOD_PLATE: case STONE_PLATE: case IRON_PLATE: case GOLD_PLATE:
		case LONG_GRASS: case DOUBLE_PLANT: case RED_ROSE: case YELLOW_FLOWER:
		case LEAVES: case LEAVES_2:
		case ANVIL:
		case TRAP_DOOR:
		case WOODEN_DOOR: case WOOD_DOOR:
		case ACACIA_DOOR: case SPRUCE_DOOR: case DARK_OAK_DOOR: case JUNGLE_DOOR: case BIRCH_DOOR:
		case FENCE_GATE: case ACACIA_FENCE_GATE: case  SPRUCE_FENCE_GATE: case DARK_OAK_FENCE_GATE: case JUNGLE_FENCE_GATE: case BIRCH_FENCE_GATE:
		case FENCE: case ACACIA_FENCE: case SPRUCE_FENCE: case DARK_OAK_FENCE: case JUNGLE_FENCE: case BIRCH_FENCE: case NETHER_FENCE:
		case COBBLE_WALL:
			return false;
		case SAND: case GRAVEL: case SNOW: case SNOW_BLOCK:
		case DIRT: case GRASS_PATH: case GRASS: case SOIL: case CLAY:
		case COBBLESTONE:
			return true;
		}
		
		return true;
	}
	
	public static boolean canReplace(Material mat){
		if(mat == Material.WATER) return true;
		if(mat == Material.STATIONARY_WATER) return true;
		if(mat == Material.AIR) return true;
		if(mat == Material.LONG_GRASS) return true;
		if(mat == Material.VINE) return true;
		if(mat == Material.TORCH) return true;
		if(mat == Material.SNOW) return true;
		if(mat == Material.SNOW_BLOCK) return true;
		return false;
	}
	
	public static boolean isDropable(Material mat){
		if(mat == Material.VINE) return true;
		if(mat == Material.TORCH) return true;
		if(mat == Material.LONG_GRASS) return true;
		return false;
	}
	
	private void updateVisually(int x, int y, int z, int waterLevel) {
		if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
		BukkitRunnable br = new VisualUpdateTask(this, x, y, z);
		br.runTask(LivelyWorld.getInstance());
	}

	public static int toWaterLevel(int level){
		return level / 32;
	}
	/*
	void tickAll(){
		
		if(!isLoaded()) return;
		double dist = Math.sqrt(distanceSquaredFromNearestPlayer());
		if(Utils.fastRandomDouble() > 1.0 / Math.max(dist, 1)) return;
		tickRandom = Utils.fastRandomDouble();
		if(!wasGenerated){
			this.saturate();
			wasGenerated = true;
		}
		
		Chunk c = null;
		if(WaterChunkThread.isChunkLoaded(world, x, z)){
			c = world.getChunkAt(x, z);
		} else {
			return;
		}
		
		byte[][] westLevels = new byte[256][16];
		byte[][] eastLevels = new byte[256][16];
		byte[][] northLevels = new byte[16][256];
		byte[][] southLevels = new byte[16][256];
		byte[][][] levels = new byte[16][256][16];
		//boolean[][][] hasChanged = new boolean[16][256][16];
		for(int x = 0; x < 16; x++){
			for(int y = 0; y < 256; y++){
				for(int z = 0; z < 16; z++){
					levels[x][y][z] = (byte) Utils.getWaterHeight(c.getBlock(x, y, z));
				}
			}
		}
		WaterChunk westC = this.getRelativeOrNull(-1, 0);
		WaterChunk eastC = this.getRelativeOrNull(1, 0);
		for(int z = 0; z < 16; z++){
			for(int y = 0; y < 256; y++){
				if(westC != null)
					westLevels[y][z] = (byte) westC.getAt(15, y, z).getLevel();
				if(eastC != null)
					eastLevels[y][z] = (byte) eastC.getAt(0, y, z).getLevel();
			}
		}
		WaterChunk northC = this.getRelativeOrNull(0, -1);
		WaterChunk southC = this.getRelativeOrNull(0, 1);
		for(int x = 0; x < 16; x++){
			for(int y = 0; y < 256; y++){
				if(northC != null)
					northLevels[x][y] = (byte) northC.getAt(x, y, 15).getLevel();
				if(southC != null)
					southLevels[x][y] = (byte) southC.getAt(x, y, 0).getLevel();
			}
		}
		
		
		Biome biome = null;
		SmallChunkData scd = WaterChunkThread.getChunkData(this);
		if(scd != null){
			synchronized(this.data){
				for(int x = 0; x < 16; x++){
					for(int z = 0; z < 16; z++){
						biome = scd.getBiome(x, z);
						if(biome != null){
							if((Utils.isOcean(biome) || biome == Biome.RIVER)){
								int y = 48;
								WaterData current = getAt(x,y,z);
								while(y > 0 && Utils.isWater(current.getBlock())){
									current.setLevelUnchecked((int) WaterData.maxLevel - 1);
									current = getAt(x,--y,z);
								}
							}
						}
					}
				}
			}
		}
		synchronized(this.data){
			for(int y = 1; y < 256; y++){
				for(int x = 0; x < 16; x++){
					for(int z = 0; z < 16; z++){
						if(WaterData.getWaterLevelAt(this,x,y,z) > 0){
							getAt(x, y, z).moveWaterDown();
						}
					}
				}
				for(int x = 0; x < 16; x++){
					for(int z = 0; z < 16; z++){
						if(WaterData.getWaterLevelAt(this, x, y, z) > 1){
							getAt(x,y,z).moveWaterHorizontally(false);
						}
					}
				}
			}
		}
		
		boolean atLeastOneChanged = false;
		for(int y = 1; y < 256; y++){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					WaterData wd = getAt(x, y, z);
					if(WaterData.toWaterLevel(wd.getLevel()) != WaterData.toWaterLevel(Byte.toUnsignedInt(levels[x][y][z]))){
						wd.sendChangedEvent();
						atLeastOneChanged = true;
					}
				}
			}
		}
		for(int z = 0; z < 16; z++){
			for(int y = 0; y < 256; y++){
				WaterData wd = null;
				if(westC != null){
					wd = westC.getAt(15, y, z);
					if(WaterData.toWaterLevel(Byte.toUnsignedInt(westLevels[y][z])) != WaterData.toWaterLevel(wd.getLevel())){
						wd.sendChangedEvent();
					}
				}
				if(eastC != null){
					wd = eastC.getAt(0, y, z);
					if(WaterData.toWaterLevel(Byte.toUnsignedInt(eastLevels[y][z])) != WaterData.toWaterLevel(wd.getLevel())){
						wd.sendChangedEvent();
					}
				}
			}
		}
		for(int x = 0; x < 16; x++){
			for(int y = 0; y < 256; y++){
				WaterData wd = null;
				if(northC != null){
					wd = northC.getAt(x, y, 15);
					if(WaterData.toWaterLevel(Byte.toUnsignedInt(northLevels[x][y])) != WaterData.toWaterLevel(wd.getLevel())){
						wd.sendChangedEvent();
					}
				}
				if(southC != null){
					wd = southC.getAt(x, y, 0);
					if(WaterData.toWaterLevel(Byte.toUnsignedInt(southLevels[x][y])) != WaterData.toWaterLevel(wd.getLevel())){
						wd.sendChangedEvent();
					}
				}
			}
		}
	}
	*/
	
	public void updateVisuallyCheckLag(){
		if(System.currentTimeMillis() - lastUpdate < 1000){
			return;
		}
		lastUpdate = System.currentTimeMillis();
		if(Utils.hasHighLag()) return;
		updateVisually();
	}
	
	public void updateVisually(){
		if(!LivelyWorld.getInstance().isEnabled()) return;
		if(!isLoaded()) return;
		//needsUpdate = false;
		//BukkitRunnable br = new WaterChunkUpdateRunnable(this, UpdateType.LEVEL);
		//br.runTask(LivelyWorld.getInstance());
	}
	
	public void updateVisuallyAsync(){
		if(!LivelyWorld.getInstance().isEnabled()) return;
		if(!isLoaded()) return;
		if(!WaterChunkThread.isChunkLoaded(world, x, z)) return;
		
	}
	
	public void addWaterAt(int x, int y, int z, int amount) {
		if(!isLoaded()) load();
		WaterData d = data[y][x][z];
		if(d.getMaxQuantity() - d.getLevel() >= amount){
			d.level += amount;
		} else {
			int i = 0;
			while(amount > 0 && (y + i) < 256){
				d = getAt(x, y + i, z);
				int added = Math.min((int) d.getMaxQuantity() - d.getLevel(), amount);
				d.level += added;
				amount -= added;
				i++;
			}
		}
	}
	
	public void setNeedsUpsate(boolean value){
		needsUpdate = value;
	}
	
	public boolean needsUpdate(){
		return needsUpdate;
	}

	public void saturate() {
		if(!isLoaded()) load();
		for(int y = 0; y < 256; y ++){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					WaterData wd = data[y][x][z];
					if(wd.getMaxQuantity() > 1 && wd.getMaxQuantity() < 254 && wd.getResistance() != getResistanceFor(Material.LEAVES)){
						wd.level = (byte) wd.getMaxQuantity();
					}
				}
			}
		}
	}

	public void drain() {
		if(!isLoaded()) load();
		for(int y = 0; y < 256; y ++){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					WaterData wd = data[y][x][z];
					if(wd.getMaxQuantity() >= 254){
						wd.level = 0;
					}
				}
			}
		}
	}

	public boolean isRelativeLoaded(int x, int z) {
		WaterChunk r = WaterChunk.chunks.get(new ChunkCoordinates(world, this.x + x, this.z + z));
		if(r == null) return false;
		return r.isLoaded();
	}

	/*public byte[] getData() {
		return data;
	}*/
	
	public static class VisualUpdateTask extends BukkitRunnable{

		final WaterChunk chunk;
		final int x;
		final int y;
		final int z;
		final int level;
		final int resistance;
		
		public VisualUpdateTask(WaterChunk c, int x, int y, int z){
			this.chunk = c;
			this.x = x;
			this.y = y;
			this.z = z;
			this.level = chunk.data[y][x][z].getLevel();
			this.resistance = chunk.data[y][x][z].getResistance();
		}
		
		@Override
		public void run() {
			if(!chunk.isLoaded() || !chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ()))
				return;
			Block b = chunk.getWorld().getChunkAt(chunk.x, chunk.z).getBlock(x, y, z);
			if(canReplace(b.getType())){
				int waterLevel = toWaterLevel(level);
				if(waterLevel != Utils.getWaterHeight(b)){
					if(waterLevel > 0 && isDropable(b.getType())){
						b.breakNaturally();
					}
					Utils.setWaterHeight(b, waterLevel, true);
				}
			} else if(b.getRelative(BlockFace.DOWN).getType() == Material.AIR && b.getType() != Material.AIR && level > 0) {
				chunk.getWorld().spawnParticle(Particle.DRIP_WATER, b.getX() + Utils.fastRandomDouble(), b.getY() - 0.01, b.getZ() + Utils.fastRandomDouble(), 1);
			}
		}
		
	}
}
