package com.kylantraynor.livelyworld;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import com.kylantraynor.livelyworld.climate.ClimateUtils;

public class Utils {
	
	private static double sqrt2PI = Math.sqrt(2 * Math.PI);
	private static long rand = System.currentTimeMillis();
	private static int[] xor128 = {123456789,362436069,521288629,88675123};
	private static XoRoShiRo128PlusRandom rdm = new XoRoShiRo128PlusRandom();
	private static int t;
	private static long tickLength;
	private static int randByte = rdm.nextInt();
	
	@SafeVarargs
	public static <T> T[] toArray(T... t){
		return t;
	}
	
	public static class ChunkCoordinates{
		final private String world;
		final private int x;
		final private int z;
		
		public ChunkCoordinates(World world, int x, int z){
			this.world = world.getUID().toString();
			this.x = x;
			this.z = z;
		}
		
		public int getX(){
			return x;
		}
		
		public int getZ(){
			return z;
		}
		
		@Override
		public int hashCode(){
			int hash = 11;
			hash*= 17 * world.hashCode();
			hash*= 13 * Integer.hashCode(x);
			hash*= 7 * Integer.hashCode(z);
			return hash;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof ChunkCoordinates){
				ChunkCoordinates cc = (ChunkCoordinates) o;
				if(cc.getX() == this.getX() && cc.getZ() == this.getZ()){
					return true;
				}
			}
			return false;
		}
		
