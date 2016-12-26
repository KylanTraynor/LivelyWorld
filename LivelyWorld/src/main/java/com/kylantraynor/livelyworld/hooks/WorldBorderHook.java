package com.kylantraynor.livelyworld.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

public class WorldBorderHook {
	
	public BorderData getWorldBorder(World w){
		return Config.Border(w.getName());
	}
	
	public boolean isWorldCircular(World w){
		BorderData border = getWorldBorder(w);
		if(border != null){
			return border.getShape();
		} else {
			return false;
		}
	}
	
	public int getWorldRadiusX(World w){
		BorderData border = getWorldBorder(w);
		if(border != null){
			return border.getRadiusX();
		} else {
			return 0;
		}
	}
	
	public int getWorldRadiusZ(World w){
		BorderData border = getWorldBorder(w);
		if(border != null){
			return border.getRadiusZ();
		} else {
			return 0;
		}
	}
	
	public Location getWorldCenter(World w){
		BorderData border = getWorldBorder(w);
		if(border != null){
			return new Location(w, border.getX(), 0, border.getZ());
		} else {
			return new Location(w, 0, 0, 0);
		}
	}
}
