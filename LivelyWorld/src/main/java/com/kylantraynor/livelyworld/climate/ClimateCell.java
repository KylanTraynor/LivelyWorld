package com.kylantraynor.livelyworld.climate;

import org.bukkit.Location;
import org.bukkit.World;

import com.kylantraynor.livelyworld.hooks.HookManager;
import com.kylantraynor.voronoi.VCell;
import com.kylantraynor.voronoi.VTriangle;
import com.kylantraynor.voronoi.VectorXZ;

public class ClimateCell extends VCell {

	private World world;
	private double lowAltitudePressure = getBasePressure();
	private double highAltitudePressure = getBasePressure();
	private double airVolume = Double.NaN;
	private double airAmount = Double.NaN;
	private Temperature temperature;

	public ClimateCell() {
		super();
	}

	public Location getLocation() {
		return new Location(world, (double) getSite().getX(), getAltitude(),
				(double) getSite().getZ());
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public World getWorld() {
		return world;
	}

	public Weather getWeather() {
		if (world.hasStorm()) {
			return Weather.RAIN;
		} else if (world.isThundering()) {
			return Weather.THUNDERSTORM;
		}
		return Weather.CLEAR;
	}

	public double getSunRadiation() {
		return Planet.getPlanet(world).getSunRadiation(
				new Location(world, getSite().getX(), 255, getSite().getZ()));
	}

	public Temperature getBaseTemperature() {
		return new Temperature(273.15 + 15);
	}

	public double getBasePressure() {
		return 1013;
	}

	public double getAltitude() {
		return world.getHighestBlockYAt((int) getSite().x, (int) getSite().z);
	}

	public Temperature getTemperature() {
		if (temperature != null)
			return temperature;
		temperature = getBaseTemperature();
		return temperature;
	}

	public double getVolume() {
		if (!Double.isNaN(airVolume))
			return airVolume;
		double volume = 0;
		for (VTriangle t : getTriangles()) {
			VectorXZ a = t.points[0];
			VectorXZ b = t.points[1];
			VectorXZ c = t.points[2];
			volume += (a.getX() * (b.getZ() - c.getZ()) + b.getX()
					* (c.getZ() - a.getZ()) + c.getX() * (a.getZ() - b.getZ())) * 0.5;
		}
		airVolume = volume * (255 - getAltitude());
		return airVolume;
	}

	public double getAmount() {
		if (!Double.isNaN(airAmount))
			return airAmount;
		airAmount = ClimateUtils.getGasAmount(getBasePressure(), getVolume(),
				getBaseTemperature());
		return airAmount;
	}

	public double getLowAltitudePressure() {
		if (!Double.isNaN(lowAltitudePressure))
			return lowAltitudePressure;
		lowAltitudePressure = getBasePressure();
		return lowAltitudePressure;
	}

	public double getHighAltitudePressure() {
		return highAltitudePressure;
	}

	public void updateMap() {
		if (HookManager.hasDynmap()) {
			HookManager.getDynmap().updateClimateCell(this);
		}
	}

	public void updateTemperature() {
		temperature = getTemperature()
				.bringTo(
						Planet.getPlanet(world).getDefaultAirTemperature(
								getLocation()), getVolume() * 0.00001);
	}

	public void updatePressure() {
		lowAltitudePressure = ClimateUtils.getGasPressure(getVolume(),
				getAmount(), getTemperature());
	}

	public void update() {
		updateTemperature();
		updatePressure();
		updateMap();
	}
}
