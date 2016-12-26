package com.kylantraynor.livelyworld.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class HookManager {
	
	private static WorldBorderHook worldBorder = null;
	
	public static boolean hasWorldBorder(){
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldBorder");
		if(plugin == null){
			return false;
		} else {
			return plugin.isEnabled();
		}
	}
	
	public static WorldBorderHook getWorldBorder(){
		if(hasWorldBorder()){
			if(worldBorder == null) worldBorder = new WorldBorderHook();
			return worldBorder;
		}
		return null;
	}
}
