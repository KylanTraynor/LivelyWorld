package com.kylantraynor.livelyworld.water;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class TideTask extends BukkitRunnable{
	
	private TidesModule module;
	private World world;
	private int x;
	private int z;
	
	public TideTask(TidesModule module, World world, int x, int z){
		this.module = module;
		this.world = world;
		this.x = x;
		this.z = z;
	}
	
	@Override
	public void run() {
		module.updateOceanLevel(new Location(world, x, 60, z));
	}

	public TidesModule getModule(){
		return module;
	}
	
	public World getWorld(){
		return world;
	}
	
}
