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
	private double humidity = 6.4;
	private double airAmountOnBlock = Double.NaN;
	private double airAmountHigh = Double.NaN;
	private Temperature temperature;
	private ClimateMap map;
	private double altitude = Double.NaN;
	private double oceanDepth = Double.NaN;
	private double cellArea = Double.NaN;
	private Weather weather = Weather.CLEAR;
	private double humidityMultiplier;

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
		return weather;
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
		humidityMultiplier = Double.NaN;
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
		if(airAmountOnBlock < 0) airAmountOnBlock = 0;
		return airAmountOnBlock;
	}

	public double getLowAltitudePressure() {
		if (Double.isNaN(lowAltitudePressure))
			lowAltitudePressure = getBasePressure();
		if(lowAltitudePressure < 0) lowAltitudePressure = 0;
		return lowAltitudePressure;
	}

	public double getHighAltitudePressure() {
		if(Double.isNaN(highAltitudePressure)){
			highAltitudePressure = ClimateUtils.getGasPressure(10, getAmountHigh(), new Temperature(getTemperature().getValue() * 0.9));
		}
		if(highAltitudePressure < 0) highAltitudePressure = 0;
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
						getAirVolumeOnBlock() * 0.01 + getWaterVolumeOnBlock());
		humidityMultiplier = Double.NaN;
		highAltitudePressure = Double.NaN;
	}

	public void updatePressure() {
		lowAltitudePressure = ClimateUtils.getGasPressure(getAirVolumeOnBlock(),
				getAmountOnBlock(), getTemperature());
	}
	
	private void moveVertically() {
		double dp = getLowAltitudePressure() - getHighAltitudePressure();
		if(dp > 0){
			double transfer = ClimateUtils.getGasAmount(dp/2, getAirVolumeOnBlock(), getTemperature());
			transfer = Math.min(transfer, getAmountOnBlock());
			airAmountOnBlock = Math.max(getAmountOnBlock() - transfer, 0);
			airAmountHigh = Math.max(getAmountHigh() + transfer, 0);
		} else {
			double transfer = ClimateUtils.getGasAmount(dp/2, 10, new Temperature(getTemperature().getValue() * 0.9));
			transfer = Math.min(transfer, getAmountHigh());
			airAmountHigh = Math.max(getAmountHigh() - transfer, 0);
			airAmountOnBlock = Math.max(getAmountOnBlock() + transfer, 0);
		}
	}
	
	private void moveLowAir(){
		ClimateCell lowestPressure = this;
		for(ClimateCell c : getNeighbours()){
			if(c == null) continue;
			if(c.getLowAltitudePressure() < lowestPressure.getLowAltitudePressure()){
				lowestPressure = c;
			}
		}
		double dp = lowestPressure.getLowAltitudePressure() - this.getLowAltitudePressure();
		if(dp < 0){
			double humidityRatio = getHumidity() / getAmountOnBlock();
			double transfer = ClimateUtils.getGasAmount(dp / 4, getAirVolumeOnBlock(), getTemperature());
			transfer = Math.min(transfer, getAmountOnBlock());
			double humidityTransfer = Math.max(transfer * humidityRatio, getHumidity());
			lowestPressure.addAmount(transfer);
			lowestPressure.addHumidity(humidityTransfer);
			airAmountOnBlock = Math.max(getAmountOnBlock() - transfer, 0);
			humidity = Math.max(getHumidity() - humidityTransfer, 0);
		}
	}
	
	private void moveHighAir(){
		ClimateCell lowestPressure = this;
		for(ClimateCell c : getNeighbours()){
			if(c == null) continue;
			if(c.getHighAltitudePressure() < lowestPressure.getHighAltitudePressure()){
				lowestPressure = c;
			}
		}
		double dp = lowestPressure.getHighAltitudePressure() - this.getHighAltitudePressure();
		if(dp < 0){
			double transfer = ClimateUtils.getGasAmount(-dp, 10, new Temperature(getTemperature().getValue() * 0.9));
			transfer = Math.min(transfer, getAmountHigh());
			lowestPressure.addHighAmount(transfer);
			airAmountHigh = Math.max(getAmountHigh() - transfer, 0);
		}
	}

	private void addHighAmount(double transfer) {
		airAmountHigh = Math.max(getAmountHigh() + transfer, 0);
	}

	private void addAmount(double transfer) {
		airAmountOnBlock = Math.max(getAmountOnBlock() + transfer, 0);
	}
	
	private void addHumidity(double transfer) {
		humidity = Math.max(getHumidity() + transfer, 0);
	}

	private double getAmountHigh() {
		if(Double.isNaN(airAmountHigh) || airAmountHigh < 0){
			airAmountHigh = ClimateUtils.getGasAmount(1015, 10, new Temperature(273.15));
		}
		if(airAmountHigh < 0 ) airAmountHigh = 0;
		return airAmountHigh;
	}

	public void update() {
		updateTemperature();
		updatePressure();
		moveVertically();
		moveLowAir();
		moveHighAir();
		updateHumidity();
		updateWeather();
		updateMap();
	}

	private void updateHumidity() {
		double saturation = (100 - getRelativeHumidity()) * 0.01;
		humidity += oceanDepth * saturation * 0.1;
		if(weather == Weather.RAIN){
			humidity -= 0.1;
		} else if(weather == Weather.STORM){
			humidity -= 0.2;
		} else if(weather == Weather.THUNDERSTORM){
			humidity -= 0.3;
		}
		humidity = (humidity < 0 ? 0 : humidity);
	}

	private void updateWeather() {
		if (getTemperature().isCelsiusAbove(30) && getRelativeHumidity() > 75) {
			weather = Weather.THUNDERSTORM;
		} else if (getRelativeHumidity() >= 55 + (Math.random() * 45)) {
			weather = Weather.RAIN;
		}else {
			weather = Weather.CLEAR;
		}
	}
	
	private void preCalcHumidity(){
		double e = Math.exp((17.67 * (getTemperature().getValue() - 273.15))/(getTemperature().getValue() - 30));
		double divider = e * 13.25;
		humidityMultiplier = getTemperature().getValue() / divider;
	}

	public double getRelativeHumidity() {
		if(Double.isNaN(humidityMultiplier)){
			preCalcHumidity();
		}
		return humidity * humidityMultiplier;
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

	public void setWeather(Weather weather) {
		this.weather = weather;
	}

	public double getHumidity() {
		return humidity;
	}
}
