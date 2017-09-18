package com.kylantraynor.livelyworld.vegetation;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.CropState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Crops;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sapling;
import org.bukkit.material.Tree;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.climate.ClimateCell;
import com.kylantraynor.livelyworld.climate.ClimateChunk;
import com.kylantraynor.livelyworld.climate.ClimateMap;
import com.kylantraynor.livelyworld.climate.ClimateTriangle;
import com.kylantraynor.livelyworld.climate.ClimateUtils;
import com.kylantraynor.livelyworld.climate.Planet;
import com.kylantraynor.livelyworld.climate.Temperature;

public class VegetationModule implements Listener {

	private LivelyWorld plugin;
	private boolean debug = false;

	public VegetationModule(LivelyWorld plugin) {
		this.setPlugin(plugin);
	}

	public void onEnable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void onDisable() {

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
		while (b.getType() == Material.AIR) {
			if (b.getLocation().getBlockY() <= 1) {
				return;
			}
			b = b.getRelative(BlockFace.DOWN);
		}
		switch (b.getType()) {
		case SAPLING:
		case LOG:
		case LOG_2:
		case LEAVES:
		case LEAVES_2:
		case FENCE:
		case DARK_OAK_FENCE:
		case SPRUCE_FENCE:
		case ACACIA_FENCE:
		case BIRCH_FENCE:
		case JUNGLE_FENCE:
			updateTree(b);
			break;
		case GRASS:
			double rdm = Math.random();
			if (rdm <= 0.75) {
				if(rdm <= 0.5)
					updateGrass(b);
				else
					tryPlantFern(b.getRelative(BlockFace.UP));
			} else {
				updateFlower(b);
			}
			break;
		case LONG_GRASS:
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
		int rand = (int) (Math.random() * 3);
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
		default:

		}

		return;
	}

	private void tryPlantDandelion(Block b) {
		if (debug)
			Bukkit.getServer().getLogger().info("Trying to plant Dandelion");
		boolean isClimateOk = false;
		if (Planet.getPlanet(b.getWorld()) != null) {
			ClimateChunk c = ClimateChunk.getAt(b.getLocation());
			Temperature averageTemp = c.getAverageTemperature();
			isClimateOk = (averageTemp.getValue() > 15.0 + 273.15 && averageTemp
					.getValue() < 25.0 + 273.15);
		}
		if (isClimateOk) {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate is Ok");
			if (isMaterialBelow(b.getLocation(), Material.STONE, (byte) 0, 10)) {
				if (debug)
					Bukkit.getServer().getLogger()
							.info("Found right material underground.");
				b.setType(Material.YELLOW_FLOWER);
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
			ClimateChunk c = ClimateChunk.getAt(b.getLocation());
			Temperature averageTemp = c.getAverageTemperature();
			isClimateOk = (averageTemp.getValue() > 10.0 + 273.15 && averageTemp
					.getValue() < 15.0 + 273.15);
		}
		if (isClimateOk) {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate is Ok");
			if (isMaterialBelow(b.getLocation(), Material.STONE, (byte) 1, 7)) {
				if (debug)
					Bukkit.getServer().getLogger()
							.info("Found right material underground.");
				b.setType(Material.RED_ROSE);
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
			Temperature averageTemp = ClimateUtils.getAltitudeWeightedTriangleTemperature(c, b.getLocation());
			isClimateOk = ClimateUtils.isAcceptableTemperature(averageTemp,
					Temperature.fromCelsius(17),
					Temperature.fromCelsius(10),
					Temperature.fromCelsius(30));
		}
		if (isClimateOk) {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate is Ok");
			if (isMaterialBelow(b.getLocation(), Material.STONE, (byte) 3, 7)) {
				if (debug)
					Bukkit.getServer().getLogger()
							.info("Found right material underground.");
				b.setType(Material.RED_ROSE);
				b.setData((byte) 8);
			}
		} else {
			if (debug)
				Bukkit.getServer().getLogger().info("Climate isn't Ok");
		}
	}

	private boolean isMaterialBelow(Location l, Material m, byte data, int depth) {
		int i = 1;
		Location currentLocation = l.clone();

		while (i < depth && currentLocation.getBlockY() > 0) {
			currentLocation.add(0, -1, 0);
			if (currentLocation.getBlock().getType() == m) {
				if (currentLocation.getBlock().getData() == data)
					return true;
			}
			i++;
		}
		return false;
	}

	private void updateGrass(Block b) {
		Block above = b.getRelative(BlockFace.UP);
		if (above.getType() == Material.AIR && above.getLightLevel() > 5) {
			above.setType(Material.LONG_GRASS);
			above.setData((byte) 1);
		} else if (above.getType() == Material.LONG_GRASS && above.getData() == 1
				&& above.getLightLevel() > 13) {
			above.setType(Material.DOUBLE_PLANT);
			above.setData((byte) 2);
			Block top = above.getRelative(BlockFace.UP);
			top.setType(Material.DOUBLE_PLANT);
			top.setData((byte) 10);
		} else if (above.getType() == Material.LONG_GRASS && above.getData() == 2
				&& above.getLightLevel() > 13){
			above.setType(Material.DOUBLE_PLANT);
			above.setData((byte) 3);
			Block top = above.getRelative(BlockFace.UP);
			top.setType(Material.DOUBLE_PLANT);
			top.setData((byte) 10);
		} else if (b.getLightFromSky() <= 1) {
			b.setType(Material.DIRT);
		}
	}
	
	private void tryPlantFern(Block b){
		if(b.getType() != Material.AIR) return;
		if(b.getLightFromSky() < 5) return;
		boolean isClimateOk = ClimateUtils.getTemperatureAt(b.getLocation()).isCelsiusBetween(
				10, 
				25);
		if(isClimateOk){
			b.setType(Material.LONG_GRASS);
			b.setData((byte) 2);
		}
		
	}

	private void updateTree(Block b) {

	}

	public void plantSapling(MaterialData data, Location location) {
		if(location.getBlock().getType().isBlock() || location.getBlock().getType().isSolid()) return;
		if(location.getBlock().getLightFromSky() < 12) return;
		if (data != null && data instanceof Sapling) {
			Material base = location.getBlock().getRelative(BlockFace.DOWN)
					.getType();
			if (base == Material.DIRT || base == Material.GRASS) {
				location.getBlock().setType(Material.SAPLING);
				location.getBlock().setData(((Sapling) data).getData(), true);
			}

		} else {
			plugin.log(Level.INFO, "Couldn't plant sapling.");
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
			case CROPS:
				plugin.getLogger().info("Processing Wheat Breaking.");
				if(crops.getState() == CropState.RIPE){
					ItemStack is = new ItemStack(Material.WHEAT, 15);
					event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
				}
				break;
			case NETHER_WARTS:
				plugin.getLogger().info("Processing Nether Warts Breaking.");
				if(crops.getState() == CropState.RIPE){
					ItemStack is = new ItemStack(Material.NETHER_WARTS, 10);
					event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
				}
				break;
			case CARROT:
				break;
			case POTATO:
				break;
			default:
				plugin.getLogger().info("Unexpected type of crop: " + crops.getItemType().toString());
				break;
			}
		} else {
			plugin.getLogger().info("Crops block wasn't instance of Crops?");
		}
	}
}
