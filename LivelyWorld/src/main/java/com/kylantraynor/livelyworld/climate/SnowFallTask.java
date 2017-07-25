package com.kylantraynor.livelyworld.climate;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;

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
			ClimateCell cell = ClimateUtils.getClimateCellAt(b.getLocation(), this.cell);
			if(ClimateUtils.getAltitudeWeightedTriangleTemperature(cell, b.getLocation()).isCelsiusAbove(3)) return;
			Block below = b.getRelative(BlockFace.DOWN);
			if (below.getType() == Material.SNOW) {
				Block snow = below;
				ClimateUtils.setSnowLayers(snow, ClimateUtils.getSnowLayers(snow) + 1);
				/*if (snow.getData() < 6) {
					snow.setData((byte) (snow.getData() + 1));
				} else {
					snow.setType(Material.SNOW_BLOCK);
				}*/
			} else if (below.getType().isSolid()) {
				if(below.getType() == Material.ICE || below.getType() == Material.PACKED_ICE || below.getType() == Material.FROSTED_ICE){
					b.setType(Material.SNOW_BLOCK);
				} else {
					b.setType(Material.SNOW);
				}
			} else if (ClimateUtils.isWater(below)){ 
				if(below.getData() == 0){
					below.setType(Material.FROSTED_ICE);
					ClimateUtils.setSnowLayers(b, ClimateUtils.getSnowLayers(b) + 1);
				} else {
					ClimateUtils.setSnowLayers(below, ClimateUtils.getWaterHeight(below));
				}
			} else if (below.getType() != Material.SIGN_POST
					&& below.getType() != Material.SIGN
					&& below.getType() != Material.RAILS) {
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
