package com.kylantraynor.livelyworld.climate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.bukkit.Location;
import org.bukkit.World;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils.SizedList;
import com.kylantraynor.livelyworld.hooks.HookManager;
import com.kylantraynor.livelyworld.hooks.WorldBorderHook;
import com.kylantraynor.voronoi.VCell;
import com.kylantraynor.voronoi.VSite;
import com.kylantraynor.voronoi.VectorXZ;
import com.kylantraynor.voronoi.Voronoi;

public class ClimateMap {

	private Voronoi<ClimateCell> generator;
	private World world;
	private int resolution;
	private boolean generated = false;
	private Temperature lowestTemperature = Temperature.fromCelsius(0);
	private Temperature highestTemperature = Temperature.fromCelsius(20);
	private SizedList<ClimateCell> cache = new SizedList<ClimateCell>(10);

	public ClimateMap(World world) {
		this(world, 80);
	}

	public ClimateMap(World world, int resolution) {
		this.world = world;
		this.resolution = resolution;
	}
	
	public double incrementGenerationZ(double z, double step){
		if(z < 0){
			z = Math.min(z + step, 0);
		}
		z = -z;
		return z;
	}

	public double zAdjustedXStep(double z, double step){
		return Math.max(step * Math.cos(z / Planet.getPlanet(world).getMaxZ()), 1);
	}
	
	public void generateMap() {
		if (HookManager.hasWorldBorder()) {

			WorldBorderHook hook = HookManager.getWorldBorder();
			Location center = hook.getWorldCenter(world);
			float minX = (float) (center.getX() - hook.getWorldRadiusX(world));
			float minZ = (float) (center.getZ() - hook.getWorldRadiusZ(world));
			float maxX = (float) (center.getX() + hook.getWorldRadiusX(world));
			float maxZ = (float) (center.getZ() + hook.getWorldRadiusZ(world));
			//int halfRes = resolution / 2;
			double ratio = (maxX - minX)/(maxZ - minZ);
			List<VSite> sites = new ArrayList<VSite>();
			
			double zRange = Math.max(Math.abs(minZ), Math.abs(maxZ));
			double zStep = (zRange * 2 * ratio) / resolution;
			double xStep = (maxX - minX) / resolution;
			
			for(double z = - zRange; z <= -zStep || z >= zStep; z = incrementGenerationZ(z, zStep)){
				if(z == 0) break;
				if(z < minZ || z > maxZ) continue;
				//LivelyWorld.getInstance().getLogger().info("Current Z = " + z);
				double zAdjustedXStep = zAdjustedXStep(z, xStep);
				for(double x = maxX; x >= minX; x -= zAdjustedXStep){
					VSite s = new VSite(
							(float) (x + Math.random() * zAdjustedXStep),
							(float) (z + Math.random() * zStep),
							1);
					sites.add(s);
				}
			}
			double zAdjustedXStep = zAdjustedXStep(0, xStep);
			for(double x = maxX; x >= minX; x -= zAdjustedXStep){
				VSite s = new VSite(
						(float) (x + Math.random() * zAdjustedXStep),
						(float) (Math.random() * zStep),
						1);
				sites.add(s);
			}
			
			/*for (int x = (int) minX + halfRes; x < maxX - halfRes; x += resolution) {
				for (int z = (int) minZ + halfRes; z < maxZ - halfRes; z += resolution) {
					VSite s = new VSite(
							(float) (x + (((2 * Math.random()) - 1) * resolution)),
							(float) (z + (((2 * Math.random()) - 1) * resolution)),
							1);
					sites.add(s);
				}
			}*/
			this.generator = new Voronoi<ClimateCell>(ClimateCell.class,
					sites.toArray(new VSite[sites.size()]), minX, minZ, maxX,
					maxZ);
		}

		if (generator == null)
			return;

		this.generator.generate();
		LivelyWorld.getInstance().getLogger().info("Attempting to load previous climate data for map...");
		List<ClimateCellData> data = loadAllData();
		if(data.size() > 0){
			LivelyWorld.getInstance().getLogger().info(data.size() + " previous data has been found."); 
		} else {
			LivelyWorld.getInstance().getLogger().warning("No previous climate data could be found.");
		}
		for(int i = 0; i < generator.getCells().length; i++){
			generator.getCells()[i].setWorld(world);
			generator.getCells()[i].setMap(this);
			if(data.size() > i){
				generator.getCells()[i].init(data.get(i));
			} else {
				generator.getCells()[i].init(null);
			}
		}
		/*for (ClimateCell c : generator.getCells()) {
			c.setWorld(world);
			c.setMap(this);
			c.init();
		}*/
		generated = true;
	}

	public ClimateCell getCell(VSite site) {
		return this.generator.getCell(site);
	}

	public ClimateCell getClimateCellAt(Location location) {
		if (location == null)
			throw new NullPointerException("Location can't be Null");
		if (generated) {
			ClimateCell result = null;
			for(ClimateCell c : cache){
				if(c != null){
					if(c.isInside(new VectorXZ((float) location.getX(), (float) location.getZ()))){
						result = c;
						break;
					}
				}
			}
			if(result == null){
				result = this.generator.getCellAt(new VectorXZ(
					(float) location.getX(), (float) location.getZ()));
				cache.add(result);
			}
			return result;
		}
		return null;
	}

