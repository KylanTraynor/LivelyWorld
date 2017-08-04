package com.kylantraynor.livelyworld.climate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils.SizedList;
import com.kylantraynor.livelyworld.hooks.HookManager;
import com.kylantraynor.livelyworld.hooks.WorldBorderHook;
import com.kylantraynor.voronoi.VCell;
import com.kylantraynor.voronoi.VSite;
import com.kylantraynor.voronoi.VectorXZ;
import com.kylantraynor.voronoi.Voronoi;

public class ClimateMap {

	private Voronoi<ClimateCell> generator;
	private World world;
	private int resolution;
	private boolean generated = false;
	private Temperature lowestTemperature = Temperature.fromCelsius(0);
	private Temperature highestTemperature = Temperature.fromCelsius(20);
	private SizedList<ClimateCell> cache = new SizedList<ClimateCell>(10);

	public ClimateMap(World world) {
		this(world, 80);
	}

	public ClimateMap(World world, int resolution) {
		this.world = world;
		this.resolution = resolution;
	}
	
	public double incrementGenerationZ(double z, double step){
		if(z < 0){
			z = Math.min(z + step, 0);
		}
		z = -z;
		return z;
	}

	public double zAdjustedXStep(double z, double step){
		return Math.max(step * Math.cos(z / Planet.getPlanet(world).getMaxZ()), 1);
	}
	
	public void generateMap() {
		if (HookManager.hasWorldBorder()) {

			WorldBorderHook hook = HookManager.getWorldBorder();
			Location center = hook.getWorldCenter(world);
			float minX = (float) (center.getX() - hook.getWorldRadiusX(world));
			float minZ = (float) (center.getZ() - hook.getWorldRadiusZ(world));
			float maxX = (float) (center.getX() + hook.getWorldRadiusX(world));
			float maxZ = (float) (center.getZ() + hook.getWorldRadiusZ(world));
			//int halfRes = resolution / 2;
			double ratio = (maxX - minX)/(maxZ - minZ);
			List<VSite> sites = new ArrayList<VSite>();
			
			double zRange = Math.max(Math.abs(minZ), Math.abs(maxZ));
			double zStep = (zRange * 2 * ratio) / resolution;
			double xStep = (maxX - minX) / resolution;
			
			for(double z = - zRange; z <= -zStep || z >= zStep; z = incrementGenerationZ(z, zStep)){
				if(z == 0) break;
				if(z < minZ || z > maxZ) continue;
				LivelyWorld.getInstance().getLogger().info("Current Z = " + z);
				double zAdjustedXStep = zAdjustedXStep(z, xStep);
				for(double x = maxX; x >= minX; x -= zAdjustedXStep){
					VSite s = new VSite(
							(float) (x + Math.random() * zAdjustedXStep),
							(float) (z + Math.random() * zStep),
							1);
					sites.add(s);
				}
			}
			double zAdjustedXStep = zAdjustedXStep(0, xStep);
			for(double x = maxX; x >= minX; x -= zAdjustedXStep){
				VSite s = new VSite(
						(float) (x + Math.random() * zAdjustedXStep),
						(float) (Math.random() * zStep),
						1);
				sites.add(s);
			}
			
			/*for (int x = (int) minX + halfRes; x < maxX - halfRes; x += resolution) {
				for (int z = (int) minZ + halfRes; z < maxZ - halfRes; z += resolution) {
					VSite s = new VSite(
							(float) (x + (((2 * Math.random()) - 1) * resolution)),
							(float) (z + (((2 * Math.random()) - 1) * resolution)),
							1);
					sites.add(s);
				}
			}*/
			this.generator = new Voronoi<ClimateCell>(ClimateCell.class,
					sites.toArray(new VSite[sites.size()]), minX, minZ, maxX,
					maxZ);
		}

		if (generator == null)
			return;

		this.generator.generate();
		for (ClimateCell c : generator.getCells()) {
			c.setWorld(world);
			c.setMap(this);
			c.init();
		}
		generated = true;
	}

	public ClimateCell getCell(VSite site) {
		return this.generator.getCell(site);
	}

	public ClimateCell getClimateCellAt(Location location) {
		if (location == null)
			throw new NullPointerException("Location can't be Null");
		if (generated) {
			ClimateCell result = null;
			for(ClimateCell c : cache){
				if(c != null){
					if(c.isInside(new VectorXZ((float) location.getX(), (float) location.getZ()))){
						result = c;
						break;
					}
				}
			}
			if(result == null){
				result = this.generator.getCellAt(new VectorXZ(
					(float) location.getX(), (float) location.getZ()));
				cache.add(result);
			}
			return result;
		}
		return null;
	}

	public ClimateCell[] getCells() {
		return this.generator.getCells();
	}

	private int lastCellUpdateId = 0;
	private double highestHumidity;
	private double lowestLowPressure;
	private double highestLowPressure;
	
	public void randomCellUpdate() {
		lastCellUpdateId = lastCellUpdateId >= getCells().length - 1 ? 0 : lastCellUpdateId + 1;
		/*if(lastCellUpdateId == 0){
			HookManager.getDynmap().updateWeather();
		}*/
		if (getCells()[lastCellUpdateId] != null)
			getCells()[lastCellUpdateId].update();
	}
	
	public Temperature getCurrentHighestTemperature(){
		double highest = highestTemperature.getValue();
		for(ClimateCell c : getCells()){
			if(c.getTemperature().getValue() > highest){
				highest = c.getTemperature().getValue();
			}
		}
		highestTemperature = new Temperature(highest);
		return highestTemperature;
	}
	
	public Temperature getCurrentLowestTemperature(){
		double lowest = lowestTemperature.getValue();
		for(ClimateCell c : getCells()){
			if(c.getTemperature().getValue() < lowest){
				lowest = c.getTemperature().getValue();
			}
		}
		lowestTemperature = new Temperature(lowest);
		return lowestTemperature;
	}

	/**
	 * Returns the temperature at the location from averaging the surrounding {@link ClimateCell}.
	 * @param location
	 * @return {@link Temperature} which can be NaN.
	 */
	public Temperature getTemperatureAt(Location location) {
		
		ClimateCell cell = getClimateCellAt(location);
		if(cell == null) return Temperature.NaN;
		VectorXZ v = new VectorXZ((float) location.getX(), (float) location.getZ());
		
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
			return cell.getTemperature();
		}
		ClimateTriangle t = new ClimateTriangle(cell, cell2, cell3);
		return t.getTemperatureAt(v);
	}

	public double getCurrentHighestHumidity() {
		double highest = highestHumidity;
		for(ClimateCell c : getCells()){
			if(c.getHumidity() > highest){
				highest = c.getHumidity();
			}
		}
		highestHumidity = highest;
		return highestHumidity;
	}

	public double getCurrentLowestLowPressure() {
		double lowest = lowestLowPressure;
		for(ClimateCell c : getCells()){
			if(c.getLowAltitudePressure() < lowest){
				lowest = c.getLowAltitudePressure();
			}
		}
		lowestLowPressure = lowest;
		return lowestLowPressure;
	}
	
	public double getCurrentHighestLowPressure() {
		double highest = highestLowPressure;
		for(ClimateCell c : getCells()){
			if(c.getLowAltitudePressure() > highest){
				highest = c.getLowAltitudePressure();
			}
		}
		highestLowPressure = highest;
		return highestLowPressure;
	}
}
