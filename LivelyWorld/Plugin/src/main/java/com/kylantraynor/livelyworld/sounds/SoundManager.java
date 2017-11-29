package com.kylantraynor.livelyworld.sounds;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;

public class SoundManager {
	boolean enabled = true;
	LivelyWorld plugin;
	BukkitRunnable ambiantSoundsTask;

	public SoundManager(LivelyWorld p) {
		this.plugin = p;
	}

	public void enable() {
		this.enabled = true;
		ambiantSoundsTask = new BukkitRunnable() {

			@Override
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers()) {
					int rx = (int) ((Math.random() - 0.5) * 50);
					int ry = (int) Math.max(
							Math.min((Math.random() - 0.5) * 50, 255), 0);
					int rz = (int) ((Math.random() - 0.5) * 50);
					playAmbiantSound(p.getWorld(), rx, ry, rz);
				}
				if (!enabled) {
					this.cancel();
				}
			}

		};
		ambiantSoundsTask.runTaskTimer(plugin, 10, 20 * 10);
	}

	protected void playAmbiantSound(World world, int x, int y, int z) {
		Location l = new Location(world, x, y, z);
		playAmbiantSound(l.getBlock().getBiome(), l);
	}

	public void disable() {
		this.enabled = false;
		try {
			ambiantSoundsTask.cancel();
		} catch (IllegalStateException e) {

		}
	}

	public void playAmbiantSound(Biome b, Location l) {
		switch (b) {
		case BEACHES:
			l.setY(48);
			if (l.getBlock().getType() == Material.SAND
					|| l.getBlock().isLiquid()) {
				playSound("waves", l, 2, 1);
			}
			break;
		case RIVER:
			if (l.getBlock().isLiquid()) {
				// l.getWorld().playSound(l, Sound.BLOCK_WATER_AMBIENT, 2, 1);
				playSound("river", l, 3, 1);
				break;
			}
			break;
		case FOREST:
		case BIRCH_FOREST:
		case JUNGLE:
			if (l.getBlock().getType() == Material.LEAVES
					|| l.getBlock().getType() == Material.LEAVES_2) {
				playSound("birds", l, 3, 1);
				break;
			}
			break;
		default:
		}
	}

	private void playSound(String string, Location l, float volume, float pitch) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.getWorld().equals(l.getWorld())) {
				// plugin.log(Level.INFO, "Playing " + string + " to " +
				// p.getName() + " from " + l.toString());
				p.playSound(l, string, volume, pitch);
			}
		}
	}
}
