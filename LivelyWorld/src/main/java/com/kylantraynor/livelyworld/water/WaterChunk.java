package com.kylantraynor.livelyworld.water;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.database.Database;

public class WaterChunk {
	WaterData[][][] data = new WaterData[16][256][16];
	
	int x;
	int z;

	public WaterChunk(int x, int z){
		this.x = x;
		this.z = z;
	}
	
	public void load(){
		Database db = LivelyWorld.getInstance().getDatabase();
		if(db != null){
			db.loadWaterChunk(this);
		}
	}
	
	public int getX(){
		return x;
	}
	public int getZ(){
		return z;
	}
	
	public String getSQLSelectStatement(String table){
		return "SELECT * FROM " + table +
				"WHERE (x BETWEEN " + (getX() << 4) + " AND " + (((getZ() + 1) << 4)-1) + ") AND "+
				"(z BETWEEN " + (getZ() << 4) + " AND " + (((getZ() + 1) << 4) - 1) + ");";
	}
}
