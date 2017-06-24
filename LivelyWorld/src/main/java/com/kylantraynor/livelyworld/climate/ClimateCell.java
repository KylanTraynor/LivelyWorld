package com.kylantraynor.livelyworld.climate;

import org.bukkit.Location;
import org.bukkit.World;

import com.kylantraynor.livelyworld.hooks.HookManager;
import com.kylantraynor.voronoi.VCell;
import com.kylantraynor.voronoi.VTriangle;
import com.kylantraynor.voronoi.VectorXZ;

public class ClimateCell extends VCell {

	private World world;
	private double lowAltitudePressure = getBasePressure();
	private double highAltitudePressure = getBasePressure();
	private double airVolume = Double.NaN;
	private Long airAmount = null;
	private Long highAirAmount = null;
	private double airAmountOnBlock = Double.NaN;
	private Temperature temperature;
	private ClimateMap map;
	private double altitude = Double.NaN;
	private double oceanDepth = Double.NaN;
	private double cellArea = Double.NaN;

	public ClimateCell() {
		super();
	}
	
	@Override
	public ClimateCell[] getNeighbours(){
		ClimateCell[] result = new ClimateCell[super.getNeighbours().length];
		for(int i = 0; i < super.getNeighbours().length; i++){
			result[i] = (ClimateCell) super.getNeighbours()[i];
		}
		return result;
	}

	public Location getLocation() {
		return new Location(world, (double) getSite().getX(), getAltitude(),
				(double) getSite().getZ());
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public World getWorld() {
		return world;
	}

	public Weather getWeather() {
		if (world.hasStorm()) {
			return Weather.RAIN;
		} else if (world.isThundering()) {
			return Weather.THUNDERSTORM;
		}
		return Weather.CLEAR;
	}

	public double getSunRadiation() {
		return Planet.getPlanet(world).getSunRadiation(
				new Location(world, getSite().getX(), 255, getSite().getZ()));
	}

	public Temperature getBaseTemperature() {
		return Temperature.fromCelsius(15);
	}

	public double getBasePressure() {
		return 1013;
	}

	public double getAltitude() {
		if(!Double.isNaN(altitude)) return altitude;
		altitude = world.getHighestBlockYAt((int) getSite().x, (int) getSite().z);
		double y = altitude;
		while(y > 1 && world.getBlockAt((int) getSite().x, (int) (y - 1), (int) getSite().z).isLiquid() )
			y--;
		
		oceanDepth = altitude - y;
		return altitude;
	}

	public Temperature getTemperature() {
		if (temperature != null)
			return temperature;
		temperature = getBaseTemperature();
		return temperature;
	}

	public double getArea(){
		if(!Double.isNaN(cellArea)){
			return cellArea;
		}
		double area = 0;
		for (VTriangle t : getTriangles()) {
			VectorXZ a = t.points[0];
			VectorXZ b = t.points[1];
			VectorXZ c = t.points[2];
			area += (a.getX() * (b.getZ() - c.getZ()) + b.getX()
					* (c.getZ() - a.getZ()) + c.getX() * (a.getZ() - b.getZ())) * 0.5;
		}
		cellArea = area;
		return cellArea;
	}
	
	public double getVolume() {
		if (!Double.isNaN(airVolume))
			return airVolume;
		airVolume = getArea() * (255 - getAltitude());
		return airVolume;
	}
	
	public double getAirVolumeOnBlock() {
		return 255 - getAltitude();
	}
	
	public double getWaterVolume(){
		return getArea() * oceanDepth;
	}
	
	public double getWaterVolumeOnBlock(){
		return oceanDepth;
	}

	/*public long getAmount() {
		if (airAmount != null)
			return airAmount;
		airAmount = ClimateUtils.getGasAmount(getBasePressure(), getVolume(),
				getBaseTemperature());
		return airAmount;
	}*/
	
	public double getAmountOnBlock(){
		if(Double.isNaN(airAmountOnBlock)){
			airAmountOnBlock = ClimateUtils.getGasAmount(getBasePressure(), getAirVolumeOnBlock(), getBaseTemperature());
		}
		return airAmountOnBlock;
	}

	public double getLowAltitudePressure() {
		if (!Double.isNaN(lowAltitudePressure))
			return lowAltitudePressure;
		lowAltitudePressure = getBasePressure();
		return lowAltitudePressure;
	}

	public double getHighAltitudePressure() {
		return highAltitudePressure;
	}

	public void updateMap() {
		if (HookManager.hasDynmap()) {
			HookManager.getDynmap().updateClimateCell(this);
		}
	}

	public void updateTemperature() {
		temperature = getTemperature()
				.bringTo(
						//Planet.getPlanet(world).getDefaultAirTemperature(getLocation()), 
						Planet.getPlanet(world).getClimate(getLocation()).getAreaTemperature(),
						getAirVolumeOnBlock() * 0.01 + getWaterVolumeOnBlock() * 0.1);
	}

	public void updatePressure() {
		lowAltitudePressure = ClimateUtils.getGasPressure(getAirVolumeOnBlock(),
				getAmountOnBlock(), getTemperature());
	}

	public void update() {
		updateTemperature();
		updatePressure();
		updateMap();
		updateWinds();
	}

	private void updateWinds() {
		for(ClimateCell c : this.getNeighbours()){
			double dt = c.getTemperature().getValue() - this.getTemperature().getValue();
			if(dt < 0){
				
			}
		}
	}

	public void setMap(ClimateMap climateMap) {
		this.map = climateMap;
	}
	
	public ClimateMap getMap(){
		return map;
	}
	
	public void init(){
		this.temperature = getTemperature().bringTo(Planet.getPlanet(world).getDefaultAirTemperature(getLocation()), 0);
		updatePressure();
		updateMap();
	}
}
