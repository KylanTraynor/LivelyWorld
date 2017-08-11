package com.kylantraynor.livelyworld;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class Utils {
	
	private static double sqrt2PI = Math.sqrt(2 * Math.PI);
	
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

	public static boolean isWater(Block block) {
		return block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER;
	}
}