package com.kylantraynor.livelyworld.climate;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

public class SnowFallTask extends BukkitRunnable {

	private ClimateModule module;
	private World world;
	private int x;
	private int y;
	private int z;

	public SnowFallTask(ClimateModule module, World w, int x, int y, int z) {
		this.module = module;
		this.world = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public void run() {

		processSnowFall();

	}

	private void processSnowFall() {
		Block b = module.getPlugin().getLowestNear(world, x, y, z).getBlock();
		if (getBlock().equals(b) || Math.random() < 0.01) {
			module.updateBiome(b);
			if (b.getRelative(BlockFace.DOWN).getType() == Material.SNOW) {
				Block snow = b.getRelative(BlockFace.DOWN);
				if (snow.getData() < 6) {
					snow.setData((byte) (snow.getData() + 1));
				} else {
					snow.setType(Material.SNOW_BLOCK);
				}
			} else if (b.getRelative(BlockFace.DOWN).getType().isSolid()) {
				b.setType(Material.SNOW);
			} else if (b.getRelative(BlockFace.DOWN).getType() != Material.SIGN_POST
					&& b.getRelative(BlockFace.DOWN).getType() != Material.SIGN) {
				b.getRelative(BlockFace.DOWN).breakNaturally();
				b.getRelative(BlockFace.DOWN).setType(Material.SNOW);
			}
		} else {
			SnowFallTask snowFallTask = new SnowFallTask(module, world,
					b.getX(), b.getY(), b.getZ());
			snowFallTask.runTaskLater(module.getPlugin(), 1);
		}
	}

	public Block getBlock() {
		return world.getBlockAt(x, y, z);
	}

}
