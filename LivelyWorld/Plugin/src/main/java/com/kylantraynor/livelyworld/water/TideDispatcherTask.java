package com.kylantraynor.livelyworld.water;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class TideDispatcherTask {// extends BukkitRunnable {
/*
	private TidesModule module;
	private int interval;

	public TideDispatcherTask(TidesModule module, int interval) {
		this.module = module;
		this.interval = interval;
	}

	@Override
	public void run() {
		World w = Bukkit.getServer().getWorld("world");
		if (w == null)
			return;
		for (Chunk c : w.getLoadedChunks()) {
			if (module.hasPlayerInRange(c.getBlock(8,
					(int) module.getBaseOceanLevel(), 8).getLocation())) {
				int delay = 1 + (int) Math.random() * (interval - 2);
				TideTask tt = new TideChunkTask(module, c);
				tt.runTaskLater(module.plugin, delay);
			}
		}
		if (!module.isEnabled()) {
			this.cancel();
		}
	}
	*/
}
