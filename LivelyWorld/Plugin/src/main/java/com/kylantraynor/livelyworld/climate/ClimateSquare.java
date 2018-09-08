package com.kylantraynor.livelyworld.climate;

import com.kylantraynor.voronoi.VectorXZ;

public class ClimateSquare {
	ClimateCell cell;
	ClimateCell cellright;
	ClimateCell celldownright;
	ClimateCell celldown;
	
	public ClimateSquare(ClimateCell cell, ClimateCell cellright,
			ClimateCell celldownright, ClimateCell celldown) {
		this.cell = cell;
		this.cellright = cellright;
		this.celldownright = celldownright;
		this.celldown = celldown;
	}
	
	Temperature getTemperatureAt(double x, double z){
		return getTemperatureAt(new VectorXZ((float) x, (float) z));
	}
	
	Temperature getTemperatureAt(VectorXZ v){
	    if(cell == null) return Temperature.NaN;
	    if(celldown == null || celldownright == null || cellright == null) return cell.getTemperature();

	    float size = cell.size;

	    double deltaTop = cellright.getTemperature().getValue() - cell.getTemperature().getValue();
	    double deltaBottom = celldownright.getTemperature().getValue() - cellright.getTemperature().getValue();

	    float relX = (v.x - cell.getX())/cell.size;
	    float relZ = (v.z - cell.getZ())/cell.size;

	    double dt = (deltaTop * relX) * (1-relZ) + (deltaBottom * relX) * (relZ);

	    return new Temperature(cell.getTemperature().getValue() + dt);
	}
	
	double getHumidityAt(double x, double z){
		return getHumidityAt(new VectorXZ((float)x, (float)z));
	}

	private double getHumidityAt(VectorXZ v) {
        if(cell == null) return Double.NaN;
        if(celldown == null || celldownright == null || cellright == null) return cell.getHumidity();

        float size = cell.size;

        double deltaTop = cellright.getHumidity() - cell.getHumidity();
        double deltaBottom = celldownright.getHumidity() - cellright.getHumidity();

        float relX = (v.x - cell.getX())/cell.size;
        float relZ = (v.z - cell.getZ())/cell.size;

        double dh = (deltaTop * relX) * (1-relZ) + (deltaBottom * relX) * (relZ);

        return cell.getHumidity() + dh;
	}
}
