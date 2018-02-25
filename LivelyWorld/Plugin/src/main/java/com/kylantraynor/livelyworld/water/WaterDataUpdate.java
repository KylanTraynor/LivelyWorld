package com.kylantraynor.livelyworld.water;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

public class WaterDataUpdate extends BukkitRunnable{

	final World world;
	final Material mat;
	final int chunkX;
	final int chunkZ;
	final int x;
	final int y;
	final int z;
	final int level;
	public WaterDataUpdate(Block b, int newLevel){
		world = b.getWorld();
		mat = b.getType();
		chunkX = b.getChunk().getX();
		chunkZ = b.getChunk().getZ();
		x = Math.floorMod(b.getX(), 16);
		y = b.getY();
		z = Math.floorMod(b.getZ(), 16);
		level = Math.max(Math.min(newLevel, (int) WaterData.maxLevel),-1);
	}
	
	public WaterDataUpdate(Block b){
		this(b, -1);
	}
	
	@Override
	public void run() {
		/*WaterChunk c = WaterChunk.get(world, chunkX, chunkZ);
		WaterData wd = c.getAt(x, y, z);
		if(wd.getResistance() != WaterChunk.getResistanceFor(mat)){
			wd.resistance = (byte) (WaterChunk.getResistanceFor(mat));
		}
		if(level > 0 && level != wd.getLevel()){
			wd.level = (byte) level;
		}
		
		wd.sendChangedEvent();
		wd.getRelative(BlockFace.DOWN).sendChangedEvent();
		wd.getRelative(BlockFace.UP).sendChangedEvent();
		wd.getRelative(BlockFace.NORTH).sendChangedEvent();
		wd.getRelative(BlockFace.SOUTH).sendChangedEvent();
		wd.getRelative(BlockFace.EAST).sendChangedEvent();
		wd.getRelative(BlockFace.WEST).sendChangedEvent();*/
	}
}
