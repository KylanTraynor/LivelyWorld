package com.kylantraynor.livelyworld.climate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.hooks.HookManager;
import com.kylantraynor.voronoi.VCell;
import com.kylantraynor.voronoi.VTriangle;
import com.kylantraynor.voronoi.VectorXZ;

public class ClimateCell extends VCell {

	private World world;
	private double lowAltitudePressure = 101300;
	private double highAltitudePressure = 70000;
	private double airVolume = Double.NaN;
	private Long airAmount = null;
	private Long highAirAmount = null;
	private double humidity = 6.4;
	private double airAmountOnBlock = Double.NaN;
	private double airAmountHigh = Double.NaN;
	private Temperature temperature = Temperature.fromCelsius(15);
	private Temperature highTemperature = new Temperature(225);
	private ClimateMap map;
	private double precipitations = 0;
	private double altitude = Double.NaN;
	private double oceanDepth = Double.NaN;
	private double cellArea = Double.NaN;
	private Weather weather = Weather.CLEAR;
	private double humidityMultiplier;
	private Temperature tropopauseTemp = new Temperature(225);
	private double largestDistance;
	private WindVector lowWind = new WindVector(0,0,0,0);
	private WindVector highWind = new WindVector(0,0,0,0);
	private double airMovingUp = 0;
	private double airMovingDown = 0;
	private double incomingLowAir = 0;
	private double outgoingLowAir = 0;
	private double incomingHighAir = 0;
	private double outgoingHighAir = 0;
	
	private Temperature minTemp;
	private Temperature maxTemp;
	
	private double humidityGeneration = Double.NaN;
	
	private int x;
	private int y;
	private int z;
	
	public ClimateCell() {
		super();
	}
	
	@Override
	public synchronized ClimateCell[] getNeighbours(){
		ClimateCell[] result = new ClimateCell[super.getNeighbours().length];
		for(int i = 0; i < super.getNeighbours().length; i++){
			result[i] = (ClimateCell) super.getNeighbours()[i];
		}
		return result;
	}

	public double getHighVolume(){
		return 10;
	}
	
	public double getHumidityGeneration(){
		return humidityGeneration;
	}
	
	public Temperature getTropopauseTemperature(){
		return tropopauseTemp;
	}
	
