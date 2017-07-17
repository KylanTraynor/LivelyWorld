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
	private double precipitations = 0;
	private double altitude = Double.NaN;
	private double oceanDepth = Double.NaN;
	private double cellArea = Double.NaN;
	private Weather weather = Weather.CLEAR;
	private double humidityMultiplier;
	private Temperature tropopauseTemp;
	private double largestDistance;

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

	public double getHighVolume(){
		return 10;
	}
	
	public Temperature getTropopauseTemperature(){
		if(tropopauseTemp == null){
			tropopauseTemp = new Temperature(225);
		}
		return tropopauseTemp;
	}
	
	public Location getLocation() {
		Block b = world.getHighestBlockAt((int)this.getSite().getX(), (int) this.getSite().getZ());
		if(b != null) b = b.getRelative(BlockFace.DOWN);
		return b.getLocation() ;
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
		return 101300;
	}

	public double getAltitude() {
		if(!Double.isNaN(altitude)) return altitude;
		altitude = world.getHighestBlockYAt((int) getSite().x, (int) getSite().z) - 1;
		double y = altitude;
		while(y > 1 && world.getBlockAt((int) getSite().x, (int) (y - 1), (int) getSite().z).isLiquid() )
			y--;
		
		oceanDepth = altitude - y;
		return altitude;
	}

	public Temperature getTemperature() {
		if (temperature != null) return temperature;
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
			lowAltitudePressure = ClimateUtils.getGasPressure(getAirVolumeOnBlock(), getAmountOnBlock(), getTemperature());
		if(lowAltitudePressure < 0) lowAltitudePressure = 0;
		return lowAltitudePressure;
	}

	public double getHighAltitudePressure() {
		if(Double.isNaN(highAltitudePressure)){
			highAltitudePressure = ClimateUtils.getGasPressure(getHighVolume(), getAmountHigh(), getTropopauseTemperature());
		}
		if(highAltitudePressure < 0) highAltitudePressure = 0;
		return highAltitudePressure;
	}

	public void updateMap() {
		if (HookManager.hasDynmap()) {
			HookManager.getDynmap().updateClimateCell(this);
		}
	};
	
	public Temperature getSurfaceTemperature(){
		return Climate.getAreaSurfaceTemperature(getLocation().getWorld(), getLocation().getBlockX(), getLocation().getBlockZ());
	}
	
	public double getDownInertia(){
		return (getAmountOnBlock() * 0.000001) + (getWaterVolumeOnBlock()*10) + (getHumidity() * 2);
	}
	
	public double getUpInertia(){
		return (getAmountOnBlock() * 0.000001) + getWaterVolumeOnBlock() + (getHumidity() * 0.5);
	}

	public void updateIrradiance() {
		Temperature target = getSurfaceTemperature();
		if(target.getValue() > temperature.getValue()){
			temperature = getTemperature().bringTo(target, getUpInertia());
		} else {
			temperature = getTemperature().bringTo(target, getDownInertia());
		}
		humidityMultiplier = Double.NaN;
		highAltitudePressure = Double.NaN;
		lowAltitudePressure = Double.NaN;
	}

	public void updatePressure() {
		lowAltitudePressure = ClimateUtils.getGasPressure(getAirVolumeOnBlock(),
				getAmountOnBlock(), getTemperature());
	}
	
	private void moveVertically() {
		double dp = getLowAltitudePressure() - getHighAltitudePressure();
		if(dp > 0){
			//Move Air Up
			double transfer = ClimateUtils.getGasAmount(Math.abs(dp), getHighVolume(), getTropopauseTemperature());
			transfer = Math.min(transfer, getAmountOnBlock());
			airAmountOnBlock = Math.max(getAmountOnBlock() - transfer, 0);
			airAmountHigh = Math.max(getAmountHigh() + transfer, 0);
			lowAltitudePressure = Double.NaN;
			highAltitudePressure = Double.NaN;
		} else if(dp < 0) {
			// Move Air Down
			double transfer = ClimateUtils.getGasAmount(Math.abs(dp), getAirVolumeOnBlock(), getTemperature());
			transfer = Math.min(transfer, getAmountHigh());
			airAmountHigh = Math.max(getAmountHigh() - transfer, 0);
			airAmountOnBlock = Math.max(getAmountOnBlock() + transfer, 0);
			lowAltitudePressure = Double.NaN;
			highAltitudePressure = Double.NaN;
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
			highestPressure.bringTemperatureTo(this.getTemperature(), (highestPressure.getAmountOnBlock() / (double) transfer) * 0.1);
			this.bringTemperatureTo(temp, (getAmountOnBlock() / (double) transfer) * 0.1);
		}
	}
	
	private void bringTemperatureTo(Temperature temp, double inertia){
		temperature = getTemperature().bringTo(temp, inertia);
		humidityMultiplier = Double.NaN;
		highAltitudePressure = Double.NaN;
		lowAltitudePressure = Double.NaN;
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
			airAmountHigh = Math.max(getAmountHigh() - transfer, 0);
			highAltitudePressure = Double.NaN;
		}
	}

	private void addHighAmount(double transfer) {
		airAmountHigh = Math.max(getAmountHigh() + transfer, 0);
		highAltitudePressure = Double.NaN;
	}

	private void addAmount(double transfer) {
		airAmountOnBlock = Math.max(getAmountOnBlock() + transfer, 0);
		lowAltitudePressure = Double.NaN;
	}
	
	private void addHumidity(double transfer) {
		humidity = Math.max(getHumidity() + transfer, 0);
	}

	public double getAmountHigh() {
		if(Double.isNaN(airAmountHigh) || airAmountHigh < 0){
			airAmountHigh = ClimateUtils.getGasAmount(70000, getHighVolume(), getTropopauseTemperature());
		}
		if(airAmountHigh < 0 ) airAmountHigh = 0;
		return airAmountHigh;
	}

	public void update() {
		updateIrradiance();
		moveHighAir();
		moveVertically();
		moveLowAir();
		//updateTemperature();
		updateHumidity();
		updateWeather();
		updateMap();
	}

	private void updateTemperature() {
		temperature = ClimateUtils.getGasTemperature(this.getLowAltitudePressure(), this.getAirVolumeOnBlock(), this.getAmountOnBlock());
	}

	private void updateHumidity() {
		double saturation = Math.max(75 - getRelativeHumidity(), 0) * 0.01;
		if(saturation > 0){
			if(oceanDepth > 0){
				humidity += 1 * saturation;
			} else {
				Material m = LivelyWorld.getInstance().getHighestMaterial(getLocation().getWorld(), getLocation().getBlockX(), getLocation().getBlockZ());
				if(m == Material.GRASS ||
						m == Material.LEAVES || 
						m == Material.LEAVES_2 ||
						m == Material.LONG_GRASS ||
						m == Material.DOUBLE_PLANT ||
						m == Material.YELLOW_FLOWER ||
						m == Material.RED_ROSE){
					humidity += 0.1 * saturation;
				} else {
					humidity -= 0.01;
				}
			}
		}
		double precipitation = 0;
		if(weather == Weather.OVERCAST){
			precipitation = 0.5 * (Math.max(getRelativeHumidity() - 50, 0) * 0.01);
		} else if(weather == Weather.RAIN || weather == Weather.SNOW){
			precipitation = 1 * (Math.max(getRelativeHumidity() - 50, 0) * 0.01);
		} else if(weather == Weather.STORM || weather == Weather.SNOWSTORM){
			precipitation = 1.5 * (Math.max(getRelativeHumidity() - 50, 0) * 0.01);
		} else if(weather == Weather.THUNDERSTORM){
			precipitation = 1.5 * (Math.max(getRelativeHumidity() - 50, 0) * 0.01);
		}
		this.precipitations += precipitation;
		humidity -= precipitation;
		humidity = (humidity < 0 ? 0 : humidity);
	}

	private void updateWeather() {
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
		this.temperature = getTemperature().bringTo(new Climate(getLocation()).getAreaSurfaceTemperature(), 0);
		Biome b = world.getBiome((int)getSite().getX(), (int)getSite().getZ());
		if(b == Biome.DESERT || 
				b == Biome.DESERT_HILLS || 
				b == Biome.MESA || 
				b == Biome.MESA_CLEAR_ROCK || 
				b == Biome.MESA_ROCK) {
			
			humidity = 0;
		}
		updatePressure();
		updateMap();
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
}
