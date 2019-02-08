package com.kylantraynor.livelyworld.water;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kylantraynor.livelyworld.climate.Planet;
import com.kylantraynor.livelyworld.deterioration.DeteriorationCause;
import com.kylantraynor.livelyworld.events.BlockDeteriorateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.api.AnimalsHelper;
import com.kylantraynor.livelyworld.api.BoatHelper;

public class TidesModule {
/*
	private double baseViewDistance = 90;
	private double baseViewDistanceSquared;
	private double baseOceanLevel = 48;

	private double minLevel = getBaseOceanLevel() - 0.1;
	private double maxLevel = getBaseOceanLevel() + 2.0;
	
	private WaterChunkThread waterThread = new WaterChunkThread();

	private boolean beachRegression = false;

	LivelyWorld plugin;
	private BukkitRunnable tidesTask;
	private boolean enabled;

	private ArrayList<Player> ignoredPlayers;
	private boolean debug;
	private Map<Material, Material> changingBlock = new HashMap<>();

	public List<String> ignoreTimeOuts = new ArrayList<>();
	private boolean realisticSimulation = true;
	
	private BoatHelper boatHelper;

	public TidesModule(LivelyWorld p) {
		this.plugin = p;
		baseViewDistanceSquared = baseViewDistance * baseViewDistance;
        try {
            final Class<?> clazz = Class.forName("com.kylantraynor.livelyworld." + LivelyWorld.getServerVersion() + ".BoatHelperHandler");
            if (BoatHelper.class.isAssignableFrom(clazz)) {
                this.boatHelper = (BoatHelper) clazz.getConstructor().newInstance();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            p.getLogger().warning("This CraftBukkit version ("+LivelyWorld.getServerVersion()+") is not supported. Boat fixes won't work.");
            this.boatHelper = null;
        }
	}

	public void enable() {
		this.enabled = true;
		this.ignoredPlayers = new ArrayList<>();

		changingBlock.put(Material.COBBLESTONE, Material.MOSSY_COBBLESTONE);
		changingBlock.put(Material.GRASS, Material.DIRT);
		changingBlock.put(Material.DIRT, Material.SAND);
		changingBlock.put(Material.MOSSY_COBBLESTONE, Material.GRAVEL);
		changingBlock.put(Material.STONE, Material.COBBLESTONE);
		changingBlock.put(Material.GRAVEL, Material.SAND);
		changingBlock.put(Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS);


		waterThread.setDaemon(true);
		waterThread.start();
	}
	
	public void disable(){
		if(this.enabled){
			this.enabled = false;
			stopWaterChunksThread();
		}
	}
	
	public void stopWaterChunksThread(){
		plugin.getLogger().info("Unloading all water chunks.");
		waterThread.interrupt();
		WaterChunk.unloadAll();
		plugin.getLogger().info("Done!");
	}

	public void updateOceanLevelFor(Player p) {
		Location current;
		Chunk c = p.getLocation().getChunk();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				current = c.getBlock(x, (int) (getBaseOceanLevel() + 1), z)
						.getLocation();
				if (isActualBeach(current)) {
					updateOceanLevel(current, baseViewDistance - 3,
							baseViewDistance + 3);
				}
			}
		}
	}

	public boolean hasPlayerInRange(Location l) {
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (p.getWorld().equals(l.getWorld())) {
				if (p.getLocation().distanceSquared(l) <= baseViewDistanceSquared) {
					return true;
				}
			}
		}
		return false;
	}

	public void updateOceanLevel(Location location) {
		if (Bukkit.getServer().getOnlinePlayers().size() <= 0)
			return;
		location.setY(getBaseOceanLevel() + 1);

		double distance = baseViewDistanceSquared;
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (ignoredPlayers.contains(p))
				continue;
			if (location.getWorld().equals(p.getWorld())) {
				if (location.distanceSquared(p.getLocation()) < baseViewDistanceSquared) {
					distance = location.distanceSquared(p.getLocation());
				}
			}
		}

		if (distance >= baseViewDistanceSquared) {
			return;
		}

		if (isActualBeach(location)) {
			updateOceanLevel(location, 0.0, baseViewDistanceSquared);
		}
	}

	public double getOceanLevel(Location l) {
		double levelDiff = maxLevel - minLevel;

		int days = (int) (l.getWorld().getFullTime() % (24000 * 8));

		double moonPhaseModifier = Math.sin((days / (24000.0 * 8.0)) * 2.0
				* Math.PI);
		double dayModifier = 1.0;
		Planet p = Planet.getPlanet(l.getWorld());
		if (p != null) {
			dayModifier = -Math.cos(p.getSunPosition(l) * Math.PI);
		}
		double waveX = 0.0;// 0.05 * Math.cos((l.getX() * 2 * Math.PI / 20) +
							// (l.getWorld().getTime() / 100.0));
		double waveZ = 0.0;// 0.05 * Math.cos((l.getZ() * 2 * Math.PI / 500) +
							// (l.getWorld().getTime() / 100.0));
		return minLevel + (levelDiff) / 2.0
				+ (((levelDiff / 2.0) * moonPhaseModifier) * dayModifier)
				+ waveX + waveZ;
	}

	private int getOceanBlockLevel(Location l) {
		return (int) Math.floor(getOceanLevel(l));
	}

	private int getOceanDataLevel(Location l) {
		double data = getOceanLevel(l) - getOceanBlockLevel(l);
		data *= 7;
		if ((int) Math.floor(data) == 0)
			return 0;
		return (int) Math.floor(8 - data);
	}

	private void updateOceanLevel(Location location, Double minRange,
			Double maxRange) {
		if (minRange == null)
			minRange = 0.0;

		for (int y = (int) Math.floor(minLevel); y <= Math.floor(maxLevel); y++) {
			location.setY(y);
			if (!isReplaceableMaterial(location))
				continue;
			Material m = Material.AIR;
			byte b = 0;
			if (y <= getOceanBlockLevel(location)) {
				if (getOceanDataLevel(location) == 0) {
					if (y < getOceanBlockLevel(location)) {
						m = Material.WATER;
					}
				} else {
					m = Material.WATER;
					if (y == getOceanBlockLevel(location)) {
						b = (byte) getOceanDataLevel(location);
					}
				}
			}
			sendBlockChange(location, m, b, minRange, maxRange);
		}
	}

	private void sendBlockChange(Location location, Material material,
			byte data, Double minRangeSquared, Double maxRangeSquared) {

		if (location == null)
			return;
		if (material == null)
			return;
		if (material == Material.WATER) {
			if (!location.getBlock().isLiquid()) {
				location.getBlock().breakNaturally();
			}
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					if (Utils.fastRandomDouble() < 0.0001) {
						Block b = location.clone().add(x, 0, z).getBlock();
						Material newMaterial = changingBlock.get(b.getType());
						if(newMaterial != null){
							BlockDeteriorateEvent event = new BlockDeteriorateEvent(b,
									DeteriorationCause.Erosion, newMaterial);
							Bukkit.getPluginManager().callEvent(event);
							if(!event.isCancelled()){
								b.setType(event.getTarget());
							}
						}
					}
				}
			}
		}
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (ignoredPlayers.contains(p)) continue;
			if(p.getGameMode() == GameMode.SPECTATOR) continue;
			
			if (location.getWorld().equals(p.getWorld())) {
				if (maxRangeSquared != null) {
					double distanceSquared = location.distanceSquared(p.getLocation());
					if (distanceSquared <= maxRangeSquared && distanceSquared >= minRangeSquared) {
						p.sendBlockChange(location, material, data);
					}
				} else {
					p.sendBlockChange(location, material, data);
				}
			}
		}
	}

	public boolean isReplaceableMaterial(Location l) {
		Material m = l.getBlock().getType();
		switch(m){
			case WATER:
			case AIR:
			case LILY_PAD:
			case TORCH:
			case SUGAR_CANE:
			case WHEAT:
			case GRASS:
			case TALL_GRASS:
			case SPRUCE_SAPLING:
			case ACACIA_SAPLING:
			case BIRCH_SAPLING:
			case OAK_SAPLING:
			case JUNGLE_SAPLING:
			case DARK_OAK_SAPLING:
			case DANDELION:
			case ROSE_BUSH:
			case SUNFLOWER:
			case BLUE_ORCHID:
			case VINE:
			case CACTUS:
			case RAIL:
			case POWERED_RAIL:
			case ACTIVATOR_RAIL:
			case DETECTOR_RAIL:
				return true;
		}
		return false;
	}

	public boolean isActualBeach(Location l) {
		if (!l.getWorld().getName().equalsIgnoreCase("world")) {
			return false;
		}
		if (beachRegression) {
			if (l.getBlock().getBiome() == Biome.BEACH) {
				l.getBlock().setBiome(Biome.PLAINS);
			}
			return false;
		}
		int oceanDepth = getOceanDepth(l);
		if (oceanDepth > 4) {
			if (l.getBlock().getBiome() == Biome.DEEP_OCEAN)
				return true;
			if (l.getBlock().getBiome() != Biome.OCEAN) {
				l.getBlock().setBiome(Biome.OCEAN);
			}
			return true;
		} else if (oceanDepth == 0) {
			Biome b = l.getBlock().getBiome();
			if (b == Biome.BEACH || b == Biome.STONE_SHORE || b == Biome.OCEAN || b == Biome.DEEP_OCEAN) {
				l.getBlock().setBiome(Biome.PLAINS);
			}
			return false;
		}
		if (isSurrounded(Biome.OCEAN, l)) {
			if (l.getBlock().getBiome() != Biome.OCEAN) {
				l.getBlock().setBiome(Biome.OCEAN);
			}
			return true;
		}
		if (l.getBlock().getBiome() == Biome.RIVER) {
			return true;
		}

		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				if ((x == -1 || x == 1) && (z == -1 || z == 1))
					continue;
				Biome biome = l.clone().add(x, 0, z).getBlock().getBiome();
				if (biome == Biome.BEACH || biome == Biome.STONE_SHORE || biome == Biome.OCEAN) {
					l.getBlock().setBiome(Biome.BEACH);
					return true;
				}
			}
		}
		return false;
	}

	private boolean isNextToBiome(Biome biome, Location l){
		if (l.getBlock().getRelative(BlockFace.EAST).getBiome() == biome
				|| l.getBlock().getRelative(BlockFace.WEST).getBiome() == biome
				|| l.getBlock().getRelative(BlockFace.NORTH).getBiome() == biome
				|| l.getBlock().getRelative(BlockFace.SOUTH).getBiome() == biome)
			return true;
		return false;
	}
	
	private boolean isSurrounded(Biome biome, Location l) {
		if (l.getBlock().getRelative(BlockFace.EAST).getBiome() == biome
				&& l.getBlock().getRelative(BlockFace.WEST).getBiome() == biome)
			return true;
		if (l.getBlock().getRelative(BlockFace.NORTH).getBiome() == biome
				&& l.getBlock().getRelative(BlockFace.SOUTH).getBiome() == biome)
			return true;
		return false;
	}

	public int getOceanDepth(Location l) {
		Location current = l.clone();
		current.setY(getBaseOceanLevel() + 1);
		if (current.getBlock().getLightFromSky() <= 0) {
			return 0;
		}
		if (current.getBlock().isLiquid()) {
			if (current.getBlock().getBiome() != Biome.RIVER) {
				current.getBlock().setBiome(Biome.RIVER);
			}
			return 0;
		}
		int depth = 0;
		while (((current.getBlock().isLiquid() || isReplaceableMaterial(current)) && depth < 5)
				|| current.getBlockY() > getBaseOceanLevel()) {
			current.add(0, -1, 0);
			if (current.getBlockY() == getBaseOceanLevel()
					&& current.getBlock().getType() == Material.AIR) {
				if (current.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
					return 0;
				} else {
					// Could set the block to water here on the server
					// But until the beaches are all set correctly, better not
					// make any permanent changes
					return 0;
				}
			}
			depth++;
		}
		return depth;
	}

	public boolean isNextToWater(Location l) {
		if (l.getBlock().getBiome() == Biome.BEACH || l.getBlock().getBiome() == Biome.STONE_SHORE) {
			if (l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)
				return false;
			return true;
		}
		if (l.getBlock().getBiome() == Biome.NETHER) {
			return false;
		}
		if (l.getBlock().getRelative(BlockFace.EAST).isLiquid())
			return true;
		if (l.getBlock().getRelative(BlockFace.WEST).isLiquid())
			return true;
		if (l.getBlock().getRelative(BlockFace.NORTH).isLiquid())
			return true;
		if (l.getBlock().getRelative(BlockFace.SOUTH).isLiquid())
			return true;
		if (l.getBlock().getRelative(BlockFace.DOWN).isLiquid())
			return true;
		if (l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SAND)
			return true;
		if (l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.GRAVEL)
			return true;
		return false;
	}

	public void onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (args.length >= 2) {
			switch (args[1].toUpperCase()) {
			case "INFO":
				sender.sendMessage("Timings: ");
				sender.sendMessage("Ocean Level: " + Utils.fitTimings(WaterChunk.total[0] / WaterChunk.samples[0]) + " (" + WaterChunk.samples[0] + ")");
				sender.sendMessage("Pressure Update: " + Utils.fitTimings(WaterChunk.total[1] / WaterChunk.samples[1]) + " (" + WaterChunk.samples[1] + ")");
				sender.sendMessage("Chunk Refresh: " + Utils.fitTimings(WaterChunk.total[3] / WaterChunk.samples[3]) + " (" + WaterChunk.samples[3] + ")");
				sender.sendMessage("Water Update: " + Utils.fitTimings(WaterChunk.total[2] / WaterChunk.samples[2]) + " (" + WaterChunk.samples[2] + ")");
				sender.sendMessage("BALANCE:  Flow: " + WaterChunk.delta[0] + " Evaporation: " + WaterChunk.delta[1] + " Rain: " + WaterChunk.delta[2] + " Plants: " + WaterChunk.delta[3]);
				break;
			case "STOPWATERTHREAD":
				sender.sendMessage("Stopping water thread.");
				stopWaterChunksThread();
				break;
			case "DRAINCHUNK":
				sender.sendMessage("Draining Chunk.");
				Player player = (Player) sender;
				Chunk c = player.getLocation().getChunk();
				if(c != null) {
					BukkitRunnable br = new BukkitRunnable(){
						@Override
						public void run() {
							WaterChunk.get(c.getWorld(), c.getX(), c.getZ()).drain();
						}
					};
					br.runTaskAsynchronously(plugin);
				}
				break;
			case "SATURATECHUNK":
				sender.sendMessage("Saturating Chunk.");
				Player player0 = (Player) sender;
				Chunk c0 = player0.getLocation().getChunk();
				if(c0 != null) {
					BukkitRunnable br = new BukkitRunnable(){
						@Override
						public void run() {
							WaterChunk.get(c0.getWorld(), c0.getX(), c0.getZ()).saturate();
						}
					};
					br.runTaskAsynchronously(plugin);
				}
				break;
			case "UPDATECHUNK":
				sender.sendMessage("Updating chunk.");
				Player player1 = (Player) sender;
				Chunk c1 = player1.getLocation().getChunk();
				WaterChunk.get(c1.getWorld(), c1.getX(), c1.getZ()).updateVisually();
				break;
			case "GET":
				if(args.length >= 3){
					final Player p = (Player) sender;
					BukkitRunnable br;
					switch(args[2].toUpperCase()) {
					case "MOISTURE":
						br = new BukkitRunnable(){
							@Override
							public void run() {
								WaterChunk c = WaterChunk.get(p.getWorld(), p.getLocation().getBlockX() >> 4, p.getLocation().getBlockZ() >> 4);
								WaterData wd = c.getAt(Math.floorMod(p.getLocation().getBlockX(),16), p.getLocation().getBlockY(), Math.floorMod(p.getLocation().getBlockZ(),16));
								p.sendMessage("DEBUG : GetData: " + Integer.toBinaryString((int) wd.getData()));
								p.sendMessage("DEBUG : Data Level: " + wd.getLevel() + ", " + Integer.toBinaryString(wd.getLevel()) + ", Data Resistance: " + wd.getResistance() + ", " + Integer.toBinaryString(wd.getResistance()) + ", Data Salt: " + wd.getSalt() + ", " + Integer.toBinaryString(wd.getSalt()));
							}
						};
						br.runTaskAsynchronously(plugin);
						break;
					case "CLOSEST":
						br = new BukkitRunnable(){
							@Override
							public void run() {
								WaterChunk c = WaterChunk.get(p.getWorld(), p.getLocation().getBlockX() >> 4, p.getLocation().getBlockZ() >> 4);
								WaterData wd = c.getAt(Math.floorMod(p.getLocation().getBlockX(),16), p.getLocation().getBlockY(), Math.floorMod(p.getLocation().getBlockZ(),16));
								while(wd.getLevel() < 1 && wd.getY() > 0){
									wd = c.getAt(wd.getX(), wd.getY() - 1, wd.getZ());
								}
								p.sendMessage("Closest water block: Y" + wd.getY() + ".");
								p.sendMessage("DEBUG : GetData: " + Integer.toBinaryString((int) wd.getData()));
								p.sendMessage("DEBUG : Data Level: " + wd.getLevel() + ", " + Integer.toBinaryString(wd.getLevel()) + ", Data Resistance: " + wd.getResistance() + ", " + Integer.toBinaryString(wd.getResistance()) + ", Data Salt: " + wd.getSalt() + ", " + Integer.toBinaryString(wd.getSalt()));
							}
						};
						br.runTaskAsynchronously(plugin);
						break;
					}
				}
				break;
			case "TOGGLE":
				if (args.length >= 3) {
					switch (args[2].toUpperCase()) {
					case "SIMULATION":
						realisticSimulation = !realisticSimulation;
						sender.sendMessage("Realistic water simulation is set to: " + realisticSimulation);
						break;
					case "WAVES":
						if (ignoredPlayers.contains((Player) sender)) {
							ignoredPlayers.remove((Player) sender);
							sender.sendMessage("Waves turned on.");
						} else {
							ignoredPlayers.add((Player) sender);
							sender.sendMessage("Waves turned off.");
						}
						break;
					case "BEACHESREGRESSION":
						if (sender.isOp()) {
							beachRegression = !beachRegression;
							if (beachRegression) {
								sender.sendMessage("Beaches Regression turned on.");
							} else {
								sender.sendMessage("Beaches Regression turned off.");
							}
						}
						break;
					case "DEBUG":
						if (sender.isOp()) {
							debug = !debug;
							if (debug) {
								sender.sendMessage("Debug for water module turned on.");
							} else {
								sender.sendMessage("Debug for water module turned off.");
							}
						}
						break;
					}
				}
			}
		}
	}
	
	public boolean isRealisticSimulation(){
		return realisticSimulation;
	}

	public void onPlayerMove(PlayerMoveEvent event) {
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public double getBaseOceanLevel() {
		return baseOceanLevel;
	}

	public void setBaseOceanLevel(double baseOceanLevel) {
		this.baseOceanLevel = baseOceanLevel;
	}
	
	public WaterChunkThread getWaterThread(){
		return waterThread;
	}

	public BoatHelper getBoatHelper() {
		return boatHelper;
	}*/
}