	public Location getLocation() {
		return new Location(world, x, y, z);
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public World getWorld() {
		return world;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public int getZ(){
		return z;
	}

	public Weather getWeather() {
		return weather;
	}

	public Temperature getBaseTemperature() {
		return Temperature.fromCelsius(15);
	}

	public double getAltitude() {
		return y; // Could be improved by making an average of the y of the surface blocks around it.
	}

	public Temperature getTemperature() {
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
		airVolume = getArea() * (256 - getAltitude());
		return airVolume;
	}
	
	public double getAirVolumeOnBlock() {
		return 256 - getAltitude();
	}
	
	public double getWaterVolume(){
		return getArea() * oceanDepth;
	}
	
	public double getWaterVolumeOnBlock(){
		return oceanDepth;
	}
	
	public double getAmountOnBlock(){
		return airAmountOnBlock;
	}

	public double getLowAltitudePressure() {
		if(Double.isNaN(lowAltitudePressure)) updateLowPressure();
		return lowAltitudePressure;
	}

	public double getHighAltitudePressure() {
		if(Double.isNaN(highAltitudePressure)) updateHighPressure();
		return highAltitudePressure;
	}

	public void updateMap() {
		if (HookManager.hasDynmap()) {
			HookManager.getDynmap().updateClimateCell(this);
		}
	};
	
	public Temperature getSurfaceTemperature(){
		if(minTemp == null || world.isChunkLoaded(x >> 4, z >> 4)){
			minTemp = Climate.getAreaSurfaceMinTemperature(world, x, z);
		}
		if(maxTemp == null || world.isChunkLoaded(x >> 4, z >> 4)){
			maxTemp = Climate.getAreaSurfaceMaxTemperature(world, x, z);
		}
		double dif = maxTemp.getValue() - minTemp.getValue();
		return new Temperature(minTemp.getValue() + (dif * Planet.getPlanet(world).getSunDirectRadiation(world, x, y, z)));
		//return Climate.getAreaSurfaceTemperature(getLocation().getWorld(), getLocation().getBlockX(), getLocation().getBlockZ());
	}
	
	public double getDownInertia(){
		return (getAmountOnBlock() * 0.00001) + (getWaterVolumeOnBlock()*100) + (getHumidity() * 5);
	}
	
	public double getUpInertia(){
		return (getAmountOnBlock() * 0.000001) + getWaterVolumeOnBlock() + (getHumidity() * 0.5);
	}

	public void updateIrradiance() {
		Temperature target = getSurfaceTemperature();
		if(target.getValue() > temperature.getValue()){
			bringTemperatureTo(target, getUpInertia());
		} else {
			bringTemperatureTo(target, getDownInertia());
		}
		bringHighTemperatureTo(getTropopauseTemperature(), 100);
	}
	
	private void moveVertically() {
		double normalDifference = 101300 - 70000;
		double currentDifference = getLowAltitudePressure() - getHighAltitudePressure();
		double dp = currentDifference - normalDifference;
		if(dp > 0){
			//Move Air Up
			double transfer = ClimateUtils.getGasAmount(Math.abs(dp), getHighVolume(), getTropopauseTemperature());
			transfer = Math.min(transfer, getAmountOnBlock());
			addHighAmount(transfer);
			addAmount(-transfer);
		} else if(dp < 0) {
			// Move Air Down
			double transfer = ClimateUtils.getGasAmount(Math.abs(dp), getAirVolumeOnBlock(), getTemperature());
			transfer = Math.min(transfer, getAmountHigh());
			addHighAmount(-transfer);
			addAmount(transfer);
		}
	}
	
	private void moveLowAir(){
		ClimateCell highestPressure = this;
		for(ClimateCell c : getNeighbours()){
			if(c == null) continue;
			if(c.getLowAltitudePressure() > highestPressure.getLowAltitudePressure()){
				highestPressure = c;
			}
		}
		double dp = highestPressure.getLowAltitudePressure() - this.getLowAltitudePressure();
		//LivelyWorld.getInstance().getLogger().info("Highest Pressure : " + highestPressure.getLowAltitudePressure() + ", this : " + this.getLowAltitudePressure());
		if(dp > 0){
			double humidityRatio = highestPressure.getHumidity() / highestPressure.getAmountOnBlock();
			double transfer = ClimateUtils.getGasAmount(Math.abs(dp), getAirVolumeOnBlock(), getTemperature());
			transfer = Math.min(transfer, highestPressure.getAmountOnBlock());
			double humidityTransfer = Math.min(transfer * humidityRatio, highestPressure.getHumidity());
			if(highestPressure.getAltitude() > 175 && highestPressure.getAltitude() > getAltitude()){
				humidityTransfer = 0;
			}
			if(getRelativeHumidity() <= 99){
				addHumidity(humidityTransfer);
				
				highestPressure.addHumidity(-humidityTransfer);
			}
			addAmount(transfer);
			highestPressure.addAmount(-transfer);
			Temperature temp = highestPressure.getTemperature();
			highestPressure.bringTemperatureTo(this.getTemperature(), (highestPressure.getAmountOnBlock() / (double) transfer) * 0.25);
			this.bringTemperatureTo(temp, (getAmountOnBlock() / (double) transfer) * 0.25);
			this.lowWind = new WindVector(this.getX() - highestPressure.getX(), this.getAltitude() - highestPressure.getAltitude(), this.getZ() - highestPressure.getZ(), transfer);//.normalize();
			//LivelyWorld.getInstance().getLogger().info("Wind set to " + lowWind.toString());
		} else {
			this.lowWind = WindVector.ZERO;
		}
	}
	
	private void bringTemperatureTo(Temperature temp, double inertia){
		temperature = getTemperature().bringTo(temp, inertia);
		humidityMultiplier = Double.NaN;
		lowAltitudePressure = Double.NaN;
	}
	
	private void bringHighTemperatureTo(Temperature temp, double inertia){
		highTemperature = getHighTemperature().bringTo(temp, inertia);
		highAltitudePressure = Double.NaN;
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
			double transfer = ClimateUtils.getGasAmount(Math.abs(dp), lowestPressure.getHighVolume(), lowestPressure.getTropopauseTemperature());
			transfer = Math.min(transfer, getAmountHigh());
			lowestPressure.addHighAmount(transfer);
			this.addHighAmount(-transfer);
			this.highWind = new WindVector(lowestPressure.getX() - this.getX(), 0, lowestPressure.getZ() - this.getZ(), transfer);//.normalize();
		} else {
			this.highWind = WindVector.ZERO;
		}
	}

	private void addHighAmount(double transfer) {
		airAmountHigh = Math.max(airAmountHigh + transfer, 0);
		highAltitudePressure = Double.NaN;
	}

	private void addAmount(double transfer) {
		airAmountOnBlock = Math.max(airAmountOnBlock + transfer, 0);
		lowAltitudePressure = Double.NaN;
	}
	
	private void addHumidity(double transfer) {
		humidity = Math.max(getHumidity() + transfer, 0);
	}

	public double getAmountHigh() {
		return airAmountHigh;
	}
	
	public WindVector getLowWind(){
		return lowWind;
	}
	
	public WindVector getHighWind(){
		return highWind;
	}

	public void update() {
		updateIrradiance();
		/*moveLowAir();
		moveVertically();
		moveHighAir();*/
		updateWinds();
		//updateTemperature();
		updateHumidity();
		updateWeather();
		//updateMap();
	}

	private void updateTemperature() {
		temperature = ClimateUtils.getGasTemperature(this.getLowAltitudePressure(), this.getAirVolumeOnBlock(), this.getAmountOnBlock());
	}

	void updateHumidity() {
		double saturation = Math.max(75 - getRelativeHumidity(), 0) * 0.01;
		if(saturation > 0){
			humidity += (getHumidityGeneration() * saturation);
		}
		double precipitation = 0;
		if(weather == Weather.OVERCAST || weather == Weather.SNOW){
			precipitation = 0.5 * (Math.max(getRelativeHumidity() - 50, 0) * 0.01);
		} else if(weather == Weather.RAIN || weather == Weather.SNOWSTORM){
			precipitation = 1 * (Math.max(getRelativeHumidity() - 50, 0) * 0.01);
		} else if(weather == Weather.STORM){
			precipitation = 1.1 * (Math.max(getRelativeHumidity() - 50, 0) * 0.01);
		} else if(weather == Weather.THUNDERSTORM){
			precipitation = 1.1 * (Math.max(getRelativeHumidity() - 50, 0) * 0.01);
		}
		this.precipitations += precipitation;
		humidity -= precipitation;
		humidity = (humidity < 0 ? 0 : humidity);
	}

	void updateWeather() {
		if (getTemperature().isCelsiusAbove(30) && getRelativeHumidity() > 75) {
			weather = Weather.THUNDERSTORM;
		} else if (getRelativeHumidity() >= 80){
			if(getTemperature().isCelsiusBelow(3)){
				weather = Weather.SNOWSTORM;
			} else {
				weather = Weather.STORM;
			}
		} else if (getRelativeHumidity() >= 55 + (Math.random() * 45)) {
			if(getTemperature().isCelsiusBelow(3)){
				weather = Weather.SNOW;
			} else {
				weather = Weather.RAIN;
			}
		} else if (getRelativeHumidity() >= 55){
			weather = Weather.OVERCAST;
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
		x = (int) this.getSite().getX();
		z = (int) this.getSite().getZ();
		y = (int) world.getHighestBlockYAt(x, z) - 1;
		humidityGeneration = Climate.getSurfaceHumidityGeneration(getWorld(), getX(), getZ());
		this.temperature = new Climate(getLocation()).getAreaSurfaceTemperature();
		Biome b = world.getBiome((int)getSite().getX(), (int)getSite().getZ());
		if(b == Biome.DESERT || 
				b == Biome.DESERT_HILLS || 
				b == Biome.MESA || 
				b == Biome.MESA_CLEAR_ROCK || 
				b == Biome.MESA_ROCK) {
			
			humidity = 0;
		}
		oceanDepth = 0;
		int oceanY = y;
		while(oceanY > 1 && world.getBlockAt(x, oceanY, z).isLiquid() ){
			oceanDepth++;
			oceanY--;
		}
		airAmountOnBlock = ClimateUtils.getGasAmount(lowAltitudePressure, getAirVolumeOnBlock(), getBaseTemperature());
		airAmountHigh = ClimateUtils.getGasAmount(highAltitudePressure, getHighVolume(), getTropopauseTemperature());
		updateMap();
	}
	
	public void updateLowPressure(){
		lowAltitudePressure = ClimateUtils.getGasPressure(getAirVolumeOnBlock(), airAmountOnBlock, temperature);
	}
	
	public void updateHighPressure(){
		highAltitudePressure = ClimateUtils.getGasPressure(getHighVolume(), airAmountHigh, getTropopauseTemperature());
	}

	public void setWeather(Weather weather) {
		this.weather = weather;
	}

	public double getHumidity() {
		return humidity;
	}
	
	public String[] getPlayersWithin(){
		List<String> result = new ArrayList<String>();
		for(Entry<String, ClimateCell> e : LivelyWorld.getInstance().getClimateModule().getPlayerCache().entrySet()){
			if(e.getValue() == this){
				result.add(e.getKey());
			}
		}
		return result.toArray(new String[result.size()]);
	}
	
	public double getMostDistance(){
		if(!Double.isNaN(largestDistance)) return largestDistance;
		double result = 0;
		for(int i = 0; i < this.getVerticesX().length; i++){
			double distSquared = this.getSite().distanceSquared(new VectorXZ((float) getVerticesX()[i], (float) getVerticesZ()[i]));
			if(distSquared > result){
				result = distSquared;
			}
		}
		largestDistance = Math.sqrt(result);
		return largestDistance;
	}

	public double getPrecipitations() {
		return precipitations;
	}

	public void updateWinds() {
		this.lowWind = WindVector.ZERO;
		this.highWind = WindVector.ZERO;
		double transfer = 0;
		ClimateCell highestTemp = this;
		ClimateCell lowestHighTemp = this;
		ClimateCell lowestLowPressure = this;
		ClimateCell lowestHighPressure = this;
		for(ClimateCell c : getNeighbours()){
			if(c == null)continue;
			if(c.getTemperature().getValue() > highestTemp.getTemperature().getValue()){
				highestTemp = c;
			}
			if(c.getHighTemperature().getValue() < lowestHighTemp.getHighTemperature().getValue()){
				lowestHighTemp = c;
			}
			if(c.getLowAltitudePressure() < lowestLowPressure.getLowAltitudePressure()){
				lowestLowPressure = c;
			}
			if(c.getHighAltitudePressure() < lowestHighPressure.getHighAltitudePressure()){
				lowestHighPressure = c;
			}
		}
		if(highestTemp == this && lowestHighTemp != this){ // Light air
			// move air up.
			transfer = incomingLowAir;
			incomingLowAir = 0;
			highAirAmount += (long) transfer;
			this.bringHighTemperatureTo(this.getTemperature(), (this.getAmountHigh() / transfer) * 0.1);
			this.bringTemperatureTo(this.getHighTemperature(), (this.getAmountOnBlock() / transfer) * 0.1);
		} else if(lowestHighTemp == this && highestTemp != this){ // Heavy air
			// move air down.
			transfer = incomingHighAir;
			incomingHighAir = 0;
			airAmountOnBlock += (long)transfer;
			this.bringHighTemperatureTo(this.getTemperature(), (this.getAmountHigh() / transfer) * 0.1);
			this.bringTemperatureTo(this.getHighTemperature(), (this.getAmountOnBlock() / transfer) * 0.1);
		} else {
			// move to lower pressure.
			if(lowestLowPressure != this){
				double dp = this.getLowAltitudePressure() - lowestLowPressure.getLowAltitudePressure();
				transfer = Math.min(ClimateUtils.getGasAmount(dp/2, this.getAirVolumeOnBlock(), this.getTemperature()), this.getAmountOnBlock());
				double humidityRatio = this.getHumidity() / this.getAmountOnBlock();
				double humidityTransfer = humidityRatio * transfer;
				lowestLowPressure.incomingLowAir += transfer;
				this.outgoingLowAir += transfer;
				lowestLowPressure.humidity += humidityTransfer;
				this.humidity -= humidityTransfer;
				lowestLowPressure.bringTemperatureTo(this.getTemperature(), (lowestLowPressure.getAmountOnBlock() / transfer) * 0.1);
				this.bringTemperatureTo(lowestLowPressure.getTemperature(), (this.getAmountOnBlock() / transfer) * 0.1);
				this.lowWind = new WindVector(lowestLowPressure.getX() - this.getX(), lowestLowPressure.getY() - this.getY(), lowestLowPressure.getZ() - this.getZ(), transfer);
			} else {
				
			}
			if(lowestHighPressure != this){
				double dp = this.getHighAltitudePressure() - lowestHighPressure.getHighAltitudePressure();
				transfer = Math.min(ClimateUtils.getGasAmount(dp/2, this.getHighVolume(), this.getHighTemperature()), this.getAmountHigh());
				lowestHighPressure.incomingHighAir += transfer;
				this.outgoingHighAir += transfer;
				lowestHighPressure.bringHighTemperatureTo(this.getHighTemperature(), (lowestHighPressure.getAmountHigh() / transfer) * 0.1);
				this.bringHighTemperatureTo(lowestHighPressure.getHighTemperature(), (this.getAmountHigh() / transfer) * 0.1);
				this.highWind = new WindVector(lowestHighPressure.getX() - this.getX(), lowestHighPressure.getY() - this.getY(), lowestHighPressure.getZ() - this.getZ(), transfer);
			}
		}
		this.addAmount(incomingLowAir - outgoingLowAir);
		this.addHighAmount(incomingHighAir - outgoingHighAir);
	}

	public Temperature getHighTemperature() {
		return highTemperature;
	}
}
