package com.kylantraynor.livelyworld.pathways;

import com.kylantraynor.livelyworld.deterioration.DeteriorationCause;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Snow;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Stairs;
import org.bukkit.material.Step;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.events.BlockDeteriorateEvent;

public class PathwaysModule {

	private LivelyWorld plugin;

	// Default multiplier for horses
	public double horseMultiplier = 8;
	// Default multiplier when the player is running
	public double runningMultiplier = 2;
	// Default probability for grass to turn into dirt
	public double probabilityDirt = 0.1;
	// Default probability for dirt to turn into Coarse dirt
	public double probabilityCoarse = 0.05;
	// Default probability for dirt to turn into Coarse dirt
	public double probabilityPath = 0.025;
	// Default probability for coarse dirt to turn into gravel
	public double probabilityGravel = 0.025;
	// Default probability for gravel to turn into cobble
	public double probabilityCobble = 0.012;
	// Default probability for cobble to turn into cobblestone stairs
	public double probabilityCblStairs = 0.0001;
	// Default probability for stairs to turn into cobble slabs
	public double probabilityCblSlab = 0.00005;

	public void onEnable(LivelyWorld basePlugin) {
		setPlugin(basePlugin);
		loadConfigValues();
	}

	// Loads data from the config, and write default values
	public void loadConfigValues() {
		// Get configuration File (config.yml)
		YamlConfiguration cfg = (YamlConfiguration) plugin.getConfig();
		// If config file contains multipliers.sprint
		if (cfg.contains("pathways.multipliers.sprint")) {
			// then get the value
			runningMultiplier = cfg.getDouble("pathways.multipliers.sprint");
		} else {
			// else write the default value in the config file
			plugin.getConfig().set("pathways.multipliers.sprint",
					runningMultiplier);
		}
		// Same, but for horse multiplier
		if (cfg.contains("pathways.multipliers.horse")) {
			horseMultiplier = cfg.getDouble("pathways.multipliers.horse");
		} else {
			plugin.getConfig().set("pathways.multipliers.horse",
					horseMultiplier);
		}
		// Same, but for dirt probability
		if (cfg.contains("pathways.probabilities.dirt")) {
			probabilityDirt = cfg.getDouble("pathways.probabilities.dirt");
		} else {
			plugin.getConfig().set("pathways.probabilities.dirt",
					probabilityDirt);
		}
		// Same, but for coarse dirt probability
		if (cfg.contains("pathways.probabilities.coarse")) {
			probabilityCoarse = cfg.getDouble("pathways.probabilities.coarse");
		} else {
			plugin.getConfig().set("pathways.probabilities.coarse",
					probabilityCoarse);
		}
		// Same, but for coarse 1.9 Path probability
		if (cfg.contains("pathways.probabilities.path")) {
			probabilityPath = cfg.getDouble("pathways.probabilities.path");
		} else {
			plugin.getConfig().set("pathways.probabilities.path",
					probabilityPath);
		}
		// Same, but for gravel probability
		if (cfg.contains("pathways.probabilities.gravel")) {
			probabilityGravel = cfg.getDouble("pathways.probabilities.gravel");
		} else {
			plugin.getConfig().set("pathways.probabilities.gravel",
					probabilityGravel);
		}
		// Same, but for cobblestone probability
		if (cfg.contains("pathways.probabilities.cobble")) {
			probabilityCobble = cfg.getDouble("pathways.probabilities.cobble");
		} else {
			plugin.getConfig().set("pathways.probabilities.cobble",
					probabilityCobble);
		}
		// Same, but for stairs probability
		if (cfg.contains("pathways.probabilities.cblstairs")) {
			probabilityCblStairs = cfg
					.getDouble("pathways.probabilities.cblstairs");
		} else {
			plugin.getConfig().set("pathways.probabilities.cblstairs",
					probabilityCblStairs);
		}
		// Same, but for slabs probability
		if (cfg.contains("pathways.probabilities.cblslab")) {
			probabilityCblSlab = cfg
					.getDouble("pathways.probabilities.cblslab");
		} else {
			plugin.getConfig().set("pathways.probabilities.cblslab",
					probabilityCblSlab);
		}
		// Actually writes the changes in config.yml
		plugin.saveConfig();
	}

	public void onVehicleMove(VehicleMoveEvent event) {
		// If this vehicle is a horse
		if (event.getVehicle().getType() == EntityType.HORSE) {
			// If the movement was made from one block to another, and not just
			// within a block
			if (event.getFrom().getBlockX() != event.getTo().getBlockX()
					|| event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
				// Then get the block the movement started from
				Block b = event.getFrom().getBlock()
						.getRelative(BlockFace.DOWN);
				// Roll dice and change the block if necessary
				changeBlock(b, horseMultiplier);
			}
		}
	}

	public void onPlayerMove(PlayerMoveEvent event) {
		// Sets the default multiplier
		double multiplier = 1.0;
		// If the player is sneaking
		if (event.getPlayer().isSneaking()) {
			// then don't change any block
			return;
		} else if (event.getPlayer().isSprinting()) {
			// Else, if the player is running, change the multiplier
			multiplier = runningMultiplier;
		}
		// if the movement was actually from one block to another, and
		// not within one block
		if (event.getFrom().getBlockX() != event.getTo().getBlockX()
				|| event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
			// Get the block the movement started from
			if (event.getFrom().getBlock().getType().isSolid()) {
				changeBlock(event.getFrom().getBlock(), multiplier);
			} else {
				Block b = event.getFrom().getBlock()
						.getRelative(BlockFace.DOWN);
				changeBlock(b, multiplier);
			}
		}
	}

