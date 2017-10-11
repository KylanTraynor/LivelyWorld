package com.kylantraynor.livelyworld;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Utils {
	
	private static double sqrt2PI = Math.sqrt(2 * Math.PI);
	
	
	public static class Enclosed<T>{
		private T value = null;
		
		public T get(){
			return value;
		}
		
		public void set(T value){
			this.value = value;
		}
	}
	
	public static class Lock {
		boolean isLocked = false;
		Thread  lockedBy = null;
		int     lockedCount = 0;

		public synchronized void lock()
			throws InterruptedException{
			Thread callingThread = Thread.currentThread();
		    while(isLocked && lockedBy != callingThread){
		    	wait();
		    }
		    isLocked = true;
		    lockedCount++;
		    lockedBy = callingThread;
		}
		
		public synchronized void unlock(){
			if(Thread.currentThread() == this.lockedBy){
				lockedCount--;
				if(lockedCount == 0){
					isLocked = false;
					notify();
		    	}
			}
		}
	}
	
	public static class SizedList<T> extends ArrayList<T>{
		private int maxSize;

		public SizedList(int size){
			this.maxSize = size;
		}
		
		@Override
		public boolean add(T item){
			if(this.size() >= getMaxSize()){
				this.remove(0);
			}
			return super.add(item);
		}

		public int getMaxSize() {
			return maxSize;
		}

		public void setMaxSize(int size) {
			this.maxSize = size;
		}
	}
	
	public static byte[] toByteArray(int value){
		return new byte[]{(byte) (value), (byte)(value >>> 8), (byte)(value >>> 16), (byte)(value >>> 24)};
	}
	public static int toInt(byte b0, byte b1, byte b2, byte b3){
		return 0 + (Byte.toUnsignedInt(b0)) + (Byte.toUnsignedInt(b1) << 8) + (Byte.toUnsignedInt(b2) << 16) + (Byte.toUnsignedInt(b3) << 24);
	}
	public static int toInt(byte[] b){
		return 0 + (Byte.toUnsignedInt(b[0])) + (Byte.toUnsignedInt(b[1]) << 8) + (Byte.toUnsignedInt(b[2]) << 16) + (Byte.toUnsignedInt(b[3]) << 24);
	}
	
	public int addSnow(Block snowBlock, int amount){
		if(snowBlock.getType() == Material.SNOW){
			return 0;
		} else if(snowBlock.getType() == Material.SNOW_BLOCK){
			return 0;
		} else {
			return 0;
		}
	}
	
	public static String getBlockLocationString(Location loc){
		return "" + loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
	}
	
	public static String getBlockLocationStringNoWorld(Location loc){
		return "" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
	}
	
	public static double sigmoid(double x){
		return 1/(1 + Math.exp(-x));
	}
	
	public static double sigmoidDerivative(double sigmoid){
		return sigmoid * (1 - sigmoid);
	}
	
	public static double normalDistrubitionDensity(double x, double mean, double stdv){
		double distanceSquared = (x - mean) * (x - mean);
		double variance = stdv * stdv;
		return (1 / (sqrt2PI * stdv)) * Math.exp((- distanceSquared) / (2 * variance));
	}
	
	public static double simpleDistributionDensity(double x, double mean, double stdv){
		double distanceSquared = (x - mean) * (x - mean);
		double variance = stdv * stdv;
		return Math.exp(- distanceSquared / (2 * variance));
	}

	public static Location getBlockLocationFromString(String key) {
		String[] args = key.split("_");
		if(args.length == 4){
			try{
				return new Location(Bukkit.getWorld(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
			} catch(Exception ex){
				ex.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	public static int constrainTo(int value, int min, int max){
		return value > max ? max : (value < min ? min : value);
	}

	public static boolean isWater(Block block) {
		return block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER;
	}
	
	public static int getWaterHeight(Block b) {
		int result = 8 - b.getData();
		if(result <= 0) result = 8;
		return result;
	}
	
	public static void setClientWaterHeight(Block b, int height){
		Material m = b.getType();
		byte data = b.getData();
		if(height == 0){
			m = Material.AIR;
			data = 0;
		} else if(height == 8){
			m = Material.STATIONARY_WATER;
			data = 0;
		} else {
			m = Material.STATIONARY_WATER;
			data = (byte) (8 - height);
		}
		
		for(Player p : Bukkit.getOnlinePlayers()){
			if(p.getWorld() == b.getWorld()){
				if(p.getLocation().distanceSquared(b.getLocation()) < 10000){
					p.sendBlockChange(b.getLocation(), m, data);
				}
			}
		}
	}
	
	public static void setWaterHeight(Block b, int height, boolean canSource){
		if(height == 0){
			b.setType(Material.AIR, false);
			b.setData((byte)0, false);
		} else if(height == 8){
			b.setType(Material.STATIONARY_WATER, false);
			if(canSource){
				b.setData((byte)0, false);
			} else {
				b.setData((byte) 8, false);
			}
		} else {
			b.setType(Material.STATIONARY_WATER, false);
			b.setData((byte) (8 - height), false);
		}
	}
}