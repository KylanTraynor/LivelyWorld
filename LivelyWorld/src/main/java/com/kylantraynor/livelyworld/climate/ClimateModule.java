package com.kylantraynor.livelyworld.climate;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;

public class ClimateModule {

	private LivelyWorld plugin;
	private Planet defaultPlanet;
	
	private final int cellUpdates = 10;

	private BukkitRunnable climateUpdater;

	static final String MessageHeader = ChatColor.GOLD + "[" + ChatColor.WHITE
			+ "Climate" + ChatColor.GOLD + "] " + ChatColor.WHITE;

	public ClimateModule(LivelyWorld plugin) {
		this.setPlugin(plugin);
	}

	public void onEnable() {
		defaultPlanet = new Planet(plugin.getServer().getWorld("world"),
				"Laramidia");
		for (Planet p : Planet.planets) {
			p.generateClimateMaps();
			this.plugin.log(Level.INFO, "Generated climate maps for planet "
					+ p.getName() + ".");
		}

		climateUpdater = new BukkitRunnable() {

			@Override
			public void run() {
				for (World w : Bukkit.getServer().getWorlds()) {
					Planet p = Planet.getPlanet(w);
					if (p != null) {
						for(int i = 0; i < cellUpdates; i++)
							p.getClimateMap(w).randomCellUpdate();
					}
				}
			}

		};

		climateUpdater.runTaskTimer(plugin, 20L, 20L);
	}

	public void onDisable() {
		Planet.planets.clear();
		climateUpdater.cancel();
	}

	public LivelyWorld getPlugin() {
		return plugin;
	}

	public void setPlugin(LivelyWorld plugin) {
		this.plugin = plugin;
	}

