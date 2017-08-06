package com.kylantraynor.livelyworld.climate;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class Temperature {

	public static final Temperature NaN = new Temperature(Double.NaN);
	
	static Map<Player, String> playerSystem = new HashMap<Player, String>();
	final double value;

	public Temperature(double v) {
		this.value = v <= 0 ? 0 : v;
	}

	public double getValue() {
		return this.value;
	}

	public Temperature add(Temperature t) {
		return new Temperature(this.value + t.value);
	}

	public Temperature bringTo(Temperature t, double inertia) {
		if(inertia < 0) inertia = 0;
		double oldt = this.value;
		double targett = t.value;
		if(inertia + 1 == 0) return new Temperature(t.getValue());
		double newt = (oldt * inertia + targett) * (1.0 / (inertia + 1));
		return new Temperature(newt);
	}

	public Temperature remove(Temperature t) {
		return new Temperature(this.value - t.value);
	}

	public double getRoundedValue(double value) {
		return ((double) Math.round(value * 100)) / 100;
	}

	public String toString(Player p) {
		if(isNaN()) return "NaN";
		if (playerSystem.containsKey(p)) {
			switch (playerSystem.get(p)) {
			case "K":
				return "" + getRoundedValue(this.value) + "\u00BA" + "K";
			case "C":
				return "" + getRoundedValue(this.value - 273.15) + "\u00BA" + "C";
			case "F":
				return "" + getRoundedValue((this.value - 273.15) * 1.8 + 32) + "\u00BA" + "F";
			}
		}
		return "" + getRoundedValue(this.value - 273.15) + "\u00BA" + "C";
	}

	public String toString(String system) {
		if(isNaN()) return "NaN";
		switch (system) {
		case "C":
			return "" + getRoundedValue(this.value - 273.15) + "\u00BA" + "C";
		case "F":
			return "" + getRoundedValue((this.value - 273.15) * 1.8 + 32) + "\u00BA" + "F";
		default:
			return "" + getRoundedValue(this.value) + "\u00BA" + "K";
		}
	}

	@Override
	public String toString() {
		if(isNaN()) return "NaN";
		return "" + getRoundedValue(this.value) + "°K";
	}
	
	public static Temperature fromCelsius(double celsius){
		return new Temperature(celsius + 273.15);
	}
	
	public static Temperature fromFahrenheit(double fahrenheit){
		return fromCelsius(((fahrenheit - 32) / 1.8));
	}
	
	public boolean isCelsiusBetween(double min, double max){
		return isBetween(min + 273.15, max + 273.15);
	}
	
	public boolean isBetween(double min, double max){
		if(isNaN()) return false;
		return this.value >= min && this.value <= max;
	}
	
	public boolean isCelsiusAbove(double min){
		return isAbove(min + 273.15);
	}
	
	public boolean isAbove(double min){
		if(isNaN()) return false;
		return this.value > min;
	}
	
	public boolean isCelsiusBelow(double max){
		return isBelow(max + 273.15);
	}
	
	public boolean isBelow(double max){
		if(isNaN()) return false;
		return this.value < max;
 	}
	
	public boolean isNaN(){
		return Double.isNaN(value);
	}
}
