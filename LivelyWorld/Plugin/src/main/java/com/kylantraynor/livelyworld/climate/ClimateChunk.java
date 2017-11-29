package com.kylantraynor.livelyworld.climate;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

public class ClimateChunk {

	public static List<ClimateChunk> cachedChunks = new ArrayList<ClimateChunk>();

	Location location;
	Chunk chunk;
	private int y;
	private List<Location> cachedCorners;
	private Temperature cachedAverageTemperature;

	public static ClimateChunk getAt(Location l) {
		int y = (int) Math.floor(l.getBlockY() / 16);
		Chunk chunk = l.getChunk();
		for (ClimateChunk c : cachedChunks) {
			if (c.getX() == chunk.getX() && c.getY() == y
					&& c.getZ() == chunk.getZ()) {
				return c;
			}
		}
		ClimateChunk c = new ClimateChunk(l);
		cachedChunks.add(c);
		if (cachedChunks.size() > 100) {
			cachedChunks.remove(0);
		}
		return c;
	}

	public ClimateChunk(Location l) {
		this.y = (int) Math.floor(l.getBlockY() / 16);
		this.chunk = l.getChunk();
	}

	public int getY() {
		return y;
	}

	public int getX() {
		return chunk.getX();
	}

	public int getZ() {
		return chunk.getZ();
	}

	public List<Location> getCorners() {
		if (cachedCorners != null)
			return cachedCorners;
		List<Location> corners = new ArrayList<Location>();
		corners.add(new Location(chunk.getWorld(), getX() * 16, getY() * 16,
				getZ() * 16));
		corners.add(new Location(chunk.getWorld(), getX() * 16 + 15,
				getY() * 16, getZ() * 16));
		corners.add(new Location(chunk.getWorld(), getX() * 16,
				getY() * 16 + 15, getZ() * 16));
		corners.add(new Location(chunk.getWorld(), getX() * 16, getY() * 16,
				getZ() * 16 + 15));
		corners.add(new Location(chunk.getWorld(), getX() * 16 + 15,
				getY() * 16 + 15, getZ() * 16));
		corners.add(new Location(chunk.getWorld(), getX() * 16 + 15,
				getY() * 16, getZ() * 16 + 15));
		corners.add(new Location(chunk.getWorld(), getX() * 16 + 15,
				getY() * 16 + 15, getZ() * 16 + 15));
		cachedCorners = corners;
		return corners;
	}

	public Temperature getAverageTemperature() {
		if (cachedAverageTemperature != null)
			return cachedAverageTemperature;
		Planet planet = Planet.getPlanet(chunk.getWorld());
		if (planet == null)
			return null;
		Location current = new Location(chunk.getWorld(), 0, 0, 0);
		double lastTemp = 0;
		double temp = 0;
		int count = 0;
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					current.setX(getX() * 16 + x);
					current.setY(getY() * 16 + y);
					current.setZ(getZ() * 16 + z);
					if (current.getBlock().getType() != Material.AIR) {
						lastTemp = planet.getClimate(current)
								.getAverageTemperature().getValue();
						temp += lastTemp;
						count++;
					} else {
						lastTemp = (lastTemp + planet
								.getDefaultAirTemperature(current).value) / 2;
						temp += lastTemp;
						count++;
					}
				}
			}
		}
		if (count <= 0) {
			return null;
		}
		cachedAverageTemperature = new Temperature(temp / count);
		return cachedAverageTemperature;
	}

	public void updateClimate() {

	}

	public Temperature getTemperature() {
		Planet planet = Planet.getPlanet(chunk.getWorld());
		if (planet == null)
			return null;
		Location current = new Location(chunk.getWorld(), 0, 0, 0);
		double temp = 0;
		double lastTemp = 0;
		int count = 0;
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					current.setX(getX() * 16 + x);
					current.setY(getY() * 16 + y);
					current.setZ(getZ() * 16 + z);
					if (current.getBlock().getType() != Material.AIR) {
						lastTemp = planet.getClimate(current).getTemperature()
								.getValue();
						temp += lastTemp;
						count++;
					} else {
						lastTemp = (lastTemp + planet
								.getDefaultAirTemperature(current).value) / 2;
						temp += lastTemp;
						count++;
					}
				}
			}
		}
		if (count <= 0) {
			return null;
		}
		return new Temperature(temp / count);
	}
}