	public void onBlockUpdate(Block block, Player p) {
		Location l = block.getLocation().clone();
		l.setY(255);
		Block b = l.getBlock();
		while (b.getType() == Material.AIR && b.getLocation().getBlockY() > 46) {
			b = b.getRelative(BlockFace.DOWN);
		}
		if (b.getType() == Material.ICE) {
			switch (b.getBiome()) {
			case FROZEN_OCEAN:
			case FROZEN_RIVER:
				ClimateChunk c = ClimateChunk.getAt(b.getLocation());
				if (c.getTemperature().getValue() > 273.15
						&& b.getLocation().getY() <= 60) {
					if (hasBiomeWithin(b.getLocation(), Biome.OCEAN, 5)) {
						b.setBiome(Biome.OCEAN);
					} else {
						b.setBiome(Biome.RIVER);
					}
					if (hasBiomeWithin(b.getLocation(), Biome.DEEP_OCEAN, 30)) {
						b.setType(Material.FROSTED_ICE);
					} else if (hasBiomeWithin(b.getLocation(), Biome.OCEAN, 30)) {
						b.setType(Material.FROSTED_ICE);
					}
				}
				break;
			default:
				if (hasBiomeWithin(b.getLocation(), Biome.DEEP_OCEAN, 30)) {
					b.setType(Material.FROSTED_ICE);
				} else if (hasBiomeWithin(b.getLocation(), Biome.OCEAN, 30)) {
					b.setType(Material.FROSTED_ICE);
				}
				break;
			}
		} else if (b.getType() == Material.SNOW
				|| b.getType() == Material.SNOW_BLOCK) {
			Block topBlock = b;
			while (topBlock.getRelative(BlockFace.UP).getType() == Material.SNOW
					|| topBlock.getRelative(BlockFace.UP).getType() == Material.SNOW_BLOCK) {
				topBlock = topBlock.getRelative(BlockFace.UP);
			}

		} else if ((b.getType() == Material.WATER || b.getType() == Material.STATIONARY_WATER)) {
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					Location loc = b.getLocation();
					loc.add(x, 0, z);
					if (loc.getBlock().getType() == Material.ICE) {
						ClimateChunk c = ClimateChunk.getAt(loc);
						if (c.getTemperature().getValue() > 273.15
								&& b.getLocation().getY() <= 60) {
							if (hasBiomeWithin(b.getLocation(), Biome.OCEAN, 5)) {
								b.setBiome(Biome.OCEAN);
							} else {
								b.setBiome(Biome.RIVER);
							}
							loc.getBlock().setType(Material.FROSTED_ICE);
						}
					}
				}
			}
		}
	}

	private boolean hasBiomeWithin(Location location, Biome biome, int i) {
		int radiusSquared = i * i;
		Location base = location.clone();
		base.setY(1);
		for (int x = -i; x < i; x++) {
			for (int z = -i; z < i; z++) {
				if (location.clone().add(x, 1, z).distanceSquared(location) <= i) {
					if (location.clone().add(x, 1, z).getBlock().getBiome()
							.equals(biome)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatColor.GRAY
					+ "/livelyworld climate get <property>");
		} else if (args.length >= 2) {
			switch (args[1].toUpperCase()) {
			case "GET":
				if (args.length == 2) {
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get AverageIrradiance");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get AverageTemperatureFeet");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get AverageTemperature");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get ChunkAverageTemperature");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get ChunkTemperature");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get CurrentObliquity");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get CurrentSolarEquator");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get Irradiance");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get PlanetRadius");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get TemperatureFeet");
					sender.sendMessage(ChatColor.GRAY
							+ "/livelyworld climate get Temperature");
				}
				if (args.length >= 3) {
					switch (args[2].toUpperCase()) {
					case "CURRENTOBLIQUITY":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							p.sendMessage(MessageHeader + ChatColor.GOLD
									+ planet.getName()
									+ " has an obliquity of "
									+ ((planet.getOb() * 180) / Math.PI)
									+ "� at the moment.");
						}
						break;
					case "CURRENTSOLAREQUATOR":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							p.sendMessage(MessageHeader + ChatColor.GOLD
									+ planet.getName()
									+ " has its solar equator at "
									+ (planet.getCurrentOffset())
									+ " Z at the moment.");
						}
						break;
					case "PLANETRADIUS":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							p.sendMessage(MessageHeader + ChatColor.GOLD
									+ planet.getName() + " has a radius of "
									+ planet.getR() / 1000 + " km.");
						}
						break;
					case "IRRADIANCE":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							p.sendMessage(MessageHeader + ChatColor.GOLD
									+ "Current irradiance here: "
									+ planet.getSunRadiation(p.getLocation())
									* 100 + "%");
						}
						break;
					case "AVERAGEIRRADIANCE":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							p.sendMessage(MessageHeader
									+ ChatColor.GOLD
									+ "Average irradiance here: "
									+ planet.getSunAverageRadiation(p
											.getLocation()) * 100 + "%");
						}
						break;
					case "TEMPERATUREFEET":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							p.sendMessage(MessageHeader
									+ ChatColor.GOLD
									+ "Current temperature at your feet: "
									+ planet.getClimate(
											p.getLocation().add(0, -1, 0))
											.getTemperature().toString(p)
									+ " ("
									+ p.getLocation().add(0, -1, 0).getBlock()
											.getType().toString() + ")");
						}
						break;
					case "AVERAGETEMPERATUREFEET":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							p.sendMessage(MessageHeader
									+ ChatColor.GOLD
									+ "Average temperature at your feet: "
									+ planet.getClimate(
											p.getLocation().add(0, -1, 0))
											.getAverageTemperature()
											.toString(p)
									+ " ("
									+ p.getLocation().add(0, -1, 0).getBlock()
											.getType().toString() + ")");
						}
						break;
					case "CHUNKTEMPERATURE":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							if (new ClimateChunk(p.getLocation())
									.getTemperature() != null) {
								p.sendMessage(MessageHeader
										+ ChatColor.GOLD
										+ "Current temperature here: "
										+ new ClimateChunk(p.getLocation())
												.getTemperature().toString(p));
							} else {
								p.sendMessage(MessageHeader
										+ ChatColor.RED
										+ "No temperature can be calculated here.");
							}
						}
						break;
					case "CHUNKAVERAGETEMPERATURE":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							if (new ClimateChunk(p.getLocation())
									.getAverageTemperature() != null) {
								p.sendMessage(MessageHeader
										+ ChatColor.GOLD
										+ "Average temperature here: "
										+ new ClimateChunk(p.getLocation())
												.getAverageTemperature()
												.toString(p));
							} else {
								p.sendMessage(MessageHeader
										+ ChatColor.RED
										+ "No temperature can be calculated here.");
							}
						}
						break;
					case "TEMPERATURE":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							if (new Climate(p.getLocation())
									.getAreaTemperature() != null) {
								p.sendMessage(MessageHeader
										+ ChatColor.GOLD
										+ "Current temperature here: "
										+ new Climate(p.getLocation())
												.getAreaTemperature().toString(
														p));
							} else {
								p.sendMessage(MessageHeader
										+ ChatColor.RED
										+ "No temperature can be calculated here.");
							}
						}
						break;
					case "AVERAGETEMPERATURE":
						if (sender instanceof Player) {
							Player p = (Player) sender;
							Planet planet = Planet.getPlanet(p.getWorld());
							if (planet != null) {
								planet = defaultPlanet;
							}
							if (new Climate(p.getLocation())
									.getAreaAverageTemperature() != null) {
								p.sendMessage(MessageHeader
										+ ChatColor.GOLD
										+ "Average temperature here: "
										+ new Climate(p.getLocation())
												.getAverageTemperature()
												.toString(p));
							} else {
								p.sendMessage(MessageHeader
										+ ChatColor.RED
										+ "No temperature can be calculated here.");
							}
						}
						break;
					}
				}
			}
		}
	}

	public void updateBiome(Block block) {
		if (block.getBiome() == Biome.RIVER)
			return;
		Climate c = new Climate(block.getLocation());
		Temperature temp = c.getAreaTemperature();
		if (temp.getValue() > 273.15 + 5) {
			switch (block.getBiome()) {
			case COLD_BEACH:
				block.setBiome(Biome.BEACHES);
				break;
			case TAIGA_COLD:
				block.setBiome(Biome.TAIGA);
				break;
			case TAIGA_COLD_HILLS:
				block.setBiome(Biome.TAIGA_HILLS);
				break;
			case TAIGA:
				block.setBiome(Biome.FOREST);
				break;
			case TAIGA_HILLS:
				block.setBiome(Biome.FOREST_HILLS);
				break;
			case ICE_MOUNTAINS:
				block.setBiome(Biome.EXTREME_HILLS);
				break;
			case ICE_FLATS:
				block.setBiome(Biome.PLAINS);
				break;
			case FROZEN_OCEAN:
				block.setBiome(Biome.OCEAN);
				break;
			case FROZEN_RIVER:
				block.setBiome(Biome.RIVER);
				break;
			default:
				break;
			}
		}

	}
}
