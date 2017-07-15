package com.kylantraynor.livelyworld.climate;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.voronoi.VectorXZ;

public class ClimateModule {

	private LivelyWorld plugin;
	private Planet defaultPlanet;
	
	private final int cellUpdates = 1;

	private BukkitRunnable climateUpdater;
	private BukkitRunnable weatherUpdater;
	private BukkitRunnable weatherEffectsUpdater;
	
	private Map<String, ClimateCell> playerCache = new HashMap<String, ClimateCell>();

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
		weatherUpdater = new BukkitRunnable() {

			@Override
			public void run() {
				for (Player p : Bukkit.getServer().getOnlinePlayers()){
					Planet pl = Planet.getPlanet(p.getWorld());
					if(pl == null) continue;
					ClimateMap map = pl.getClimateMap(p.getWorld());
					if(map == null) continue;
					ClimateCell c = map.getClimateCellAt(p.getLocation());
					switch (c.getWeather()){
					case CLEAR:
					case OVERCAST:
						p.setPlayerWeather(WeatherType.CLEAR);
						break;
					case RAIN:
					case SNOW:
					case STORM:
					case SNOWSTORM:
					case THUNDERSTORM:
						p.setPlayerWeather(WeatherType.DOWNFALL);
						break;
					default:
						break;
					}
				}
			}
			
		};
		
		weatherEffectsUpdater = new BukkitRunnable() {

			@Override
			public void run() {
				for(ClimateCell c : playerCache.values()){
					int mostDist = (int) c.getMostDistance();
					int doubleMostDist = 2 * mostDist;
					switch (c.getWeather()){
					case CLEAR:
						if(Math.random() <= 1.0){
							for(int i = 0; i < 20; i++){
								int random_x = (int) ((Math.random() * doubleMostDist) - mostDist);
								int random_z = (int) ((Math.random() * doubleMostDist) - mostDist);
								int x = (int)c.getSite().getX() + random_x;
								int z = (int)c.getSite().getZ() + random_z;
								int chunkX = x >> 4; // /16
								int chunkZ = z >> 4; // /16
								if(!c.getWorld().isChunkLoaded(chunkX, chunkZ)){
									continue;
								}
								Block b = c.getWorld().getHighestBlockAt(x, z);
								if(!b.getChunk().isLoaded()) return;
								while(b.getType() == Material.AIR){
									b = b.getRelative(BlockFace.DOWN);
								}
								if(c.getTemperature().isCelsiusAbove(5)){
									ClimateUtils.melt(b);
								}
							}
						}
						break;
					case OVERCAST:
						break;
					case RAIN:
						break;
					case SNOW:
						if(Math.random() <= 0.95){
							for(int i = 0; i < 20; i++){
								int random_x = (int) ((Math.random() * doubleMostDist) - mostDist);
								int random_z = (int) ((Math.random() * doubleMostDist) - mostDist);
								int x = (int)c.getSite().getX() + random_x;
								int z = (int)c.getSite().getZ() + random_z;
								int chunkX = x >> 4; // /16
								int chunkZ = z >> 4; // /16
								if(!c.getWorld().isChunkLoaded(chunkX, chunkZ)){
									continue;
								}
								Block b = c.getWorld().getHighestBlockAt((int) c.getSite().getX() + random_x, (int)c.getSite().getZ() + random_z);
								SnowFallTask task = new SnowFallTask(getPlugin().getClimateModule(), b.getWorld(), b.getX(), b.getY() + 1, b.getZ());
								task.runTaskLater(getPlugin(), 1);
							}
						}
						break;
					case STORM:
						break;
					case SNOWSTORM:
						if(Math.random() <= 1.0){
							for(int i = 0; i < 40; i++){
								int random_x = (int) ((Math.random() * doubleMostDist) - mostDist);
								int random_z = (int) ((Math.random() * doubleMostDist) - mostDist);
								int x = (int)c.getSite().getX() + random_x;
								int z = (int)c.getSite().getZ() + random_z;
								int chunkX = x >> 4; // /16
								int chunkZ = z >> 4; // /16
								if(!c.getWorld().isChunkLoaded(chunkX, chunkZ)){
									continue;
								}
								Block b = c.getWorld().getHighestBlockAt((int) c.getSite().getX() + random_x, (int)c.getSite().getZ() + random_z);
								SnowFallTask task = new SnowFallTask(getPlugin().getClimateModule(), b.getWorld(), b.getX(), b.getY() + 1, b.getZ());
								task.runTaskLater(getPlugin(), 1);
							}
						}
						break;
					case THUNDERSTORM:
						if(Math.random() <= 0.1){
							int random_x = (int) ((Math.random() * doubleMostDist) - mostDist);
							int random_z = (int) ((Math.random() * doubleMostDist) - mostDist);
							int x = (int)c.getSite().getX() + random_x;
							int z = (int)c.getSite().getZ() + random_z;
							int chunkX = x >> 4; // /16
							int chunkZ = z >> 4; // /16
							if(!c.getWorld().isChunkLoaded(chunkX, chunkZ)){
								continue;
							}
							Block b = c.getWorld().getHighestBlockAt((int) c.getSite().getX() + random_x, (int)c.getSite().getZ() + random_z);
							spawnLightning(b.getRelative(BlockFace.UP));
						}
						break;
					default:
						break;
					}
				}
			}
			
		};
		
