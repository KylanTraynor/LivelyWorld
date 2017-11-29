package com.kylantraynor.livelyworld.climate;

import com.kylantraynor.voronoi.VectorXZ;

public class ClimateTriangle {
	ClimateCell cell1;
	ClimateCell cell2;
	ClimateCell cell3;
	
	public ClimateTriangle(ClimateCell cell1, ClimateCell cell2,
			ClimateCell cell3) {
		this.cell1 = cell1;
		this.cell2 = cell2;
		this.cell3 = cell3;
	}
	
	Temperature getTemperatureAt(double x, double z){
		return getTemperatureAt(new VectorXZ((float) x, (float) z));
	}
	
	Temperature getTemperatureAt(VectorXZ v){
		double d1 = cell1.getSite().distanceSquared(v);
		double d2 = cell2.getSite().distanceSquared(v);
		double d3 = cell3.getSite().distanceSquared(v);
		
		double cooef1 = d2 + d3;
		double cooef2 = d1 + d3;
		double cooef3 = d1 + d2;
		double total = (cooef1 + cooef2 + cooef3);
		
		double t1 = cell1.getTemperature().getValue();
		double t2 = cell2.getTemperature().getValue();
		double t3 = cell3.getTemperature().getValue();
		
		
		return new Temperature((t1 * cooef1 + t2 * cooef2 + t3 * cooef3) / total);
	}
	
	double getHumidityAt(double x, double z){
		return getHumidityAt(new VectorXZ((float)x, (float)z));
	}

	private double getHumidityAt(VectorXZ v) {
		double d1 = cell1.getSite().distanceSquared(v);
		double d2 = cell2.getSite().distanceSquared(v);
		double d3 = cell3.getSite().distanceSquared(v);
		
		double cooef1 = d2 + d3;
		double cooef2 = d1 + d3;
		double cooef3 = d1 + d2;
		double total = (cooef1 + cooef2 + cooef3);
		
		double t1 = cell1.getHumidity();
		double t2 = cell2.getHumidity();
		double t3 = cell3.getHumidity();
		
		
		return (t1 * cooef1 + t2 * cooef2 + t3 * cooef3) / total;
	}
}