		public static ChunkCoordinates fromBlockCoordinates(Block b){
			return new ChunkCoordinates(b.getWorld(), b.getX() >> 4, b.getZ() >> 4);
		}
	}
	
	public static class SmallChunkData{
		private String worldName = "world";
		private int x = 0;
		private int z = 0;
		
		private Biome[][] biomes = new Biome[16][16];
		//private MaterialData[][][] data = new MaterialData[256][16][16];
		
		public SmallChunkData(Chunk c){
			x = c.getX();
			z = c.getZ();
			worldName = c.getWorld().getName();
			for(int x = 0; x < 16; x ++){
				for(int z = 0; z < 16; z ++){
					biomes[x][z] = c.getBlock(x, 0, z).getBiome();
				}
			}
			/*for(int y = 0; y < 256; y ++){
				for(int x = 0; x < 16; x ++){
					for(int z = 0; z < 16; z ++){
						data[y][x][z] = new MaterialData(c.getBlock(x, y, z).getType(), c.getBlock(x, y, z).getData());
					}
				}
			}*/
		}
		
		public int getX(){
			return x;
		}
		
		public int getZ(){
			return z;
		}
		
		public String getWorldName(){
			return worldName;
		}
		
		public Biome getBiome(int x, int z){
			return biomes[x][z];
		}
		
		/*public Material getMaterial(int x, int y, int z){
			return data[y][x][z].getItemType();
		}
		
		public MaterialData getData(int x, int y, int z){
			return data[y][x][z];
		}

		public void setData(int x2, int y, int z2, MaterialData materialData) {
			data[y][x2][z2] = materialData;
		}*/
	}
	
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
	
	public static class PrioritizedLock extends Lock{
		long priority = 0;
		boolean priorityWaiting = false;
		public PrioritizedLock(long l){
			this.priority = l;
		}
		
		public synchronized void lock()
			throws InterruptedException{
			Thread callingThread = Thread.currentThread();
		    while((isLocked && lockedBy != callingThread) && (priorityWaiting && callingThread.getId() != priority)){
		    	if(callingThread.getId() == priority){
		    		priorityWaiting = true;
		    	}
		    	wait();
		    }
		    isLocked = true;
		    priorityWaiting = false;
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
		return isWater(block.getType());
	}
	
	public static boolean isWater(Material mat){
		return mat == Material.WATER || mat == Material.STATIONARY_WATER;
	}
	
	public static int getWaterHeight(Block b) {
		if(!isWater(b)) return 0; 
		return getWaterHeight(b.getData());
	}
	
	public static int getWaterHeight(Material type, byte data){
		if(!isWater(type)) return 0;
		return getWaterHeight(data);
	}
	
	public static int getWaterHeight(byte data){
		int result = 8 - data;
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
		setWaterHeight(b, height, canSource, true);
	}
	
	public static void setWaterHeight(Block b, int height, boolean canSource, boolean updateLight){
		if(height == 0){
			b.setType(Material.AIR, updateLight);
			b.setData((byte)0, updateLight);
		} else if(height == 8){
			b.setType(Material.STATIONARY_WATER, updateLight);
			if(canSource){
				b.setData((byte)0, updateLight);
			} else {
				b.setData((byte) 8, updateLight);
			}
		} else {
			b.setType(Material.STATIONARY_WATER, updateLight);
			b.setData((byte) (8 - height), updateLight);
		}
	}
	
	public static boolean isOcean(Biome biome) {
		if(biome == Biome.OCEAN) return true;
		if(biome == Biome.DEEP_OCEAN) return true;
		if(biome == Biome.FROZEN_OCEAN) return true;
		return false;
	}
	
	public static long randomLong(){
		return rdm.nextLong();
		/*rand ^= (rand << 21);
		rand ^= (rand >>> 35);
		rand ^= (rand << 4);
		return rand;*/
	}
	
	public static int fastRandomInt(){
		return rdm.nextInt();
		/*t = xor128[0] ^ (xor128[0] << 11);
	    xor128[0] = xor128[1]; xor128[1] = xor128[2]; xor128[2] = xor128[3];
	    return xor128[3] = xor128[3] ^ (xor128[3] >>> 19) ^ t ^ (t >>> 8);*/
	}
	
	public static int fastRandomInt(int max){
		return rdm.nextInt(max);
		//return Math.floorMod(fastRandomInt(), max);
	}
	
	/**
	 * Fast modulus for powers of two.
	 * @param n
	 * @param power
	 * @return
	 */
	static public int floorMod2(final int n, final int power){
		final int d = n >> power;
		return n - (d << power);
	}
	
	/**
	 * Returns a double between [0 and 1]
	 * @return
	 */
	public static double fastRandomDouble(){
		return rdm.nextDoubleFast();
		//return ((double)(fastRandomInt()) - ((double)Integer.MIN_VALUE)) / (-(Integer.MIN_VALUE * 2.0));
	}
	
	/**
	 * Returns a random int between [0 and 255]
	 * @return
	 */
	public static int superFastRandomInt(){
		int x = (randByte << 1);
		int y = (randByte >>> 1);
		return (randByte = x ^ ~y) & 0xFF;
	}
	
	public static void superFastRandomIntReset(){
		randByte = fastRandomInt();
	}
	
	public static boolean hasPlayerWithinChunk(int x, int z, int range) {
		int px = 0;
		int pz = 0;
		for(Player p : Bukkit.getOnlinePlayers()){
			px = p.getLocation().getChunk().getX();
			pz = p.getLocation().getChunk().getZ();
			if(px <= x + range && px >= x - range){
				if(pz <= z + range && pz >= z - range){
					return true;
				}
			}
		}
		return false;
	}
	
	public static String getLoreInfo(ItemStack is, String info){
		ItemMeta m = is.getItemMeta();
		if(m == null) return null;
		List<String> lore = m.getLore();
		if(lore == null) return null;
		for(String s : lore){
			if(s.startsWith(info + ": ")){
				return s.substring(info.length() + 2);
			}
		}
		return null;
	}
	
	public static int keepBetween(int min, int value, int max){
		if(value < min) return min;
		if(value > max) return max;
		return value;
	}
	
	public static void setLoreInfo(ItemStack is, String info, String value){
		ItemMeta m = is.getItemMeta();
		if(m == null) return;
		List<String> lore = m.getLore();
		if(lore != null){
			boolean updated = false;
			for(int i = 0; i < lore.size(); i++){
				if(lore.get(i).startsWith(info + ": ")){
					lore.set(i, info + ": " + value);
					updated = true;
				}
			}
			if(!updated) lore.add(info+": " + value);
		} else {
			lore = new ArrayList<String>();
			lore.add(info+": " + value);
		}
		m.setLore(lore);
		is.setItemMeta(m);
		return;
	}
	public static void setTickLength(long l) {
		tickLength = l;
	}
	
	public static int getTickLength(){
		return (int) tickLength;
	}
	
	public static boolean hasLag(){
		return tickLength > 52;
	}
	
	public static boolean hasHighLag(){
		return tickLength > 55;
	}
	
	public static int toUnsignedInt(byte b){
		return ((int)b) & 0xFF;
	}
	
	public static Block getHighestSnowBlockAround(Block b, int range) {
		Block result = b;
		for(int x = b.getX() - range; x <= b.getX() + range; x++){
			for(int z = b.getZ() - range; z <= b.getZ() + range; z++){
				Block block = b.getWorld().getBlockAt(x, b.getY(), z);
				switch(block.getType()){
				case SNOW_BLOCK:
				case SNOW:
					if(block.getType() == Material.SNOW_BLOCK){
						while(ClimateUtils.isSnow(block.getRelative(BlockFace.UP))){
							block = block.getRelative(BlockFace.UP);
						}
					}
					if(ClimateUtils.getSnowLayers(block) > ClimateUtils.getSnowLayers(result) || block.getY() > result.getY()){
						result = block;
					}
					break;
				default:
					
				}
			}
		}
		return result;
	}
	
	public static void spawnLightning(Block b) {
		Location loc = b.getLocation();
		b.getLocation().getWorld().spigot().strikeLightning(loc, true);
		b.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_IMPACT, 20, 1);
		loc.setY(255);
		b.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_THUNDER, 300, 1);
	}
}