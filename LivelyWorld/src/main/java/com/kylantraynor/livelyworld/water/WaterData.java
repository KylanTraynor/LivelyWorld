package com.kylantraynor.livelyworld.water;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import com.kylantraynor.livelyworld.Utils;

public class WaterData {
	
	private WaterChunk chunk = null;
	private int x = 0;
	private int y = 0;
	private int z = 0;
	
	private static int moistureCode = 0;
	private static int inCurrentCode = 3;
	private static int outCurrentCode = 9;
	private static int inStrengthCode = 6;
	private static int outStrengthCode = 12;
	private static int saltCode = 15;
	
	public WaterData(WaterChunk chunk, int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
		this.chunk = chunk;
	}
	
	public WaterData(Chunk chunk, int x, int y, int z){
		this(WaterChunk.get(chunk.getWorld(), chunk.getX(), chunk.getZ()), x, y, z);
	}
	
	public WaterData(World world, int x, int y, int z){
		this(WaterChunk.get(world, x >> 4, z >> 4), x % 16, y, z % 16);
	}
	
	public WaterData(Location loc){
		this(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public Location getLocation(){
		return new Location(getWorld(), getX(), getY(), getZ());
	}
	
	public World getWorld(){
		return chunk.getWorld();
	}
	
	public int getChunkX(){ return x; }
	
	public int getChunkZ(){ return z; }
	
	public int getX(){ return (chunk.getX() << 4) + x; }
	
	public int getY(){ return y; }
	
	public int getZ(){ return (chunk.getZ() << 4) + z; }
	
	public int getData(){
		return chunk.getData(x, y, z);
	}
	
	public void setData(int value){
		chunk.setData(value, x, y, z);
	}
	
	public int getLevel(){
		return (getData() & (7 << moistureCode)) >> moistureCode;
	}
	
	public void setLevel(int value){
		setData((getData() & (~(7 << moistureCode))) + (Utils.constrainTo(value, 0, 7) << moistureCode));
	}
	
	public int getInCurrentDirection(){
		return (getData() & (7 << inCurrentCode)) >> inCurrentCode;
	}
	
	public void setInCurrentDirection(int value){
		setData((getData() & (~(7 << inCurrentCode))) + (Utils.constrainTo(value, 0, 7) << inCurrentCode));
	}
	
	public int getOutCurrentDirection(){
		return (getData() & (7 << outCurrentCode)) >> outCurrentCode;
	}
	
	public void setOutCurrentDirection(int value){
		setData((getData() & (~(7 << outCurrentCode))) + (Utils.constrainTo(value, 0, 7) << outCurrentCode));
	}
	
	public int getInCurrentStrength(){
		return (getData() & (7 << inStrengthCode)) >> inStrengthCode;
	}
	
	public void setInCurrentStrength(int value){
		setData((getData() & (~(7 << inStrengthCode))) + (Utils.constrainTo(value, 0, 7) << inStrengthCode));
	}
	
	public int getOutCurrentStrength(){
		return (getData() & (7 << outStrengthCode)) >> outStrengthCode;
	}
	
	public void setOutCurrentStrength(int value){
		setData((getData() & (~(7 << outStrengthCode))) + (Utils.constrainTo(value, 0, 7) << outStrengthCode));
	}
	
	public int getSalt(){
		return (getData() & (7 << saltCode)) >> saltCode;
	}
	
	public void setSalt(int value){
		setData((getData() & (~(7 << saltCode))) + (Utils.constrainTo(value, 0, 7) << saltCode));
	}
	
	public boolean isSalted(){
		return getSalt() > 0;
	}
}