	public ClimateCell[] getCells() {
		return this.generator.getCells();
	}

	private int lastCellUpdateId = 0;
	private double highestHumidity = 0;
	private double lowestLowPressure = 101300;
	private double highestLowPressure = 101300;
	private double highestHighPressure = 70000;
	private double lowestHighPressure = 70000;
	private double highestWindSpeed = 0;
	public boolean hasChanged;
	private Temperature highestHighTemperature = Temperature.fromCelsius(0);
	private Temperature lowestHighTemperature = Temperature.fromCelsius(0);
	
	public void randomCellUpdate() {
		lastCellUpdateId = (int) Math.floor(Math.random() * getCells().length);// = lastCellUpdateId >= getCells().length - 1 ? 0 : lastCellUpdateId + 1;
		/*if(lastCellUpdateId == 0){
			this.highestHumidity = 0;
		}*/
		ClimateCell c = getCells()[lastCellUpdateId];
		if (c != null)
		{
			for(ClimateCell cell : c.getNeighbours()){
				if(cell != null){
					cell.updateIrradiance();
				}
			}
			c.update();
			for(ClimateCell cell : c.getNeighbours()){
				if(cell != null){
					cell.updateHumidity();
					cell.updateWeather();
					cell.updateMap();
				}
			}
			if(c.getTemperature().getValue() < lowestTemperature.getValue()){
				lowestTemperature = c.getTemperature();
			}
			if(c.getTemperature().getValue() > highestTemperature.getValue()){
				highestTemperature = c.getTemperature();
			}
			if(c.getHighTemperature().getValue() < lowestHighTemperature.getValue()){
				lowestHighTemperature = c.getHighTemperature();
			}
			if(c.getHighTemperature().getValue() > lowestHighTemperature.getValue()){
				highestHighTemperature = c.getHighTemperature();
			}
			if(c.getLowAltitudePressure() > highestLowPressure){
				highestLowPressure = c.getLowAltitudePressure();
			}
			if(c.getLowAltitudePressure() < lowestLowPressure){
				lowestLowPressure = c.getLowAltitudePressure();
			}
			if(c.getHighAltitudePressure() > highestHighPressure){
				highestHighPressure = c.getHighAltitudePressure();
			}
			if(c.getHighAltitudePressure() < lowestHighPressure){
				lowestHighPressure = c.getHighAltitudePressure();
			}
			if(c.getHumidity() > highestHumidity){
				highestHumidity = c.getHumidity();
			}
			if(c.getHumidity() < highestHumidity){
				highestHumidity = c.getHumidity();
			}
			if(c.getLowWind().getSpeed() > highestWindSpeed){
				highestWindSpeed = c.getLowWind().getSpeed();
			}
		}
	}
	
	public Temperature getCurrentHighestTemperature(){
		if(!hasChanged) return highestTemperature;
		updateMinMaxValues();
		return highestTemperature;
	}
	
	public Temperature getCurrentLowestTemperature(){
		if(!hasChanged) return lowestTemperature;
		updateMinMaxValues();
		return lowestTemperature;
	}
	
	public Temperature getCurrentHighestHighTemperature(){
		if(!hasChanged) return highestHighTemperature;
		updateMinMaxValues();
		return highestHighTemperature;
	}
	
	public Temperature getCurrentLowestHighTemperature(){
		if(!hasChanged) return lowestHighTemperature;
		updateMinMaxValues();
		return lowestHighTemperature;
	}

	/**
	 * Returns the temperature at the location from averaging the surrounding {@link ClimateCell}.
	 * @param location
	 * @return {@link Temperature} which can be NaN.
	 */
	public Temperature getTemperatureAt(Location location) {
		
		ClimateCell cell = getClimateCellAt(location);
		if(cell == null) return Temperature.NaN;
		VectorXZ v = new VectorXZ((float) location.getX(), (float) location.getZ());
		
		ClimateCell cell2 = null;
		ClimateCell cell3 = null;
		
		for(VCell c : cell.getNeighbours()){
			if(c == null) continue;
			if(cell3 == null) cell3 = (ClimateCell)c;
			else {
				if(cell3.getSite().distanceSquared(v) > c.getSite().distanceSquared(v)){
					if(cell2 == null) cell2 = (ClimateCell) c;
					else if(cell2.getSite().distanceSquared(v) > c.getSite().distanceSquared(v)){
						cell3 = cell2;
						cell2 = (ClimateCell)c;
					} else {
						cell3 = (ClimateCell)c;
					}
				}
			}
		}
		
		if(cell2 == null || cell3 == null){
			// Shouldn't happen.
			return cell.getTemperature();
		}
		ClimateTriangle t = new ClimateTriangle(cell, cell2, cell3);
		return t.getTemperatureAt(v);
	}

	public double getCurrentHighestHumidity() {
		if(!hasChanged) return highestHumidity;
		updateMinMaxValues();
		return highestHumidity;
	}

