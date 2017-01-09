package com.kylantraynor.livelyworld.water;

import org.bukkit.Chunk;
import org.bukkit.Location;

public class TideChunkTask extends TideTask {

	private Chunk chunk;

	public TideChunkTask(TidesModule module, Chunk cs) {
		super(module, cs.getWorld(), cs.getX(), cs.getZ());
		chunk = cs;
	}

	@Override
	public void run() {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int blockX = (chunk.getX() * 16) + x;
				int blockZ = (chunk.getZ() * 16) + z;
				getModule().updateOceanLevel(
						new Location(this.getWorld(), blockX, 60, blockZ));
			}
		}
	}
}
