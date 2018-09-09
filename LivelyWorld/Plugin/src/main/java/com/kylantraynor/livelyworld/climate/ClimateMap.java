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
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.kylantraynor.livelyworld.Utils;
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

	private int power = 1;
	// use to go from cell coordinates to block coordinates
    // cellX = (blockX >> power) + offsetx;
    // cellZ = (blockZ >> power) + offsetz;
    // blockX = (cellX - offsetx) << power;
    // blockZ = (cellZ - offsetz) << power;
	private int offsetx = 0;
	private int offsetz = 0;
	public ClimateCell[][] cells;
	private int minX = 0;
	private int minZ = 0;
	private int maxX = 0;
	private int maxZ = 0;
	public int xCount = 0;
	public int zCount = 0;

	public ClimateMap(World world) {
		this(world, 8);
	}

	public ClimateMap(World world, int power) {
		this.world = world;
		this.power = power;
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

	public void generateMap(){
	    if(HookManager.hasWorldBorder()){
            WorldBorderHook hook = HookManager.getWorldBorder();
            Location center = hook.getWorldCenter(world);
            minX = (int) (center.getX() - hook.getWorldRadiusX(world));
            minZ = (int) (center.getZ() - hook.getWorldRadiusZ(world));
            maxX = (int) (center.getX() + hook.getWorldRadiusX(world));
            maxZ = (int) (center.getZ() + hook.getWorldRadiusZ(world));

            int minCellX = minX >> power;
            int maxCellX = maxX >> power;
            int minCellZ = minZ >> power;
            int maxCellZ = maxZ >> power;

            xCount = maxCellX - minCellX;
            zCount = maxCellZ - minCellZ;

            cells = new ClimateCell[xCount][zCount];

            offsetx = -minCellX;
            offsetz = -minCellZ;

            for(int x = 0; x < xCount; x++){
                for(int z = 0; z < zCount; z++){
                    cells[x][z] = new ClimateCell();
                    cells[x][z].setWorld(world);
                    cells[x][z].setMap(this);
                    cells[x][z].setCoords(x,z);
                    cells[x][z].size = 1 << power;
                }
            }

            LivelyWorld.getInstance().getLogger().info("Attempting to load previous climate data for map...");
            List<ClimateCellData> data = loadAllData();
            if(data.size() > 0){
                LivelyWorld.getInstance().getLogger().info(data.size() + " previous data has been found.");
            } else {
                LivelyWorld.getInstance().getLogger().warning("No previous climate data could be found.");
            }

            int i =0;

            for(int x = 0; x < xCount; x++){
                for(int z =0; z < zCount; z++){
                    if(i < data.size()){
                        cells[x][z].init(data.get(i++));
                    } else {
                        cells[x][z].init(null);
                    }
                }
                LivelyWorld.getInstance().getLogger().warning("Loading completed at " + (int) ((float)x / xCount * 100) + "%");
            }

            generated = true;
        }
    }

	@Deprecated
	public void generateMapOld() {
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

	public ClimateCell getClimateCellAt(int x, int z){
		if (generated) {
		    int cellX = (x >> power) + offsetx;
		    int cellZ = (z >> power) + offsetz;
			return cells[x][z];
		}
		return null;
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
	    int x = Utils.fastRandomInt(xCount);
	    int z = Utils.fastRandomInt(zCount);
		ClimateCell c = cells[x][z];
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
        for(ClimateCell[] cl : cells){
            for(ClimateCell c : cl){
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
		byte[] bytes = new byte[xCount * zCount * ClimateCellData.getByteSize()];
		ByteBuffer buff = ByteBuffer.wrap(bytes);
		for(int x = 0; x < xCount; x++){
		    for(int z = 0; z < zCount; z++){
		        cells[x][z].getData().saveInto(buff);
            }
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

	public int toBlockX(int cellX){
	    return (cellX - offsetx) << power;
    }

    public int toBlockZ(int cellZ){
	    return (cellZ - offsetz) << power;
    }

    public int toCellX(int blockX){
	    return (blockX >> power) + offsetx;
    }
    public int toCellZ(int blockZ){
	    return (blockZ >> power) + offsetz;
    }
}
