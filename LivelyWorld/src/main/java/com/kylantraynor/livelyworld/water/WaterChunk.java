package com.kylantraynor.livelyworld.water;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.bukkit.World;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;

public class WaterChunk {
	static List<WaterChunk> loadedChunks = Collections.synchronizedList(new ArrayList<WaterChunk>()); 
	byte[] data = new byte[16 * 16 * 256 * 4];
	private Utils.Lock dataLock = new Utils.Lock();
	private Utils.Lock fileLock = new Utils.Lock();
	
	boolean isLoaded = false;
	final int x;
	final int z;
	final World world;
	
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
				if(isLoaded) return;
				loadFromFile();
				isLoaded = true;
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
				if(!isLoaded) return;
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
				if(!isLoaded) return;
				save();
				loadedChunks.remove(this);
				isLoaded = false;
			} finally {
				fileLock.unlock();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public int getX(){ return x; }
	
	public int getZ(){ return z; }
	
	public WaterData getAt(int x, int y, int z){
		if(!isLoaded) load();
		return new WaterData(this, x, y, z);
	}
	
	void setData(int data, int x, int y, int z){
		if(!isLoaded) load();
		byte[] b = Utils.toByteArray(data);
		int index = getIndex(x,y,z);
		try {
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
		}
	}
	
	private int getIndex(int x, int y, int z){
		return (x * 16 * 256 * 4) + (z * 256 * 4) + y * 4;
	}
	
	int getData(int x, int y, int z) {
		if(!isLoaded) load();
		int index = getIndex(x, y, z);
		try {
			dataLock.lock();
			try{
				Utils.toInt(data[index], data[index + 1], data[index + 2], data[index + 3]);
			} finally {
				dataLock.unlock();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public World getWorld() {
		return world;
	}
	
	public static WaterChunk get(World world, int x, int z){
		synchronized(loadedChunks){
			for(WaterChunk c : loadedChunks){
				if(c.getWorld() == world && c.getX() == x && c.getZ() == z){
					return c;
				}
			}
		}
		return new WaterChunk(world, x, z);
	}
	
	public static void unloadAll(){
		synchronized(loadedChunks){
			for(WaterChunk c : loadedChunks.toArray(new WaterChunk[loadedChunks.size()])){
				c.unload();
			}
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
		File f = new File(dir2, "" + getX() + "_" + getZ() + ".cbd");
		if(!f.exists()){
			try {
				LivelyWorld.getInstance().getLogger().info("Creating file " + f.getName() +".");
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return f;
	}
	
	public void saveToFile(){
		try {
			fileLock.lock();
			try{
				OutputStream s = null;
				try {
					s = new DeflaterOutputStream(new FileOutputStream(getFile()));
					try {
						dataLock.lock();
						try{
							s.write(data);
						} finally {
							dataLock.unlock();
						}
						s.flush();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e){
						e.printStackTrace();
					} finally {
						try {
							s.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} finally{
				fileLock.unlock();
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	public void loadFromFile(){
		try {
			fileLock.lock();
			try{
				try {
					if(getFile().length() <= 0) return;
					InputStream s = new InflaterInputStream(new FileInputStream(getFile()));
					int length =16 *16*256*4;
					byte[] bucket = new byte[length];
					ByteArrayOutputStream o = null;
					try{
						o = new ByteArrayOutputStream(bucket.length);
						int bytesRead = 0;
						while(bytesRead >= 0){
							bytesRead = s.read(bucket);
							if(bytesRead > 0){
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
						try{
							dataLock.lock();
							try{
								data = o.toByteArray();
							} finally {
								dataLock.unlock();
							}
						} catch (InterruptedException e){
							e.printStackTrace();
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
		}
	}
}
