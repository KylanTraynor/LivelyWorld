package com.kylantraynor.livelyworld;

import java.time.Instant;
import java.util.List;
import java.util.logging.Level;

import com.kylantraynor.livelyworld.burn.BurnModule;
import com.kylantraynor.livelyworld.creatures.CreaturesModule;
import com.kylantraynor.livelyworld.database.Database;
import com.kylantraynor.livelyworld.database.sqlite.SQLite;
import com.kylantraynor.livelyworld.deterioration.DeteriorationModule;
import com.kylantraynor.livelyworld.gravity.GravityModule;
import com.kylantraynor.livelyworld.hooks.HookManager;
import com.kylantraynor.livelyworld.pathways.PathwaysModule;
import com.kylantraynor.livelyworld.sounds.SoundManager;
import com.kylantraynor.livelyworld.utils.MathUtil;
import com.kylantraynor.livelyworld.vegetation.VegetationModule;
import com.kylantraynor.livelyworld.waterV2.WaterModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Crops;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.climate.ClimateCell;
import com.kylantraynor.livelyworld.climate.ClimateListener;
import com.kylantraynor.livelyworld.climate.ClimateModule;
import com.kylantraynor.livelyworld.climate.ClimateUtils;
import com.kylantraynor.livelyworld.climate.SnowFallTask;
import com.kylantraynor.livelyworld.climate.Temperature;

public class LivelyWorld extends JavaPlugin implements Listener {

	private static final String PLUGIN_NAME = "LivelyWorld";
	private static boolean allowTasks = false;
	
	private Database database;
	private boolean useSQLite = true;

	private boolean usingPathways = true;
	private PathwaysModule pathways;

	private boolean usingDeterioration = true;
	private DeteriorationModule deterioration;

	private boolean usingCreatures = true;
	private CreaturesModule creatures;

	private boolean usingVegetation = true;
	private VegetationModule vegetation;

	protected int updateRadius = 250;

	private boolean usingBurn = true;
	private BurnModule burn;

	private boolean usingClimate = true;
	private ClimateModule climate;

	private BukkitRunnable randomBlockPicker;

	private SoundManager sounds;
	private boolean usingSounds = true;

	private boolean usingTides = true;
	private WaterModule tides;

	private boolean usingGravity = true;
	private GravityModule gravity;

	private static LivelyWorld currentInstance;

	private Instant lastBlockUpdate = Instant.now();
	private long blockUpdatePeriod = 2L;
	private Location worldCenter;
	private int worldBorder = 4800;
	
	private String packageName;
	private String version;

	private long mainThreadId;

	private BukkitRunnable lagChecker;

	public void log(Level level, String message) {
		getLogger().log(level, "[" + PLUGIN_NAME + "] " + message);
	}
	
	public static String getServerVersion(){
		return getInstance().version;
	}

	@Override
	public void onEnable() {
		mainThreadId = Thread.currentThread().getId();

		currentInstance = this;

		allowTasks = true;
		packageName = LivelyWorld.getInstance().getServer().getClass().getPackage().getName();
	    version = packageName.substring(packageName.lastIndexOf('.') + 1);
		saveDefaultConfig();
		worldCenter = new Location(Bukkit.getWorld("world"), 1600, 100, 1600);
		PluginManager pm = getServer().getPluginManager();

		loadConfig();
		loadDatabase();

		if (usingPathways) {
			pathways = new PathwaysModule();
			pathways.onEnable(this);
		}

		if (usingClimate) {
			climate = new ClimateModule(this);
			this.getServer().getPluginManager().registerEvents(new ClimateListener(), this);
			climate.onEnable();
		}

		if (usingDeterioration) {
			deterioration = new DeteriorationModule();
			deterioration.onEnable(this);
		}

		if (usingCreatures) {
			creatures = new CreaturesModule();
			creatures.onEnable(this);
		}

		if (usingVegetation) {
			vegetation = new VegetationModule(this);
			vegetation.onEnable();
		}

		if (usingBurn) {
			burn = new BurnModule(this);
			burn.onEnable();
		}

		if (usingSounds) {
			sounds = new SoundManager(this);
			sounds.enable();
		}

		/*tides = new TidesModule(this);
		if (usingTides) {
			tides.enable();
			pm.registerEvents(new WaterListener(), this);
		}*/
		tides = new WaterModule(this);

		if (usingGravity) {
			gravity = new GravityModule(this);
			gravity.enable();
			gravity.reloadProperties(getConfig());
		}

		pm.registerEvents(this, this);
		startLagChecker();
		startRandomBlockUpdater();
		
		startRandomChunkUpdater();
	}
	
