package com.kylantraynor.livelyworld.creatures;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;

public class CreaturesModule {

	private LivelyWorld plugin;
	private BukkitRunnable runnable;

	public void onEnable(LivelyWorld plugin) {
		this.setPlugin(plugin);
		this.runnable = new BukkitRunnable(){

			@Override
			public void run() {
				for(World world : getPlugin().getServer().getWorlds()){
					for(Chunk chunk : world.getLoadedChunks()){
						double rdm = Math.random();
						if(rdm >= 0.5) continue;
						Entity[] entities = chunk.getEntities();
						for(Entity e : entities){
							if(isAnimal(e)){
								if(rdm >= 0.1) continue;
								Ageable animal = (Ageable) e;
								boolean ate = false;
								if(isEdibleBlock(e.getLocation().getBlock())){
									Block b = e.getLocation().getBlock();
									if(isBreakableBlock(b)){
										b.setType(Material.AIR);
										b.getWorld().playSound(e.getLocation(), Sound.BLOCK_GRASS_BREAK, 1, 1);
									} else {
										b.setType(Material.DIRT);
										b.getWorld().playSound(e.getLocation(), Sound.BLOCK_GRASS_BREAK, 1, 1);
									}
									ate = true;
								} else if(isEdibleBlock(e.getLocation().getBlock().getRelative(BlockFace.DOWN))){
									Block b = e.getLocation().getBlock().getRelative(BlockFace.DOWN);
									if(isBreakableBlock(b)){
										b.setType(Material.AIR);
										b.getWorld().playSound(e.getLocation(), Sound.BLOCK_GRASS_BREAK, 1, 1);
									} else {
										b.setType(Material.DIRT);
										b.getWorld().playSound(e.getLocation(), Sound.BLOCK_GRASS_BREAK, 1, 1);
									}
									ate = true;
								}
								if(ate == true){
									double mxHealth = animal.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
									if(animal.getHealth() >= mxHealth - 2 && animal.isAdult()){
										animal.setBreed(true);
									}
									animal.setHealth(Math.min(animal.getHealth() + 1, mxHealth));
								} else {
									animal.damage(1);
								}
							}
						}
					}
				}
			}
			
		};
		this.runnable.runTaskTimer(getPlugin(), 10, 20 * 5);
	}
	
	public boolean isAnimal(Entity e) {
		if(e.getType() == EntityType.COW) return true;
		if(e.getType() == EntityType.SHEEP) return true;
		return false;
	}
	
	public boolean isEdibleBlock(Block block){
		if(block.getType() == Material.GRASS) return true;
		if(block.getType() == Material.LONG_GRASS) return true;
		if(block.getType() == Material.DOUBLE_PLANT) return true;
		if(block.getType() == Material.LEAVES) return true;
		if(block.getType() == Material.LEAVES_2) return true;
		if(block.getType() == Material.CROPS) return true;
		return false;
	}
	
	public boolean isBreakableBlock(Block block){
		if(block.getType() == Material.LONG_GRASS) return true;
		if(block.getType() == Material.DOUBLE_PLANT) return true;
		if(block.getType() == Material.LEAVES) return true;
		if(block.getType() == Material.LEAVES_2) return true;
		if(block.getType() == Material.CROPS) return true;
		return false;
	}

	public void onDisable() {
		if(this.runnable != null){
			this.runnable.cancel();
		}
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
