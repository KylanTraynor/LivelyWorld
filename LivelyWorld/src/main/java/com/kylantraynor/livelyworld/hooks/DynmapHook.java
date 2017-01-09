package com.kylantraynor.livelyworld.hooks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import com.kylantraynor.livelyworld.climate.ClimateCell;
import com.kylantraynor.livelyworld.climate.ClimateMap;
import com.kylantraynor.livelyworld.climate.Planet;

public class DynmapHook {
	private DynmapAPI api;
	private MarkerAPI markerAPI;

	private MarkerSet weatherSet;

	private HashMap<String, Marker> markerList = new HashMap<String, Marker>();

	public DynmapHook() {
		api = (DynmapAPI) Bukkit.getServer().getPluginManager()
				.getPlugin("dynmap");
		markerAPI = api.getMarkerAPI();

		loadWeatherMarkerSet();
	}

	private void loadWeatherMarkerSet() {
		weatherSet = markerAPI.getMarkerSet("livelyworld.markerset.weather");
		if (weatherSet == null) {
			weatherSet = markerAPI.createMarkerSet(
					"livelyworld.markerset.weather", "Weather", null, false);
		} else {
			weatherSet.setMarkerSetLabel("Weather");
		}
		if (weatherSet == null) {
			Bukkit.getServer().getLogger().severe("Error creating marker set");
			return;
		}
		int minzoom = 0;
		if (minzoom > 0) {
			weatherSet.setMinZoom(minzoom);
		}
		weatherSet.setLayerPriority(10);
		weatherSet.setHideByDefault(true);
	}

	public void updateWeather() {
		for (World w : Bukkit.getServer().getWorlds()) {
			Planet p = Planet.getPlanet(w);
			if (p != null) {
				updateWeather(w);
			}
		}
	}

	private void updateWeather(World w) {
		Planet planet = Planet.getPlanet(w);
		ClimateMap map = planet.getClimateMap(w);
		for (ClimateCell c : map.getCells()) {
			updateClimateCell(c);
		}
	}

	public void updateClimateCell(ClimateCell c) {
		String id = "" + (int) c.getSite().getX() + "_"
				+ (int) c.getSite().getZ() + "_camp";
		String weatherMarker = "weather_"
				+ c.getWeather().toString().toLowerCase();
		MarkerIcon weatherIcon = null;
		if (weatherMarker != null) {
			weatherIcon = markerAPI.getMarkerIcon(weatherMarker);
			if (weatherIcon == null) {
				Bukkit.getServer().getLogger()
						.info("Invalid Weather Icon: " + weatherMarker);
				weatherIcon = markerAPI.getMarkerIcon("blueicon");
			}
		}
		if (weatherIcon != null) {
			Marker weather = markerList.remove(id);
			if (weather == null) {
				weather = weatherSet.createMarker(id,
						c.getTemperature().toString("C") + "/"
								+ c.getTemperature().toString("F"), c
								.getWorld().getName(), (double) c.getSite()
								.getX(), c.getAltitude(), (double) c.getSite()
								.getZ(), weatherIcon, false);
			} else {
				weather.setLocation(c.getWorld().getName(), c.getSite().getX(),
						c.getAltitude(), c.getSite().getZ());
				weather.setLabel(c.getTemperature().toString("C") + "/"
						+ c.getTemperature().toString("F"));
				weather.setMarkerIcon(weatherIcon);
			}
			StringBuilder sb = new StringBuilder();
			sb.append("Weather: " + c.getWeather().toString().toLowerCase());
			sb.append("<br />Temperature: " + c.getTemperature().toString("C")
					+ "/" + c.getTemperature().toString("F"));
			sb.append("<br />Pressure: " + (int) c.getLowAltitudePressure()
					+ " hPa");
			weather.setDescription(sb.toString());
			markerList.put(id, weather);
		}
	}
}
