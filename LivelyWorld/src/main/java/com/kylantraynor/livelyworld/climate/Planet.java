package com.kylantraynor.livelyworld.climate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;

public class Planet {

	static ArrayList<Planet> planets = new ArrayList<Planet>();

	static double HalfPI = Math.PI / 2;

	public static Planet getPlanet(World w) {
		for (Planet p : planets) {
			if (p.isAffectedWorld(w)) {
				return p;
			}
		}
		return null;
	}

	private boolean isAffectedWorld(World w) {
		if (this.mainWorld.equals(w))
			return true;
		if (worlds.contains(w))
			return true;
		return false;
	}

	private ArrayList<World> worlds = new ArrayList<World>();
	private Map<World, ClimateMap> maps = new HashMap<World, ClimateMap>();
	private World mainWorld;
	private String name;

	public Planet(World w, String name) {
		setWorld(w);
		this.name = name;
		planets.add(this);
	}

	public ClimateMap getClimateMap(World world) {
		if (world == null)
			world = mainWorld;
		ClimateMap map = maps.get(world);
		if (map == null) {
			map = new ClimateMap(world);
			maps.put(world, map);
		}
		return map;
	}

	public ClimateMap getClimateMap() {
		return getClimateMap(null);
	}

	public long getRadius() {
		return 6000000L / 100; // 100 times smaller than earth.
	}

	public long getR() {
		return getRadius();
	}

	public double getC() {
		return getCircumference();
	}

	public double getOb() {
		return getObliquity();
	}

	public double getCircumference() {
		return (getR() * 2 * Math.PI);
	}

	public double getAngleFromEquator(double zPosition) {
		return (Math.abs(zPosition) / getMaxZ()) * HalfPI;
	}

	public Calendar getCurrentIRLDate() {
		return Calendar.getInstance();
	}

	public int getDaysSinceIRLJanuary() {
		return getCurrentIRLDate().get(Calendar.DAY_OF_YEAR);
	}

	public int getDaysSinceIRLMarch21() {
		return getDaysSinceIRLJanuary();
	}

	public int getDaysInIRLYear() {
		return getCurrentIRLDate().getMaximum(Calendar.DAY_OF_YEAR);
	}

	public double getObliquity() {
		return getInclination()
				* (-Math.cos(2
						* Math.PI
						* ((double) getDaysSinceIRLJanuary() / (double) getDaysInIRLYear())));
	}

	public double getMaxZ() {
		return getR() * HalfPI;
	}

	public double getCurrentOffset() {
		return getR() * getOb();
	}

	static long getTicksInAYear() {
		return 8766000L;
	}

	public double getInclination() {
		return (21 * Math.PI) / 180;
	}

	public double getSunAverageRadiation(double zPos) {
		return Math.max(Math.cos((zPos + getCurrentOffset()) / getR()), 0);
	}

	public double getSunAverageRadiation(Location l) {
		return Math.max(Math.cos((l.getZ() + getCurrentOffset()) / getR()), 0)
				* (l.getBlock().getLightFromSky() / 15.0);
	}

	public double getSunRadiation(Location l) {
		return getSunAverageRadiation(l) * getDayLight(l);
	}

	public int getDayTime(Location l) {
		return (int) ((l.getWorld().getFullTime() - 6000) % 24000);
	}

	public double getDayLight(Location l) {
		return Math.max((getSunPosition(l) + 0.5) / 1.5, 0.0);
	}

	public double getSunPosition(Location l) {
		double day = getDayTime(l) / 24000.0;
		return Math.cos((day) * (Math.PI * 2));
	}

	public Temperature getDefaultAirTemperature(Location l) {
		double max = 30;
		double base = 273.15 + 15;
		double daily = base + ((getSunPosition(l) + 0.5) / 1.5) * max;
		double altitude = daily - 0.001 * (l.getY() - 49) * (l.getY() - 49);
		double radiation = (altitude - max / 2) + max
				* getSunAverageRadiation(l);
		return new Temperature(radiation);
	}

	public Climate getClimate(Location l) {
		return new Climate(l);
	}

	public World getWorld() {
		return mainWorld;
	}

	public void setWorld(World world) {
		addWorld(world);
		this.mainWorld = world;
	}

	public void addWorld(World world) {
		if (worlds.contains(world))
			return;
		this.worlds.add(world);
	}

	public void removeWorld(World world) {
		if (worlds.contains(world)) {
			this.worlds.remove(world);
		}
	}

	public Temperature getTMin() {
		return new Temperature(180);
	}

	public String getName() {
		return this.name;
	}

	public void generateClimateMaps() {
		for (World w : worlds) {
			getClimateMap(w).generateMap();
		}
	}
}
