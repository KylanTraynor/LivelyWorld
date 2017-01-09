package com.kylantraynor.livelyworld.climate;

public class ClimateUtils {

	static double R = 0.083144598;
	static double invertedR = 1 / R;

	static Temperature getGasTemperature(double pressure, double volume,
			double amount) {
		return new Temperature((pressure * 100) * volume * (1 / amount)
				* invertedR);
	}

	static double getGasPressure(double volume, double amount,
			Temperature temperature) {
		return (amount * temperature.getValue() * R * invertedR * (1 / volume)) * 0.01;
	}

	static double getGasAmount(double pressure, double volume,
			Temperature temperature) {
		return (pressure * 100) * volume * invertedR
				* (1 / temperature.getValue());
	}
}