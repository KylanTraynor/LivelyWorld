package com.kylantraynor.livelyworld.climate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

public class Climate {

	public static double getHumidity(Material m) {
		switch (m) {
		case COBBLESTONE:
		case GRAVEL:
		case SAND:
		case STONE:
		case SMOOTH_BRICK:
		case DOUBLE_STEP:
		case STEP:
		case SANDSTONE:
		case RED_SANDSTONE:
		case STAINED_CLAY:
			return 0.0;
		case SNOW:
			return 40;
		case GRASS:
			return 65.0;
		case LEAVES:
		case LEAVES_2:
			return 75.0;
		case WATER:
		case STATIONARY_WATER:
			return 100.0;
		default:
			return 50.0;
		}
	}

	public static Temperature getMaxTemperatureFor(Material m) {
		double temp = 0;
		switch (m) {
		case PACKED_ICE:
			temp = 220;
			break;
		case ICE:
		case FROSTED_ICE:
			temp = 273.15;
			break;
		case SNOW:
			temp = 275;
			break;
		case WATER:
		case STATIONARY_WATER:
			temp = 300;
			break;
		case LEAVES:
		case LEAVES_2:
			temp = 300;
			break;
		case GRASS:
			temp = 305;
			break;
		case DIRT:
			temp = 310;
			break;
		case COBBLESTONE:
		case GRAVEL:
		case FURNACE:
		case COBBLESTONE_STAIRS:
			temp = 325;
			break;
		case SAND:
		case STONE:
		case SMOOTH_BRICK:
		case DOUBLE_STEP:
		case STEP:
		case SANDSTONE:
		case RED_SANDSTONE:
		case STAINED_CLAY:
			temp = 330;
			break;
		case FIRE:
		case TORCH:
		case BURNING_FURNACE:
			temp = 1200;
			break;
		case LAVA:
		case STATIONARY_LAVA:
			temp = 1400;
			break;
		default:
			temp = 300.15;
			break;
		}
		return new Temperature(temp);
	}

	public static Temperature getInertialTemperatureFor(Material m) {
		double temp = 0;
		switch (m) {
		case PACKED_ICE:
			temp = 175;
		case SNOW:
		case ICE:
		case FROSTED_ICE:
			temp = 210;
			break;
		case WATER:
		case STATIONARY_WATER:
			temp = 274;
			break;
		case LEAVES:
		case LEAVES_2:
			temp = 280;
			break;
		case GRASS:
			temp = 280;
			break;
		case DIRT:
			temp = 288;
			break;
		case COBBLESTONE:
		case GRAVEL:
		case FURNACE:
		case COBBLESTONE_STAIRS:
			temp = 288;
			break;
		case SAND:
		case STONE:
		case SMOOTH_BRICK:
		case DOUBLE_STEP:
		case STEP:
			temp = 290;
			break;
		case FIRE:
		case TORCH:
		case BURNING_FURNACE:
			temp = 1200;
			break;
		case LAVA:
		case STATIONARY_LAVA:
			temp = 1400;
			break;
		default:
			temp = 288.15;
			break;
		}
		return new Temperature(temp);
	}

	Location location;

	public Climate(Location l) {
		this.location = l;
	}

	public Planet getPlanet() {
		return Planet.getPlanet(location.getWorld());
	}

	public boolean isHeatSource() {
		if (location.getBlock().getType() == Material.FIRE)
			return true;
		if (location.getBlock().getType() == Material.TORCH)
			return true;
		if (location.getBlock().getType() == Material.LAVA)
			return true;
		if (location.getBlock().getType() == Material.STATIONARY_LAVA)
			return true;
		if (location.getBlock().getType() == Material.BURNING_FURNACE)
			return true;
		return false;
	}

	public Temperature getAverageTemperature() {
		double tMax = Climate.getMaxTemperatureFor(
				location.getBlock().getType()).getValue();
		double tMin = Climate.getInertialTemperatureFor(
				location.getBlock().getType()).getValue();
		if (isHeatSource()) {
			return new Temperature(tMax);
		} else {
			double temp = tMin;
			temp += (tMax - tMin)
					* getPlanet().getSunAverageRadiation(location);
			return new Temperature(temp);
		}
	}

	public Temperature getTemperature() {
		double tMax = Climate.getMaxTemperatureFor(
				location.getBlock().getType()).getValue();
		double tMin = Climate.getInertialTemperatureFor(
				location.getBlock().getType()).getValue();
		if (isHeatSource()) {
			return new Temperature(tMax);
		} else {
			double temp = tMin;
			temp += (tMax - tMin) * getPlanet().getSunRadiation(location);
			return new Temperature(temp);
		}
	}

	public double getHumidity() {
		if (location.getBlock().getBiome() == Biome.RIVER)
			return 100.0;
		return Climate.getHumidity(location.getBlock().getType());
	}

	public World getWorld() {
		return location.getWorld();
	}

	public Temperature getAreaTemperature() {
		double temp = 0;
		for (int x = location.getBlockX() - 8; x <= location.getBlockX() + 8; x++) {
			for (int y = location.getBlockY() - 8; y <= location.getBlockY() + 8; y++) {
				for (int z = location.getBlockZ() - 8; z <= location
						.getBlockZ() + 8; z++) {
					Block b = getWorld().getBlockAt(x, y, z);
					double distance = b.getLocation().add(0.5, 0.5, 0.5)
							.distance(location);
					temp += (new Climate(b.getLocation()).getTemperature().value - temp)
							/ (distance * distance);
				}
			}
		}
		return new Temperature(temp);
	}

	public Temperature getAreaAverageTemperature() {
		double temp = 0;
		for (int x = location.getBlockX() - 8; x <= location.getBlockX() + 8; x++) {
			for (int y = location.getBlockY() - 8; y <= location.getBlockY() + 8; y++) {
				for (int z = location.getBlockZ() - 8; z <= location
						.getBlockZ() + 8; z++) {
					Block b = getWorld().getBlockAt(x, y, z);
					double distance = b.getLocation().add(0.5, 0.5, 0.5)
							.distance(location);
					temp += (new Climate(b.getLocation())
							.getAverageTemperature().value - temp)
							/ (distance * distance);
				}
			}
		}
		return new Temperature(temp);
	}
}
