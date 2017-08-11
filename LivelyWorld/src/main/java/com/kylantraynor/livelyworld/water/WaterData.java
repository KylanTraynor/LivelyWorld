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
	private byte moisture = 0;
	private float currentDirection = 0;
	private byte currentStrength = 0;
	
	public WaterData(Location loc, byte moisture, float currentDirection, byte currentStrength){
		this.loc = loc;
		this.moisture = moisture;
		this.currentDirection = currentDirection;
		this.currentStrength = currentStrength;
	}
	
	public Location getLocation(){
		return loc;
	}
	
	public byte getMoisture(){
		return moisture;
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
				"(id,moisture,currentDirection,currentStrength,x,y,z) " +
				"VALUES("+
				Utils.getBlockLocationStringNoWorld(loc)+","+
				moisture+","+
				currentDirection+","+
				currentStrength+","+
				loc.getBlockX()+","+
				loc.getBlockY()+","+
				loc.getBlockZ()+");";
	}
}
