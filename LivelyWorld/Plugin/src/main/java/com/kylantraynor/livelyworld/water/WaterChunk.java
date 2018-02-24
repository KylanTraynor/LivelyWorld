package com.kylantraynor.livelyworld.water;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
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
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import sun.misc.Unsafe;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.Utils.ChunkCoordinates;
import com.kylantraynor.livelyworld.Utils.SmallChunkData;

public class WaterChunk {
	final static Map<ChunkCoordinates, WaterChunk> chunks = new ConcurrentHashMap<ChunkCoordinates, WaterChunk>(); 
	final static int sectorLength = 4096;
	final static int currentVersion = 1;
	final static int xInc = 1<<6;
	final static int yInc = 1<<10;
	final static int zInc = 1<<2;
	static boolean disabled = false;
	
	double tickRandom = Utils.fastRandomDouble();
	
	final byte[] data = new byte[16 * 16 * 256 * 4];
	final int[] pressure = new int[16*16*256];
	//final WaterData[][][] data = new WaterData[256][16][16];
	//final int dataLength = data.length * data[0].length * data[0][0].length;
	//final int dataByteLength = dataLength * 4;
	private boolean isLoaded = false;
	private final int x;
	private final int z;
	private final World world;
	private boolean needsUpdate = false;
	private boolean wasGenerated = false;
	private WeakReference<Chunk> weakChunk;
	private static Utils.Lock fileLock = new Utils.Lock();
	private long lastUpdate = System.currentTimeMillis();
	
	public static long[] total = new long[4];
	public static long[] samples = new long[4];
	
