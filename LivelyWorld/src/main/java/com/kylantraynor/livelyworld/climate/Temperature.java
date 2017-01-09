package com.kylantraynor.livelyworld.climate;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class Temperature {

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
		double oldt = this.value;
		double targett = t.value;
		double newt = (oldt * inertia + targett) / (inertia + 1);
		return new Temperature(newt);
	}

	public Temperature remove(Temperature t) {
		return new Temperature(this.value - t.value);
	}

	public double getRoundedValue(double value) {
		return ((double) Math.round(value * 100)) / 100;
	}

	public String toString(Player p) {
		if (playerSystem.containsKey(p)) {
			switch (playerSystem.get(p)) {
			case "K":
				return "" + getRoundedValue(this.value) + "\u00BA" + "K";
			case "C":
				return "" + getRoundedValue(this.value - 273.15) + "\u00BA" + "C";
			case "F":
				return "" + getRoundedValue((this.value - 273.15) * 1.8) + "\u00BA" + "F";
			}
		}
		return "" + getRoundedValue(this.value - 273.15) + "\u00BA" + "C";
	}

	public String toString(String system) {
		switch (system) {
		case "C":
			return "" + getRoundedValue(this.value - 273.15) + "\u00BA" + "C";
		case "F":
			return "" + getRoundedValue((this.value - 273.15) * 1.8) + "\u00BA" + "F";
		default:
			return "" + getRoundedValue(this.value) + "\u00BA" + "K";
		}
	}

	@Override
	public String toString() {
		return "" + getRoundedValue(this.value) + "°K";
	}
}
