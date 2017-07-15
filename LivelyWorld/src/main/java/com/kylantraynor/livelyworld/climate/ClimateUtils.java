package com.kylantraynor.livelyworld.climate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.kylantraynor.livelyworld.LivelyWorld;

public class ClimateUtils {

	static double R = 0.083144598;
	static double invertedR = 1 / R;

	public static Temperature getGasTemperature(double pressure, double volume,
			double amount) {
		return new Temperature((pressure * 100) * volume * (1.0 / amount)
				* invertedR);
	}

	public static double getGasPressure(double volume, double amount,
			Temperature temperature) {
		return (amount * temperature.getValue() * R * (1.0 / volume)) * 0.01;
	}

	public static double getGasAmount(double pressure, double volume,
			Temperature temperature) {
		return ((pressure * 100) * (volume) * (1 / (R * temperature.getValue())));
	}
	
	public static Temperature getTemperatureAt(Location location){
		Planet planet = Planet.getPlanet(location.getWorld());
		if(planet != null){
			ClimateMap map = planet.getClimateMap(location.getWorld());
			if(map == null) return Temperature.NaN;
			return map.getTemperatureAt(location);
		}
		return Temperature.NaN;
	}
	
	public static ClimateCell getClimateCellAt(Location location){
		Planet planet = Planet.getPlanet(location.getWorld());
		if(planet == null) return null;
		ClimateMap map = planet.getClimateMap(location.getWorld());
		if(map == null) return null;
		return map.getClimateCellAt(location);
	}
	
	public static ClimateCell getClimateCellFor(Player p){
		return LivelyWorld.getInstance().getClimateModule().getClimateCellFor(p);
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
		int oldLayers = getSnowLayers(block);
		if(layers == 0){
			block.setType(Material.AIR);
			block.setData((byte) 0);
		} else if(layers == 8){
			block.setType(Material.SNOW_BLOCK);
			block.setData((byte) 0);
		} else {
			block.setType(Material.SNOW);
			block.setData((byte) (layers - 1));
			if(block.getRelative(BlockFace.DOWN).getType() == Material.GRASS){
				block.getRelative(BlockFace.DOWN).setType(Material.DIRT);
			}
		}
		/*if(oldLayers < layers && Math.random() < 0.1){
			updateGlacier(block);
		}*/
	}
	
	public static void updateGlacier(Block block){
		int depth = 0;
		while(block.getType() == Material.SNOW || block.getType() == Material.SNOW_BLOCK || block.getType() == Material.FROSTED_ICE || block.getType() == Material.ICE || block.getType() == Material.PACKED_ICE){
			block = block.getRelative(BlockFace.DOWN);
			depth++;
		}
		while(depth > 2)
		{
			block = block.getRelative(BlockFace.UP);
			depth--;
			if(block.getType() == Material.ICE && depth > 10){
				block.setType(Material.PACKED_ICE);
			} else if(block.getType() == Material.FROSTED_ICE && depth > 5) {
				block.setType(Material.ICE);
			} else if (block.getType() == Material.SNOW_BLOCK && depth > 2){
				block.setType(Material.FROSTED_ICE);
			}
		}
	}

	public static boolean isWater(Block block) {
		return block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER;
	}

	public static void melt(Block b) {
		if(isSnow(b)){
			if(getSnowLayers(b) > 1){
				setSnowLayers(b, getSnowLayers(b) - 1);
			} else {
				setWaterHeight(b, getSnowLayers(b), false);
			}
		} else if(b.getType() == Material.FROSTED_ICE){
			
		} else if(b.getType() == Material.ICE){
			b.setType(Material.FROSTED_ICE);
		} else if(b.getType() == Material.PACKED_ICE){
			b.setType(Material.ICE);
		}
	}
	
	public static void setWaterHeight(Block b, int height, boolean canSource){
		if(height == 0){
			b.setType(Material.AIR);
			b.setData((byte)0);
		} else if(height == 8){
			b.setType(Material.WATER);
			if(canSource){
				b.setData((byte)0);
			} else {
				b.setData((byte) 8);
			}
		} else {
			b.setType(Material.WATER);
			b.setData((byte) (8 - height));
		}
	}
}