	public WaterChunk(World w, int x, int z){
		this.world = w;
		this.x = x;
		this.z = z;
		/*for(int y = 255; y >= 0; y--){
			for(int x1 = 0; x1 < 16; x1++){
				for(int z1 = 0; z1 < 16; z1++){
					if(data[y][x1][z1] == null){
						data[y][x1][z1] = new WaterData(0, x1, y, z1);
					}
				}
			}
		}*/
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
		return new WaterData(this, getData(x,y,z), getPressure(x,y,z), x, y, z);
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
	
	int getData(int x, int y, int z) {
		if(!isLoaded) load();
		int index = getIndex(x, y, z);
		return Utils.toInt(data[index], data[index + 1], data[index + 2], data[index + 3]);
	}
	
	int getPressure(int x, int y, int z){
		int index = getIndex(x, y, z);
		return getPressure(index);
	}
	
	int getPressure(int index){
		return pressure[index >> 2];
	}
	
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
	
	public static WaterChunk getOrNull(World world, int x, int z){
		return chunks.get(new ChunkCoordinates(world, x, z));
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
				/*for(int y = 0; y < data.length; y++){
					for(int x = 0; x < data[y].length; x++){
						for(int z = 0; z < data[y][x].length; z++){
							dos.write(data[y][x][z].getByteArray());
						}
					}
				}*/
				dos.write(data);
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
			int locationIndex = ((Utils.floorMod2(getX(),5) << 5) + Utils.floorMod2(getZ(),5)) << 2;
			int sizeIndex = (4096 + ((Utils.floorMod2(getX(),5) << 5) + Utils.floorMod2(getZ(),5)) << 2);
			
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
					if(version == currentVersion && baos.size() == data.length + 8){
						int chunkData = Utils.toInt(array[4], array[5], array[6], array[7]);
						synchronized(data){
							for(int i = 8; i < data.length + 8; i++){
								/*int index = (i * 4) + 8;
								int y = (i >> 8) & (data.length - 1);
								int x = (i >> 4) & (data[0].length - 1);
								int z = i & (data[0][0].length - 1);
								byte[] b = new byte[]{array[index], array[index + 1], array[index + 2], array[index + 3]};
								//int d = Utils.toInt(array[index], array[index + 1], array[index + 2], array[index + 3]);
								data[y][x][z] = new WaterData(b, x, y, z);
								*/
								data[i - 8] = array[i];
							}
						}
						needsUpdate = ((chunkData & 1) == 1);
						wasGenerated = ((chunkData & 2) == 2);
					} else if(baos.size() == data.length) {
						synchronized(data){
							for(int i = 0; i < data.length; i++){
								/*int index = (i * 4);
								int y = (i >> 8) & (data.length - 1);
								int x = (i >> 4) & (data[0].length - 1);
								int z = i & (data[0][0].length - 1);
								byte[] b = new byte[]{array[index], array[index + 1], array[index + 2], array[index + 3]};
								//int d = Utils.toInt(array[index], array[index + 1], array[index + 2], array[index + 3]);
								data[y][x][z] = new WaterData(b, x, y, z);*/
								data[i] = array[i];
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
		/*
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
		}*/
		
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
	
	public void processWaterMove(final int x, final int y, final int z){
		
		int index = getIndex(x,y,z);
		if(getLevelUnsafe(index) == 0) return;
		if(isSolidUnsafe(index)){
			if(Utils.superFastRandomInt() <= getResistanceUnsafe(index)){
				return;
			}
		}
		
		//if(x > 0 && x < 15 && z > 0 && z < 15){
			processWaterMoveDirectData(x,y,z);
		//} else {
		//	processWaterMoveWaterData(x,y,z);
		//}
		
	}
	
	private void processWaterMoveWaterData(int x, int y, int z) {
		int index = getIndex(x,y,z);
		int downIndex = y > 0 ? index - yInc : -1;
		int upIndex = y < 255 ? index + yInc : -1;
		
		WaterData west = null;
		if(x > 0){
			west = getAt(x -1, y, z);
		} else if(this.isRelativeLoaded(-1, 0)){
			west = getRelative(-1,0).getAt(15, y, z);
		}
		WaterData east = null;
		if(x < 15){
			east = getAt(x+1,y,z);
		} else if(this.isRelativeLoaded(1, 0)){
			east = getRelative(1,0).getAt(0,y,z);
		}
		WaterData north = null;
		if(z > 0){
			north = getAt(x,y,z-1);
		} else if(this.isRelativeLoaded(0, -1)) {
			north = getRelative(0,-1).getAt(x,y,15);
		}
		WaterData south = null;
		if(z < 15){
			south = getAt(x, y, z+1);
		} else if(this.isRelativeLoaded(0, 1)){
			south = getRelative(0,1).getAt(x, y, 0);
		}
		
		boolean stable = false;
		while(getLevel(index) > 0 && !stable){
			if(y > 0){
				int max = getMaxQuantityRDM(downIndex);
				if(getLevel(downIndex) < max
						&& getPressure(downIndex) < getPressure(index) + max && Utils.fastRandomInt(256) > getResistance(downIndex)){
					data[downIndex]++;
					if(isSolid(downIndex)){
						pressure[downIndex>>2]++;
					}
					data[index]--;
					pressure[index>>2]--;
					stable = false;
					//d.lastDirection = 0;
					continue;
				}
			}
			if(y < 255){
				int max = getMaxQuantityRDM(index);
				if(getPressure(upIndex) < getPressure(index) - max){
					if(getLevel(upIndex) < getMaxQuantityRDM(upIndex)){
						data[upIndex]++;
						pressure[upIndex>>2]++;
						data[index]--;
						if(isSolid(index)){
							pressure[index>>2]--;
						}
						stable = false;
						//d.lastDirection = 0;
						continue;
					}
				}
			}
			WaterData min = getMinPressure(north, east, west, south);
			if(min == null) {
				stable = true;
				//d.lastDirection = 0;
			} else if(min.pressure < getPressure(index)){
				if(min.getLevel() < min.getMaxQuantityRDM()){
					min.level++;
					min.pressure++;
					data[index]--;
					pressure[index>>2]--;
					stable = false;
					/*if(min == north) d.lastDirection = 1;
					if(min == east) d.lastDirection = 2;
					if(min == west) d.lastDirection = 3;
					if(min == south) d.lastDirection = 4;*/
				} else {
					stable = true;
				}
			} else {
				stable = true;
				//d.lastDirection = 0;
			}
		}
		if(west != null) west.update();
		if(east != null) east.update();
		if(north != null) north.update();
		if(south != null) south.update();
	}

	private void processWaterMoveDirectData(final int x, final int y, final int z) {
		int index = getIndex(x,y,z);
		int upIndex = y < 255 ? index + yInc : -1;
		int downIndex = y > 0 ? index - yInc : -1;
		int northIndex = z > 0 ? index - zInc : getIndex(x, y, 15);
		int southIndex = z < 15 ? index + zInc : getIndex(x, y, 0);
		int westIndex = x > 0 ? index - xInc : getIndex(15, y, z);
		int eastIndex = x < 15 ? index + xInc : getIndex(0, y, z);
		boolean stable = false;
		while(getLevel(index) > 0 && !stable){
			if(y > 0){
				int max = getMaxQuantityRDM(downIndex);
				if(getLevelUnsafe(downIndex) < max
						&& getPressureUnsafe(downIndex) < getPressureUnsafe(index) + max && Utils.superFastRandomInt() > getResistance(downIndex)){
					data[downIndex]++;
					if(isSolidUnsafe(downIndex)){
						pressure[downIndex>>2]++;
					}
					data[index]--;
					pressure[index>>2]--;
					stable = false;
					//d.lastDirection = 0;
					continue;
				}
			}
			if(y < 255){
				int max = getMaxQuantityRDM(index);
				if(getPressureUnsafe(upIndex) < getPressureUnsafe(index) - max){
					if(getLevelUnsafe(upIndex) < getMaxQuantityRDM(upIndex)){
						data[upIndex]++;
						pressure[upIndex>>2]++;
						data[index]--;
						if(isSolidUnsafe(index)){
							pressure[index>>2]--;
						}
						stable = false;
						//d.lastDirection = 0;
						continue;
					}
				}
			}
			WaterChunk minC = this;
			int minIndex = -1;
			if(x == 0 || x == 15 || z == 0 || z == 15){
				int[] indices = new int[]{northIndex, southIndex, westIndex, eastIndex};
				WaterChunk[] chunks = new WaterChunk[]{
						z > 0? this : getRelative(0,-1),
						z < 15? this : getRelative(0,1),
						x > 0? this : getRelative(-1,0),
						x < 15? this : getRelative(1,0)
					};
				int min = getMinPressureDirectData(indices, chunks);
				minC = chunks[min];
				minIndex = indices[min];
			} else {
				minIndex = getMinPressureDirectData(northIndex, southIndex, westIndex, eastIndex);
			}
			if(minC.getPressureUnsafe(minIndex) < getPressureUnsafe(index)){
				if(minC.getLevelUnsafe(minIndex) < minC.getMaxQuantityRDM(minIndex)){
					minC.data[minIndex]++;
					minC.pressure[minIndex>>2]++;
					data[index]--;
					pressure[index>>2]--;
					stable = false;
					/*if(min == north) d.lastDirection = 1;
					if(min == east) d.lastDirection = 2;
					if(min == west) d.lastDirection = 3;
					if(min == south) d.lastDirection = 4;*/
				} else {
					stable = true;
				}
			} else {
				stable = true;
				//d.lastDirection = 0;
			}
		}
	}

	private int getMinPressureDirectData(int... indices) {
		int min = indices[0];
		for(int i = 1; i < indices.length; i++){
			if(isSolidUnsafe(indices[i]) && Utils.superFastRandomInt() >= getResistanceUnsafe(indices[i])) continue;
			if(getPressureUnsafe(indices[i]) < getPressureUnsafe(min)){
				min = indices[i];
			}
		}
		return min;
	}
	
	private int getMinPressureDirectData(int[] indices, WaterChunk[] chunks) {
		int min = 0;
		for(int i = 1; i < indices.length; i++){
			if(chunks[i].isSolidUnsafe(indices[i]) && Utils.superFastRandomInt() >= chunks[i].getResistanceUnsafe(indices[i])) continue;
			if(chunks[i].getPressureUnsafe(indices[i]) < chunks[min].getPressureUnsafe(indices[min])){
				min = i;
			}
		}
		return min;
	}

	void updateData(WaterData d) {
		int index = getIndex(d.getX(), d.getY(), d.getZ());
		data[index] = d.level;
		data[index+1] = d.resistance;
		data[index+2] = 0;
		data[index+3] = (byte) (d.isSolid ? 0x10 : 0x00);;
		pressure[index >> 2] = d.pressure;
	}

	public WaterData getMinPressure(WaterData... data){
		WaterData min = data[0];
		for(int i = 1; i < data.length; i++){
			if(data[i] == null) continue;
			if(data[i].isSolid && Utils.superFastRandomInt() >= data[i].getResistance()) continue;
			if(min == null) {
				min = data[i];
			} else if(data[i].pressure < min.pressure){
				min = data[i];
			}
		}
		return min;
	}

	public void updatePressure(int index){
		int l = getLevelUnsafe(index);
		int p = getLevelUnsafe(index);
		if(isSolidUnsafe(index)){
			p += getResistanceUnsafe(index);
		} else if(index >> 10 < 255 && l == 0xFF){
			//int upIndex = index + yInc;
			//if(!isSolidUnsafe(upIndex)){
				p += getPressureUnsafe(index + yInc);
			//}
		}
		setPressureUnsafe(index, p);
	}
	
	public void safeUpdatePressure(int x, int y, int z){
		int index = getIndex(x,y,z);
		int p = getLevel(index);
		if(isSolid(index)){
			p += getResistance(index);
		} else if(y < 255 && getLevel(index) == 0xFF){
			int upIndex = getIndex(x, y+1,z);
			if(isSolid(upIndex)){
				p += getPressure(upIndex);
			}
		}
		pressure[index >> 2] = p;
	}
	
	void update(){
		if(!isLoaded) return;
		
		int dist = (int) Math.sqrt(distanceSquaredFromNearestPlayer());
		float det = (float) (2.0f / (Math.max(dist, 1) >> 2));
		if(Utils.fastRandomFloat() > det) return;
		
		// If the chunk was not generated, generate it.
		if(!wasGenerated){
			this.saturate();
			wasGenerated = true;
		}
		
		// Get a snapshot of the actual chunk.
		//SmallChunkData c = null;
		if(!WaterChunkThread.isChunkLoaded(world, x, z)){
			return;
		}
		weakChunk = new WeakReference<Chunk>(world.getChunkAt(x, z));

		long time = System.nanoTime();
		// Saturate Oceans.
		Biome biome = null;
		for(int x = 0; x < 16; x++){
			for(int z = 0; z < 16; z++){
				if(weakChunk.get() != null){
					biome = weakChunk.get().getBlock(x, 0, z).getBiome();
				}
				if(biome != null){
					if((Utils.isOcean(biome) || biome == Biome.RIVER)){
						int y = 48;
						if(weakChunk.get() != null){
							Material m = weakChunk.get().getBlock(x, y, z).getType();
							while(y > 0 && (Utils.isWater(m) || m == Material.AIR)){
								setLevelUnsafe(getIndex(x,y,z), (byte) 0xFF);
								y--;
							}
						}
					}
				}
			}
		}
		time = System.nanoTime() - time;
		total[0] += time;
		if(total[0] < 0){
			total[0] = time;
			samples[0] = 1;
		} else {
			samples[0]++;
		}
		
		boolean refresh = dist <= 2 ? true : (Utils.fastRandomFloat() > (det/2) ? true : false);
		if(refresh){
			time = System.nanoTime();
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					if(weakChunk.get() != null){
						for(int y = 0; y < 256; y++){
							int index = getIndex(x, y, z);
							if(weakChunk.get() != null){
								Material m = weakChunk.get().getBlock(x, y, z).getType();
								setResistanceUnsafe(index, (byte) getResistanceFor(m));
								setSolidUnsafe(index, isSolid(m));
							} else {
								break;
							}
						}
					} else {
						break;
					}
				}
			}
			time = System.nanoTime() - time;
			total[3] += time;
			if(total[3] < 0){
				total[3] = time;
				samples[3] = 1;
			} else {
				samples[3]++;
			}
		}
		// Update Pressure.
		time = System.nanoTime();
		for(int i = data.length -4; i >= 0; i -= 4){
			updatePressure(i);
		}
		/*for(int y = 255; y >= 0; y--){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					updatePressure(x, y, z);
				}
			}
		}*/
		
		time = System.nanoTime() - time;
		total[1] += time;
		if(total[1] < 0){
			total[1] = time;
			samples[1] = 1;
		} else {
			samples[1]++;
		}
		
		// Reset super fast random
		Utils.superFastRandomIntReset();
		
		// Update Level
		int xStep = Utils.superFastRandomInt() < 128 ? -1 : 1;
		int zStep = Utils.superFastRandomInt() < 128 ? -1 : 1;
		
		time = System.nanoTime();
		for(int y = 0; y < 256; y++){
			for(int x = xStep == 1 ? 0 : 15; xStep == 1 ? x < 16 : x >= 0; x += xStep){
				for(int z = zStep == 1 ? 0 : 15; zStep == 1 ? z < 16 : z >= 0; z += zStep){
					processWaterMove(x, y, z);
				}
			}
		}
		
		time = System.nanoTime() - time;
		total[2] += time;
		if(total[2] < 0){
			total[2] = time;
			samples[2] = 1;
		} else {
			samples[2]++;
		}
		
		if(System.currentTimeMillis() - lastUpdate < 1000) return;
		if(dist > 10) return;
		if(dist > 2 && Utils.superFastRandomInt() > 2) return;
		if(!refresh) return;
		
		for(int y = 0; y < 256; y++){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					if(weakChunk.get() != null){
						Material m = weakChunk.get().getBlock(x, y, z).getType();
						if(canReplace(m)){
							int waterLevel = 0;
							if(Utils.isWater(m)){
								waterLevel = Utils.getWaterHeight(weakChunk.get().getBlock(x, y, z).getData());
							}
							int index = getIndex(x,y,z);
							if(waterLevel != toWaterLevel(getLevel(index))){
								updateVisually(x,y,z, dist <= 2);
							}
						}
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
			return 0;
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
		case LEAVES: case LEAVES_2: case VINE: case WATER_LILY: case CROPS:
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
		case LONG_GRASS: case DOUBLE_PLANT: case RED_ROSE: case YELLOW_FLOWER: case VINE: case WATER_LILY: case CROPS:
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
		if(mat == Material.RED_ROSE) return true;
		if(mat == Material.YELLOW_FLOWER) return true;
		if(mat == Material.DOUBLE_PLANT) return true;
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
		if(mat == Material.RED_ROSE) return true;
		if(mat == Material.YELLOW_FLOWER) return true;
		if(mat == Material.DOUBLE_PLANT) return true;
		return false;
	}
	
	private void updateVisually(int x, int y, int z, boolean progressive) {
		if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
		BukkitRunnable br = new VisualUpdateTask(this, x, y, z);
		br.runTaskLater(LivelyWorld.getInstance(), progressive? Utils.fastRandomInt(20) : 1);
	}

	public static int toWaterLevel(int level){
		return (level + 16) >> 5;
	}
	
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
		if(!isLoaded()) return;
		int index = getIndex(x,y,z);
		if(getMaxQuantity(index) - getLevel(index) >= amount){
			data[index] += amount;
		} else {
			int i = 0;
			while(amount > 0 && (y + i) < 256){
				index = getIndex(x,y+i,z);
				int added = Math.min((int) getMaxQuantity(index) - getLevel(index), amount);
				data[index] += added;
				amount -= added;
				i++;
			}
		}
	}
	
	public int getLevel(int index){
		return Byte.toUnsignedInt(data[index]);
	}
	
	public int getLevel(int x, int y, int z){
		int index = getIndex(x,y,z);
		return getLevel(index);
	}
	
	public int getResistance(int index){
		return Byte.toUnsignedInt(data[index + 1]);
	}
	
	public int getResistance(int x, int y, int z){
		int index = getIndex(x,y,z);
		return getResistance(index);
	}
	
	private void setResistance(int index, byte value){
		data[index + 1] = value;
	}
	
	public int getMaxQuantity(int index){
		return 0xFF - Byte.toUnsignedInt(data[index + 1]);
	}
	
	public int getMaxQuantity(int x, int y, int z){
		int index = getIndex(x,y,z);
		return getMaxQuantity(index);
	}
	
	public int getMaxQuantityRDM(int index){
		if(isSolid(index)){
			int result = getMaxQuantity(index) - Utils.superFastRandomInt();
			return result > 0 ? result : 0;
		} else {
			return getMaxQuantity(index);
		}
	}
	
	public boolean isSolid(int index){
		return ((data[index + 3] & 0x10) >> 4) == 1;
	}
	
	public boolean isSolid(int x, int y, int z){
		int index = getIndex(x,y,z);
		return isSolid(index);
	}
	
	private void setSolid(int index, boolean value){
		byte old = data[index + 3];
		byte b = (byte) ((old & ~0x10) + (value ? 0x10 : 0x00));
		data[index + 3] = b;
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
					int index = getIndex(x,y,z);
					if(getMaxQuantity(index) > 1 && getMaxQuantity(index) < 254 && getResistance(index) != getResistanceFor(Material.LEAVES)){
						data[index] = (byte) getMaxQuantity(index);
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
					int index = getIndex(x,y,z);
					if(getMaxQuantity(index) >= 254){
						data[index] = (byte) 0;
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
			this.level = chunk.getLevel(x, y, z);
			this.resistance = chunk.getResistance(x, y, z);
		}
		
		@Override
		public void run() {
			if(!chunk.isLoaded() || !chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ()))
				return;
			Block b = chunk.getWorld().getChunkAt(chunk.x, chunk.z).getBlock(x, y, z);
			if(canReplace(b.getType())){
				int waterLevel = toWaterLevel(level);
				if(waterLevel != Utils.getWaterHeight(b)){
					if(isDropable(b.getType())){
						if(waterLevel > 3){
							b.breakNaturally();
							Utils.setWaterHeight(b, waterLevel, true, true);
						}
					} else if(waterLevel > 0) {
						Utils.setWaterHeight(b, waterLevel, true, true);
					} else if(Utils.isWater(b.getType())){
						Utils.setWaterHeight(b, 0, true, true);
					}
				}
			} else if(b.getRelative(BlockFace.DOWN).getType() == Material.AIR && b.getType() != Material.AIR && level > 0) {
				chunk.getWorld().spawnParticle(Particle.DRIP_WATER, b.getX() + Utils.fastRandomDouble(), b.getY() - 0.01, b.getZ() + Utils.fastRandomDouble(), 1);
			}
		}
		
	}

	/**
	 * Sets the water level at x, y, z, and enforces that level to be between
	 * 0 and 255.
	 * @param x in [0,15]
	 * @param y in [0,255]
	 * @param z in [0,15]
	 * @param level
	 */
	public void setLevel(int x, int y, int z, int level) {
		if(level < 0) level = 0;
		if(level > 0xFF) level = 0xFF;
		int index = getIndex(x,y,z);
		data[index] = (byte) level;
	}
	
	
	private int getPressureUnsafe(int index){
		return Utils.unsafe.getInt(pressure, Utils.baseAddressInts + index);
	}
	
	private int getLevelUnsafe(int index){
		return Byte.toUnsignedInt(Utils.unsafe.getByte(data, Utils.baseAddressBytes + index));
	}
	
	private int getResistanceUnsafe(int index){
		return Byte.toUnsignedInt(Utils.unsafe.getByte(data, Utils.baseAddressBytes + index + 1));
	}
	
	private boolean isSolidUnsafe(int index){
		byte b = Utils.unsafe.getByte(data, Utils.baseAddressBytes + index + 3);
		return (b & 0x10) == 0x10;
	}
	
	private void setPressureUnsafe(int index, int value){
		Utils.unsafe.putInt(pressure, Utils.baseAddressInts + index, value);
	}
	
	private void setLevelUnsafe(int index, byte value){
		Utils.unsafe.putByte(data, Utils.baseAddressBytes + index, value);
	}
	
	private void setResistanceUnsafe(int index, byte value){
		Utils.unsafe.putByte(data, Utils.baseAddressBytes + index + 1, value);
	}
	
	private void setSolidUnsafe(int index, boolean value){
		byte old = Utils.unsafe.getByte(data, Utils.baseAddressBytes + index + 3);
		byte b = (byte) ((old & ~0x10) + (value ? 0x10 : 0x00));
		Utils.unsafe.putByte(data, Utils.baseAddressBytes + index + 3, b);
	}
	
}
