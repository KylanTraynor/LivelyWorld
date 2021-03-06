package com.kylantraynor.livelyworld.climate;

import com.kylantraynor.livelyworld.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import com.kylantraynor.livelyworld.LivelyWorld;

public class Climate {

	public static double getHumidity(Material m) {
		if(Utils.isTerracota(m) || Utils.isConcretePowder(m)) return -0.2;
		if(Utils.isLeaves(m)) return 0.4;
		switch (m) {
			case SAND:
			case STONE:
			case SANDSTONE:
			case RED_SANDSTONE:
				return -0.2;
			case SNOW:
				return 0.3;
			case GRASS_BLOCK:
				return 0.4;
			case TALL_GRASS:
			case GRASS:
			case DANDELION:
			case POPPY:
			case OXEYE_DAISY:
				return 0.4;
			case WATER:
				return 1;
			default:
				return -0.01;
		}
	}

	public static Temperature getMaxTemperatureFor(Material m) {
		double temp;
		switch (m) {
			case PACKED_ICE:
				temp = -40;
				break;
			case ICE:
			case FROSTED_ICE:
				temp = 0;
				break;
			case SNOW:
			case SNOW_BLOCK:
				temp = 0;
				break;
			case WATER:
				temp = 35;
				break;
			case TALL_GRASS:
			case GRASS:
			case DANDELION:
			case POPPY:
			case OXEYE_DAISY:
				temp = 25;
				break;
			case GRASS_BLOCK:
				temp = 35;
				break;
			case DIRT:
				temp = 40;
				break;
			case COBBLESTONE:
			case GRAVEL:
			case COBBLESTONE_STAIRS:
				temp = 55;
				break;
			case SAND:
			case STONE:
			case STONE_BRICKS:
			case STONE_SLAB: case STONE_BRICK_SLAB:
			case SANDSTONE:
			case RED_SANDSTONE:
			case SANDSTONE_SLAB: case RED_SANDSTONE_SLAB:
				temp = 60;
				break;
			case IRON_BLOCK:
			case GOLD_BLOCK:
				temp = 120;
				break;
			case FIRE:
			case TORCH:
				temp = 500;
				break;
			case LAVA:
				temp = 1400;
				break;
			default:
				temp = 27;
				break;
		}
		if(Utils.isLeaves(m)) temp = 25;
		if(Utils.isTerracota(m)) temp = 60;
		return Temperature.fromCelsius(temp);
	}

	public static Temperature getInertialTemperatureFor(Material m) {
		double temp;
		switch (m) {
			case PACKED_ICE:
				temp = -70;
				break;
			case SNOW:
			case SNOW_BLOCK:
			case ICE:
			case FROSTED_ICE:
				temp = -30;
				break;
			case WATER:
				temp = 0;
				break;
			case TALL_GRASS:
			case GRASS:
			case DANDELION:
			case POPPY:
				temp = 0;
				break;
			case GRASS_BLOCK:
				temp = 0;
				break;
			case DIRT:
				temp = -5;
				break;
			case COBBLESTONE:
			case GRAVEL:
			case FURNACE:
			case COBBLESTONE_STAIRS:
				temp = 0;
				break;
			case SAND:
			case STONE:
			case STONE_BRICKS:
			case STONE_SLAB:
			case STONE_BRICK_SLAB:
			case SANDSTONE:
			case RED_SANDSTONE:
			case SANDSTONE_SLAB: case RED_SANDSTONE_SLAB:
				temp = 0;
				break;
			case FIRE:
			case TORCH:
				temp = 500;
				break;
			case LAVA:
				temp = 1400;
				break;
			default:
				temp = 15;
				break;
		}
		if(Utils.isLeaves(m)) temp = 0;
		if(Utils.isTerracota(m)) temp = 0;
		return Temperature.fromCelsius(temp);
	}

	Location location;

	public Climate(Location l) {
		this.location = l;
	}

	public Planet getPlanet() {
		return Planet.getPlanet(location.getWorld());
	}

	public boolean isHeatSource() {
		return isHeatSource(location.getBlock().getType());
	}
	
	public static boolean isHeatSource(Material mat){
	    switch(mat){
            case FIRE:
            case TORCH:
            case LAVA:
                return true;
        }
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
			if(location.getBlock().getType() == Material.AIR){
				temp += ClimateUtils.getTemperatureAt(location).getValue();
			} else {
				temp += (tMax - tMin) * getPlanet().getSunRadiation(location.clone().add(0, 1, 0));
			}
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
		return getAreaTemperatureFor(location);
	}
	
	public static Temperature getAreaTemperatureFor(Location location){
		double temp = 0;
		for (int x = location.getBlockX() - 8; x <= location.getBlockX() + 8; x++) {
			for (int y = location.getBlockY() - 8; y <= location.getBlockY() + 8; y++) {
				for (int z = location.getBlockZ() - 8; z <= location
						.getBlockZ() + 8; z++) {
					double dx = location.getX() - (x + 0.5);
					double dy = location.getY() - (y + 0.5);
					double dz = location.getZ() - (z + 0.5);
					double distanceSquared = dx * dx + dy * dy + dz * dz;
					if(distanceSquared == 0){
						temp += (Climate.getTemperatureFor(location.getWorld().getBlockAt(x, y, z).getType(), location.getWorld(), x, y, z, true).value - temp);
					} else {
						temp += (Climate.getTemperatureFor(location.getWorld().getBlockAt(x, y, z).getType(), location.getWorld(), x, y, z, true).value - temp)
								/ (distanceSquared);
					}
				}
			}
		}
		return new Temperature(temp);
	}
	
