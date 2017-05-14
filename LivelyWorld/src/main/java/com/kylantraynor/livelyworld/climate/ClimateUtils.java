package com.kylantraynor.livelyworld.climate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class ClimateUtils {

	static double R = 0.083144598;
	static double invertedR = 1 / R;

	public static Temperature getGasTemperature(double pressure, double volume,
			long amount) {
		return new Temperature(0.000001 * (pressure * 100) * volume * (double)(1.0d / amount)
				* invertedR);
	}

	public static double getGasPressure(double volume, long amount,
			Temperature temperature) {
		return 1000000 * (amount * (long) temperature.getValue() * R * (1 / volume)) * 0.01;
	}

	public static long getGasAmount(double pressure, double volume,
			Temperature temperature) {
		return (long) ((pressure * 100) * (volume * 0.000001) * (1 / (R * temperature.getValue())));
	}
	
	public static Temperature getTemperatureAt(Location location){
		Planet planet = Planet.getPlanet(location.getWorld());
		if(planet != null){
			ClimateMap map = planet.getClimateMap(location.getWorld());
			ClimateCell cell = map.getClimateCellAt(location);
			if(cell != null){
				return cell.getTemperature();
			}
		}
		return Temperature.NaN;
	}
	
	public static boolean isSnow(Block block){
		if(block.getType() == Material.SNOW || block.getType() == Material.SNOW_BLOCK) return true;
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public static int getSnowLayers(Block block){
		if(!isSnow(block)) return 0;
		return block.getType() == Material.SNOW ? block.getData() + 1 : 8;
	}
	
	public static void setSnowLayers(Block block, int layers){
		if(layers == 0){
			block.setType(Material.AIR);
			block.setData((byte) 0);
		} else if(layers == 8){
			block.setType(Material.SNOW_BLOCK);
			block.setData((byte) 0);
		} else {
			block.setType(Material.SNOW);
			block.setData((byte) (layers - 1));
		}
	}

	public static boolean isWater(Block block) {
		return block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER;
	}
}