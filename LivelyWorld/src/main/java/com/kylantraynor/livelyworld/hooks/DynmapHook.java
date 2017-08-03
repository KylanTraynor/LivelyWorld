package com.kylantraynor.livelyworld.hooks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.World;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.PolyLineMarker;

import com.kylantraynor.livelyworld.climate.ClimateCell;
import com.kylantraynor.livelyworld.climate.ClimateMap;
import com.kylantraynor.livelyworld.climate.ClimateUtils;
import com.kylantraynor.livelyworld.climate.Planet;
import com.kylantraynor.livelyworld.climate.Temperature;

public class DynmapHook {
	private DynmapAPI api;
	private MarkerAPI markerAPI;

	private MarkerSet weatherSet;
	private MarkerSet windSet;
	private MarkerSet temperaturesSet;

	private HashMap<String, Marker> markerList = new HashMap<String, Marker>();

	public DynmapHook() {
		api = (DynmapAPI) Bukkit.getServer().getPluginManager()
				.getPlugin("dynmap");
		markerAPI = api.getMarkerAPI();

		loadWeatherMarkerSet();
		loadWindMarkerSet();
		loadTemperaturesMarkerSet();
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
	
	private void loadWindMarkerSet() {
		windSet = markerAPI.getMarkerSet("livelyworld.markerset.wind");
		if (windSet == null) {
			windSet = markerAPI.createMarkerSet(
					"livelyworld.markerset.wind", "Wind", null, false);
		} else {
			windSet.setMarkerSetLabel("Wind");
		}
		if (windSet == null) {
			Bukkit.getServer().getLogger().severe("Error creating marker set");
			return;
		}
		int minzoom = 0;
		if (minzoom > 0) {
			windSet.setMinZoom(minzoom);
		}
		windSet.setLayerPriority(10);
		windSet.setHideByDefault(true);
	}
	
	private void loadTemperaturesMarkerSet() {
		temperaturesSet = markerAPI.getMarkerSet("livelyworld.markerset.temperatures");
		if (temperaturesSet == null) {
			temperaturesSet = markerAPI.createMarkerSet(
					"livelyworld.markerset.temperatures", "Temperatures", null, false);
		} else {
			temperaturesSet.setMarkerSetLabel("Temperatures");
		}
		if (temperaturesSet == null) {
			Bukkit.getServer().getLogger().severe("Error creating marker set");
			return;
		}
		int minzoom = 0;
		if (minzoom > 0) {
			temperaturesSet.setMinZoom(minzoom);
		}
		temperaturesSet.setLayerPriority(10);
		temperaturesSet.setHideByDefault(true);
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
		String id = "" + c.getX() + "_" + c.getZ() + "_weathericon";
		String cellid = "" + c.getX() + "_" + c.getZ() + "_weathercell";
		String windid = "" + c.getX() + "_" + c.getZ() + "_weatherwind";
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
			Temperature temp = ClimateUtils.getAltitudeWeightedTemperature(c, c.getAltitude());
			sb.append("<br />Temperature at " + c.getAltitude() + "Y: " + temp.toString("C") + "/" + temp.toString("F"));
			sb.append("<br />Temperature at Ocean Height: " + c.getTemperature().toString("C") + "/" + c.getTemperature().toString("F"));
			sb.append("<br />Humidity: " + (int) c.getRelativeHumidity() + "%");
			sb.append("<br />Precipitations: " + (int) c.getPrecipitations() + " g/m3");
			sb.append("<br />Pressure: " + (int) (c.getLowAltitudePressure() * 0.01) + " hPa");
			sb.append("<br />Altitude: " + (int) c.getAltitude() + " m");
			
			sb.append("<br />~DEBUG~");
			/*sb.append("<br />Surface Temperature: " + c.getSurfaceTemperature().toString("C") + "/" + c.getSurfaceTemperature().toString("F"));
			sb.append("<br />Surface Block: " + c.getLocation().getBlock().getType().toString());*/
			sb.append("<br />Water Vapor amount: " + (int) c.getHumidity() + " g/m3");
			sb.append("<br />Air Volume: " + (int) c.getAirVolumeOnBlock() + " m3");
			sb.append("<br />Air Particles: " + (int) c.getAmountOnBlock() + " moles");
			sb.append("<br />High Air Particles: " + (int) c.getAmountHigh() + " moles");
			sb.append("<br />High Air Pressure: " + (int) (c.getHighAltitudePressure() * 0.01) + " hPa");
			weather.setDescription(sb.toString());
			markerList.put(id, weather);
			
			// Creates Wind Marker
			double[] xline = {c.getX(), c.getX() + (-c.getLowWind().getX())};
			double[] yline = {c.getAltitude(), c.getAltitude() + (-c.getLowWind().getY())};
			double[] zline = {c.getZ(), c.getZ() + (-c.getLowWind().getZ())};
			PolyLineMarker l = windSet.createPolyLineMarker(windid, "" + c.getLowWind().getSpeed(), false, c.getWorld().getName(), xline, yline, zline, false);
			if(l == null){
				l = windSet.findPolyLineMarker(windid);
				if(l == null){
					Bukkit.getServer().getLogger().severe("Failed to create Wind display.");
				} else {
					l.setCornerLocation(1, xline[1], yline[1], zline[1]);
					//l.setCornerLocations(xline, yline, zline);
					l.setLabel("" + c.getLowWind().getSpeed());
				}
			} else {
				l.setLineStyle(1, 2, Color.BLACK.asRGB());
			}
			
			// Creates Area Marker
			AreaMarker m = temperaturesSet.createAreaMarker(cellid, c.getTemperature().toString("C") + "/"
					+ c.getTemperature().toString("F")
					, false, c.getWorld().getName(), c.getVerticesX(), c.getVerticesZ(), false);
			if(m == null){
				m = temperaturesSet.findAreaMarker(cellid);
				if(m == null){
					Bukkit.getServer().getLogger().severe("Failed to create marker area.");
					return;
				}
				m.setLabel(c.getTemperature().toString("C") + "/" + c.getTemperature().toString("F"));
			}
			double min = c.getMap().getCurrentLowestTemperature().getValue();
			double max = c.getMap().getCurrentHighestTemperature().getValue();
			double cappedTemperature = Math.max(Math.min(c.getTemperature().getValue(), max), min) - min;
			int value = (int) (cappedTemperature * 255 / (max - min));
			int red = value;
			int green = (int) (255 * Math.sin((value) * Math.PI / 255));
			int blue = 255 - value;
			m.setFillStyle(0.80, Color.fromRGB(red, green, blue).asRGB());
			m.setLineStyle(1, 0, Color.fromRGB(red, green, blue).asRGB());
		}
	}
}