		weatherUpdater.runTaskTimer(plugin, 20L, 60L);
		weatherEffectsUpdater.runTaskTimer(plugin, 20L, 1L);
		
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

		climateUpdater.runTaskTimer(plugin, 20L, 1L);
	}

	public void onDisable() {
		Planet.planets.clear();
		climateUpdater.cancel();
		weatherUpdater.cancel();
		weatherEffectsUpdater.cancel();
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
				if (map.getTemperatureAt(b.getLocation()).getValue() > 273.15
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
			if(map.getTemperatureAt(topBlock.getLocation()).isCelsiusAbove(0)){
				ClimateUtils.setSnowLayers(topBlock, ClimateUtils.getSnowLayers(topBlock) - 1);
			} else if (map.getClimateCellAt(topBlock.getLocation()).getWeather() != Weather.CLEAR){
				SnowFallTask task = new SnowFallTask(this, topBlock.getWorld(), topBlock.getX(), topBlock.getY(), topBlock.getZ());
				task.runTaskLater(this.getPlugin(), 1);
			}
		} else if ((b.getType() == Material.WATER || b.getType() == Material.STATIONARY_WATER)) {
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

	private void spawnLightning(Block b) {
		b.getLocation().getWorld().spigot().strikeLightning(b.getLocation(), true);
		b.getWorld().playSound(b.getLocation(), Sound.ENTITY_LIGHTNING_IMPACT, 20, 1);
		b.getWorld().playSound(b.getLocation().add(0, 255 - b.getLocation().getY(), 0), Sound.ENTITY_LIGHTNING_THUNDER, 300, 1);
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
			case "SET":
				if(args.length == 2){
					sender.sendMessage(ChatColor.GRAY + "/livelyworld climate set Weather <weather>");
					return;
				}
				switch(args[2].toUpperCase()){
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
							ClimateCell c = ClimateUtils.getClimateCellFor(p);
							if (c != null) {
								p.sendMessage(MessageHeader
										+ ChatColor.GOLD
										+ "Current temperature here: "
										+ c.getTemperature().toString(p));
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
		Planet p = Planet.getPlanet(block.getWorld());
		if(p == null) return;
		ClimateMap map = p.getClimateMap(block.getWorld());
		if(map == null) return;
		Temperature temp = map.getTemperatureAt(block.getLocation());
		if(temp.isNaN()) return;
		switch (block.getBiome()) {
		case COLD_BEACH:
			if(temp.isCelsiusAbove(5)){
				block.setBiome(Biome.STONE_BEACH);
			}
			break;
		case STONE_BEACH:
			if(temp.isCelsiusBelow(5)){
				block.setBiome(Biome.COLD_BEACH);
			} else if(temp.isCelsiusAbove(20)){
				block.setBiome(Biome.BEACHES);
			}
			break;
		case BEACHES:
			if(temp.isCelsiusBelow(15)){
				block.setBiome(Biome.STONE_BEACH);
			}
			break;
		case TAIGA_COLD:
			if(temp.isCelsiusAbove(5)){
				block.setBiome(Biome.TAIGA);
			}
			break;
		case TAIGA_COLD_HILLS:
			if(temp.isCelsiusAbove(5)){
				block.setBiome(Biome.TAIGA_HILLS);
			}
			break;
		case TAIGA:
			if(temp.isCelsiusAbove(20)){
				block.setBiome(Biome.FOREST);
			} else if(temp.isCelsiusBelow(5)){
				block.setBiome(Biome.TAIGA_COLD);
			}
			break;
		case TAIGA_HILLS:
			if(temp.isCelsiusAbove(20)){
				block.setBiome(Biome.FOREST_HILLS);
			} else if(temp.isCelsiusBelow(5)){
				block.setBiome(Biome.TAIGA_COLD_HILLS);
			}
			break;
		case ICE_MOUNTAINS:
			if(temp.isCelsiusAbove(5)){
				block.setBiome(Biome.EXTREME_HILLS);
			}
			break;
		case EXTREME_HILLS:
			break;
		case ICE_FLATS:
			if(temp.isCelsiusAbove(5)){
				block.setBiome(Biome.PLAINS);
			}
			break;
		case PLAINS:
			if(temp.isCelsiusBelow(0)){
				block.setBiome(Biome.ICE_FLATS);
			} else if(temp.isCelsiusAbove(25)){
				block.setBiome(Biome.SAVANNA);
			}
			break;
		case SAVANNA:
			if(temp.isCelsiusBelow(20)){
				block.setBiome(Biome.PLAINS);
			} else if(temp.isCelsiusAbove(35)){
				block.setBiome(Biome.DESERT);
			}
			break;
		case FROZEN_OCEAN:
			if(temp.isCelsiusAbove(-5)){
				block.setBiome(Biome.OCEAN);
			}
			break;
		case OCEAN:
			if(temp.isCelsiusBelow(-10)){
				block.setBiome(Biome.FROZEN_OCEAN);
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
		return playerCache.get(p.getUniqueId().toString());
	}

	public Map<String, ClimateCell> getPlayerCache() {
		return playerCache;
	}
}
