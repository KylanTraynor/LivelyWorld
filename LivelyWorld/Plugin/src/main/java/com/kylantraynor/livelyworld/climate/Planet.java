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
	static double TwoPI = Math.PI * 2;
	private double inclination = (21 * Math.PI) / 180;
	private long radius = 6000000L / 400; // 400 times smaller than earth. 15000;

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
		return radius;
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
		return (getR() * TwoPI);
	}

	public double getAngleFromEquator(double zPosition) {
		return (Math.abs(zPosition) / getMaxZ()) * HalfPI;
	}
	
	public double getZFromAngle(double angle){
		return getZFromRadAngle(angle * (HalfPI / 90));
	}
	
	public double getZFromRadAngle(double radAngle){
		return (radAngle / HalfPI) * getMaxZ();
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
				* (-Math.cos(TwoPI
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
		return inclination;
	}

	public double getSunAverageRadiation(double zPos) {
		return Math.max(Math.cos((zPos + getCurrentOffset()) / (getR() * HalfPI)), 0);
	}

	public double getSunAverageRadiation(Location l) {
		return getSunAverageRadiation(l.getZ()) * (l.getBlock().getLightFromSky() / 15.0);
	}

	public double getSunDirectRadiation(World w, int x, int y, int z){
		return getSunAverageRadiation(z) * getDayLight(w);
	}
	
	public double getSunRadiation(Location l) {
		return getSunAverageRadiation(l) * getDayLight(l);
	}

	// 0 - 6h
	public int getDayTime(Location l) {
		return (int) (((l.getWorld().getFullTime() + 6000) + (24000 * l.getX() / this.getC())) % 24000);
	}
	
	public int getDayTime(World w){
		return (int) ((w.getFullTime() + 6000) % 24000);
	}

	public double getDayLight(Location l) {
		return Math.max((getSunPosition(l) + 0.5) /1.5, 0.0);
	}
	
	public double getDayLight(World w){
		return Math.max((getSunPosition(w) + 0.5) / 1.5, 0.0);
	}

	public double getSunPosition(Location l) {
		double day = getDayTime(l) / 24000.0;
		return -Math.cos((day) * TwoPI);
	}
	
	public double getSunPosition(World w){
		double day = getDayTime(w) / 24000.0;
		return -Math.cos((day) * TwoPI);
	}

	public Temperature getDefaultAirTemperature(Location l) {
		double max = 30;
		double base = 273.15 + 15;
		double daily = base + ((getSunPosition(l) + 0.5) / 1.5) * max;
		double altitude = daily - 0.001 * (l.getY() - 49) * (l.getY() - 49);
		double radiation = (altitude - max * 0.5) + max
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
