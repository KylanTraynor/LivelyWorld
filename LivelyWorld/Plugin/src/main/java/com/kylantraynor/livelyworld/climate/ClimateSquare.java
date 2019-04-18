package com.kylantraynor.livelyworld.climate;

import com.kylantraynor.livelyworld.Utils;
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

	    float relX = (v.x - cell.getX())/size;
	    float relZ = (v.z - cell.getZ())/size;

	    double t = Utils.bilerp(cell.getTemperature().getValue(),
				cellright.getTemperature().getValue(),
				celldown.getTemperature().getValue(),
				celldownright.getTemperature().getValue(),
				relX, relZ);

	    return new Temperature(t);
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
