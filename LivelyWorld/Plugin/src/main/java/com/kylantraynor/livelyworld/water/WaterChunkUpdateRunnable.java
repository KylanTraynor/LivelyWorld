package com.kylantraynor.livelyworld.water;

import java.util.Collection;

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

	public enum UpdateType{
		LEVEL,
		RESISTANCE;
	}
	
	private final WaterChunk chunk;
	private final UpdateType updateType;
	
	public WaterChunkUpdateRunnable(WaterChunk chunk, UpdateType type){
		this.chunk = chunk;
		this.updateType = type;
	}
	
	@Override
	public void run() {
		int level = 0;
		Block currentBlock = null;
		if(updateType == UpdateType.LEVEL){
			if(!chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ())) return;
			if(!Utils.hasPlayerWithinChunk(chunk.getX(), chunk.getZ(), 10)) return;
			if(!Utils.hasPlayerWithinChunk(chunk.getX(), chunk.getZ(), 2) && Utils.fastRandomDouble() > 0.01) return;
			for(int y = 0; y < 256; y++){
				for(int x = 0; x < 16; x++){
					for(int z = 0; z < 16; z++){
						level = WaterData.getWaterLevelAt(chunk, x, y, z);
						currentBlock = chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ()).getBlock(x, y, z);
						if(canReplace(currentBlock.getType())){
							if(WaterData.toWaterLevel(level) != Utils.getWaterHeight(currentBlock)){
								if(level > 0 && isDropable(currentBlock.getType())){
									currentBlock.breakNaturally();
								}
								Utils.setWaterHeight(currentBlock, WaterData.toWaterLevel(level), true);
							}
						}
					}
				}
			}
		} else if(updateType == UpdateType.RESISTANCE){
			if(!chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ())) return;
			for(int y = 0; y < 256; y++){
				for(int x = 0; x < 16; x++){
					for(int z = 0; z < 16; z++){
						currentBlock = chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ()).getBlock(x, y, z);
						WaterData.setResistanceAt(chunk, x, y, z, WaterData.getResistanceFor(currentBlock.getType()));
					}
				}
			}
		}
	}
	
	public boolean canReplace(Material mat){
		if(mat == Material.WATER) return true;
		if(mat == Material.STATIONARY_WATER) return true;
		if(mat == Material.AIR) return true;
		if(mat == Material.LONG_GRASS) return true;
		if(mat == Material.VINE) return true;
		if(mat == Material.TORCH) return true;
		if(mat == Material.SNOW) return true;
		if(mat == Material.SNOW_BLOCK) return true;
		return false;
	}
	
	public boolean isDropable(Material mat){
		if(mat == Material.VINE) return true;
		if(mat == Material.TORCH) return true;
		if(mat == Material.LONG_GRASS) return true;
		return false;
	}
}
