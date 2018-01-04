package com.kylantraynor.livelyworld.water;

import org.bukkit.scheduler.BukkitRunnable;

public class WaterResistanceUpdate extends BukkitRunnable {
	
	final int newResistance;
	final WaterData waterData;
	
	public WaterResistanceUpdate(WaterData wd, int res){
		waterData = wd;
		newResistance = res;
	}
	
	@Override
	public void run() {
		waterData.setResistance(newResistance);
	}

}
