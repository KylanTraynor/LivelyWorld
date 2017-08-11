package com.kylantraynor.livelyworld.water;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.World;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.database.Database;

public class WaterChunk {
	WaterData[][][] data = new WaterData[16][256][16];
	
	boolean isLoaded = false;
	int x;
	int z;
	World world;
	
	public WaterChunk(World w, int x, int z){
		this.world = w;
		this.x = x;
		this.z = z;
	}
	
	public void load(){
		if(isLoaded) return;
		Database db = LivelyWorld.getInstance().getDatabase();
		if(db != null){
			db.loadWaterChunk(this);
		}
	}
	
	public void save(){
		if(!isLoaded) return;
		Database db = LivelyWorld.getInstance().getDatabase();
		if(db != null){
			db.saveWaterChunk(this);
		}
	}
	
	public int getX(){
		return x;
	}
	
	public int getZ(){
		return z;
	}
	
	public void setAt(WaterData d){
		if(!isLoaded) load();
		this.data[d.getLocation().getBlockX() % 16][d.getLocation().getBlockY() % 255][d.getLocation().getBlockZ() % 16] = d;
	}
	
	public WaterData getAt(int x, int y, int z){
		if(!isLoaded) load();
		return this.data[x][y][z];
	}
	
	public String getSQLSelectStatement(String table){
		return "SELECT * FROM " + table +
				"WHERE (x BETWEEN " + (getX() << 4) + " AND " + (((getZ() + 1) << 4)-1) + ") AND "+
				"(z BETWEEN " + (getZ() << 4) + " AND " + (((getZ() + 1) << 4) - 1) + ");";
	}
	
	public World getWorld() {
		return world;
	}

	public Iterator<String> getSQLReplaceStatements(String table) {
		List<String> list = new ArrayList<String>();
		for(int y = 0; y < 256; y++){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					list.add(data[x][y][z].getSQLReplaceString(table));
				}
			}
		}
		return list.iterator();
	}
}
