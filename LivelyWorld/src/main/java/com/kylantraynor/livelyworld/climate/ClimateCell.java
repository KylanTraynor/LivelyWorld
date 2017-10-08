package com.kylantraynor.livelyworld.climate;

import java.nio.ByteBuffer;
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
	private double normalPressureDifference = lowAltitudePressure - highAltitudePressure;
	private double airVolume = Double.NaN;
	private Long airAmount = null;
	private Long highAirAmount = null;
	private double humidity = 6.4;
	private double airAmountOnBlock = Double.NaN;
	private double airAmountHigh = Double.NaN;
	private Temperature temperature = Temperature.fromCelsius(15);
	private Temperature highTemperature = new Temperature(225);
	private Temperature ntd = new Temperature(temperature.getValue() - highTemperature.getValue());
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
		if(temperature.isNaN()) temperature = getBaseTemperature();
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
			HookManager.getDynmap().updateClimateCellArea(this);
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
		return (getAmountOnBlock() * 0.0001) + (getWaterVolumeOnBlock()*10) + (getHumidity() * 5);
	}
	
	public double getUpInertia(){
		return (getAmountOnBlock() * 0.00001) + (getWaterVolumeOnBlock()) + (getHumidity() * 0.5);
	}

	public void updateIrradiance() {
		Temperature target = getSurfaceTemperature();
		if(target.getValue() > temperature.getValue()){
			bringTemperatureTo(target, getUpInertia());
		} else {
			bringTemperatureTo(target, getDownInertia());
		}
		//bringHighTemperatureTo(getTropopauseTemperature(), 100);
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
		updateMap();
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
			precipitation = 1.1 * (Math.max(getRelativeHumidity() - 20, 0) * 0.01);
		} else if(weather == Weather.STORM){
			precipitation = 1.3 * (Math.max(getRelativeHumidity() - 20, 0) * 0.01);
		} else if(weather == Weather.THUNDERSTORM){
			precipitation = 1.3 * (Math.max(getRelativeHumidity() - 20, 0) * 0.01);
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
		} else if (getRelativeHumidity() >= 55 + (Math.random() * 15)) {
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
	
	public void init(ClimateCellData data){
		x = (int) this.getSite().getX();
		z = (int) this.getSite().getZ();
		y = (int) world.getHighestBlockYAt(x, z) - 1;
		humidityGeneration = Climate.getSurfaceHumidityGeneration(getWorld(), getX(), getZ());
		if(data != null){
			this.temperature = new Temperature(data.getTemperature());
			this.highTemperature = new Temperature(data.getHighTemperature());
			humidity = data.getHumidity();
			lowAltitudePressure = data.getPressure();
			highAltitudePressure = data.getHighPressure();
		} else {
			this.temperature = new Climate(getLocation()).getAreaSurfaceTemperature();
			Biome b = world.getBiome((int)getSite().getX(), (int)getSite().getZ());
			if(b == Biome.DESERT || 
					b == Biome.DESERT_HILLS || 
					b == Biome.MESA || 
					b == Biome.MESA_CLEAR_ROCK || 
					b == Biome.MESA_ROCK) {
				
				humidity = 0;
			}
		}
		oceanDepth = 0;
		int oceanY = y;
		while(oceanY > 1 && world.getBlockAt(x, oceanY, z).isLiquid() ){
			oceanDepth++;
			oceanY--;
		}
		airAmountOnBlock = ClimateUtils.getGasAmount(lowAltitudePressure, getAirVolumeOnBlock(), getTemperature());
		airAmountHigh = ClimateUtils.getGasAmount(highAltitudePressure, getHighVolume(), getHighTemperature());
		updateMap();
	}
	
	public void updateLowPressure(){
		lowAltitudePressure = ClimateUtils.getGasPressure(getAirVolumeOnBlock(), airAmountOnBlock, getTemperature());
	}
	
	public void updateHighPressure(){
		highAltitudePressure = ClimateUtils.getGasPressure(getHighVolume(), airAmountHigh, getHighTemperature());
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
		ClimateCell highestTemp = null;
		ClimateCell lowestHighTemp = null;
		ClimateCell lowestLowPressure = null;
		ClimateCell highestLowPressure = null;
		ClimateCell lowestHighPressure = null;
		ClimateCell highestHighPressure = null;
		ClimateCell lowestTemp = null;
		ClimateCell highestHighTemp = null;
		for(ClimateCell c : getNeighbours()){
			if(c == null)continue;
			if(highestTemp == null){
				highestTemp = c;
				lowestHighTemp = c;
				lowestLowPressure = c;
				highestLowPressure = c;
				lowestHighPressure = c;
				highestHighPressure = c;
				lowestTemp = c;
				highestHighTemp = c;
				continue;
			}
			if(c.getTemperature().getValue() > highestTemp.getTemperature().getValue()){
				highestTemp = c;
			}
			if(c.getTemperature().getValue() < lowestTemp.getTemperature().getValue()){
				lowestTemp = c;
			}
			if(c.getHighTemperature().getValue() < lowestHighTemp.getHighTemperature().getValue()){
				lowestHighTemp = c;
			}
			if(c.getHighTemperature().getValue() > highestHighTemp.getHighTemperature().getValue()){
				highestHighTemp = c;
			}
			if(c.getLowAltitudePressure() < lowestLowPressure.getLowAltitudePressure()){
				lowestLowPressure = c;
			}
			if(c.getLowAltitudePressure() > highestLowPressure.getLowAltitudePressure()){
				highestLowPressure = c;
			}
			if(c.getHighAltitudePressure() < lowestHighPressure.getHighAltitudePressure()){
				lowestHighPressure = c;
			}
			if(c.getHighAltitudePressure() > highestHighPressure.getHighAltitudePressure()){
				highestHighPressure = c;
			}
		}
		/*if(highestTemp != null && lowestHighTemp != null && lowestTemp != null && highestHighTemp != null){
			if(highestTemp.getTemperature().getValue() < this.getTemperature().getValue()){
				highestTemp = this;
			}
			if(lowestHighTemp.getHighTemperature().getValue() > this.getHighTemperature().getValue()){
				lowestHighTemp = this;
			}
			
			if(highestTemp == this || lowestHighTemp == this){
				processVerticalTransfer(highestTemp, lowestHighTemp);
			}
		}*/
		
		if(lowestLowPressure == null || highestLowPressure == null || lowestHighPressure == null || highestHighPressure == null) return;
		
		if(lowestLowPressure.getLowAltitudePressure() > this.getLowAltitudePressure()){
			lowestLowPressure = this;
		}
		if(highestLowPressure.getLowAltitudePressure() < this.getLowAltitudePressure()){
			highestLowPressure = this;
		}
		if(lowestHighPressure.getHighAltitudePressure() > this.getHighAltitudePressure()){
			lowestHighPressure = this;
		}
		if(highestHighPressure.getHighAltitudePressure() < this.getHighAltitudePressure()){
			highestHighPressure = this;
		}
		
		// move to lower pressure.
		if(lowestLowPressure != this){
			processLowTransfer(this, lowestLowPressure);
		} else if(highestLowPressure != this) {
			processLowTransfer(highestLowPressure, this);
		}
		if(lowestHighPressure != this){
			processHighTransfer(this, lowestHighPressure);
		} else if(highestHighPressure != this) {
			processHighTransfer(highestHighPressure, this);
		}
		/*this.addAmount(incomingLowAir - outgoingLowAir);
		this.addHighAmount(incomingHighAir - outgoingHighAir);*/
	}
	
	public void processVerticalTransfer(ClimateCell from, ClimateCell to){
		double dt = (from.getTemperature().getValue() - ntd.getValue()) - to.getHighTemperature().getValue();
		if(dt > 0){ // move light air up
			double meanHighT = ((from.getTemperature().getValue() - ntd.getValue()) + to.getHighTemperature().getValue()) / 2;
			double meanLowT = (from.getTemperature().getValue() + (to.getHighTemperature().getValue() + ntd.getValue())) / 2;
			double lowExcess = Math.abs(ClimateUtils.getGasAmount(from.getLowAltitudePressure(), from.getAirVolumeOnBlock(), new Temperature(meanLowT)));
			double highLack = Math.abs(ClimateUtils.getGasAmount(to.getHighAltitudePressure(), to.getHighVolume(), new Temperature(meanHighT)));
			double transfer = Math.min(lowExcess, highLack);
			to.highTemperature = new Temperature(meanHighT);
			from.temperature = new Temperature(meanLowT);
			/*to.bringHighTemperatureTo(meanHighT, (to.getAmountHigh() / transfer) * 0.001);
			from.bringTemperatureTo(to.getHighTemperature().add(ntd), (from.getAmountOnBlock() / transfer) * 0.001);*/
			to.addHighAmount(transfer);
			from.addAmount(-transfer);
		} else if(dt < 0){ // move heavy air down
			double meanHighT = (from.getHighTemperature().getValue() + (to.getTemperature().getValue() - ntd.getValue())) / 2;
			double meanLowT = ((from.getHighTemperature().getValue() + ntd.getValue()) + to.getTemperature().getValue()) / 2;
			double lowLack = Math.abs(ClimateUtils.getGasAmount(to.getLowAltitudePressure(), to.getAirVolumeOnBlock(), new Temperature(meanLowT)));
			double highExcess = Math.abs(ClimateUtils.getGasAmount(from.getHighAltitudePressure(), from.getHighVolume(), new Temperature(meanHighT)));
			double transfer = Math.min(lowLack, highExcess);
			to.temperature = new Temperature(meanLowT);
			from.highTemperature = new Temperature(meanHighT);
			/*to.bringTemperatureTo(from.getHighTemperature().add(ntd), (to.getAmountHigh() / transfer) * 0.001);
			from.bringHighTemperatureTo(to.getTemperature().remove(ntd), (from.getAmountOnBlock() / transfer) * 0.001);*/
			to.addAmount(transfer);
			from.addHighAmount(-transfer);
		}
	}
	
	public void processLowTransfer(ClimateCell from, ClimateCell to){
		double dp = from.getLowAltitudePressure() - to.getLowAltitudePressure();
		if(dp <= 0) return;
		double meanP = (to.getLowAltitudePressure() + from.getLowAltitudePressure())/2;
		double fromExcess = Math.abs(ClimateUtils.getGasAmount(meanP, from.getAirVolumeOnBlock(), from.getTemperature()) - from.getAmountOnBlock());
		double toLack = Math.abs(ClimateUtils.getGasAmount(meanP, to.getAirVolumeOnBlock(), to.getTemperature()) - to.getAmountOnBlock());
		double transfer = Math.min(fromExcess, toLack);
		double humidityRatio = from.getHumidity() / from.getAmountOnBlock();
		double humidityTransfer = humidityRatio * transfer;
		to.humidity += humidityTransfer;
		from.humidity -= humidityTransfer;
		Temperature toTemp = to.getTemperature();
		to.bringTemperatureTo(from.getTemperature(), (to.getAmountOnBlock() / transfer) * 0.001);
		from.bringTemperatureTo(toTemp, (from.getAmountOnBlock() / transfer) * 0.001);
		to.addAmount(transfer);
		from.addAmount(-transfer);
		from.lowWind = new WindVector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ(), transfer).normalize();
	}
	
	public void processHighTransfer(ClimateCell from, ClimateCell to){
		double dp = to.getHighAltitudePressure() - from.getHighAltitudePressure();
		if(dp <= 0) return;
		double meanP = (to.getHighAltitudePressure() + from.getHighAltitudePressure())/2;
		double fromExcess = Math.abs(ClimateUtils.getGasAmount(meanP, from.getHighVolume(), from.getHighTemperature()) - from.getAmountHigh());
		double toLack = Math.abs(ClimateUtils.getGasAmount(meanP, to.getHighVolume(), to.getHighTemperature()) - to.getAmountHigh());
		double transfer = Math.min(fromExcess, toLack);
		Temperature toTemp = to.getHighTemperature();
		to.bringHighTemperatureTo(from.getHighTemperature(), (to.getAmountHigh() / transfer) * 0.001);
		from.bringHighTemperatureTo(toTemp, (from.getAmountHigh() / transfer) * 0.001);
		to.addHighAmount(transfer);
		from.addHighAmount(-transfer);
		from.highWind = new WindVector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ(), transfer).normalize();
	}

	public Temperature getHighTemperature() {
		if(highTemperature.isNaN()) highTemperature = getTropopauseTemperature();
		return highTemperature;
	}
	
	public ClimateCellData getData(){
		return new ClimateCellData(
				getTemperature().getValue(), 
				getHighTemperature().getValue(), 
				getLowAltitudePressure(),
				getHighAltitudePressure(),
				getHumidity());
	}
}
