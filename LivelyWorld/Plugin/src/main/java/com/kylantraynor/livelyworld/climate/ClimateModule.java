package com.kylantraynor.livelyworld.climate;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.api.PacketMapChunk;
import com.kylantraynor.voronoi.VectorXZ;

public class ClimateModule {

	private LivelyWorld plugin;
	private Planet defaultPlanet;
	
	private final int cellUpdates = 1;
	private final int weatherEffectBlocks = 200;
	
	private String mapType = "TEMP";

	private BukkitRunnable climateUpdater;
	private BukkitRunnable weatherUpdater;
	private BukkitRunnable weatherEffectsUpdater;
	
	private Map<String, ClimateCell> playerCache = new HashMap<>();

	static final String MessageHeader = ChatColor.GOLD + "[" + ChatColor.WHITE
			+ "Climate" + ChatColor.GOLD + "] " + ChatColor.WHITE;

	public ClimateModule(LivelyWorld plugin) {
		this.setPlugin(plugin);
	}

	public void onEnable() {
		
		defaultPlanet = new Planet(plugin.getServer().getWorld("world"), "Laramidia");
		
		for (Planet p : Planet.planets) {
			p.generateClimateMaps();
			this.plugin.log(Level.INFO, "Generated climate maps for planet "
					+ p.getName() + ".");
		}
		weatherUpdater = new WeatherUpdaterRunnable();
		weatherEffectsUpdater = new WeatherEffectsRunnable(plugin.getServer().getWorld("world"), weatherEffectBlocks);
		
		weatherUpdater.runTaskTimer(plugin, 20L, 60L);
		weatherEffectsUpdater.runTaskTimer(plugin, 20L, 1L);

		climateUpdater = new BukkitRunnable() {

			@Override
			public void run() {
				for(Planet p : Planet.planets){
					for(int i = 0; i < cellUpdates; i++){
						p.getClimateMap().randomCellUpdate();
					}
				}
			}

		};

		climateUpdater.runTaskTimer(plugin, 21L, 1L);
	}

	public void disable() {
		saveClimateMaps();
		Planet.planets.clear();
		climateUpdater.cancel();
		weatherUpdater.cancel();
		weatherEffectsUpdater.cancel();
	}

	private void saveClimateMaps() {
		plugin.getLogger().info("Cleaning up all climate data.");
		LivelyWorld.getInstance().getDatabase().clearClimateCellsData();
		for(Planet p : Planet.planets){
			ClimateMap map = p.getClimateMap();
			if(map != null){
				plugin.getLogger().info("Saving climate data for planet " + p.getName()+ "...");
				map.saveAllData();
				plugin.getLogger().info("Saved!");
			}
		}
	}

	public LivelyWorld getPlugin() {
		return plugin;
	}

	public void setPlugin(LivelyWorld plugin) {
		this.plugin = plugin;
	}

