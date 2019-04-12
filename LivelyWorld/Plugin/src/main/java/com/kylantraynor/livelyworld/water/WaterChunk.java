package com.kylantraynor.livelyworld.water;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import com.kylantraynor.livelyworld.utils.AsyncChunk;
import com.kylantraynor.livelyworld.utils.ShortQueue;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.Utils.ChunkCoordinates;

public class WaterChunk {
    /*
	final static Map<ChunkCoordinates, WaterChunk> chunks = new ConcurrentHashMap<>();
	final private static int sectorLength = 4096;
	final private static int currentVersion = 1;
	final private static int xInc = 1<<6;
	final private static int yInc = 1<<10;
	final private static int zInc = 1<<2;
	static public int[] delta = new int[4];
	static boolean disabled = false;
	
	double tickRandom = Utils.fastRandomDouble();
	
	final private byte[] data = new byte[16 * 16 * 256 * 4];
	final private int[] pressure = new int[16*16*256];
	final private ShortQueue updateQueue;
	private boolean isLoaded = false;
	private final int x;
	private final int z;
	private final World world;
	private boolean needsUpdate = false;
	private boolean wasGenerated = false;
	private static Utils.Lock fileLock = new Utils.Lock();
	private long lastUpdate = System.currentTimeMillis();
	
	public static long[] total = new long[4];
	public static long[] samples = new long[4];
	
	public WaterChunk(World w, int x, int z){
		this.world = w;
		this.x = x;
		this.z = z;
		this.updateQueue = new ShortQueue(4096);
	}

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
		ChunkCoordinates[] keys = chunks.keySet().toArray(new ChunkCoordinates[0]);
		for(ChunkCoordinates cc : keys){
			WaterChunk c = chunks.get(cc);
			if(c == null) continue;
			c.unload();
			chunks.remove(cc);
			LivelyWorld.getInstance().getLogger().info("" + chunks.size() + " remaining.");
		}
	}
	
	private File getFile(){
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
	
	private void saveToFile(){
		
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
					//LivelyWorld.getInstance().getLogger().info(getFile().getName()+": Rewriting at location: " + location + " (" + location*sectorLength + ") with a size of " + baos.size() + " with padding: " + finalPadding + " and " + newSectors + " sectors" + ". Moving " + nextChunks.length + " bytes.");
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

	private void loadFromFile(){
		int compressedSize = 0;
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

			f.seek(location * sectorLength);
			
			//LivelyWorld.getInstance().getLogger().info("Reading from location: " + location + " (" + location*sectorLength + ") with a size of " + size);
			
			ByteArrayOutputStream baos;
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
							System.arraycopy(array, 8, data,0, data.length);
						}
						needsUpdate = ((chunkData & 1) == 1);
						wasGenerated = ((chunkData & 2) == 2);
					} else if(baos.size() == data.length) {
						synchronized(data){
							System.arraycopy(array, 0, data, 0, data.length);
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
                if (f != null) {
                    f.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
	}

	public boolean isLoaded() {
		return this.isLoaded;
	}
	
	private void setLoaded(boolean b){
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
	
	private void processWaterMove(final int index){

	    //if(!needsUpdate(index)) return;
	    int level = getLevelUnsafe(index);
		if(level == 0) return;
		if(isSolidUnsafe(index)){
			if(Utils.superFastRandomInt() <= getResistanceUnsafe(index)){
				return;
			}
		}
		processWaterMoveDirectData(index);
		
	}


	private void processWaterMoveDirectData(int index) {
		final int y = index >> 10;
		final int x = (index >> 6) & 0x0F;
		final int z = (index >> 2) & 0x0F;
		int upIndex = y < 255 ? index + yInc : -1;
		int downIndex = y > 0 ? index - yInc : -1;
		int northIndex = z > 0 ? index - zInc : getIndex(x, y, 15);
		int southIndex = z < 15 ? index + zInc : getIndex(x, y, 0);
		int westIndex = x > 0 ? index - xInc : getIndex(15, y, z);
		int eastIndex = x < 15 ? index + xInc : getIndex(0, y, z);
		boolean stable = false;
		while(getLevelUnsafe(index) > 0 && !stable){
			if(y > 0){
				int max = getMaxQuantityRDMUnsafe(downIndex);
				if(getLevelUnsafe(downIndex) < max
						&& getPressureUnsafe(downIndex) < getPressureUnsafe(index) + max && Utils.superFastRandomInt() > getResistanceUnsafe(downIndex)){
					setLevelUnsafe(downIndex, (byte) (getLevelUnsafe(downIndex) + 1));
					if(isSolidUnsafe(downIndex)){
						setPressureUnsafe(downIndex, getPressureUnsafe(downIndex) + 1);
					}
					setLevelUnsafe(index, (byte) (getLevelUnsafe(index) - 1));
					setPressureUnsafe(index, getPressureUnsafe(index) - 1);
					stable = false;
					//d.lastDirection = 0;
					continue;
				}
			}
			if(y < 255){
				int max = getMaxQuantityRDMUnsafe(index);
				if(getPressureUnsafe(upIndex) < getPressureUnsafe(index) - max){
					if(getLevelUnsafe(upIndex) < getMaxQuantityRDMUnsafe(upIndex)){
						setLevelUnsafe(upIndex, (byte) (getLevelUnsafe(index) + 1));
						setPressureUnsafe(upIndex, getPressureUnsafe(upIndex) + 1);
						setLevelUnsafe(index, (byte) (getLevelUnsafe(index) - 1));
						if(isSolidUnsafe(index)){
							setPressureUnsafe(index, getPressureUnsafe(index) - 1);
						}
						stable = false;
						//d.lastDirection = 0;
						continue;
					}
				}
			}
			WaterChunk minC = this;
			int minIndex;
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
			if(!minC.isLoaded()) return;
			if(minC.getPressureUnsafe(minIndex) < getPressureUnsafe(index)){
				if(minC.getLevelUnsafe(minIndex) < minC.getMaxQuantityRDMUnsafe(minIndex)){
					minC.setLevelUnsafe(minIndex, (byte) (minC.getLevelUnsafe(minIndex) + 1));
					minC.setPressureUnsafe(minIndex, minC.getPressureUnsafe(minIndex) + 1);
					setLevelUnsafe(index, (byte) (getLevelUnsafe(index) - 1));
					setPressureUnsafe(index, getPressureUnsafe(index) - 1);
					stable = false;
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
		int l = indices.length;
		for(int i = 1; i < l; i++){
			if(isSolidUnsafe(indices[i]) && Utils.superFastRandomInt() >= getResistanceUnsafe(indices[i])) continue;
			if(getPressureUnsafe(indices[i]) < getPressureUnsafe(min)){
				min = indices[i];
			}
		}
		return min;
	}
	
	private int getMinPressureDirectData(int[] indices, WaterChunk[] chunks) {
		int min = 0;
		int l = indices.length;
		for(int i = 1; i < l; i++){
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
		data[index+3] = (byte) (d.isSolid ? 0x10 : 0x00);
		pressure[index >> 2] = d.pressure;
	}

	private void updatePressure(int index){
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

	void updateQueue(){
	    if(!isLoaded) return;
	    int length = updateQueue.size();
	    int i = 0;
	    while(i++ < length){
	        int index = Short.toUnsignedInt(updateQueue.removeShort()) << 2;
	        updatePressure(index);
        }
    }
	
	void update(){
		if(!isLoaded) return;
		if(!LivelyWorld.allowsTasks()) return;
		
		int dist = (int) Math.sqrt(distanceSquaredFromNearestPlayer());
		float det = 2.0f / (Math.max(dist, 1) >> 1);
		if(Utils.fastRandomFloat() > det) return;
		if(dist > 10) return;
		
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

		AsyncChunk futureChunk = AsyncChunk.getAt(world, x, z);

        long time = System.nanoTime();
		// Saturate Oceans.
		Biome biome = null;
		for(int x = 0; x < 16; x++){
			for(int z = 0; z < 16; z++){
			    biome = futureChunk.getBiome(x,z);
				if(biome != null){
					if((Utils.isOcean(biome) || biome == Biome.RIVER)){
						int y = 48;
                        Material m = futureChunk.getType(x,y,z);
                        while(y > 0 && (Utils.isWater(m) || m == Material.AIR)){
                            setLevelUnsafe(getIndex(x,y,z), (byte) 0xFF);
                            y--;
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
		
		boolean refresh = dist <= 2 || (Utils.fastRandomFloat() > (det/2));
		if(refresh){
			time = System.nanoTime();
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
                    for(int y = 0; y < 256; y++){
                        int index = getIndex(x, y, z);
                        Material m = futureChunk.getType(x,y,z);
                        if(m == null) continue;
                        setResistanceUnsafe(index, (byte) getResistanceFor(m));
                        setSolidUnsafe(index, isSolid(m));
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

		int sum = Utils.sumOf(data, 4);
		time = System.nanoTime();
		for(int y = 0; y < 256; y++) {
            for (int x = xStep == 1 ? 0 : 15; xStep == 1 ? x < 16 : x >= 0; x += xStep) {
                for (int z = zStep == 1 ? 0 : 15; zStep == 1 ? z < 16 : z >= 0; z += zStep) {
                    processWaterMove(getIndex(x, y, z));
                }
            }
        }
		time = System.nanoTime() - time;
		delta[0] += Utils.sumOf(data,4) - sum;
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
                   Material m = futureChunk.getType(x,y,z);
                    BlockData d = futureChunk.getBlockData(x,y,z);
                    if(m == null || d == null) continue;
                    if(canReplace(m)){
                        int waterLevel = 0;
                        if(Utils.isWater(m)){
                            waterLevel = Utils.getWaterHeight(d);
                        }
                        int index = getIndex(x,y,z);
                        if(waterLevel != toWaterLevel(getLevel(index))){
                            updateVisually(x,y,z, dist <= 2);
                        }
                    } else if(m == Material.FARMLAND){
                        if(getLevel(getIndex(x,y,z)) > 1){
                            updateVisually(x,y,z, dist <= 2);
                        }
                    }
				}
			}
		}
	}
	
	private static int getResistanceFor(Material material){
		if(material == null) return 0;
		switch (material){
			case WATER: case AIR:
				return 0;
			case ACACIA_BUTTON: case SPRUCE_BUTTON: case JUNGLE_BUTTON: case OAK_BUTTON: case DARK_OAK_BUTTON: case BIRCH_BUTTON: case STONE_BUTTON:
			case LADDER: case RAIL: case TORCH: case IRON_BARS: case WALL_SIGN: case SIGN:
			case ACACIA_PRESSURE_PLATE: case SPRUCE_PRESSURE_PLATE: case JUNGLE_PRESSURE_PLATE: case OAK_PRESSURE_PLATE: case DARK_OAK_PRESSURE_PLATE:
			case BIRCH_PRESSURE_PLATE: case STONE_PRESSURE_PLATE: case LIGHT_WEIGHTED_PRESSURE_PLATE: case HEAVY_WEIGHTED_PRESSURE_PLATE:
				return 2;
			case ACACIA_TRAPDOOR: case SPRUCE_TRAPDOOR: case JUNGLE_TRAPDOOR: case BIRCH_TRAPDOOR: case OAK_TRAPDOOR: case DARK_OAK_TRAPDOOR:
			case ACACIA_DOOR: case SPRUCE_DOOR: case OAK_DOOR: case DARK_OAK_DOOR: case JUNGLE_DOOR: case BIRCH_DOOR:
			case OAK_FENCE_GATE: case ACACIA_FENCE_GATE: case  SPRUCE_FENCE_GATE: case DARK_OAK_FENCE_GATE: case JUNGLE_FENCE_GATE: case BIRCH_FENCE_GATE:
			case OAK_FENCE: case ACACIA_FENCE: case SPRUCE_FENCE: case DARK_OAK_FENCE: case JUNGLE_FENCE: case BIRCH_FENCE: case NETHER_BRICK_FENCE:
				return 20;
			case GRASS: case TALL_GRASS: case ROSE_BUSH: case DANDELION: case LILAC:
			case ACACIA_LEAVES: case SPRUCE_LEAVES: case JUNGLE_LEAVES: case BIRCH_LEAVES: case OAK_LEAVES: case DARK_OAK_LEAVES: case VINE: case LILY_PAD:
			case WHEAT: case MELON_STEM: case PUMPKIN_STEM:
				return 30;
			case ANVIL:
				return 100;
			case SAND: case GRAVEL: case SNOW: case SNOW_BLOCK:
				return 200;
			case DIRT: case GRASS_PATH: case GRASS_BLOCK: case FARMLAND: case CLAY:
				return 215;
			case COBBLESTONE: case COBBLESTONE_WALL:
				return 225;
			default:
				return 255;
		}
	}
	
	private static boolean isSolid(Material mat){
		if(mat == null) return true;
		switch (mat){
			case WATER: case AIR:
			case ACACIA_BUTTON: case SPRUCE_BUTTON: case JUNGLE_BUTTON: case OAK_BUTTON: case DARK_OAK_BUTTON: case BIRCH_BUTTON: case STONE_BUTTON:
			case LADDER: case RAIL: case TORCH: case IRON_BARS: case WALL_SIGN: case SIGN:
			case ACACIA_PRESSURE_PLATE: case SPRUCE_PRESSURE_PLATE: case JUNGLE_PRESSURE_PLATE: case OAK_PRESSURE_PLATE: case DARK_OAK_PRESSURE_PLATE:
			case BIRCH_PRESSURE_PLATE: case STONE_PRESSURE_PLATE: case LIGHT_WEIGHTED_PRESSURE_PLATE: case HEAVY_WEIGHTED_PRESSURE_PLATE:
			case GRASS: case TALL_GRASS: case ROSE_BUSH: case DANDELION: case VINE: case LILY_PAD:
			case WHEAT: case MELON_STEM: case PUMPKIN_STEM:
			case ACACIA_LEAVES: case SPRUCE_LEAVES: case JUNGLE_LEAVES: case BIRCH_LEAVES: case OAK_LEAVES: case DARK_OAK_LEAVES:
			case ANVIL:
			case ACACIA_TRAPDOOR: case SPRUCE_TRAPDOOR: case JUNGLE_TRAPDOOR: case BIRCH_TRAPDOOR: case OAK_TRAPDOOR: case DARK_OAK_TRAPDOOR:
			case ACACIA_DOOR: case SPRUCE_DOOR: case OAK_DOOR: case DARK_OAK_DOOR: case JUNGLE_DOOR: case BIRCH_DOOR:
			case OAK_FENCE_GATE: case ACACIA_FENCE_GATE: case  SPRUCE_FENCE_GATE: case DARK_OAK_FENCE_GATE: case JUNGLE_FENCE_GATE: case BIRCH_FENCE_GATE:
			case OAK_FENCE: case ACACIA_FENCE: case SPRUCE_FENCE: case DARK_OAK_FENCE: case JUNGLE_FENCE: case BIRCH_FENCE: case NETHER_BRICK_FENCE:
			case COBBLESTONE_WALL:
				return false;
			case SAND: case GRAVEL: case SNOW: case SNOW_BLOCK:
			case DIRT: case GRASS_PATH: case GRASS_BLOCK: case FARMLAND: case CLAY:
			case COBBLESTONE:
				return true;
		}
		
		return true;
	}
	
	private static boolean canReplace(Material mat){
		if(mat == Material.WATER) return true;
		if(mat == Material.AIR) return true;
		if(mat == Material.GRASS) return true;
		if(mat == Material.ROSE_BUSH) return true;
		if(mat == Material.DANDELION) return true;
		if(mat == Material.TALL_GRASS) return true;
		if(mat == Material.VINE) return true;
		if(mat == Material.TORCH) return true;
		if(mat == Material.SNOW) return true;
		if(mat == Material.SNOW_BLOCK) return true;
		return false;
	}
	
	private static boolean isDropable(Material mat){
		if(mat == Material.VINE) return true;
		if(mat == Material.TORCH) return true;
		if(mat == Material.GRASS) return true;
		if(mat == Material.ROSE_BUSH) return true;
		if(mat == Material.DANDELION) return true;
		if(mat == Material.TALL_GRASS) return true;
		return false;
	}
	
	private void updateVisually(int x, int y, int z, boolean progressive) {
		if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
		if(Utils.hasLag()) return;
		BukkitRunnable br = new VisualUpdateTask(this, x, y, z);
		br.runTaskLater(LivelyWorld.getInstance(), progressive? Utils.fastRandomInt(20) : 1);
	}

	private static int toWaterLevel(int level){
		return (level + 16) >> 5;
	}
	
	public void updateVisually(){
		if(!LivelyWorld.getInstance().isEnabled()) return;
		if(!isLoaded()) return;
		//needsUpdate = false;
		//BukkitRunnable br = new WaterChunkUpdateRunnable(this, UpdateType.LEVEL);
		//br.runTask(LivelyWorld.getInstance());
	}
	
	public void addWaterAt(int x, int y, int z, int amount) {
		if(!isLoaded()) return;
        checkRange(x,y,z);
		int index = getIndex(x,y,z);
		if(getMaxQuantity(index) - getLevel(index) >= amount){
			data[index] += amount;
		} else {
			int i = 0;
			while(amount > 0 && (y + i) < 256){
				index = getIndex(x,y+i,z);
				int added = Math.min(getMaxQuantity(index) - getLevel(index), amount);
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
        checkRange(x,y,z);
		int index = getIndex(x,y,z);
		return getLevel(index);
	}
	
	private int getResistance(int index){
		return Byte.toUnsignedInt(data[index + 1]);
	}
	
	public int getResistance(int x, int y, int z){
        checkRange(x,y,z);
		int index = getIndex(x,y,z);
		return getResistance(index);
	}
	
	private void setResistance(int index, byte value){
		data[index + 1] = value;
	}
	
	private int getMaxQuantity(int index){
		return 0xFF - Byte.toUnsignedInt(data[index + 1]);
	}
	
	public int getMaxQuantity(int x, int y, int z){
        checkRange(x,y,z);
		int index = getIndex(x,y,z);
		return getMaxQuantity(index);
	}
	
	private int getMaxQuantityRDM(int index){
		if(isSolid(index)){
			int result = getMaxQuantity(index) - Utils.superFastRandomInt();
			return result > 0 ? result : 0;
		} else {
			return getMaxQuantity(index);
		}
	}
	
	private boolean isSolid(int index){
		return ((data[index + 3] & 0x10) >> 4) == 1;
	}
	
	public boolean isSolid(int x, int y, int z){
        checkRange(x,y,z);
		int index = getIndex(x,y,z);
		return isSolid(index);
	}
	
	private void setSolid(int index, boolean value){
		byte old = data[index + 3];
		byte b = (byte) ((old & ~0x10) + (value ? 0x10 : 0x00));
		data[index + 3] = b;
	}

	private boolean needsUpdate(int index){
	    return ((data[index + 3] & 0b10000000)) == 0b10000000;
    }

    private void setNeedsUpdate(int index, boolean value){
        byte old = data[index + 3];
        byte b = (byte) ((old & ~0b10000000) + (value ? 0b10000000 : 0b00000000));
        data[index + 3] = b;
    }

	public void saturate() {
		if(!isLoaded()) load();
		for(int y = 0; y < 256; y ++){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					int index = getIndex(x,y,z);
					if(getMaxQuantity(index) > 1 && getMaxQuantity(index) < 254 && getResistance(index) != getResistanceFor(Material.OAK_LEAVES)){
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
			if(canReplace(b.getType())) {
                int waterLevel = toWaterLevel(level);
                if (waterLevel != Utils.getWaterHeight(b)) {
                    if (isDropable(b.getType())) {
                        if (waterLevel > 3) {
                            b.breakNaturally();
                            Utils.setWaterHeight(b, waterLevel, true, true);
                        }
                    } else if (waterLevel > 0) {
                        Utils.setWaterHeight(b, waterLevel, true, true);
                    } else if (Utils.isWater(b.getType())) {
                        Utils.setWaterHeight(b, 0, true, true);
                    }
                }
            } else if(b.getType() == Material.FARMLAND && level > 0){
				Farmland frmld = (Farmland) b.getBlockData();
			    frmld.setMoisture(7);
			    b.setBlockData(frmld, false);
			} else if(b.getRelative(BlockFace.DOWN).getType() == Material.AIR && b.getType() != Material.AIR && level > 0) {
				chunk.getWorld().spawnParticle(Particle.DRIP_WATER, b.getX() + Utils.fastRandomDouble(), b.getY() - 0.01, b.getZ() + Utils.fastRandomDouble(), 1);
			}
		}
		
	}

	private void checkRange(int x, int y, int z){
        if(x >= 16 || x < 0) throw new OutOfRangeException(x, 0, 15);
        if(z >= 16 || z < 0) throw new OutOfRangeException(z, 0, 15);
        if(y >= 256 || y < 0) throw new OutOfRangeException(y, 0, 255);
    }

	public void setLevel(int x, int y, int z, int level) {
		if(level < 0) level = 0;
		if(level > 0xFF) level = 0xFF;
		checkRange(x,y,z);
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

	private boolean needsUpdateUnsafe(int index){
	    byte b = Utils.unsafe.getByte(data, Utils.baseAddressBytes + index + 3);
	    return (b & 0b10000000) == 0b10000000;
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

	private void setNeedsUpdateUnsafe(int index, boolean value){
        byte old = Utils.unsafe.getByte(data, Utils.baseAddressBytes + index + 3);
        byte b = (byte) ((old & ~0b10000000) + (value ? 0b10000000 : 0b00000000));
        Utils.unsafe.putByte(data, Utils.baseAddressBytes + index + 3, b);
    }
	
	private int getMaxQuantityUnsafe(int index){
		return 0xFF - getResistanceUnsafe(index);
	}
	
	private int getMaxQuantityRDMUnsafe(int index){
		if(isSolidUnsafe(index)){
			int result = getMaxQuantityUnsafe(index) - Utils.superFastRandomInt();
			return result > 0 ? result : 0;
		} else {
			return getMaxQuantityUnsafe(index);
		}
	}
	*/
}