	public static Temperature getTemperatureFor(Material type, World w, int x, int y, int z, boolean shaded){
		double tMax = Climate.getMaxTemperatureFor(type).getValue();
		double tMin = Climate.getInertialTemperatureFor(type).getValue();
		if(type == Material.AIR){
			tMax = ClimateUtils.getTemperatureAt(new Location(w, x, y, z)).getValue();
		}
		if (isHeatSource(type)) {
			return new Temperature(tMax);
		} else {
			Planet p = Planet.getPlanet(w);
			if(p == null ) return new Temperature(tMax);
			double temp = tMin;
			if(shaded){
				temp += (tMax - tMin) * p.getSunRadiation(new Location(w, x, y + 1, z));
			} else {
				if(type == Material.AIR){
					temp += (tMax - tMin);
				} else {
					temp += (tMax - tMin) * p.getSunDirectRadiation(w, x, y + 1, z);
				}
			}
			return new Temperature(temp);
		}
	}
	
	public Temperature getAreaSurfaceTemperature(){
		return getAreaSurfaceTemperature(location.getWorld(), location.getBlockX(), location.getBlockZ());
		/*double temp = 0;
		int count = 0;
		for(int x = location.getBlockX() - 8; x <= location.getBlockX() + 8; x++){
			for(int z = location.getBlockZ() - 8; z <= location.getBlockZ() + 8; z++){
				Material mat = LivelyWorld.getInstance().getHighestMaterial(location.getWorld(), x, z);
				if(mat == null) continue;
				temp += Climate.getTemperatureFor(mat, location.getWorld(), x, 0, z, false).getValue();
				//temp += (new Climate(b.getLocation()).getTemperature().value);
				count++;
			}
		}
		return new Temperature(temp/count);*/
	}
	
	public static Temperature getAreaSurfaceMinTemperature(World w, int blockX, int blockZ){
		double temp = 0;
		int count = 0;
		for(int x = blockX - 8; x <= blockX + 8; x++){
			for(int z = blockZ - 8; z <= blockZ + 8; z++){
				Material mat = Utils.getHighestMaterial(w, x, z);
				if(mat == null) continue;
				//int y = w.getHighestBlockYAt(x, z);
				temp += Climate.getInertialTemperatureFor(mat).getValue();
				//temp += Climate.getTemperatureFor(mat, w, x, 0, z, false).getValue();
				//temp += (new Climate(b.getLocation()).getTemperature().value);
				count++;
			}
		}
		return new Temperature(temp/count);
	}
	
	public static Temperature getAreaSurfaceMaxTemperature(World w, int blockX, int blockZ){
		double temp = 0;
		int count = 0;
		for(int x = blockX - 8; x <= blockX + 8; x++){
			for(int z = blockZ - 8; z <= blockZ + 8; z++){
				Material mat = Utils.getHighestMaterial(w, x, z);
				if(mat == null) continue;
				temp += Climate.getMaxTemperatureFor(mat).getValue();
				//temp += Climate.getTemperatureFor(mat, w, x, 0, z, false).getValue();
				//temp += (new Climate(b.getLocation()).getTemperature().value);
				count++;
			}
		}
		return new Temperature(temp/count);
	}
	
	public static Temperature getAreaSurfaceTemperature(World w, int blockX, int blockZ){
		double temp = 0;
		int count = 0;
		for(int x = blockX - 8; x <= blockX + 8; x++){
			for(int z = blockZ - 8; z <= blockZ + 8; z++){
				Material mat = Utils.getHighestMaterial(w, x, z);
				if(mat == null) continue;
				temp += Climate.getTemperatureFor(mat, w, x, 0, z, false).getValue();
				//temp += (new Climate(b.getLocation()).getTemperature().value);
				count++;
			}
		}
		return new Temperature(temp/count);
	}

	public Temperature getAreaAverageTemperature() {
		double temp = 0;
		for (int x = location.getBlockX() - 8; x <= location.getBlockX() + 8; x++) {
			for (int y = location.getBlockY() - 8; y <= location.getBlockY() + 8; y++) {
				for (int z = location.getBlockZ() - 8; z <= location
						.getBlockZ() + 8; z++) {
					Block b = getWorld().getBlockAt(x, y, z);
					double distanceSquared = b.getLocation().add(0.5, 0.5, 0.5)
							.distanceSquared(location);
					temp += (new Climate(b.getLocation())
							.getAverageTemperature().value - temp)
							/ (distanceSquared);
				}
			}
		}
		return new Temperature(temp);
	}

	public static double getSurfaceHumidityGeneration(World world, int blockX, int blockZ) {
		double humidity = 0;
		int count = 0;
		for(int x = blockX - 8; x <= blockX + 8; x++){
			for(int z = blockZ - 8; z <= blockZ + 8; z++){
				Material mat = Utils.getHighestMaterial(world, x, z);
				if(mat == null) continue;
				humidity += Climate.getHumidity(mat);
				//temp += Climate.getTemperatureFor(mat, w, x, 0, z, false).getValue();
				//temp += (new Climate(b.getLocation()).getTemperature().value);
				count++;
			}
		}
		return humidity/count;
	}
}
