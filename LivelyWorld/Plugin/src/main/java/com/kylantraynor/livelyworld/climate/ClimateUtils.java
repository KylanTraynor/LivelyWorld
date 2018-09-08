package com.kylantraynor.livelyworld.climate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.voronoi.VCell;
import com.kylantraynor.voronoi.VectorXZ;

public class ClimateUtils {

	static double R = 8.314;
	static double massAir = 0.028964;
	static double invertedR = 1 / R;
	static double RspecificAir = R / massAir;
	
	/**
	 * Get the density of air from pressure (Pa) and temperature.
	 * @param pressure in Pa
	 * @param t
	 * @return
	 */
	public static double getAirDensity(double pressure, Temperature t){
		return pressure / (t.getValue() * RspecificAir);
	}

	/**
	 * Get the temperature of the gas from pressure, volume and amount.
	 * @param pressure in Pa
	 * @param volume in m3
	 * @param amount in moles
	 * @return
	 */
	public static Temperature getGasTemperature(double pressure, double volume,
			double amount) {
		// T = pV/nR
		return new Temperature((pressure * volume) / (amount * R));
	}

	/**
	 * Get the pressure of the gas from the volume, amount and temperature.
	 * @param volume in m3
	 * @param amount in moles
	 * @param temperature
	 * @return
	 */
	public static double getGasPressure(double volume, double amount,
			Temperature temperature) {
		// p = nRT/V
		return (amount * temperature.getValue() * R) / volume;
	}

	/**
	 * Get the amount of gas from the pressure, volume and temperature
	 * @param pressure in Pa
	 * @param volume in m3
	 * @param temperature
	 * @return
	 */
	public static double getGasAmount(double pressure, double volume,
			Temperature temperature) {
		// n = pV/RT
		return (pressure * volume) / (R * temperature.getValue());
	}
	
	public static Temperature getTemperatureAt(Location location){
		Planet planet = Planet.getPlanet(location.getWorld());
		if(planet != null){
			ClimateMap map = planet.getClimateMap(location.getWorld());
			if(map == null) return Temperature.NaN;
			ClimateCell c = map.getClimateCellAt(location);
			if(c == null) return Temperature.NaN;
			c.getTemperature();
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

	public static ClimateCell getClimateCellAt(World w, int x, int z){
		return getClimateCellAt(w, x, z, null);
	}

	public static ClimateCell getClimateCellAt(World w, int x, int z, ClimateCell ref){
		Planet planet = Planet.getPlanet(w);
		if(planet == null) return null;
		ClimateMap map = planet.getClimateMap(w);
		if(map == null) return null;
		return map.getClimateCellAt(x, z);
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

	public static int melt(Block b, int d) {
		if(d <= 0) return 0;
		if(isSnow(b)){
			if(getSnowLayers(b) > d){
				setSnowLayers(b, getSnowLayers(b) - d);
				return d;
			} else {
				int melted = getSnowLayers(b);
				Utils.setWaterHeight(b, melted, false);
				return melted;
			}
		} else if(b.getType() == Material.FROSTED_ICE){
			//setWaterHeight(b, 8, true);
			return 0;
		} else if(b.getType() == Material.ICE){
			Utils.setWaterHeight(b, 8, false);
			return 8;
		} else if(b.getType() == Material.PACKED_ICE){
			b.setType(Material.ICE);
			return 0;
		}
		return 0;
	}
	
	@Deprecated
	public static void setWaterHeight(Block b, int height, boolean canSource){
		Utils.setWaterHeight(b, height, canSource);
	}
	
	public static Temperature getAltitudeWeightedTemperature(ClimateCell c, double y1){
		if(c == null) return Temperature.NaN;
		double temp = c.getTemperature().getValue();
		
		double result = temp - ((y1 - 48) * 0.08);
		return new Temperature(result);
	}
	
	public static Temperature getAltitudeWeightedTemperature(Location l){
		ClimateSquare t = getClimateSquare(l);
		if(t == null) return Temperature.NaN;
		double temp = t.getTemperatureAt(l.getX(), l.getZ()).getValue();
		
		double result = temp - ((l.getY() - 48) * 0.08);
		return new Temperature(result);
	}
	
	public static ClimateSquare getClimateSquare(Location location){
		VectorXZ v = new VectorXZ((float) location.getX(), (float) location.getZ());
		ClimateCell cell = ClimateUtils.getClimateCellAt(location);
		if(cell == null) return null;
		return new ClimateSquare(cell, cell.getRelative(1,0), cell.getRelative(1,1), cell.getRelative(0,1));
	}

	@Deprecated
	public static int getWaterHeight(Block b) {
		return Utils.getWaterHeight(b);
	}
	
	public static boolean isAcceptableTemperature(Temperature current, Temperature ideal, Temperature min, Temperature max){
		if(current.isNaN()) return false;
		if(current.isBelow(min)) return false;
		if(current.isAbove(max)) return false;
		double cValue = current.getValue();
		double iValue = ideal.getValue();
		double minValue = min.getValue();
		double maxValue = max.getValue();
		double rdm = Utils.fastRandomDouble();
		double probability = 1;
		if(cValue > iValue){
			probability = Utils.simpleDistributionDensity(cValue, iValue, (maxValue - iValue) / 3);
		} else {
			probability = Utils.simpleDistributionDensity(cValue, iValue, (iValue - minValue) / 3);
		}
		if(rdm <= probability) return true;
		return false;
	}

	/**
	 * Returns the amount of sun radiation at this location.
	 * @param loc as Location
	 * @return [0-1]
	 */
	public static double getSunRadiation(Location loc){
		Planet p = Planet.getPlanet(loc.getWorld());
		if(p == null) return ((int)loc.getBlock().getLightFromSky()) / 15.0;
		return p.getSunRadiation(loc);
	}
}