package com.kylantraynor.livelyworld.water;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.ResourcePackStatus;
import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.climate.Planet;
import com.kylantraynor.livelyworld.deterioration.DeteriorationCause;
import com.kylantraynor.livelyworld.events.BlockDeteriorateEvent;

public class TidesModule {

	private double baseViewDistance = 90;
	private double baseViewDistanceSquared;
	private double baseOceanLevel = 48;

	private double minLevel = getBaseOceanLevel() - 0.1;
	private double maxLevel = getBaseOceanLevel() + 2.0;

	private boolean beachRegression = false;

	LivelyWorld plugin;
	private BukkitRunnable tidesTask;
	private boolean enabled;

	private ArrayList<Player> ignoredPlayers;
	private boolean debug;
	private Map<Material, MaterialData> changingBlock = new HashMap<Material, MaterialData>();

	public List<String> ignoreTimeOuts = new ArrayList<String>();

	public TidesModule(LivelyWorld p) {
		this.plugin = p;
		baseViewDistanceSquared = baseViewDistance * baseViewDistance;
	}

	public void enable() {
		this.enabled = true;
		this.ignoredPlayers = new ArrayList<Player>();

		changingBlock.put(Material.COBBLESTONE, new MaterialData(Material.MOSSY_COBBLESTONE));
		changingBlock.put(Material.GRASS, new MaterialData(Material.DIRT));
		changingBlock.put(Material.DIRT, new MaterialData(Material.SAND));
		changingBlock.put(Material.MOSSY_COBBLESTONE, new MaterialData(Material.GRAVEL));
		changingBlock.put(Material.STONE, new MaterialData(Material.COBBLESTONE));
		changingBlock.put(Material.GRAVEL, new MaterialData(Material.SAND));

		int interval = 20 * 30;
		tidesTask = new TideDispatcherTask(this, interval);

		tidesTask.runTaskTimer(plugin, 20 * 10, interval);

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
		/*
		 * Material material = null; byte data = 0;
		 */
		if (isActualBeach(location)) {
			updateOceanLevel(location, 0.0, baseViewDistance);
		}
		/*
		 * long days = location.getWorld().getFullTime() / 24000; int phase =
		 * (int) (days%8); switch(phase){ case 0: case 4:
		 * if(isNextToWater(location) && (isReplaceableMaterial(location) ||
		 * location.getBlock().isLiquid())){ material = Material.AIR; data = 0;
		 * } if(location.getBlock().getType() == Material.STATIONARY_WATER ||
		 * location.getBlock().getType() == Material.WATER){
		 * sendBlockChange(location.clone().add(0, -1, 0), Material.WATER,
		 * (byte) 1); } break; case 1: case 3: case 5: case 7:
		 * if(isNextToWater(location) && (isReplaceableMaterial(location) ||
		 * location.getBlock().isLiquid())){ material = Material.WATER; data =
		 * 0; } if(location.getBlock().getType() == Material.STATIONARY_WATER ||
		 * location.getBlock().getType() == Material.WATER){
		 * sendBlockChange(location.clone().add(0, -1, 0), Material.WATER,
		 * (byte) 0); } break; case 2: case 6: if(isNextToWater(location) &&
		 * (isReplaceableMaterial(location) || location.getBlock().isLiquid())){
		 * material = Material.WATER; data = 6; }
		 * if(location.getBlock().getType() == Material.STATIONARY_WATER ||
		 * location.getBlock().getType() == Material.WATER){
		 * sendBlockChange(location.clone().add(0, -1, 0), Material.WATER,
		 * (byte) 0); } break; } } sendBlockChange(location, material, data);
		 */
	}

