package com.kylantraynor.livelyworld.creatures;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.api.AnimalsHelper;

public class CreaturesModule {

	private boolean autobreed = true;
	private LivelyWorld plugin;
	private BukkitRunnable runnable;
	private AnimalsHelper helper;
	private Map<UUID, Location> endangeredAnimals = new HashMap<>();

	public void onEnable(LivelyWorld plugin) {
		this.setPlugin(plugin);
		
		String packageName = this.getPlugin().getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);

        try {
            final Class<?> clazz = Class.forName("com.kylantraynor.livelyworld." + version + ".AnimalsHelperHandler");
            // Check if we have a WildAnimalHandler class at that location.
            if (AnimalsHelper.class.isAssignableFrom(clazz)) { // Make sure it actually implements WildAnimal
                this.helper = (AnimalsHelper) clazz.getConstructor().newInstance(); // Set our handler
            }
        } catch (final Exception e) {
            e.printStackTrace();
            this.getPlugin().getLogger().warning("This CraftBukkit version ("+version+") is not supported. Auto-Breed will not work.");
            this.helper = null;
        }
		this.runnable = new BukkitRunnable(){

			@Override
			public void run() {
				for(World world : getPlugin().getServer().getWorlds()){
					for(Chunk chunk : world.getLoadedChunks()){
						if(Utils.fastRandomDouble() >= 0.5) continue;
						Entity[] entities = chunk.getEntities();
						for(Entity e : entities){
							if(isAnimal(e)){
								if(e.getPassengers().size() > 0) continue;
								if(Utils.fastRandomDouble() >= 0.05) continue;
								Location lastLoc = endangeredAnimals.get(e.getUniqueId());
								if(lastLoc != null){
									if(lastLoc.getBlock() == e.getLocation().getBlock()){
										continue;
									} else {
										endangeredAnimals.remove(e.getUniqueId());
									}
								}
								Animals animal = (Animals) e;
								if(!animal.isAdult()) continue;
								boolean ate = false;
								Block edibleBlock = getEdibleBlockAround(e.getLocation().getBlock());
								if(edibleBlock != null){
									if(isBreakableBlock(edibleBlock)){
										edibleBlock.setType(Material.AIR);
										edibleBlock.getWorld().playSound(e.getLocation(), Sound.BLOCK_GRASS_BREAK, 1, 1);
									} else {
										if(edibleBlock.getType() != Material.HAY_BLOCK){
											edibleBlock.setType(Material.DIRT);
										}
										edibleBlock.getWorld().playSound(e.getLocation(), Sound.BLOCK_GRASS_BREAK, 1, 1);
									}
									ate = true;
								}
								if(ate){
									double mxHealth = animal.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
									if(animal.getHealth() >= mxHealth - 2 && animal.canBreed()){
										if(getHelper() != null){
											if(!getHelper().isInLoveMode(animal)){
												if(Utils.fastRandomDouble() < 0.1){
													getHelper().startLoveMode(animal);
												}
											}
											getHelper().moveTowardOthers(animal);
										}
										//animal.setBreed(true);
									}
									animal.setHealth(Math.min(animal.getHealth() + 5, mxHealth));
								} else {
									animal.damage(1);
									if(getHelper() != null){
										if(!moveTowardFood(animal)){
											getHelper().moveAwayFromOthers(animal);
										}
									}
									endangeredAnimals.put(animal.getUniqueId(), animal.getLocation());
								}
							}
						}
					}
				}
			}
			
		};
		if(autobreed){
			this.runnable.runTaskTimer(getPlugin(), 10, 20 * 5);
		}
	}
	
	protected boolean moveTowardFood(Animals animal) {
		Vector v = null;
		for(int x = -3; x < 4; x++){
			for(int z = -3; z < 4; z++){
				for(int y = 2; y > -3; y--){
					if(isEdibleBlock(animal.getLocation().getBlock().getRelative(x, y, z))){
						Vector temp = animal.getLocation().clone().add(x, y, z).toVector();
						if(v == null){
							v = temp;
							continue;
						}
						if(v.getBlockX() + v.getBlockY() + v.getBlockZ() > temp.getBlockX() + temp.getBlockY() + temp.getBlockZ()){
							v = temp;
						}
					}
				}
			}
		}
		if(v == null) return false;
		getHelper().moveTo(animal, animal.getLocation().clone().add(v), 1);
		return true;
	}

	public AnimalsHelper getHelper(){
		return this.helper;
	}
	
	public boolean isAnimal(Entity e) {
		if(e.getType() == EntityType.COW) return true;
		if(e.getType() == EntityType.SHEEP) return true;
		if(e.getType() == EntityType.HORSE) return true;
		if(e.getType() == EntityType.DONKEY) return true;
		if(e.getType() == EntityType.LLAMA) return true;
		return false;
	}
	
	public Block getEdibleBlockAround(Block block){
		int xs = Utils.fastRandomDouble() > 0.5 ? 1 : -1;
		int zs = Utils.fastRandomDouble() > 0.5 ? 1 : -1;
		int xcrement = xs < 0 ? 1 : -1;
		int zcrement = zs < 0 ? 1 : -1;
		for(int x = xs; x != xs * -1; x += xcrement){
			for(int z = zs; z != zs * -1; z += zcrement){
				for(int y = 1; y >= -1; y--){
					Block b = block.getRelative(x, y, z);
					if(isEdibleBlock(b)) return b;
				}
			}
		}
		return null;
	}
	
	public boolean isEdibleBlock(Block block){
		if(block.getType() == Material.GRASS_BLOCK) return true;
		if(block.getType() == Material.GRASS) return true;
		if(block.getType() == Material.TALL_GRASS) return true;
		if(block.getType() == Material.FERN) return true;
		if(block.getType() == Material.LARGE_FERN) return true;
		if(block.getType() == Material.OAK_LEAVES) return true;
		if(block.getType() == Material.DARK_OAK_LEAVES) return true;
		if(block.getType() == Material.ACACIA_LEAVES) return true;
		if(block.getType() == Material.SPRUCE_LEAVES) return true;
		if(block.getType() == Material.BIRCH_LEAVES) return true;
		if(block.getType() == Material.JUNGLE_LEAVES) return true;
		if(block.getType() == Material.POPPY) return true;
		if(block.getType() == Material.OXEYE_DAISY) return true;
		if(block.getType() == Material.BLUE_ORCHID) return true;
		if(block.getType() == Material.DANDELION) return true;
		if(block.getType() == Material.WHEAT) return true;
		if(block.getType() == Material.CARROTS) return true;
		if(block.getType() == Material.HAY_BLOCK) return true;
		return false;
	}
	
	public boolean isBreakableBlock(Block block){
		if(block.getType() == Material.GRASS) return true;
		if(block.getType() == Material.TALL_GRASS) return true;
		if(block.getType() == Material.FERN) return true;
		if(block.getType() == Material.LARGE_FERN) return true;
		if(block.getType() == Material.OAK_LEAVES) return true;
		if(block.getType() == Material.DARK_OAK_LEAVES) return true;
		if(block.getType() == Material.ACACIA_LEAVES) return true;
		if(block.getType() == Material.SPRUCE_LEAVES) return true;
		if(block.getType() == Material.BIRCH_LEAVES) return true;
		if(block.getType() == Material.JUNGLE_LEAVES) return true;
		if(block.getType() == Material.POPPY) return true;
		if(block.getType() == Material.OXEYE_DAISY) return true;
		if(block.getType() == Material.BLUE_ORCHID) return true;
		if(block.getType() == Material.DANDELION) return true;
		if(block.getType() == Material.WHEAT) return true;
		if(block.getType() == Material.CARROTS) return true;
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
					if (Utils.fastRandomInt(100) <= 20) {
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
