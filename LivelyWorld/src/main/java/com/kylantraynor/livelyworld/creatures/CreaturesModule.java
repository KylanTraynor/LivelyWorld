package com.kylantraynor.livelyworld.creatures;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.kylantraynor.livelyworld.LivelyWorld;

public class CreaturesModule {

	private LivelyWorld plugin;

	public void onEnable(LivelyWorld plugin) {
		this.setPlugin(plugin);
	}

	public void onDisable() {

	}

	public void onBlockUpdate(Block b, Player p) {
		if (p == null)
			return;
		// plugin.log(Level.INFO, "Creature update!");
		if (!b.getChunk().isLoaded())
			return;
		if (b.getChunk().getEntities().length >= Bukkit.getServer()
				.getMonsterSpawnLimit())
			return;
		while (!b.isLiquid()) {
			if (b.getY() <= 48) {
				b = b.getRelative(BlockFace.UP);
				if (b.getY() >= 49)
					break;
			} else {
				b = b.getRelative(BlockFace.DOWN);
				if (b.getY() <= 48)
					break;
			}
		}
		// plugin.log(Level.INFO, "Creature update1!");
		if (b.isLiquid()
				&& (b.getBiome() == Biome.OCEAN || b.getBiome() == Biome.DEEP_OCEAN)) {
			// plugin.log(Level.INFO, "Creature update2!");
			if (b.getLocation().distanceSquared(p.getLocation()) > 400) {
				// plugin.log(Level.INFO, "Creature update3!");
				if (b.getBiome() == Biome.DEEP_OCEAN) {
					if (Math.random() * 100 <= 20) {
						b.getLocation()
								.getWorld()
								.spawnEntity(b.getLocation(),
										EntityType.GUARDIAN);
					} else {
						b.getLocation().getWorld()
								.spawnEntity(b.getLocation(), EntityType.SQUID);
					}
				} else {
					b.getLocation().getWorld()
							.spawnEntity(b.getLocation(), EntityType.SQUID);
				}
			}
		}

	}

	public LivelyWorld getPlugin() {
		return plugin;
	}

	public void setPlugin(LivelyWorld plugin) {
		this.plugin = plugin;
	}
}
