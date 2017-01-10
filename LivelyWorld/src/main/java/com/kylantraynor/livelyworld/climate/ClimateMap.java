package com.kylantraynor.livelyworld.climate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

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

	public ClimateMap(World world) {
		this(world, 500);
	}

	public ClimateMap(World world, int resolution) {
		this.world = world;
		this.resolution = resolution;
	}

	public void generateMap() {
		if (HookManager.hasWorldBorder()) {

			WorldBorderHook hook = HookManager.getWorldBorder();
			Location center = hook.getWorldCenter(world);
			float minX = (float) (center.getX() - hook.getWorldRadiusX(world));
			float minZ = (float) (center.getZ() - hook.getWorldRadiusZ(world));
			float maxX = (float) (center.getX() + hook.getWorldRadiusX(world));
			float maxZ = (float) (center.getZ() + hook.getWorldRadiusZ(world));
			List<VSite> sites = new ArrayList<VSite>();
			for (int x = (int) minX + resolution; x < maxX - resolution; x += resolution) {
				for (int z = (int) minZ + resolution; z < maxZ - resolution; z += resolution) {
					VSite s = new VSite(
							(float) (x + Math.random() * resolution - resolution / 2),
							(float) (z + Math.random() * resolution - resolution / 2),
							1);
					sites.add(s);
				}
			}
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
			ClimateCell cell = this.generator.getCellAt(new VectorXZ(
					(float) location.getX(), (float) location.getZ()));
			return cell;
		}
		return null;
	}

	public ClimateCell[] getCells() {
		return this.generator.getCells();
	}

	public void randomCellUpdate() {
		int i = (int) (Math.random() * getCells().length);
		if (getCells()[i] != null)
			getCells()[i].update();
	}
	
	public Temperature getCurrentHighestTemperature(){
		double highest = Double.NaN;
		for(ClimateCell c : getCells()){
			if(Double.isNaN(highest)) {
				highest = c.getTemperature().getValue();
				continue;
			}
			if(c.getTemperature().getValue() > highest){
				highest = c.getTemperature().getValue();
			}
		}
		return new Temperature(highest);
	}
	
	public Temperature getCurrentLowestTemperature(){
		double lowest = Double.NaN;
		for(ClimateCell c : getCells()){
			if(Double.isNaN(lowest)) {
				lowest = c.getTemperature().getValue();
				continue;
			}
			if(c.getTemperature().getValue() < lowest){
				lowest = c.getTemperature().getValue();
			}
		}
		return new Temperature(lowest);
	}
}
