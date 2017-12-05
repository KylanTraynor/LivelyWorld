package com.kylantraynor.livelyworld.water;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.Utils.ChunkCoordinates;
import com.kylantraynor.livelyworld.Utils.SmallChunkData;
import com.kylantraynor.livelyworld.water.WaterChunkUpdateRunnable.UpdateType;

public class WaterChunk {
	final static Map<ChunkCoordinates, WaterChunk> chunks = new ConcurrentHashMap<ChunkCoordinates, WaterChunk>(); 
	final static int sectorLength = 4096;
	final static int currentVersion = 1;
	static boolean disabled = false;
	
	final byte[] data = new byte[16 * 16 * 256 * 4];
	private boolean isLoaded = false;
	private final int x;
	private final int z;
	private final World world;
	private boolean needsUpdate = false;
	private boolean wasGenerated = false;
	private static Utils.Lock fileLock = new Utils.Lock();
	
	public WaterChunk(World w, int x, int z){
		this.world = w;
		this.x = x;
		this.z = z;
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
	
	public WaterData getAt(int x, int y, int z){
		if(!isLoaded) load();
		return new WaterData(this, x, y, z);
	}
	
	void setDataUnchecked(int data, int x, int y, int z){
		if(!isLoaded) load();
		byte[] b = Utils.toByteArray(data);
		int index = getIndex(x,y,z);
		this.data[index    ] = b[0];
		this.data[index + 1] = b[1];
		this.data[index + 2] = b[2];
		this.data[index + 3] = b[3];
	}
	
	void setData(int data, int x, int y, int z){
		if(!isLoaded) load();
		byte[] b = Utils.toByteArray(data);
		int index = getIndex(x,y,z);
		synchronized(this.data){
			this.data[index    ] = b[0];
			this.data[index + 1] = b[1];
			this.data[index + 2] = b[2];
			this.data[index + 3] = b[3];
		}
		/*try {
			dataLock.lock();
			try{
				this.data[index    ] = b[0];
				this.data[index + 1] = b[1];
				this.data[index + 2] = b[2];
				this.data[index + 3] = b[3];
			} finally {
				dataLock.unlock();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
	}
	
	private int getIndex(int x, int y, int z){
		return (y << 10) + (x << 6) + (z << 2);
	}
	
	int getData(int x, int y, int z) {
		if(!isLoaded) load();
		int index = getIndex(x, y, z);
		return Utils.toInt(data[index], data[index + 1], data[index + 2], data[index + 3]);
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
	
	public static void unloadAll(){
		if(disabled) return;
		disabled = true;
		while(!chunks.isEmpty()){
			WaterChunk c = chunks.get(0);
			c.unload();
			chunks.remove(0);
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
					if(version == currentVersion && baos.size() == data.length + 8){
						int chunkData = Utils.toInt(array[4], array[5], array[6], array[7]);
						synchronized(data){
							for(int i = 8; i < data.length + 8; i++){
								data[i - 8] = array[i];
							}
						}
						needsUpdate = ((chunkData & 1) == 1);
						wasGenerated = ((chunkData & 2) == 2);
					} else if(baos.size() == data.length) {
						synchronized(data){
							for(int i = 0; i < data.length; i++){
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
	
	void tickAll(){
		if(!isLoaded()) return;
		if(!wasGenerated){
			this.saturate();
			wasGenerated = true;
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
		if(LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()){
			if(needsUpdate() || Utils.fastRandomDouble() < 0.01) updateVisually(true);
		}
		if(Utils.fastRandomDouble() < 0.01){
			if(!isLoaded()) return;
			BukkitRunnable br = new WaterChunkUpdateRunnable(this, UpdateType.RESISTANCE);
			br.runTask(LivelyWorld.getInstance());
		}
	}
	
	public void updateVisually(final boolean fullUpdate){
		if(!LivelyWorld.getInstance().isEnabled()) return;
		if(!isLoaded()) return;
		needsUpdate = false;
		BukkitRunnable br = new WaterChunkUpdateRunnable(this, UpdateType.LEVEL);
		br.runTask(LivelyWorld.getInstance());
	}
	
	public void addWaterAt(int x, int y, int z, int amount) {
		WaterData d = getAt(x, y, z);
		if(WaterData.maxLevel - d.getLevel() >= amount){
			d.setLevel(d.getLevel() + amount);
		} else {
			int i = 0;
			while(amount > 0 && (y + i) < 256){
				d = getAt(x, y + i, z);
				int added = Math.min((int) WaterData.maxLevel - d.getLevel(), amount);
				d.setLevel(d.getLevel() + added);
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
		synchronized(this.data){
			for(int y = 0; y < 256; y ++){
				for(int x = 0; x < 16; x++){
					for(int z = 0; z < 16; z++){
						WaterData data = this.getAt(x, y, z);
						if(data.getMaxQuantity() > 1 && data.getMaxQuantity() < 254 && data.getResistance() != WaterData.getResistanceFor(Material.LEAVES)){
							data.setLevelUnchecked(data.getMaxQuantity());
						}
					}
				}
			}
		}
	}

	public void drain() {
		if(!isLoaded()) load();
		synchronized(this.data){
			for(int y = 0; y < 256; y ++){
				for(int x = 0; x < 16; x++){
					for(int z = 0; z < 16; z++){
						WaterData data = this.getAt(x, y, z);
						if(data.getMaxQuantity() >= 254){
							data.setLevelUnchecked(0);
						}
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
}