	public void onBlockUpdate(Block block, Player p) {
		Location l = block.getLocation().clone();
		Planet planet = Planet.getPlanet(block.getWorld());
		if(planet == null) return;
		ClimateMap map = planet.getClimateMap(block.getWorld());
		if(map == null) return;
		l.setY(255);
		Block b = l.getBlock();
		while (b.getType() == Material.AIR && b.getLocation().getBlockY() > 46) {
			b = b.getRelative(BlockFace.DOWN);
		}
		updateBiome(b);
		ClimateCell c = map.getClimateCellAt(b.getLocation());
		/*if(c.getWeather() == Weather.THUNDERSTORM && Math.random() <= (((double) Bukkit.getOnlinePlayers().size()) / Math.max(c.getPlayersWithin().length, 1))){
			spawnLightning(b);
		}*/
		if (b.getType() == Material.ICE) {
			switch (b.getBiome()) {
			case FROZEN_OCEAN:
			case FROZEN_RIVER:
				
				//ClimateChunk c = ClimateChunk.getAt(b.getLocation());
				if (ClimateUtils.getTemperatureAt(b.getLocation()).getValue() > 273.15
						&& b.getLocation().getY() <= 49) {
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
			if(ClimateUtils.getAltitudeWeightedTemperature(c, topBlock.getY()).isCelsiusAbove(0)){
				ClimateUtils.setSnowLayers(topBlock, ClimateUtils.getSnowLayers(topBlock) - 1);
			} else if (c.getWeather() != Weather.CLEAR){
				SnowFallTask task = new SnowFallTask(this, c, topBlock.getX(), topBlock.getY(), topBlock.getZ());
				task.runTaskLater(this.getPlugin(), 1);
			}
		} else if ((b.getType() == Material.WATER)) {
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					Location loc = b.getLocation();
					loc.add(x, 0, z);
					if (loc.getBlock().getType() == Material.ICE) {
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
				if (location.clone().add(x, 1, z).distanceSquared(location) <= radiusSquared) {
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
			case "REFRESHCHUNK":
				if(sender instanceof Player) {
					Player p = (Player) sender;
					Chunk c = p.getLocation().getChunk();
					PacketMapChunk.refreshChunk(c);
					sender.sendMessage("Refreshed Chunk.");
				}
				break;
			case "CLEARMAPDATA":
				for(Planet p : Planet.planets){
					p.getClimateMap().clearMinMaxValues();
				}
				sender.sendMessage(ChatColor.GREEN + "Cleared map data!");
				break;
			case "SET":
				if(args.length == 2){
					sender.sendMessage(ChatColor.GRAY + "/livelyworld climate set Weather <weather>");
					sender.sendMessage(ChatColor.GRAY + "/livelyworld climate set MapType <type>");
					return;
				}
				switch(args[2].toUpperCase()){
				case "MAPTYPE":
					if(sender.isOp()){
						this.mapType = args[3].toUpperCase();
					} else {
						sender.sendMessage(ChatColor.RED + "You need to be op to use this command.");
					}
					break;
				case "WEATHER":
					Player p = (Player) sender;
					Planet pl = Planet.getPlanet(p.getWorld());
					if(pl == null) {
						sender.sendMessage(ChatColor.RED + "This world doesn't have a climate map.");
						return;
					}
					ClimateMap map = pl.getClimateMap(p.getWorld());
					if(map == null) {
						sender.sendMessage(ChatColor.RED + "This world doesn't have a climate map.");
						return;
					}
					ClimateCell c = map.getClimateCellAt(p.getLocation());
					if(c == null){
						sender.sendMessage(ChatColor.RED + "There is no climate cell here.");
						return;
					}
					try{
						c.setWeather(Weather.valueOf(args[3].toUpperCase()));
					} catch (Exception e){
						c.setWeather(Weather.CLEAR);
					}
					sender.sendMessage(ChatColor.GREEN + "Weather has been set to " + c.getWeather() + " in this cell.");
					break;
				}
				break;
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
									+ "° at the moment.");
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
							ClimateCell c = ClimateUtils.getClimateCellFor(p);
							if (c != null) {
								p.sendMessage(MessageHeader
										+ ChatColor.GOLD
										+ "Current temperature here: "
										+ ClimateUtils.getAltitudeWeightedTemperature(p.getLocation()).toString("C")
										+ " / "
										+ ClimateUtils.getAltitudeWeightedTemperature(p.getLocation()).toString("F"));
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
	
	public void updateBiome(Block block){
		updateBiome(block, null);
	}

	public void updateBiome(Block block, ClimateCell ref) {
		ClimateCell c = ClimateUtils.getClimateCellAt(block.getLocation(), ref);
		if(c == null) return;
		ClimateSquare t = ClimateUtils.getClimateSquare(block.getLocation());
		Temperature temp = t.getTemperatureAt(block.getX(), block.getZ());
		double humidity = t.getHumidityAt(block.getX(), block.getZ());
		if(c.getMap().getCurrentHighestHumidity() == 0) return;
		if(temp.isNaN()) return;
		switch (block.getBiome()) {
			case SNOWY_BEACH:
				if(temp.isCelsiusAbove(5)){
					block.setBiome(Biome.STONE_SHORE);
				}
				break;
			case STONE_SHORE:
				if(temp.isCelsiusBelow(5)){
					block.setBiome(Biome.SNOWY_BEACH);
				} else if(temp.isCelsiusAbove(20)){
					block.setBiome(Biome.BEACH);
				}
				break;
			case BEACH:
				if(temp.isCelsiusBelow(15)){
					block.setBiome(Biome.STONE_SHORE);
				}
				break;
			case SNOWY_TAIGA:
				if(temp.isCelsiusAbove(5)){
					block.setBiome(Biome.TAIGA);
				}
				break;
			case SNOWY_TAIGA_HILLS:
				if(temp.isCelsiusAbove(5)){
					block.setBiome(Biome.TAIGA_HILLS);
				}
				break;
			case TAIGA:
				if(temp.isCelsiusAbove(20)){
					block.setBiome(Biome.FOREST);
				} else if(temp.isCelsiusBelow(5)){
					block.setBiome(Biome.SNOWY_TAIGA);
				}
				break;
			case TAIGA_HILLS:
				if(temp.isCelsiusAbove(20)){
					block.setBiome(Biome.WOODED_HILLS);
				} else if(temp.isCelsiusBelow(5)){
					block.setBiome(Biome.SNOWY_TAIGA_HILLS);
				}
				break;
			case SNOWY_MOUNTAINS:
				if(temp.isCelsiusAbove(5)){
					block.setBiome(Biome.MOUNTAINS);
				}
				break;
			case MOUNTAINS:
				if(temp.isCelsiusBelow(0)){
					block.setBiome(Biome.SNOWY_MOUNTAINS);
				} else if(temp.isCelsiusAbove(25)){
					if(humidity / c.getMap().getCurrentHighestHumidity() > 0.25){
						block.setBiome(Biome.JUNGLE_HILLS);
					} else {
						block.setBiome(Biome.SAVANNA_PLATEAU);
					}
				}
				break;
			case JUNGLE_HILLS:
				if(temp.isCelsiusBelow(20)){
					block.setBiome(Biome.MOUNTAINS);
				} else if(humidity / c.getMap().getCurrentHighestHumidity() <  0.20){
					block.setBiome(Biome.SAVANNA_PLATEAU);
				}
				break;
			case SAVANNA_PLATEAU:
				if(temp.isCelsiusBelow(20)){
					block.setBiome(Biome.MOUNTAINS);
				} else if(temp.isCelsiusAbove(35) && (humidity / c.getMap().getCurrentHighestHumidity() < 0.1)){
					block.setBiome(Biome.DESERT_HILLS);
				} else if(humidity / c.getMap().getCurrentHighestHumidity() > 0.25){
					block.setBiome(Biome.JUNGLE_HILLS);
				}
				break;
			case DESERT_HILLS:
				if(humidity / c.getMap().getCurrentHighestHumidity() > 0.15){
					block.setBiome(Biome.SAVANNA_PLATEAU);
				}
			case SNOWY_TUNDRA:
				if(temp.isCelsiusAbove(5)){
					block.setBiome(Biome.PLAINS);
				}
				break;
			case PLAINS:
				if(temp.isCelsiusBelow(0)){
					block.setBiome(Biome.SNOWY_TUNDRA);
				} else if(temp.isCelsiusAbove(25)){
					if(humidity / c.getMap().getCurrentHighestHumidity() > 0.25){
						block.setBiome(Biome.JUNGLE);
					} else {
						block.setBiome(Biome.SAVANNA);
					}
				}
				break;
			case SAVANNA:
				if(temp.isCelsiusBelow(20)){
					block.setBiome(Biome.PLAINS);
				} else if(temp.isCelsiusAbove(35) && (humidity / c.getMap().getCurrentHighestHumidity() < 0.1)){
					block.setBiome(Biome.DESERT);
				} else if(humidity / c.getMap().getCurrentHighestHumidity() > 0.25){
					block.setBiome(Biome.JUNGLE);
				}
				break;
			case DESERT:
				if(humidity / c.getMap().getCurrentHighestHumidity() > 0.15){
					block.setBiome(Biome.SAVANNA);
				}
			case FROZEN_OCEAN:
				if(temp.isCelsiusAbove(-5)){
					block.setBiome(Biome.COLD_OCEAN);
				}
				break;
            case COLD_OCEAN:
                if(temp.isCelsiusBelow(-6)){
                    block.setBiome(Biome.FROZEN_OCEAN);
                } else if(temp.isCelsiusAbove(10)){
                    block.setBiome(Biome.OCEAN);
                }
                break;
			case OCEAN:
				if(temp.isCelsiusBelow(9)){
					block.setBiome(Biome.COLD_OCEAN);
				} else if(temp.isCelsiusAbove(20)){
					block.setBiome(Biome.LUKEWARM_OCEAN);
				}
				break;
			case LUKEWARM_OCEAN:
				if(temp.isCelsiusBelow(19)){
					block.setBiome(Biome.OCEAN);
				} else if(temp.isCelsiusAbove(26)){
					block.setBiome(Biome.WARM_OCEAN);
				}
				break;
			case WARM_OCEAN:
				if(temp.isCelsiusBelow(25)){
					block.setBiome(Biome.LUKEWARM_OCEAN);
				}
				break;
            case DEEP_FROZEN_OCEAN:
                if(temp.isCelsiusAbove(-5)){
                    block.setBiome(Biome.DEEP_COLD_OCEAN);
                }
                break;
            case DEEP_COLD_OCEAN:
                if(temp.isCelsiusBelow(-6)){
                    block.setBiome(Biome.DEEP_FROZEN_OCEAN);
                } else if(temp.isCelsiusAbove(10)){
                    block.setBiome(Biome.DEEP_OCEAN);
                }
                break;
            case DEEP_OCEAN:
                if(temp.isCelsiusBelow(9)){
                    block.setBiome(Biome.DEEP_COLD_OCEAN);
                } else if(temp.isCelsiusAbove(20)){
                    block.setBiome(Biome.DEEP_LUKEWARM_OCEAN);
                }
                break;
            case DEEP_LUKEWARM_OCEAN:
                if(temp.isCelsiusBelow(19)){
                    block.setBiome(Biome.DEEP_OCEAN);
                } else if(temp.isCelsiusAbove(26)){
                    block.setBiome(Biome.DEEP_WARM_OCEAN);
                }
                break;
            case DEEP_WARM_OCEAN:
                if(temp.isCelsiusBelow(25)){
                    block.setBiome(Biome.DEEP_LUKEWARM_OCEAN);
                }
                break;
			case FROZEN_RIVER:
				if(temp.isCelsiusAbove(3)){
					block.setBiome(Biome.RIVER);
				}
				break;
			case RIVER:
				if(temp.isCelsiusBelow(0)){
					block.setBiome(Biome.FROZEN_RIVER);
				}
				break;
			default:
				break;
		}
	}

	public void updatePlayerCell(Player player) {
		ClimateCell c = playerCache.get(player.getUniqueId().toString());
		if(c == null){
			playerCache.put(player.getUniqueId().toString(), ClimateUtils.getClimateCellAt(player.getLocation()));
			return;
		} else {
			VectorXZ pv = new VectorXZ((float)player.getLocation().getX(), (float) player.getLocation().getZ());
			if(c.isInside(pv)){
				return;
			} else {
				for(ClimateCell nc : c.getNeighbours()){
					if(nc == null) continue;
					if(nc.isInside(pv)){
						playerCache.put(player.getUniqueId().toString(), nc);
						return;
					}
				}
				playerCache.put(player.getUniqueId().toString(), ClimateUtils.getClimateCellAt(player.getLocation()));
			}
		}
	}

	public ClimateCell getClimateCellFor(Player p) {
		ClimateCell result = playerCache.get(p.getUniqueId().toString());
		if(result == null){
			updatePlayerCell(p);
			result = playerCache.get(p.getUniqueId().toString());
		}
		return result;
	}

	public Map<String, ClimateCell> getPlayerCache() {
		return playerCache;
	}

	public String getMapType() {
		return mapType;
	}
}
