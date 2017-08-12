package com.kylantraynor.livelyworld.water;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.database.Database;

public class WaterData {
	private static LoadingCache<String, WaterData> loadedData =
			CacheBuilder.newBuilder()
				.maximumSize(1000)
				.expireAfterAccess(5, TimeUnit.MINUTES)
				.build(new CacheLoader<String, WaterData>(){
					public WaterData load(String key){
						Database db = LivelyWorld.getInstance().getDatabase();
						if(db != null){
							Location loc = Utils.getBlockLocationFromString(key);
							if(loc == null) return null;
							return db.getWaterDataAt(loc);
						}
						return null;
					}
				});
	private Location loc = null;
	private int data = 0;
	private byte moisture = 0;
	private byte currentDirection = 0;
	private byte currentStrength = 0;
	
	private static int moistureCode = 0;
	private static int inCurrentCode = 3;
	private static int outCurrentCode = 9;
	private static int inStrengthCode = 6;
	private static int outStrengthCode = 12;
	
	public WaterData(Location loc, int moisture, int inCurrent, int outCurrent, int inStrength, int outStrength){
		this.loc = loc;
		moisture = Utils.constrainTo(moisture, 0, 7);
		inCurrent = Utils.constrainTo(inCurrent, 0, 7);
		outCurrent = Utils.constrainTo(outCurrent,  0, 7);
		inStrength = Utils.constrainTo(inStrength, 0, 7);
		outStrength = Utils.constrainTo(outStrength, 0, 7);
		data = 0;
		data += moisture;
		data += (inCurrent << inCurrentCode);
		data += (outCurrent << outCurrentCode);
		data += (inStrength << inStrengthCode);
		data += (outStrength << outStrengthCode);
	}
	
	public WaterData(Location location, int data) {
		this.loc = location;
		this.data = data;
	}

	public Location getLocation(){
		return loc;
	}
	
	public int getMoisture(){
		return (data & (7 << moistureCode)) >> moistureCode;
	}
	
	public void setMoisture(int value){
		data = (data & (~(7 << moistureCode))) + (Utils.constrainTo(value, 0, 7) << moistureCode);
	}
	
	public int getInCurrentDirection(){
		return (data & (7 << inCurrentCode)) >> inCurrentCode;
	}
	
	public void setInCurrentDirection(int value){
		data = (data & (~(7 << inCurrentCode))) + (Utils.constrainTo(value, 0, 7) << inCurrentCode);
	}
	
	public int getOutCurrentDirection(){
		return (data & (7 << outCurrentCode)) >> outCurrentCode;
	}
	
	public void setOutCurrentDirection(int value){
		data = (data & (~(7 << outCurrentCode))) + (Utils.constrainTo(value, 0, 7) << outCurrentCode);
	}
	
	public int getInCurrentStrength(){
		return (data & (7 << inStrengthCode)) >> inStrengthCode;
	}
	
	public void setInCurrentStrength(int value){
		data = (data & (~(7 << inStrengthCode))) + (Utils.constrainTo(value, 0, 7) << inStrengthCode);
	}
	
	public int getOutCurrentStrength(){
		return (data & (7 << outStrengthCode)) >> outStrengthCode;
	}
	
	public void setOutCurrentStrength(int value){
		data = (data & (~(7 << outStrengthCode))) + (Utils.constrainTo(value, 0, 7) << outStrengthCode);
	}
	
	/**
	 * Gets the water data at this location.
	 * Try to keep this async whenever possible since it might try to load from database.
	 * @param loc
	 * @return
	 */
	public static WaterData getAt(Location loc){
		WaterData d = null;
		try {
			d = loadedData.get(Utils.getBlockLocationString(loc));
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return d;
	}
	
	/**
	 * Gets the water data at this location, but only if it's already been loaded.
	 * @param loc
	 * @return
	 */
	public static WaterData getLoadedWaterDataAt(Location loc){
		return loadedData.getIfPresent(Utils.getBlockLocationString(loc));
	}
	
	public String getSQLReplaceString(String table){
		return "REPLACE INTO " + table + " " +
				"(id,data,x,y,z) " +
				"VALUES("+
				Utils.getBlockLocationStringNoWorld(loc)+","+
				data+","+
				loc.getBlockX()+","+
				loc.getBlockY()+","+
				loc.getBlockZ()+");";
	}
}
