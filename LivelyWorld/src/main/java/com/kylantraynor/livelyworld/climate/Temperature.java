package com.kylantraynor.livelyworld.climate;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class Temperature {
	
	static Map<Player, String> playerSystem = new HashMap<Player, String>();
	double value;
	
	public Temperature(double v){
		this.value = v;
	}
	
	public double getValue(){
		return this.value;
	}
	
	public double getRoundedValue(double value){
		return ((double) Math.round(value * 100)) / 100;
	}
	
	public String toString(Player p){
		if(playerSystem.containsKey(p)){
			switch(playerSystem.get(p)){
			case "K":
				return "" + getRoundedValue(this.value) + "°K";
			case "C":
				return "" + getRoundedValue(this.value - 273.15) + "°C";
			case "F":
				return "" + getRoundedValue((this.value - 273.15) * 1.8) + "°F";
			}
		}
		return "" + getRoundedValue(this.value - 273.15) + "°C";
	}
	
	@Override
	public String toString(){
		return "" + getRoundedValue(this.value) + "°K";
	}
}
