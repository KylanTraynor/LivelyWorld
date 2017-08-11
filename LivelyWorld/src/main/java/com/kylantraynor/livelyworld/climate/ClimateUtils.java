package com.kylantraynor.livelyworld.climate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.voronoi.VCell;
import com.kylantraynor.voronoi.VectorXZ;

public class ClimateUtils {

	static double R = 0.08;
	static double invertedR = 1 / R;

	public static Temperature getGasTemperature(double pressure, double volume,
			double amount) {
		return new Temperature((pressure) * volume * (1.0 / amount)
				* invertedR);
	}

	public static double getGasPressure(double volume, double amount,
			Temperature temperature) {
		return (amount * temperature.getValue() * R * (1.0 / volume));
	}

	public static double getGasAmount(double pressure, double volume,
			Temperature temperature) {
		return ((pressure) * (volume) * (1 / (R * temperature.getValue())));
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
		return getClimateCellAt(location, null);
	}
	
	public static ClimateCell getClimateCellAt(Location location, ClimateCell ref){
		if(ref != null){
			if(location.getWorld() == ref.getWorld()){
				VectorXZ v = new VectorXZ((float) location.getBlockX(), (float) location.getBlockZ());
				if(ref.isInside(v)) return ref;
				for(ClimateCell c : ref.getNeighbours()){
					if(c == null) continue;
					if(c.isInside(v)) {
						return c;
					} else {
						for (ClimateCell c2 : c.getNeighbours()){
							if (c2 == null) continue;
							if(c2.isInside(v)) return c2;
						}
					}
				}
			}
		}
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
		layers = layers > 8 ? 8 : layers;
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
		if(oldLayers < layers && Math.random() < 0.1){
			updateGlacier(block);
		}
	}
	
	public static void updateGlacier(Block block){
		int depth = 0;
		while(block.getType() == Material.SNOW || block.getType() == Material.SNOW_BLOCK || block.getType() == Material.FROSTED_ICE || block.getType() == Material.ICE || block.getType() == Material.PACKED_ICE){
			block = block.getRelative(BlockFace.DOWN);
			depth++;
		}
		while(depth > 1)
		{
			block = block.getRelative(BlockFace.UP);
			depth--;
			if(block.getType() == Material.ICE && depth > 10){
				block.setType(Material.PACKED_ICE);
			} else if ((block.getType() == Material.SNOW_BLOCK) && depth > 4){
				block.setType(Material.ICE);
			} else if (isWater(block)){
				block.setType(Material.FROSTED_ICE); // Should eventually be changed into frosted ICE.
			} else {
				block.setType(Material.SNOW_BLOCK);
			}
		}
	}

	@Deprecated
	public static boolean isWater(Block block) {
		return Utils.isWater(block);
	}

	public static void melt(Block b, int d) {
		if(d <= 0) return;
		if(isSnow(b)){
			if(getSnowLayers(b) > d){
				setSnowLayers(b, getSnowLayers(b) - d);
			} else {
				setWaterHeight(b, getSnowLayers(b), false);
			}
		} else if(b.getType() == Material.FROSTED_ICE){
			//setWaterHeight(b, 8, true);
		} else if(b.getType() == Material.ICE){
			setWaterHeight(b, 8, false);
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
	
	public static Temperature getAltitudeWeightedTemperature(ClimateCell c, double y1){
		if(c == null) return Temperature.NaN;
		double temp = c.getTemperature().getValue();
		
		double result = temp - ((y1 - 48) * 0.08);
		return new Temperature(result);
	}
	
	public static Temperature getAltitudeWeightedTriangleTemperature(ClimateCell c, Location l){
		ClimateTriangle t = getClimateTriangle(l, c);
		double temp = t.getTemperatureAt(l.getX(), l.getZ()).getValue();
		
		double result = temp - ((l.getY() - 48) * 0.08);
		return new Temperature(result);
	}
	
	public static ClimateTriangle getClimateTriangle(Location location, ClimateCell ref){
		VectorXZ v = new VectorXZ((float) location.getX(), (float) location.getZ());
		ClimateCell cell = null;
		if(ref != null){
			if(ref.isInside(v)){
				cell = ref;
			} else {
				for(ClimateCell c : ref.getNeighbours()){
					if(c == null) continue;
					if(c.isInside(v)) cell = c;
				}
				if(cell == null){
					cell = getClimateCellAt(location);
				}
			}
		} else {
			cell = ClimateUtils.getClimateCellAt(location);
		}
		
		if(cell == null) return null;
		
		ClimateCell cell2 = null;
		ClimateCell cell3 = null;
		
		for(VCell c : cell.getNeighbours()){
			if(c == null) continue;
			if(cell3 == null) cell3 = (ClimateCell)c;
			else {
				if(cell3.getSite().distanceSquared(v) > c.getSite().distanceSquared(v)){
					if(cell2 == null) cell2 = (ClimateCell) c;
					else if(cell2.getSite().distanceSquared(v) > c.getSite().distanceSquared(v)){
						cell3 = cell2;
						cell2 = (ClimateCell)c;
					} else {
						cell3 = (ClimateCell)c;
					}
				}
			}
		}
		
		if(cell2 == null || cell3 == null){
			// Shouldn't happen.
			return new ClimateTriangle(cell, cell, cell);
		}
		return new ClimateTriangle(cell, cell2, cell3);
	}

	public static int getWaterHeight(Block b) {
		int result = 8 - b.getData();
		if(result <= 0) result = 8;
		return result;
	}
	
	public static boolean isAcceptableTemperature(Temperature current, Temperature ideal, Temperature min, Temperature max){
		if(current.isBelow(min)) return false;
		if(current.isAbove(max)) return false;
		double cValue = current.getValue();
		double iValue = ideal.getValue();
		double minValue = min.getValue();
		double maxValue = max.getValue();
		double rdm = Math.random();
		double probability = 1;
		if(cValue > iValue){
			probability = Utils.simpleDistributionDensity(cValue, iValue, (maxValue - iValue) / 3);
		} else {
			probability = Utils.simpleDistributionDensity(cValue, iValue, (iValue - minValue) / 3);
		}
		if(rdm <= probability) return true;
		return false;
	}

	public static double getSunRadiation(Location loc){
		Planet p = Planet.getPlanet(loc.getWorld());
		if(p == null) return loc.getBlock().getLightFromSky() / 15;
		return p.getSunRadiation(loc);
	}
}