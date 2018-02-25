package com.kylantraynor.livelyworld.climate;

import java.nio.ByteBuffer;

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
	
	public static int getByteSize() {
		int result = 0;
		result += 8; // temperature
		result += 8; // highTemp
		result += 8; // pressure
		result += 8; // highpressure
		result += 8; // humidity
		return result;
	}

	public void saveInto(ByteBuffer buff) {
		buff.putDouble(temperature);
		buff.putDouble(highTemperature);
		buff.putDouble(pressure);
		buff.putDouble(highPressure);
		buff.putDouble(humidity);
	}
	
	public static ClimateCellData loadFrom(ByteBuffer buff){
		double temperature = buff.getDouble();
		double highTemperature = buff.getDouble();
		double pressure = buff.getDouble();
		double highPressure = buff.getDouble();
		double humidity = buff.getDouble();
		return new ClimateCellData(temperature, highTemperature, pressure, highPressure, humidity);
	}
}
