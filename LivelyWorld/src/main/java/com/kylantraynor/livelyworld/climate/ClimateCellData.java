package com.kylantraynor.livelyworld.climate;

public class ClimateCellData {
	private double temperature;
	private double highTemperature;
	private double pressure;
	private double highPressure;
	private double humidity;
	
	public ClimateCellData(double temperature, double highTemperature, double pressure, double highPressure, double humidity){
		this.temperature = temperature;
		this.highTemperature = highTemperature;
		this.pressure = pressure;
		this.highPressure = highPressure;
		this.humidity = humidity;
	}

	public double getTemperature() {
		return temperature;
	}

	public double getHighTemperature() {
		return highTemperature;
	}

	public double getPressure() {
		return pressure;
	}

	public double getHighPressure() {
		return highPressure;
	}

	public double getHumidity() {
		return humidity;
	}
}
