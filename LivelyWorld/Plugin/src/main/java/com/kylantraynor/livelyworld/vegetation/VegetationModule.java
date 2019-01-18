package com.kylantraynor.livelyworld.vegetation;

import org.bukkit.Bukkit;
import org.bukkit.CropState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Crops;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sapling;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.climate.ClimateCell;
import com.kylantraynor.livelyworld.climate.ClimateUtils;
import com.kylantraynor.livelyworld.climate.Planet;
import com.kylantraynor.livelyworld.climate.Temperature;
import com.kylantraynor.livelyworld.water.WaterChunk;

public class VegetationModule implements Listener {

	private LivelyWorld plugin;
	private boolean debug = false;
	private boolean isEnabled = false;

	public VegetationModule(LivelyWorld plugin) {
		this.setPlugin(plugin);
	}

	public void onEnable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		this.isEnabled = true;
	}

	public void onDisable() {
		this.isEnabled = false;
	}

	public LivelyWorld getPlugin() {
		return plugin;
	}

	public void setPlugin(LivelyWorld plugin) {
		this.plugin = plugin;
	}

	public void onBlockUpdate(Block b, Player p) {
		if (p != null) {
			if (b.getLocation().distanceSquared(p.getLocation()) < 25)
				return;
		}
		/*while (b.getType() == Material.AIR) {
			if (b.getLocation().getBlockY() <= 1) {
				return;
			}
			b = b.getRelative(BlockFace.DOWN);
		}*/
		switch (b.getType()) {
			case OAK_SAPLING:
			case DARK_OAK_SAPLING:
            case SPRUCE_SAPLING:
            case ACACIA_SAPLING:
            case BIRCH_SAPLING:
            case JUNGLE_SAPLING:
            case OAK_LOG:
            case DARK_OAK_LOG:
            case ACACIA_LOG:
            case SPRUCE_LOG:
            case BIRCH_LOG:
            case JUNGLE_LOG:
            case OAK_LEAVES:
            case DARK_OAK_LEAVES:
            case SPRUCE_LEAVES:
            case ACACIA_LEAVES:
            case BIRCH_LEAVES:
            case JUNGLE_LEAVES:
			case OAK_FENCE:
			case DARK_OAK_FENCE:
			case SPRUCE_FENCE:
			case ACACIA_FENCE:
			case BIRCH_FENCE:
			case JUNGLE_FENCE:
				updateTree(b);
				break;
			case GRASS_BLOCK:
				double rdm = Utils.fastRandomDouble();
				if (rdm <= 0.75) {
					if(rdm <= 0.5)
						updateGrass(b);
					else
						tryPlantFern(b.getRelative(BlockFace.UP));
				} else {
					updateFlower(b);
				}
				break;
			case GRASS:
				if(Math.random() < 0.1)
					updateGrass(b.getRelative(BlockFace.DOWN));
				break;
			default:
				break;
		}
	}

	private void updateFlower(Block b) {
		Block above = b.getRelative(BlockFace.UP);
		if (above.getType() != Material.AIR)
			return;
		int rand = Utils.fastRandomInt(4);
		switch (rand) {
		case 0:
			tryPlantDandelion(above);
			break;
		case 1:
			tryPlantPoppy(above);
			break;
		case 2:
			tryPlantOxeyeDaisy(above);
			break;
		case 3:
			tryPlantBlueOrchid(above);
			break;
		default:

		}

		return;
	}

	private void tryPlantBlueOrchid(Block b) {
		if (debug)
			Bukkit.getServer().getLogger().info("Trying to plant Blue Orchid");
		boolean isClimateOk = false;
		if (Planet.getPlanet(b.getWorld()) != null) {
			Temperature averageTemp = ClimateUtils.getAltitudeWeightedTemperature(b.getLocation());
			isClimateOk = ClimateUtils.isAcceptableTemperature(averageTemp, 
					Temperature.fromCelsius(21), 
					Temperature.fromCelsius(15), 
					Temperature.fromCelsius(35));
		}
		if (isClimateOk) {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate is Ok");
			if (isWaterLevelBelow(b.getLocation(), 0xFF / 8, 10)) {
				if (debug)
					Bukkit.getServer().getLogger()
							.info("Found enough water underground.");
				b.setType(Material.BLUE_ORCHID, false);
			}
		} else {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate isn't Ok");
		}
	}

	private void tryPlantDandelion(Block b) {
		if (debug)
			Bukkit.getServer().getLogger().info("Trying to plant Dandelion");
		boolean isClimateOk = false;
		if (Planet.getPlanet(b.getWorld()) != null) {
			Temperature averageTemp = ClimateUtils.getAltitudeWeightedTemperature(b.getLocation());
			isClimateOk = ClimateUtils.isAcceptableTemperature(averageTemp, 
					Temperature.fromCelsius(20), 
					Temperature.fromCelsius(15), 
					Temperature.fromCelsius(25));
		}
		if (isClimateOk) {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate is Ok");
			if (isMaterialBelow(b.getLocation(), Material.STONE, 10)) {
				if (debug)
					Bukkit.getServer().getLogger()
							.info("Found right material underground.");
				b.setType(Material.DANDELION, false);
			}
		} else {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate isn't Ok");
		}
	}

	private void tryPlantPoppy(Block b) {
		if (debug)
			Bukkit.getServer().getLogger().info("Trying to plant Poppy");
		boolean isClimateOk = false;
		if (Planet.getPlanet(b.getWorld()) != null) {
			Temperature averageTemp = ClimateUtils.getAltitudeWeightedTemperature(b.getLocation());
			isClimateOk = ClimateUtils.isAcceptableTemperature(averageTemp, 
					Temperature.fromCelsius(12), 
					Temperature.fromCelsius(10), 
					Temperature.fromCelsius(15));
		}
		if (isClimateOk) {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate is Ok");
			if (isMaterialBelow(b.getLocation(), Material.GRANITE, 7)) {
				if (debug)
					Bukkit.getServer().getLogger()
							.info("Found right material underground.");
				b.setType(Material.POPPY, false);
			}
		} else {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate isn't Ok");
		}
	}

	private void tryPlantOxeyeDaisy(Block b) {
		if (debug)
			Bukkit.getServer().getLogger().info("Trying to plant OxeyeDaisy");
		boolean isClimateOk = false;
		if (Planet.getPlanet(b.getWorld()) != null) {
			ClimateCell c = ClimateUtils.getClimateCellAt(b.getLocation());
			Temperature averageTemp = ClimateUtils.getAltitudeWeightedTemperature(b.getLocation());
			isClimateOk = ClimateUtils.isAcceptableTemperature(averageTemp,
					Temperature.fromCelsius(17),
					Temperature.fromCelsius(10),
					Temperature.fromCelsius(30));
		}
		if (isClimateOk) {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate is Ok");
			if (isMaterialBelow(b.getLocation(), Material.DIORITE, 7)) {
				if (debug)
					Bukkit.getServer().getLogger()
							.info("Found right material underground.");
				b.setType(Material.OXEYE_DAISY, false);
			}
		} else {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate isn't Ok");
		}
	}

	private boolean isMaterialBelow(Location l, Material m, int depth) {
		int i = 1;
		Location currentLocation = l.clone();

		while (i < depth && currentLocation.getBlockY() > 0) {
			currentLocation.add(0, -1, 0);
			if (currentLocation.getBlock().getType() == m) {
				return true;
			}
			i++;
		}
		return false;
	}
	
	private boolean isWaterLevelBelow(Location l, int level, int depth) {
		int i = 1;
		WaterChunk wc = WaterChunk.get(l.getWorld(), l.getBlockX() >> 4, l.getBlockZ() >> 4);
		int x = Utils.floorMod2(l.getBlockX(), 4);
		int z = Utils.floorMod2(l.getBlockZ(), 4);
		int y = l.getBlockY();
		while (i < depth && y > 0) {
			if(wc.getLevel(x, y, z) >= level){
				return true;
			}
			y--;
			i++;
		}
		return false;
	}

	private void updateGrass(Block b) {
		ClimateCell cell = ClimateUtils.getClimateCellAt(b.getLocation());
		if(cell != null){
			Temperature temp = ClimateUtils.getAltitudeWeightedTemperature(b.getLocation());
			boolean isClimateOk = ClimateUtils.isAcceptableTemperature(temp,
					Temperature.fromCelsius(25),
					Temperature.fromCelsius(10),
					Temperature.fromCelsius(35));
			if(!isClimateOk) return;
			if(cell.getRelativeHumidity() < Utils.fastRandomDouble()) return;
		}
		Block above = b.getRelative(BlockFace.UP);
		if (above.getType() == Material.AIR && above.getLightLevel() > 5) {
			above.setType(Material.GRASS, false);
		} else if (above.getType() == Material.GRASS
				&& above.getLightLevel() > 13 && above.getRelative(BlockFace.UP).getType() == Material.AIR) {
			above.setType(Material.TALL_GRASS, false);
            Bisected bs = (Bisected) above.getBlockData();
            bs.setHalf(Bisected.Half.BOTTOM);
            above.setBlockData(bs, false);
			Block top = above.getRelative(BlockFace.UP);
			top.setType(Material.TALL_GRASS, false);
            bs = (Bisected)top.getBlockData();
            bs.setHalf(Bisected.Half.TOP);
            top.setBlockData(bs, false);
		} else if (above.getType() == Material.FERN
				&& above.getLightLevel() > 13 && above.getRelative(BlockFace.UP).getType() == Material.AIR){
			above.setType(Material.LARGE_FERN, false);
            Bisected bs = (Bisected) above.getBlockData();
            bs.setHalf(Bisected.Half.BOTTOM);
            above.setBlockData(bs, false);
			Block top = above.getRelative(BlockFace.UP);
			top.setType(Material.LARGE_FERN, false);
            bs = (Bisected) top.getBlockData();
            bs.setHalf(Bisected.Half.BOTTOM);
            top.setBlockData(bs, false);
		} else if (b.getLightFromSky() <= 1) {
			b.setType(Material.DIRT, true);
		}
	}
	
	private void tryPlantFern(Block b){
		if(b.getType() != Material.AIR) return;
		if(b.getLightFromSky() < 5) return;
		Temperature temp = ClimateUtils.getAltitudeWeightedTemperature(b.getLocation());
		boolean isClimateOk = ClimateUtils.isAcceptableTemperature(temp,
				Temperature.fromCelsius(20),
				Temperature.fromCelsius(10),
				Temperature.fromCelsius(25));
		if(isClimateOk){
			b.setType(Material.FERN);
		}
		
	}

	private void updateTree(Block b) {

	}

	public void plantSapling(Material sap, Location location) {
		if(location.getBlock().getType().isBlock() || location.getBlock().getType().isSolid()) return;
		if(location.getBlock().getLightFromSky() < 12) return;
        Material base = location.getBlock().getRelative(BlockFace.DOWN)
                .getType();
        if (base == Material.DIRT || base == Material.COARSE_DIRT || base == Material.PODZOL || base == Material.GRASS_BLOCK) {
            location.getBlock().setType(sap);
        }
	}

	public void onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (args.length >= 2) {
			switch (args[1].toUpperCase()) {
			case "TOGGLE":
				if (args.length >= 3) {
					switch (args[2].toUpperCase()) {
					case "DEBUG":
						this.debug = !this.debug;
						if (this.debug) {
							sender.sendMessage("Debug is now turned on for the Vegetation Module!");
						} else {
							sender.sendMessage("Debug is now turned off for the Vegetation Module!");
						}
					}
				}
			}
		}
	}

	public void onBreakCrops(BlockBreakEvent event) {
		plugin.getLogger().info("Processing Crops Breaking.");
		BlockState state = event.getBlock().getState();
		if(state.getData() instanceof Crops){
			Crops crops = (Crops) state.getData();
			switch(crops.getItemType()){
			case WHEAT:
				plugin.getLogger().info("Processing Wheat Breaking.");
				if(crops.getState() == CropState.RIPE){
					ItemStack is = new ItemStack(Material.WHEAT, 15);
					event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
				}
				break;
			case NETHER_WART_BLOCK:
				plugin.getLogger().info("Processing Nether Warts Breaking.");
				if(crops.getState() == CropState.RIPE){
					ItemStack is = new ItemStack(Material.NETHER_WART, 10);
					event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
				}
				break;
			case CARROTS:
                plugin.getLogger().info("Processing Carrots Breaking.");
                if(crops.getState() == CropState.RIPE){
                    ItemStack is = new ItemStack(Material.CARROT, 6);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
                }
				break;
			case POTATOES:
                plugin.getLogger().info("Processing Potatoes Breaking.");
                if(crops.getState() == CropState.RIPE){
                    ItemStack is = new ItemStack(Material.POTATO, 6);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
                }
				break;
			default:
				plugin.getLogger().info("Unexpected type of crop: " + crops.getItemType().toString());
				break;
			}
		} else {
			plugin.getLogger().info("Crops block wasn't instance of Crops?");
		}
	}

	public boolean isEnabled() {
		return isEnabled ;
	}
}
