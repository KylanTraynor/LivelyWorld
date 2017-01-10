package com.kylantraynor.livelyworld.climate;

public class ClimateUtils {

	static double R = 0.083144598;
	static double invertedR = 1 / R;

	static Temperature getGasTemperature(double pressure, double volume,
			long amount) {
		return new Temperature(0.000001 * (pressure * 100) * volume * (double)(1.0d / amount)
				* invertedR);
	}

	static double getGasPressure(double volume, long amount,
			Temperature temperature) {
		return 1000000 * (amount * (long) temperature.getValue() * R * (1 / volume)) * 0.01;
	}

	static long getGasAmount(double pressure, double volume,
			Temperature temperature) {
		return ((long) (pressure * 100)) * (long) ( (long) (volume * 0.000001) * (1 / (R * temperature.getValue())));
	}
}