	private void startLagChecker() {
		lagChecker = new BukkitRunnable(){
			private long lastupdate = System.currentTimeMillis();
			@Override
			public void run() {
				Utils.setTickLength(System.currentTimeMillis() - lastupdate);
				lastupdate = System.currentTimeMillis();
			}
			
		};
		lagChecker.runTaskTimer(this, 1, 1);
	}

	private void startRandomBlockUpdater(){
		randomBlockPicker = new BukkitRunnable() {

			@Override
			public void run() {
				try {
					if (getServer().getOnlinePlayers().size() == 0) {
						World world = Bukkit.getWorlds().get((int) Math.floor(Math.random() * Bukkit.getWorlds().size()));
						int randomX = 0;
						int randomY = (int) (255 * Math.random());
						int randomZ = 0;
						if(HookManager.hasWorldBorder()){
							worldCenter = HookManager.getWorldBorder().getWorldCenter(world);
							double worldRadiusX = HookManager.getWorldBorder().getWorldRadiusX(world);
							double worldRadiusZ = HookManager.getWorldBorder().getWorldRadiusZ(world);
							if(worldRadiusX == 0 || worldRadiusZ == 0) return;
 							randomX = (int) Math.round(Math.random() * (worldRadiusX * 2) - worldRadiusX);
							randomZ = (int) Math.round(Math.random() * (worldRadiusZ * 2) - worldRadiusZ);
							randomX += worldCenter.getBlockX();
							randomZ += worldCenter.getBlockZ();
						} else {
							return;
						}
						Location l = new Location(worldCenter.getWorld(),
								randomX, randomY, randomZ);
						new BukkitRunnable() {
							@Override
							public void run() {
								try {
									updateBlock(l.getBlock(), null);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

						}.runTask(currentInstance);
					} else {
						int size = getServer().getOnlinePlayers().size();
						int random = (int) Math.floor(Math.random() * size);
						Player p = getServer().getOnlinePlayers().toArray(
								new Player[size])[random];
						int randomX = (int) Math
								.round((Math.random() * (updateRadius * 2 + 1))
										- updateRadius);
						int randomY = (int) (255 * Math.random());
						int randomZ = (int) Math
								.round((Math.random() * (updateRadius * 2 + 1))
										- updateRadius);
						Location l = p.getLocation().add(randomX, randomY,
								randomZ);
						new BukkitRunnable() {
							@Override
							public void run() {
								try {
									if (l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) {
										updateBlock(l.getBlock(), p);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

						}.runTask(currentInstance);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		randomBlockPicker.runTaskTimerAsynchronously(this, 10L,
				blockUpdatePeriod);
	}

    /**
     * Start the task triggering random chunk updates.
     */
	private void startRandomChunkUpdater() {
		BukkitRunnable br = new BukkitRunnable(){

			@Override
			public void run() {
				UpdateManager.processRandomChunkUpdate(Bukkit.getWorld("world"));
			}
			
		};
		br.runTaskTimer(this, 200, 5);
	}

	private void loadDatabase() {
		this.database = new SQLite(this);
		this.database.load();
	}

	protected void updateBlock(Block b, Player p) {
		if (!isInValidWorld(b.getLocation()))
			return;
		
		if(Utils.hasLag()) return;
		
		if (usingClimate) {
			climate.onBlockUpdate(b, p);
		}
		if (usingDeterioration) {
			deterioration.onBlockUpdate(b, p);
		}
		if (usingCreatures) {
			creatures.onBlockUpdate(b, p);
		}
		/*if (usingVegetation) {
			vegetation.onBlockUpdate(b, p);
		}*/
		if (usingBurn) {
			burn.onBlockUpdate(b, p);
		}
		/*if (usingGravity) {
			gravity.onBlockUpdate(b, p);
		}*/
	}

	private boolean isInValidWorld(Location location) {
		if (location.getWorld() == worldCenter.getWorld()) {
			return (location.getBlockX() < worldBorder + worldCenter.getBlockX()
					&& location.getBlockX() > -worldBorder
							+ worldCenter.getBlockX()
					&& location.getBlockZ() < worldBorder
							+ worldCenter.getBlockZ()
					&& location.getBlockZ() > -worldBorder
							+ worldCenter.getBlockZ()
					&& location.getBlockY() > 0 && location.getBlockY() < 255);
		}
		return false;
	}

	private void loadConfig() {
		YamlConfiguration cfg = (YamlConfiguration) getConfig();
		// If config file contains usepathways
		if (cfg.contains("usepathways")) {
			// then get the value
			usingPathways = cfg.getBoolean("usepathways");
		} else {
			// else write the default value in the config file
			getConfig().set("usepathways", usingPathways);
		}
		// If config file contains useclimate
		if (cfg.contains("useclimate")) {
			// then get the value
			usingClimate = cfg.getBoolean("useclimate");
		} else {
			// else write the default value in the config file
			getConfig().set("useclimate", usingClimate);
		}
		// If config file contains usedeterioration
		if (cfg.contains("usedeterioration")) {
			// then get the value
			usingDeterioration = cfg.getBoolean("usedeterioration");
		} else {
			// else write the default value in the config file
			getConfig().set("usedeterioration", usingDeterioration);
		}
		// If config file contains usecreatures
		if (cfg.contains("usecreatures")) {
			// then get the value
			usingCreatures = cfg.getBoolean("usecreatures");
		} else {
			// else write the default value in the config file
			getConfig().set("usecreatures", usingCreatures);
		}
		// If config file contains usevegetation
		if (cfg.contains("usevegetation")) {
			// then get the value
			usingVegetation = cfg.getBoolean("usevegetation");
		} else {
			// else write the default value in the config file
			getConfig().set("usevegetation", usingVegetation);
		}
		// If config file contains useburn
		if (cfg.contains("useburn")) {
			// then get the value
			usingBurn = cfg.getBoolean("useburn");
		} else {
			// else write the default value in the config file
			getConfig().set("useburn", usingBurn);
		}
		// If config file contains usetides
		if (cfg.contains("usetides")) {
			// then get the value
			usingTides = cfg.getBoolean("usetides");
		} else {
			// else write the default value in the config file
			getConfig().set("usetides", usingTides);
		}
		// If config file contains usesounds
		if (cfg.contains("usesounds")) {
			// then get the value
			usingSounds = cfg.getBoolean("usesounds");
		} else {
			// else write the default value in the config file
			getConfig().set("usesounds", usingSounds);
		}
		// If config file contains usesounds
		if (cfg.contains("usegravity")) {
			// then get the value
			usingGravity = cfg.getBoolean("usegravity");
		} else {
			// else write the default value in the config file
			getConfig().set("usegravity", usingGravity);
		}
		// If config file contains updateradius
		if (cfg.contains("updateradius")) {
			// then get the value
			updateRadius = cfg.getInt("updateradius");
		} else {
			// else write the default value in the config file
			getConfig().set("updateradius", updateRadius);
		}
		saveConfig();
	}

	@Override
	public void onDisable() {
	    allowTasks = false;
		getServer().getScheduler().cancelTasks(this);
		if (sounds != null) {
			sounds.disable();
		}
		if (gravity != null) {
			gravity.setProperties(getConfig());
		}
		if (climate != null) {
			climate.disable();
		}
		tides.disable();
		lagChecker = null;
		randomBlockPicker = null;
		saveConfig();
	}

	@EventHandler
	public void onBlockForm(BlockFormEvent event) {
		if (event.isCancelled())
			return;
		if (event.getNewState().getType() == Material.SNOW) {
			if (usingClimate) {
				climate.updateBiome(event.getBlock());
			}
			event.setCancelled(true);
			SnowFallTask snowFallTask = new SnowFallTask(climate, ClimateUtils.getClimateCellAt(event.getBlock().getLocation()), event.getBlock().getX(), event
					.getBlock().getY(), event.getBlock().getZ());

			snowFallTask.runTaskLater(this, 1);
		} else if(event.getNewState().getType() == Material.ICE){
			if(usingClimate){
				climate.updateBiome(event.getBlock());
				if(ClimateUtils.getAltitudeWeightedTemperature(event.getBlock().getLocation()).isCelsiusAbove(0)){
					event.setCancelled(true);
				}
			}
		}
	}

	public Location getLowestNear(Location location) {
		return getLowestNear(location.getWorld(), location.getBlockX(),
				location.getBlockY(), location.getBlockZ());
	}

	public Location getLowestNear(World world, int startX, int startY,
			int startZ) {
		int snowBaseLevel = 8;
		Block sb = world.getBlockAt(startX, startY - 1, startZ);
		if (sb.getType() == Material.SNOW) {
			snowBaseLevel = ClimateUtils.getSnowLayers(sb);
		}

		int xIncrement = Utils.fastRandomDouble() >= 0.5 ? 1 : -1; 
		int zIncrement = Utils.fastRandomDouble() >= 0.5 ? 1 : -1;
		for (int x = startX - xIncrement; x <= startX + 1 && x >= startX - 1; x += xIncrement) {
			for (int z = startZ - zIncrement; z <= startZ + 1 && z >= startZ - 1; z += zIncrement) {
				if (x * x == 1 && z * z == 1)
					continue;
				Block b = world.getBlockAt(x, startY, z);
				if (b.getType() == Material.AIR) {
					b = b.getRelative(BlockFace.DOWN);
					if (b.getType() == Material.AIR) {
						while (b.getRelative(BlockFace.DOWN).getType() == Material.AIR && b.getY() > 1) {
							b = b.getRelative(BlockFace.DOWN);
						}
						if (b.getType() == Material.AIR) {
							return new Location(world, x, b.getY(), z);
						} else {
							return new Location(world, x, b.getY() + 1, z);
						}
					} else if (b.getType() == Material.SNOW) {
						if (ClimateUtils.getSnowLayers(b) < snowBaseLevel) {
							return new Location(world, x, b.getY() + 1, z);
						}
					}
				}
			}
		}
		return new Location(world, startX, startY, startZ);
	}

	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.getTo().getBlock() != event.getFrom().getBlock()) {
			if (usingPathways) {
				pathways.onPlayerMove(event);
			}
			if (usingGravity) {
				Location l = event.getTo().clone().add(0, -1, 0);
				if (l.getBlockY() < 255 && l.getBlockY() > 0) {
					gravity.onBlockUpdate(l.getBlock(), event.getPlayer());
				}
			}
			/*if (usingTides) {
				tides.onPlayerMove(event);
			}
			// this part tries to remove the fall damage when not touching
			// blocks for a while
			// but not actually falling, so it checks if the player's velocity
			// is actually
			// in the direction of -Y, if not, it sets the falling distance to
			// 0.
			if (event.getPlayer().getFallDistance() > 2) {
				if (event.getPlayer().getVelocity().getY() > 0 && getWaterModule().getOceanLevel(event.getFrom()) >= event.getFrom().getBlockY()) {
					event.getPlayer().setFallDistance(2);
				}
			}*/
		}
	}

	@EventHandler
	public void onVehicleMove(VehicleMoveEvent event) {
		if (event.getTo().getBlock() != event.getFrom().getBlock()) {
			if (usingPathways) {
				pathways.onVehicleMove(event);
			}
			if (usingGravity) {
				Location l = event.getTo().add(0, -1, 0);
				if (l.getBlockY() < 255 && l.getBlockY() > 0) {
					gravity.onBlockUpdate(l.getBlock(), null);
				}
			}
		}
	}

	@EventHandler
	public void onEntiryDeath(EntityDeathEvent event) {
		switch (event.getEntityType()) {
            case CHICKEN:
                for (int i = 0; i < Utils.fastRandomInt(5) + 8; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.FEATHER, 1));
                }
                break;
            case COW:
                for (int i = 0; i < Utils.fastRandomInt(5) + 8; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.BEEF, 1));
                }
                for (int i = 1; i < Utils.fastRandomInt(2) + 1; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.LEATHER, 1));
                }
                for (int i = 0; i < Utils.fastRandomInt(5) + 5; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.BONE, 1));
                }
                break;
            case GUARDIAN:
                break;
            case HORSE:
                for (int i = 1; i < Utils.fastRandomInt(2) + 1; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(
                                    event.getEntity().getLocation(),
                                    new ItemStack(Material.LEATHER, 1));
                }
                // No break: Continue to Zombie and Skeleton horses
            case ZOMBIE_HORSE: case SKELETON_HORSE:
                for (int i = 0; i < Utils.fastRandomInt(5) + 5; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.BONE, 1));
                }
                break;
            case MUSHROOM_COW:
                break;
            case OCELOT:
                break;
            case PIG:
                for (int i = 0; i < Utils.fastRandomInt(5) + 3; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.PORKCHOP, 1));
                }
                for (int i = 0; i < Utils.fastRandomInt(5) + 2; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.BONE, 1));
                }
                break;
            case RABBIT:
                for (int i = 0; i < Utils.fastRandomInt(2) + 1; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.BONE, 1));
                }
                break;
            case SHEEP:
                for (int i = 0; i < Utils.fastRandomInt(5) + 5; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.MUTTON, 1));
                }
                for (int i = 0; i < Utils.fastRandomInt(4) + 4; i++) {
                    event.getEntity()
                            .getLocation()
                            .getWorld()
                            .dropItemNaturally(event.getEntity().getLocation(),
                                    new ItemStack(Material.BONE, 1));
                }
                break;
            default:
                break;

        }
	}

	public boolean hasPlayerInRange(Location l, double range) {
		double rangeSquared = range * range;
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (p.getWorld().equals(l.getWorld())) {
				if (p.getLocation().distanceSquared(l) <= rangeSquared) {
					return true;
				}
			}
		}
		return false;
	}

	@EventHandler
	public void onItemSpawn(ItemSpawnEvent event){
		Item item = event.getEntity();
		if(usingGravity){
			List<Entity> ents = item.getNearbyEntities(2, 2, 2);
			for(Entity e : ents){
				if(e.getType() == EntityType.FALLING_BLOCK){
					FallingBlock fb = (FallingBlock) e;
					Material m = fb.getBlockData().getMaterial();
					if(m == item.getItemStack().getType()){
						if(!item.getLocation().getBlock().getType().isSolid()){
							BukkitRunnable br = new BukkitRunnable(){

								@Override
								public void run() {
									item.getLocation().getBlock().breakNaturally();
									item.getLocation().getBlock().setType(m);
									item.getLocation().getBlock().setBlockData(fb.getBlockData());
									item.remove();
								}
								
							};
							br.runTaskLater(this, 10);
						}
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onItemDespawn(ItemDespawnEvent event) {
		// log(Level.INFO, "Item has despawned!");
		Item item = event.getEntity();
		if (item != null) {
			// log(Level.INFO, item.getItemStack().getType().toString());
			switch (item.getItemStack().getType()) {
                case OAK_SAPLING:
                case DARK_OAK_SAPLING:
                case BIRCH_SAPLING:
                case JUNGLE_SAPLING:
                case ACACIA_SAPLING:
                case SPRUCE_SAPLING:
                    if (usingVegetation) {
                        //log(Level.INFO, "Attempting to plant sapling.");
                        int chunkX = MathUtil.toChunk(event.getLocation().getX());
                        int chunkZ = MathUtil.toChunk(event.getLocation().getZ());
                        if (event.getLocation().getWorld().isChunkLoaded(chunkX, chunkZ)) {
                            vegetation.plantSapling(item.getItemStack().getType(), event.getLocation());
                        } else {
                            event.setCancelled(true);
                        }
                    }
                    break;
                default:
                    break;
			}
		} else {
			// log(Level.INFO, "There was no item!");
		}
	}

	@EventHandler
	public void onBlockIgnite(BlockIgniteEvent event) {
		// log(Level.INFO, "On Block Ignite.");
		if (usingBurn && event.getBlock().getChunk().isLoaded()) {
			burn.onBlockIgnite(event);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

		switch (cmd.getName().toUpperCase()) {
		case "LIVELYWORLD":
			if (args.length >= 1) {
				switch (args[0].toUpperCase()) {
				case "WATER":
					tides.onCommand(sender, cmd, label, args);
					break;
				case "DETERIORATION":
					deterioration.onCommand(sender, cmd, label, args);
					break;
				case "CLIMATE":
					climate.onCommand(sender, cmd, label, args);
					break;
				case "VEGETATION":
					vegetation.onCommand(sender, cmd, label, args);
					break;
				case "GRAVITY":
					gravity.onCommand(sender, cmd, label, args);
				}
			} else {

			}
			break;
		}

		return false;
	}

	@EventHandler
	public void onBlockBurn(BlockBurnEvent event) {
		// log(Level.INFO, "On Block Burn.");
		if (usingBurn && event.getBlock().getChunk().isLoaded()) {
			burn.onBlockBurn(event);
		}
	}
	
	@EventHandler
	public void onBlockGrow(BlockGrowEvent event) {
		if (usingClimate) {
			if(Math.random() > 0.05){
				event.setCancelled(true);
				return;
			}
			BlockState state = event.getBlock().getState();
			if(state instanceof Crops){
				Crops crops = (Crops) state;
				if(ClimateUtils.getSunRadiation(state.getLocation()) <= Math.random() && crops.getItemType() != Material.NETHER_WART_BLOCK){
					event.setCancelled(true);
					return;
				}
				Temperature temp = null;
				switch(crops.getItemType()){
                    case WHEAT: // WHEAT
                        temp = ClimateUtils.getTemperatureAt(event.getBlock().getLocation());
                        if(temp.isNaN()) return;
                        if(!ClimateUtils.isAcceptableTemperature(temp, Temperature.fromCelsius(18.5), Temperature.fromCelsius(4), Temperature.fromCelsius(37))){
                            event.setCancelled(true);
                            return;
                        }
                        break;
                    case CARROT: case BEETROOTS:
                        temp = ClimateUtils.getTemperatureAt(event.getBlock().getLocation());
                        if(temp.isNaN()) return;
                        if(!ClimateUtils.isAcceptableTemperature(temp, Temperature.fromCelsius(16.5), Temperature.fromCelsius(5), Temperature.fromCelsius(35))){
                            event.setCancelled(true);
                            return;
                        }
                        break;
                    case POTATO:
                        temp = ClimateUtils.getTemperatureAt(event.getBlock().getLocation());
                        if(temp.isNaN()) return;
                        if(!ClimateUtils.isAcceptableTemperature(temp, Temperature.fromCelsius(17.5), Temperature.fromCelsius(5), Temperature.fromCelsius(30))){
                            event.setCancelled(true);
                            return;
                        }
                        break;
                    case NETHER_WART_BLOCK:
                        temp = ClimateUtils.getTemperatureAt(event.getBlock().getLocation());
                        if(temp.isNaN() || event.getBlock().getBiome() == Biome.NETHER) return;
                        if(!ClimateUtils.isAcceptableTemperature(temp,
                                Temperature.fromCelsius(60), Temperature.fromCelsius(40), Temperature.fromCelsius(100))){
                            event.setCancelled(true);
                            return;
                        }
                        break;
                    default:
                        break;
				}
			} else {
				ClimateCell c = null;
				Temperature temp = null;
				switch (event.getBlock().getType()) {
				case SUGAR_CANE:
					c = ClimateUtils.getClimateCellAt(event.getBlock().getLocation());
					temp = ClimateUtils.getAltitudeWeightedTemperature(event.getBlock().getLocation());
					if(temp.isNaN()) return;
					if(!ClimateUtils.isAcceptableTemperature(
							temp, Temperature.fromCelsius(32),
							Temperature.fromCelsius(25), Temperature.fromCelsius(38))){
						event.setCancelled(true);
						return;
					}
					break;
				case CACTUS:
					c = ClimateUtils.getClimateCellAt(event.getBlock().getLocation());
					temp = ClimateUtils.getAltitudeWeightedTemperature(event.getBlock().getLocation());
					if(temp.isNaN()) return;
					double tempDistance = temp.getValue() - (273.15 + 35);
					if (Math.random() * Math.abs(tempDistance) > 1) {
						event.setCancelled(true);
						return;
					}
					break;
				default:
					break;
				}
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event){
		if(usingVegetation){
			if(Utils.isCrop(event.getBlock().getType())){
				vegetation.onBreakCrops(event);
			}
		}
		if(usingGravity && event.getBlock().getLocation().getBlockY() < 255) {
			gravity.checkGravityOn(event.getBlock().getRelative(BlockFace.UP));
		}
	}
	
	@EventHandler
	public void onEntityChangeBlockEvent(EntityChangeBlockEvent event){
		if(usingGravity){
			if(event.getEntityType() == EntityType.FALLING_BLOCK) gravity.checkGravityOn(event.getBlock());
		}
		if(event.getBlock().getType() == Material.FARMLAND && event.getTo() == Material.DIRT){
			event.setCancelled(true);
			return;
		}
	}

	public ClimateModule getClimateModule() {
		return climate;
	}
	
	public static LivelyWorld getInstance(){
		return currentInstance;
	}

	public Database getDatabase() {
		return database;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public int getOceanY() {
		return 48;
	}

	public WaterModule getWaterModule() {
		return tides;
	}

	public long getMainThreadId() {
		return mainThreadId ;
	}

	public GravityModule getGravityModule() {
		return gravity;
	}

	public VegetationModule getVegetationModule() {
		return vegetation;
	}

	public static boolean allowsTasks(){
	    return allowTasks;
    }
}
