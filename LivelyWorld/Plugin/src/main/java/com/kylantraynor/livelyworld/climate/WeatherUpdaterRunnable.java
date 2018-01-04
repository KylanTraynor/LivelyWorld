package com.kylantraynor.livelyworld.climate;

import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WeatherUpdaterRunnable extends BukkitRunnable{

	@Override
	public void run() {
		for (Player p : Bukkit.getServer().getOnlinePlayers()){
			Planet pl = Planet.getPlanet(p.getWorld());
			if(pl == null) continue;
			ClimateMap map = pl.getClimateMap(p.getWorld());
			if(map == null) continue;
			ClimateCell c = map.getClimateCellAt(p.getLocation());
			switch (c.getWeather()){
			case CLEAR:
			case OVERCAST:
				p.setPlayerWeather(WeatherType.CLEAR);
				break;
			case RAIN:
			case SNOW:
			case STORM:
			case SNOWSTORM:
			case THUNDERSTORM:
				p.setPlayerWeather(WeatherType.DOWNFALL);
				break;
			default:
				break;
			}
		}
	}

}