	public double getCurrentLowestLowPressure() {
		if(!hasChanged) return lowestLowPressure;
		updateMinMaxValues();
		return lowestLowPressure;
	}
	
	public double getCurrentHighestLowPressure() {
		if(!hasChanged) return highestLowPressure;
		updateMinMaxValues();
		return highestLowPressure;
	}

	public double getCurrentLowestHighPressure() {
		if(!hasChanged) return lowestHighPressure;
		updateMinMaxValues();
		return lowestHighPressure;
	}
	
	public double getCurrentHighestHighPressure() {
		if(!hasChanged) return highestHighPressure;
		updateMinMaxValues();
		return highestHighPressure;
	}
	
	public double getCurrentMaxWindSpeed(){
		if(!hasChanged) return highestWindSpeed;
		updateMinMaxValues();
		return highestWindSpeed;
	}
	
	public void updateMinMaxValues(){
		for(ClimateCell c : getCells()){
			if(c.getTemperature().getValue() > highestTemperature.getValue()){
				highestTemperature = c.getTemperature();
			}
			if(c.getTemperature().getValue() < lowestTemperature.getValue()){
				lowestTemperature = c.getTemperature();
			}
			if(c.getHighTemperature().getValue() > highestHighTemperature.getValue()){
				highestHighTemperature = c.getTemperature();
			}
			if(c.getHighTemperature().getValue() < lowestHighTemperature.getValue()){
				lowestHighTemperature = c.getTemperature();
			}
			if(c.getLowAltitudePressure() > highestLowPressure){
				highestLowPressure = c.getLowAltitudePressure();
			}
			if(c.getLowAltitudePressure() < lowestLowPressure){
				lowestLowPressure = c.getLowAltitudePressure();
			}
			if(c.getHighAltitudePressure() > highestHighPressure){
				highestHighPressure = c.getHighAltitudePressure();
			}
			if(c.getHighAltitudePressure() < lowestHighPressure){
				lowestHighPressure = c.getHighAltitudePressure();
			}
			if(c.getHumidity() > highestHumidity){
				highestHumidity = c.getHumidity();
			}
			if(c.getHumidity() < highestHumidity){
				highestHumidity = c.getHumidity();
			}
			if(c.getLowWind().getSpeed() > highestWindSpeed){
				highestWindSpeed = c.getLowWind().getSpeed();
			}
		}
		this.hasChanged = false;
	}
	
	public void clearMinMaxValues(){
		highestTemperature = Temperature.fromCelsius(20);
		lowestTemperature = Temperature.fromCelsius(0);
		highestLowPressure = 101300;
		lowestLowPressure = 101300;
		highestHighPressure = 70000;
		lowestHighPressure = 70000;
		highestHumidity = 0;
		highestWindSpeed = 0;
		this.hasChanged = true;
	}
	
	public File getFile(){
		File d1 = new File(LivelyWorld.getInstance().getDataFolder(), "ClimateMaps");
		File f = new File(d1, world.getName() + ".cmp");
		if(!d1.exists()){
			d1.mkdir();
		}
		if(!f.exists()){
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return f;
	}

	public void saveAllData() {
		byte[] bytes = new byte[getCells().length * ClimateCellData.getByteSize()];
		ByteBuffer buff = ByteBuffer.wrap(bytes);
		for(int i = 0; i < getCells().length; i++){
			getCells()[i].getData().saveInto(buff);
		}
		
		try{
			OutputStream out = null;
			try{
				out = new DeflaterOutputStream(new FileOutputStream(getFile()));
				out.write(bytes);
			} finally {
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*for(int i = 0; i < getCells().length; i++){
			LivelyWorld.getInstance().getDatabase().setClimateCellData(i, getCells()[i].getData());
		}*/
	}
	
	public List<ClimateCellData> loadAllData(){
		List<ClimateCellData> data = new ArrayList<ClimateCellData>();
		byte[] result = null;
	    try {
	    	InputStream input =  new InflaterInputStream(new FileInputStream(getFile()));
	    	result = readAndClose(input);
	    } catch (FileNotFoundException ex){
	    	ex.printStackTrace();
	    }
	    if(result != null){
	    	ByteBuffer buff = ByteBuffer.wrap(result);
	    	while(buff.remaining() >= ClimateCellData.getByteSize()){
	    		data.add(ClimateCellData.loadFrom(buff));
	    	}
	    }
	    return data;
	}
	
	public byte[] readAndClose(InputStream stream){
		byte[] bucket = new byte[32*1024]; 
	    ByteArrayOutputStream result = null; 
	    try  {
	    	try {
	    		result = new ByteArrayOutputStream(bucket.length);
	    		int bytesRead = 0;
	    		while(bytesRead != -1){
	    			bytesRead = stream.read(bucket);
	    			if(bytesRead > 0){
	    				result.write(bucket, 0, bytesRead);
	    			}
	    		}
	    	} finally {
	    		stream.close();
	    	}
	    } catch (IOException ex){
	      ex.printStackTrace();;
	    }
	    return result.toByteArray();
	}
}
