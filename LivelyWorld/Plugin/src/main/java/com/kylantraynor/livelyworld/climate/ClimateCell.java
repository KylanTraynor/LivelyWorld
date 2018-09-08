package com.kylantraynor.livelyworld.climate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.kylantraynor.livelyworld.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

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

	private ClimateCell[] neighbours;

	public int cellX;
	public int cellZ;
	public int size;
	
	private int x;
	private int y;
	private int z;

	public double[] verticesX;
	public double[] verticesZ;
	
	public ClimateCell() {
		super();
	}

	public void setCoords(int x, int z){
		this.cellX = x;
		this.cellZ = z;

		this.x = map.toBlockX(cellX);
		this.z = map.toBlockZ(cellZ);

		verticesX = new double[]{map.toBlockX(cellX), map.toBlockX(cellX + 1), map.toBlockX(cellX + 1), map.toBlockX(cellX)};
		verticesZ = new double[]{map.toBlockZ(cellZ), map.toBlockZ(cellZ), map.toBlockZ(cellZ + 1), map.toBlockZ(cellZ + 1)};
	}

	public ClimateCell getRelative(int x, int z){
		return map.getClimateCellAt(this.x + x, this.z + z);
	}
	/**
	 * Get the neighbouring climate cells.
	 * Can be contain null objects if the cell is next to the map border.
	 * @return ClimateCell[]
	 */
	@Override
	public synchronized ClimateCell[] getNeighbours(){
		if(neighbours == null){
			if(z > 0 && z < map.zCount - 1){
				neighbours = new ClimateCell[4];
				neighbours[3] = map.getClimateCellAt(x, z - 1);
				neighbours[4] = map.getClimateCellAt(x, z + 1);
			} else {
				neighbours = new ClimateCell[3];
				if(z == 0){
					neighbours[3] = map.getClimateCellAt(x, z + 1);
				} else {
					neighbours[3] = map.getClimateCellAt(x, z - 1);
				}
			}
			neighbours[1] = map.getClimateCellAt(x > 0 ? x - 1 : map.xCount - 1, z);
			neighbours[2] = map.getClimateCellAt(x < map.xCount - 1 ? x + 1 : 0, z);
		}
		return neighbours;
	}

	/**
	 * Get high atmosphere volume.
	 * @return
	 */
	public double getHighVolume(){
		return 10;
	}
	
	/**
	 * Get the amount of humidity this cell can generate per update.
	 * @return
	 */
	public double getHumidityGeneration(){
		return humidityGeneration;
	}
	
	/**
	 * Get the tropopause temperature.
	 * @return Temperature
	 */
	public Temperature getTropopauseTemperature(){
		return tropopauseTemp;
	}
	
	/**
	 * Get this cell's center location.
	 * @return Location
	 */
	public Location getLocation() {
		return new Location(world, x, y, z);
	}

	public void setWorld(World world) {
		this.world = world;
	}

	/**
	 * Get this cell's world.
	 * @return World
	 */
	public World getWorld() {
		return world;
	}
	
	/**
	 * Get this cell's center X.
	 * @return int
	 */
	public int getX(){
		return x;
	}
	
	/**
	 * Get this cell's center Y.
	 * @return int
	 */
	public int getY(){
		return y;
	}
	
	/**
	 * Get this cell's center Z.
	 * @return int
	 */
	public int getZ(){
		return z;
	}

	/**
	 * Get the current weather in this cell.
	 * @return Weather
	 */
	public Weather getWeather() {
		return weather;
	}

	/**
	 * Get the base temperature.
	 * @return Temperature
	 */
	public Temperature getBaseTemperature() {
		return Temperature.fromCelsius(15);
	}

	/**
	 * Get the reference altitude of this cell.
	 * @return double
	 */
	public double getAltitude() {
		return y; // Could be improved by making an average of the y of the surface blocks around it.
	}

	/**
	 * Get the current temperature of this cell.
	 * @return Temperature
	 */
	public Temperature getTemperature() {
		if(temperature.isNaN()) temperature = getBaseTemperature();
		return temperature;
	}
	
	/**
	 * Get the total area covered by this cell.
	 * @return double
	 */
	public double getArea(){
		if(!Double.isNaN(cellArea)){
			return cellArea;
		}
		cellArea = size*size;
		return cellArea;
	}
	
	/**
	 * Get the total volume covered by this cell.
	 * @return double
	 */
	public double getVolume() {
		if (!Double.isNaN(airVolume))
			return airVolume;
		airVolume = getArea() * (256 - getAltitude());
		return airVolume;
	}
	
	/**
	 * Get the number of air blocks above this cell.
	 * @return
	 */
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
		return (getAmountOnBlock() * 0.00004) + (getWaterVolumeOnBlock()*10) + (getHumidity() * 5);
	}
	
	public double getUpInertia(){
		return (getAmountOnBlock() * 0.00002) + (getWaterVolumeOnBlock()*1) + (getHumidity() * 0.5);
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
	
	private static void processLowTransfer(ClimateCell source, ClimateCell target, double transfer){
		if(transfer <= 0) return;
		double humidityRatio = source.getHumidity() / source.getAmountOnBlock();
		double humidityTransfer = transfer * humidityRatio;
		if(target.getAltitude() != 0){
			humidityTransfer *= source.getAltitude()/target.getAltitude();
		}
		humidityTransfer = Math.min(humidityTransfer, source.getHumidity());
		target.addHumidity(humidityTransfer);
		source.addHumidity(-humidityTransfer);
		Temperature toTemp = target.getTemperature();
		double toTargetInertia = target.getAmountOnBlock() / transfer;
		double toSourceInertia = source.getAmountOnBlock() / transfer;
		target.bringTemperatureTo(source.getTemperature(), toTargetInertia);
		source.bringTemperatureTo(toTemp, toSourceInertia);
		target.addAmount(transfer);
		source.addAmount(-transfer);
		target.lowWind = new WindVector(target.getX() - source.getX(), target.getAltitude() - source.getAltitude(), target.getZ() - source.getZ(), transfer).normalize();
	}
	
	private void moveLowAir(){
		/*ClimateCell highestPressure = this;
		for(ClimateCell c : getNeighbours()){
			if(c == null) continue;
			if(c.getLowAltitudePressure() > highestPressure.getLowAltitudePressure()){
				highestPressure = c;
			}
		}
		*/
		double[] diff;
		int minDiff;
		int cellsToFill;
		for(int i = 0; i < getNeighbours().length; i++){
			diff = new double[getNeighbours().length];
			minDiff = -1;
			cellsToFill = 0;
			
			// Populates the differences array and checks which one is the smallest one.
			for(int m = 0; m < getNeighbours().length; m++){
				if(getNeighbours()[m] == null) continue;
				// Calculates the difference, and caps it to the difference between the target's max level and its current level.
				diff[m] = getLowAltitudePressure() - getNeighbours()[m].getLowAltitudePressure();
				// If there is a positive difference.
				if(diff[m] > 0){
					// Adds one to the number of columns to transfer water to.
					cellsToFill ++;
					// Updates the minDifference if needed.
					minDiff = (minDiff == -1 ? m : (diff[m] < diff[minDiff] ? m : minDiff));
				}
			}
			
			// Fills up all columns if possible.
			if(cellsToFill > 0){
				// Calculates the amount of water to move to each column for equilibrium.
				double transfer = diff[minDiff] / (cellsToFill + 1);
				// If there's at least 1 level to transfer to each column.
				if(transfer > 0){
					// Go through each column.
					for(int i2 = 0; i2 < getNeighbours().length; i2++){
						if(getNeighbours()[i2] == null) continue;
						// If the column can be filled.
						if(diff[i2] > 0){
							ClimateCell target = getNeighbours()[i2];
							double fromExcess = getAmountOnBlock() - ClimateUtils.getGasAmount(this.getLowAltitudePressure() - transfer, getAirVolumeOnBlock(), getTemperature());
							double toLack = ClimateUtils.getGasAmount(target.getLowAltitudePressure() + transfer, target.getAirVolumeOnBlock(), target.getTemperature()) - target.getAmountOnBlock();
							double amount = Math.min(fromExcess, toLack);
							if(amount > 0){
								ClimateCell.processLowTransfer(this, target, amount);
							}
						}
					}
				}
			} else {
				// There was no column to fill, process can stop.
				break;
			}
		}
		
		
		/*double dp = highestPressure.getLowAltitudePressure() - this.getLowAltitudePressure();
		//LivelyWorld.getInstance().getLogger().info("Highest Pressure : " + highestPressure.getLowAltitudePressure() + ", this : " + this.getLowAltitudePressure());
		if(dp > 0){
			double transfer = ClimateUtils.getGasAmount(Math.abs(dp), getAirVolumeOnBlock(), getTemperature());
			transfer = Math.min(transfer, highestPressure.getAmountOnBlock());
			ClimateCell.processLowTransfer(highestPressure, this, transfer);
		} else {
			this.lowWind = WindVector.ZERO;
		}*/
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
		moveLowAir();
		/*moveVertically();
		moveHighAir();*/
		//updateWinds();
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
		} else if (getRelativeHumidity() >= 55 + (Utils.fastRandomInt(15))) {
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
		to.bringTemperatureTo(from.getTemperature(), (to.getAmountOnBlock() / transfer));
		from.bringTemperatureTo(toTemp, (from.getAmountOnBlock() / transfer));
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
		to.bringHighTemperatureTo(from.getHighTemperature(), (to.getAmountHigh() / transfer) * 0.0001);
		from.bringHighTemperatureTo(toTemp, (from.getAmountHigh() / transfer) * 0.0001);
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
