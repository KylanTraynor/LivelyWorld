package com.kylantraynor.livelyworld.water;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.Utils;

/**
 * Must be used run on the main thread only.
 * @author Baptiste
 *
 */
public class WaterChunkUpdateRunnable extends BukkitRunnable {

	private final WaterChunk chunk;
	
	public WaterChunkUpdateRunnable(WaterChunk chunk){
		this.chunk = chunk;
	}
	
	@Override
	public void run() {
		int level = 0;
		Block currentBlock = null;
		if(!chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ())) return;
		for(int y = 0; y < 256; y++){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					level = WaterData.getWaterLevelAt(chunk, x, y, z);
					currentBlock = chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ()).getBlock(x, y, z);
					if(canReplace(currentBlock.getType())){
						if(WaterData.toWaterLevel(level) != Utils.getWaterHeight(currentBlock)){
							if(level > 0 && isDropable(currentBlock.getType())){
								ItemStack is = new ItemStack(currentBlock.getType());
								currentBlock.getWorld().dropItemNaturally(currentBlock.getLocation(), is);
							}
							Utils.setWaterHeight(currentBlock, WaterData.toWaterLevel(level), true);
						}
					}
				}
			}
		}
	}
	
	public boolean canReplace(Material mat){
		if(mat == Material.WATER) return true;
		if(mat == Material.STATIONARY_WATER) return true;
		if(mat == Material.AIR) return true;
		if(mat == Material.TORCH) return true;
		return false;
	}
	
	public boolean isDropable(Material mat){
		if(mat == Material.TORCH) return true;
		return false;
	}
}
