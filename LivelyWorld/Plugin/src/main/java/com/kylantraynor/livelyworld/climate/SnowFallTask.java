package com.kylantraynor.livelyworld.climate;

import com.kylantraynor.livelyworld.waterV2.BlockLocation;
import com.kylantraynor.livelyworld.waterV2.WaterChunk;
import com.kylantraynor.livelyworld.waterV2.WaterWorld;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;

public class SnowFallTask extends BukkitRunnable {

	private ClimateModule module;
	private ClimateCell cell;
	private int x;
	private int y;
	private int z;

	public SnowFallTask(ClimateModule module, ClimateCell cell, int x, int y, int z) {
		this.module = module;
		this.cell = cell;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public void run() {

		processSnowFall();

	}

	private void processSnowFall() {
		Block b = module.getPlugin().getLowestNear(cell.getWorld(), x, y, z).getBlock();
		if (getBlock().equals(b) || Math.random() < 0.01) {
			module.updateBiome(b);
			// Stop if temperature is above 1
			//ClimateCell cell = ClimateUtils.getClimateCellAt(b.getLocation(), this.cell);
			if(ClimateUtils.getAltitudeWeightedTemperature(b.getLocation()).isCelsiusAbove(3)){
				WaterWorld w = LivelyWorld.getInstance().getWaterModule().getWorld(b.getWorld());
				if(w == null) return;
				WaterChunk wc = w.getChunk(b.getChunk().getX(), b.getChunk().getZ());
				if(wc == null) return;
				wc.addWaterIn(new BlockLocation(Utils.floorMod2(b.getX(), 4), b.getY(), Utils.floorMod2(b.getZ(), 4)), 4);
				return;
			}
			Block below = b.getRelative(BlockFace.DOWN);
			if (below.getType() == Material.SNOW) {
				ClimateUtils.setSnowLayers(below, ClimateUtils.getSnowLayers(below) + 1);
				/*if (snow.getData() < 6) {
					snow.setData((byte) (snow.getData() + 1));
				} else {
					snow.setType(Material.SNOW_BLOCK);
				}*/
			} else if (below.getType().isSolid()) {
				/*if(below.getType() == Material.ICE || below.getType() == Material.PACKED_ICE || below.getType() == Material.FROSTED_ICE){
					b.setType(Material.SNOW_BLOCK);
				} else {*/
					b.setType(Material.SNOW);
				//}
			} else if (Utils.isWater(below)){
                WaterWorld w = LivelyWorld.getInstance().getWaterModule().getWorld(b.getWorld());
                if(w == null) return;
                WaterChunk wc = w.getChunk(b.getChunk().getX(), b.getChunk().getZ());
                if(wc == null) return;
                wc.addWaterIn(new BlockLocation(Utils.floorMod2(b.getX(), 4), b.getY(), Utils.floorMod2(b.getZ(), 4)), 4);
                return;
				/*if(below.getData() == 0){
					if(below.getRelative(BlockFace.EAST).getType().isSolid()
							|| below.getRelative(BlockFace.NORTH).getType().isSolid()
							|| below.getRelative(BlockFace.SOUTH).getType().isSolid()
							|| below.getRelative(BlockFace.WEST).getType().isSolid()){
						below.setType(Material.FROSTED_ICE);
						ClimateUtils.setSnowLayers(b, ClimateUtils.getSnowLayers(b) + 1);
					}
				} else {
					if(below.getRelative(BlockFace.EAST).getType().isSolid()
							|| below.getRelative(BlockFace.NORTH).getType().isSolid()
							|| below.getRelative(BlockFace.SOUTH).getType().isSolid()
							|| below.getRelative(BlockFace.WEST).getType().isSolid()){
						ClimateUtils.setSnowLayers(below, ClimateUtils.getWaterHeight(below));
					}
				}*/
			} else if (below.getType() != Material.SIGN
					&& below.getType() != Material.WALL_SIGN
					&& below.getType() != Material.RAIL) {
				below.breakNaturally();
				below.setType(Material.SNOW);
			}
		} else {
			SnowFallTask snowFallTask = new SnowFallTask(module, cell,
					b.getX(), b.getY(), b.getZ());
			snowFallTask.runTaskLater(module.getPlugin(), 2);
		}
	}

	public Block getBlock() {
		return cell.getWorld().getBlockAt(x, y, z);
	}

}