	// Roll a dice and change block if necessary
	@SuppressWarnings("deprecation")
	public void changeBlock(Block b, double multiplier) {
		if (b.getRelative(BlockFace.UP).getType() == Material.GRASS) {
			b.getRelative(BlockFace.UP).breakNaturally();
			b.getWorld().playSound(b.getLocation(), Sound.BLOCK_GRASS_STEP, 2,
					(float) ((Math.random() + 1) / 2));
		} else if (b.getRelative(BlockFace.UP).getType() == Material.SNOW) {
			Block snow = b.getRelative(BlockFace.UP);
			Snow sn = (Snow) snow.getBlockData();
			if (sn.getLayers() > 1) {
				sn.setLayers (sn.getLayers() - 1);
				snow.setBlockData(sn, false);
			} else {
				snow.breakNaturally();
			}
			snow.getWorld().playSound(snow.getLocation(),
					Sound.BLOCK_SNOW_BREAK, 1,
					(float) ((Utils.fastRandomDouble() + 1) / 2));
			return;
		}
		// If block is Grass
		if (b.getType() == Material.GRASS_BLOCK) {
			// then if a random double is bellow the probability for dirt to
			// appear
			if (Utils.fastRandomDouble() <= probabilityDirt * multiplier) {
				// Change the grass to dirt
				b.setType(Material.DIRT, false);
			}
		} else if (b.getType() == Material.DIRT) {
			// Same for coarse (coarse = dirt with data value of 1)
			if (Utils.fastRandomDouble() <= probabilityCoarse * multiplier) {
				if (b.getBiome() == Biome.FOREST
						|| b.getBiome() == Biome.JUNGLE) {
					b.setType(Material.PODZOL, false);
				} else {
					b.setType(Material.COARSE_DIRT, false);
				}
			}
		} else if (b.getType() == Material.COARSE_DIRT || b.getType() == Material.PODZOL) {
			if (b.getRelative(BlockFace.UP).getType() == Material.RAIL)
				return;
			// Same for grassPath
			if (Utils.fastRandomDouble() <= probabilityPath * multiplier) {
				b.setType(Material.GRASS_PATH, false);
			}
		} else if (b.getType() == Material.GRAVEL) {
			// Same for Cobblestone
			/*if (Math.random() <= probabilityCobble * multiplier) {
				b.setType(Material.COBBLESTONE);
			}*/
		} else if (b.getType() == Material.COBBLESTONE) {
			if (b.getRelative(BlockFace.UP).getType() == Material.RAIL)
				return;
			// Same for stairs
			if (Utils.fastRandomDouble() <= probabilityCblStairs * multiplier) {
				turnToCobbleStairs(b);
			}
		} else if (b.getType() == Material.COBBLESTONE_STAIRS) {
			// Same for slabs
			if (Utils.fastRandomDouble() <= probabilityCblStairs * multiplier) {
				turnToCobbleSlab(b);
			}
		} else if (b.getType() == Material.SNOW_BLOCK) {
			b.setType(Material.SNOW);
			Snow snow = (Snow) b.getBlockData();
			snow.setLayers(snow.getMaximumLayers());
			b.setBlockData(snow, false);
			b.getWorld().playSound(b.getLocation(), Sound.BLOCK_SNOW_BREAK, 1,
					(float) ((Utils.fastRandomDouble() + 1) / 2));
		}
	}
	
	private void turnToCobbleStairs(Block b){
		BlockDeteriorateEvent event = new BlockDeteriorateEvent(b, DeteriorationCause.Walking, Material.COBBLESTONE_STAIRS);
		Bukkit.getPluginManager().callEvent(event);
		
		if(!event.isCancelled()){
			b.setType(Material.COBBLESTONE_STAIRS, false);
			// Get the current state of the block
			BlockState state = b.getState();
			// Since the state has to be stairs, just get a stairs object
			Stairs stairs = (Stairs) state.getData();
			// For a value between 0 and 3...
			switch (Utils.fastRandomInt(4)) {
			case 0:
				// if it's 0, set the stairs to face north
				stairs.setFacingDirection(BlockFace.NORTH);
				break;
			case 1:
				// if it's 1, set the stairs to face east
				stairs.setFacingDirection(BlockFace.EAST);
				break;
			case 2:
				// if it's 2, set the stairs to face south
				stairs.setFacingDirection(BlockFace.SOUTH);
				break;
			case 3:
				// if it's 3, set the stairs to face west
				stairs.setFacingDirection(BlockFace.WEST);
				break;
			}
			// Update the data of the state
			state.setData(stairs);
			// Update the state of the block
			state.update(false, false);
		}
	}
	
	public void turnToCobbleSlab(Block b){
		BlockDeteriorateEvent event = new BlockDeteriorateEvent(b, DeteriorationCause.Walking, Material.COBBLESTONE_SLAB);
		Bukkit.getPluginManager().callEvent(event);
		
		if(!event.isCancelled()){
			b.setType(event.getTarget(), false);
		}
	}

	public LivelyWorld getPlugin() {
		return plugin;
	}

	public void setPlugin(LivelyWorld plugin) {
		this.plugin = plugin;
	}
}