	private double getOceanLevel(Location l) {
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
		double currentLevel = minLevel + (levelDiff) / 2.0
				+ (((levelDiff / 2.0) * moonPhaseModifier) * dayModifier)
				+ waveX + waveZ;
		return currentLevel;
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
			byte data, Double minRange, Double maxRange) {

		double minRangeSquared = minRange * minRange;
		double maxRangeSquared = maxRange * maxRange;

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
					if (Math.random() < 0.0001) {
						Block b = location.clone().add(x, 0, z).getBlock();
						BlockState state = b.getState();
						MaterialData newMaterial = changingBlock.get(state.getData());
						if(newMaterial != null){
							BlockDeteriorateEvent event = new BlockDeteriorateEvent(b, 
									DeteriorationCause.Erosion, newMaterial);
							Bukkit.getPluginManager().callEvent(event);
							if(!event.isCancelled()){
								state.setData(newMaterial);
								state.update();
							}
						}
					}
				}
			}
		}
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (ignoredPlayers.contains(p))
				continue;
			if (location.getWorld().equals(p.getWorld())) {
				if (maxRange != null) {
					if (location.distanceSquared(p.getLocation()) <= maxRangeSquared
							&& location.distanceSquared(p.getLocation()) >= minRangeSquared) {
						p.sendBlockChange(location, material, data);
					}
				} else {
					p.sendBlockChange(location, material, data);
				}
			}
		}
	}

	public boolean isReplaceableMaterial(Location l) {
		if (l.getBlock().getType() == Material.WATER)
			return true;
		if (l.getBlock().getType() == Material.STATIONARY_WATER)
			return true;
		if (l.getBlock().getType() == Material.AIR)
			return true;
		if (l.getBlock().getType() == Material.WATER_LILY)
			return true;
		if (l.getBlock().getType() == Material.TORCH)
			return true;
		if (l.getBlock().getType() == Material.SUGAR_CANE)
			return true;
		if (l.getBlock().getType() == Material.SUGAR_CANE_BLOCK)
			return true;
		if (l.getBlock().getType() == Material.CROPS)
			return true;
		if (l.getBlock().getType() == Material.LONG_GRASS)
			return true;
		if (l.getBlock().getType() == Material.DOUBLE_PLANT)
			return true;
		if (l.getBlock().getType() == Material.SAPLING)
			return true;
		if (l.getBlock().getType() == Material.RED_ROSE)
			return true;
		if (l.getBlock().getType() == Material.YELLOW_FLOWER)
			return true;
		if (l.getBlock().getType() == Material.VINE)
			return true;
		if (l.getBlock().getType() == Material.CACTUS)
			return true;
		if (l.getBlock().getType() == Material.SNOW)
			return true;
		if (l.getBlock().getType() == Material.RAILS)
			return true;
		return false;
	}

	public boolean isActualBeach(Location l) {
		if (!l.getWorld().getName().equalsIgnoreCase("world")) {
			return false;
		}
		if (beachRegression) {
			if (l.getBlock().getBiome() == Biome.BEACHES) {
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
			if (b == Biome.BEACHES || b == Biome.OCEAN || b == Biome.DEEP_OCEAN) {
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
				if (biome == Biome.BEACHES || biome == Biome.OCEAN) {
					l.getBlock().setBiome(Biome.BEACHES);
					return true;
				}
			}
		}
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
		if (l.getBlock().getBiome() == Biome.BEACHES) {
			if (l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)
				return false;
			return true;
		}
		if (l.getBlock().getBiome() == Biome.HELL) {
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
			case "TOGGLE":
				if (args.length >= 3) {
					switch (args[2].toUpperCase()) {
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

	public void onChunkLoad(ChunkLoadEvent event) {
		/*
		 * for(int x = 0; x < 16; x++){ for(int z = 0; z < 16; z++){ Location
		 * location = event.getChunk().getBlock(x, 0, z).getLocation();
		 * location.setY(baseOceanLevel + 1); if(isActualBeach(location)){
		 * updateOceanLevel(location, null); } } }
		 */
	}

	public void onPlayerMove(PlayerMoveEvent event) {
	}

	private Map<Vehicle, Float> acceleration = new HashMap<Vehicle, Float>();

	public void onVehicleMove(VehicleMoveEvent event) {
		if (event.getVehicle().getType() == EntityType.BOAT) {

			if ((event.getTo().clone().add(0, 1, 0).getBlock().isLiquid() || event
					.getFrom().clone().add(0, 0, 0).getBlock().isLiquid())
					&& event.getVehicle().getPassenger() != null) {
				if (event.getVehicle().getPassenger().getVelocity().getY() < 0) {
					event.getVehicle().setVelocity(
							event.getVehicle()
									.getPassenger()
									.getVelocity()
									.add(new Vector(0d, Math.abs(event
											.getVehicle().getPassenger()
											.getVelocity().getY() + 0.5), 0d))
									.add(event
											.getVehicle()
											.getPassenger()
											.getLocation()
											.getDirection()
											.setY(0)
											.add(event.getVehicle()
													.getPassenger()
													.getLocation()
													.getDirection().setY(0)
													.multiply(0.2))));
				} else {
				}
			}
		}
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
}
