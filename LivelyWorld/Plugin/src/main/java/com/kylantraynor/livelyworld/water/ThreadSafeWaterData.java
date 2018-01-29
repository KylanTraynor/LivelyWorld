package com.kylantraynor.livelyworld.water;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;

public class ThreadSafeWaterData {
	/*
	public static final int solidCode = 28;//   1 (1 bit) 0001 0000 0000 0000 0000 0000 0000 0000
	private WaterChunk chunk = null;
	private int x = 0;
	private int y = 0;
	private int z = 0;
	//private boolean needsVisualUpdate = false;
	
	public static long maxLevel = 0xffL;
	public static int moistureCode = 0; // 255 (1 byte) 0000 0000 0000 0000 0000 0000 1111 1111
	public static long maxResistance = 0xffL;
	public static int resistanceCode = 8;
	public static long maxPressure = 0xfL; // 255 (1 byte) 0000 0000 1111 1111 0000 0000 0000 0000
	public static int pressureCode = 16;
	private static long maxSalt = 0xfL;
	private static int saltCode = 24; // 15 (4 bits) 0000 1111 0000 0000 0000 0000 0000 0000
	private static int updateCode = 31; // 1 (1 bit) 1000 0000 0000 0000 0000 0000 0000 0000 
	private static int heatCode = 30; //   1 (1 bit) 0100 0000 0000 0000 0000 0000 0000 0000
	
	public ThreadSafeWaterData(WaterChunk chunk, int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
		this.chunk = chunk;
	}
	
	public WaterData(Chunk chunk, int x, int y, int z){
		this(WaterChunk.get(chunk.getWorld(), chunk.getX(), chunk.getZ()), x, y, z);
	}
	
	public WaterData(World world, int x, int y, int z){
		this(WaterChunk.get(world, x >> 4, z >> 4), Math.floorMod(x, 16), y, Math.floorMod(z, 16));
	}
	
	public WaterData(Location loc){
		this(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public Location getLocation(){
		return new Location(getWorld(), getX(), getY(), getZ());
	}
	
	public World getWorld(){
		return chunk.getWorld();
	}
	
	public int getChunkX(){ return x; }
	
	public int getChunkZ(){ return z; }
	
	public int getX(){ return (chunk.getX() << 4) + x; }
	
	public int getY(){ return y; }
	
	public int getZ(){ return (chunk.getZ() << 4) + z; }
	
	public WaterData getRelative(int x, int y, int z){
		if(this.y + y < 0 || this.y + y > 255){
			return null;
		}
		if(this.x + x < 0 || this.x + x > 15 || this.z + z < 0 || this.z + z > 15){
			return new WaterData(chunk.getWorld(), this.getX() + x, this.getY() + y, this.getZ() + z);
		}
		return new WaterData(chunk, this.x + x, this.y + y, this.z + z);
	}
	
	public WaterData getRelative(BlockFace bf){
		switch(bf){
		case DOWN:
			return getRelative(0,-1,0);
		case EAST:
			return getRelative(1,0,0);
		case NORTH:
			return getRelative(0,0,-1);
		case NORTH_EAST:
			return getRelative(1,0,-1);
		case NORTH_WEST:
			return getRelative(-1,0,-1);
		case SELF:
			return this;
		case SOUTH:
			return getRelative(0,0,1);
		case SOUTH_EAST:
			return getRelative(1,0,1);
		case SOUTH_WEST:
			return getRelative(-1,0,1);
		case UP:
			return getRelative(0,1,0);
		case WEST:
			return getRelative(-1,0,0);
		default:
			return null;
		}
	}
	
	public long getData(){
		return ((long) chunk.getData(x, y, z)) & 0xFFFFFFFFL;
	}
	
	public void setData(long value){
		chunk.setData((int) (value & 0xFFFFFFFFL), x, y, z);
	}
	
	void setDataUnchecked(long value){
		chunk.setDataUnchecked((int) (value & 0xFFFFFFFFL), x, y, z);
	}
	
	public int getLevel(){
		return (int) (getData() & maxLevel);
	}
	
	public void setLevel(int value){
		long newData = (getData() & (~maxLevel)) | ((long) value);
		if(toWaterLevel(value) != toWaterLevel(getLevel()) && getResistance() <= 1){
			setData(newData);
			chunk.setNeedsUpsate(true);
		} else {
			setData(newData);
		}
	}
	public int getResistance(){
		return (int) ((getData() >>> resistanceCode) & maxResistance);
	}
	
	public void setResistance(int value){
		if(value > maxResistance){
			LivelyWorld.getInstance().getLogger().info("DEBUG: Resistance was too high! (" + value + ">" + maxResistance + ")");
			value = (int) maxResistance;
		} else if(value < 0){
			LivelyWorld.getInstance().getLogger().info("DEBUG: Resistance was too low! (" + value + "<0)");
			value = 0;
		}
		//LivelyWorld.getInstance().getLogger().info("DEBUG:");
		//LivelyWorld.getInstance().getLogger().info("Start:" + Integer.toBinaryString(getData()));
		//LivelyWorld.getInstance().getLogger().info(Integer.toBinaryString((getData() & (~(maxLevel << moistureCode)))) + " | " + Integer.toBinaryString((Utils.constrainTo(value, 0, maxLevel) << moistureCode)));
		long newData = (getData() & (~(maxResistance << resistanceCode))) | (((long) value) << resistanceCode);
		//LivelyWorld.getInstance().getLogger().info("Finish:" + Integer.toBinaryString(newData));
		if(getData() != newData){
			setData(newData);
		}
	}
	
	public int getPressure(){
		return (int) ((getData() >>> pressureCode) & maxPressure);
	}
	
	public void setPressure(int value){
		if(value > maxPressure){
			LivelyWorld.getInstance().getLogger().info("DEBUG: Pressure was too high! (" + value + ">" + maxPressure + ")");
			value = (int) maxPressure;
		} else if(value < 0){
			LivelyWorld.getInstance().getLogger().info("DEBUG: Pressure was too low! (" + value + "<0)");
			value = 0;
		}
		//LivelyWorld.getInstance().getLogger().info("DEBUG:");
		//LivelyWorld.getInstance().getLogger().info("Start:" + Integer.toBinaryString(getData()));
		//LivelyWorld.getInstance().getLogger().info(Integer.toBinaryString((getData() & (~(maxLevel << moistureCode)))) + " | " + Integer.toBinaryString((Utils.constrainTo(value, 0, maxLevel) << moistureCode)));
		long newData = (getData() & (~(maxPressure << pressureCode))) | (((long) value) << pressureCode);
		//LivelyWorld.getInstance().getLogger().info("Finish:" + Integer.toBinaryString(newData));
		if(getData() != newData){
			setData(newData);
		}
	}
	
	public int getSalt(){
		return (int) (getData() & (maxSalt << saltCode)) >>> saltCode;
	}
	
	public void setSalt(int value){
		if(value > maxSalt){
			value = (int) maxSalt;
		}
		if(value < 0){
			value = 0;
		}
		setData((getData() & (~(maxSalt << saltCode))) | ((long) value) << saltCode);
	}
	
	public boolean needsUpdate(){
		return (getData() & (1 << updateCode)) >>> updateCode == 1;
	}
	
	
	public void setUpdate(boolean value){
		setData((getData() & (~(1L << updateCode))) | ((value ? 1L : 0L) << updateCode));
	}
	
	public boolean isHeatSource(){
		return ((getData() & (1 << heatCode)) >>> heatCode) == 1;
	}
	
	
	public void setHeatSource(boolean value){
		setData((getData() & (~(1L << heatCode))) | ((value ? 1L : 0L) << heatCode));
	}
	
	public boolean isSalted(){
		return getSalt() > 0;
	}

	private static BlockFace[] order = new BlockFace[] {BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST};
	
	public void updateResistance(){
		if(LivelyWorld.getInstance().getMainThreadId() == Thread.currentThread().getId()){
			if(chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ()))
				return;
		} else {
			if(!chunk.isLoaded() || !WaterChunkThread.isChunkLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ()))
				return;
		}
		Material mat = chunk.getWorld().getBlockAt(getX(), getY(), getZ()).getType();
		setResistance(getResistanceFor(mat));
	}
	
	public int getMaxQuantity(){
		int resistance = getResistance();
		if(resistance == 0) {
			updateResistance();
			resistance = getResistance();
		}
		return (int) (maxLevel - resistance);
	}
	
	public double getPermeability(){
		int resistance = getResistance();
		if(resistance == 0 || Utils.fastRandomDouble() < 0.01) {
			if(!chunk.isLoaded() || !WaterChunkThread.isChunkLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ()))
				return resistance == 0 || resistance == maxResistance ? 0 : 1.0 / resistance;
			int id = chunk.getWorld().getBlockTypeIdAt(getX(), getY(), getZ());
			resistance = getResistanceFor(Material.getMaterial(id));
			setResistance(resistance);
		}
		if(resistance == 15) return 0;
		return 1.0 / resistance;
	}
	
	public static int getMaxQuantityFor(Material material){
		return (int) (maxLevel - getResistanceFor(material));
	}
	
	public static int getMaxSaltFor(int level){
		return (int) Math.max((int) maxSalt * ((double) level / ((int) maxLevel)), 1);
	}
	
	public static int getResistanceFor(Material material){
		if(material == null) return 0;
		switch (material){
		case WATER: case STATIONARY_WATER: case AIR:
			return 1;
		case WOOD_BUTTON: case STONE_BUTTON:
		case LADDER: case RAILS: case TORCH: case IRON_FENCE: case WALL_SIGN: case SIGN_POST:
		case WOOD_PLATE: case STONE_PLATE: case IRON_PLATE: case GOLD_PLATE:
			return 2;
		case TRAP_DOOR:
		case WOODEN_DOOR: case WOOD_DOOR:
		case ACACIA_DOOR: case SPRUCE_DOOR: case DARK_OAK_DOOR: case JUNGLE_DOOR: case BIRCH_DOOR:
		case FENCE_GATE: case ACACIA_FENCE_GATE: case  SPRUCE_FENCE_GATE: case DARK_OAK_FENCE_GATE: case JUNGLE_FENCE_GATE: case BIRCH_FENCE_GATE:
		case FENCE: case ACACIA_FENCE: case SPRUCE_FENCE: case DARK_OAK_FENCE: case JUNGLE_FENCE: case BIRCH_FENCE: case NETHER_FENCE:
			return 20;
		case LONG_GRASS: case DOUBLE_PLANT: case RED_ROSE: case YELLOW_FLOWER:
		case LEAVES: case LEAVES_2:
			return 30;
		case ANVIL:
			return 100;
		case SAND: case GRAVEL: case SNOW: case SNOW_BLOCK:
			return 200;
		case DIRT: case GRASS_PATH: case GRASS: case SOIL: case CLAY:
			return 215;
		case COBBLESTONE: case COBBLE_WALL:
			return 225;
		default:
			return 255;
		}
	}
	
	public static int getWaterLevelAt(World world, int x, int y, int z){
		WaterChunk wc = WaterChunk.get(world, x >> 4, z >> 4);
		return getWaterLevelAt(wc, Math.floorMod(x, 16), y, Math.floorMod(z, 16));
	}
	
	public static int getWaterLevelAt(WaterChunk chunk, int x, int y, int z){
		return (int) (chunk.getData(x, y, z) & maxLevel);
	}
	
	public static void setLevelAt(World world, int x, int y, int z, int value){
		WaterChunk wc = WaterChunk.get(world, x >> 4, z >> 4);
		setLevelAt(wc, Math.floorMod(x, 16), y, Math.floorMod(z, 16), value);
	}
	
	public static void setLevelAt(WaterChunk chunk, int x, int y, int z, int value){
		if(value > maxResistance){
			LivelyWorld.getInstance().getLogger().info("DEBUG: Resistance was too high! (" + value + ">" + maxResistance + ")");
			value = (int) maxResistance;
		} else if(value < 0){
			LivelyWorld.getInstance().getLogger().info("DEBUG: Resistance was too low! (" + value + "<0)");
			value = 0;
		}
		//LivelyWorld.getInstance().getLogger().info("DEBUG:");
		//LivelyWorld.getInstance().getLogger().info("Start:" + Integer.toBinaryString(getData()));
		//LivelyWorld.getInstance().getLogger().info(Integer.toBinaryString((getData() & (~(maxLevel << moistureCode)))) + " | " + Integer.toBinaryString((Utils.constrainTo(value, 0, maxLevel) << moistureCode)));
		long newData = ((((long) chunk.getData(x, y, z)) & 0xFFFFFFFFL) & (~maxLevel)) | ((long) value);
		//LivelyWorld.getInstance().getLogger().info("Finish:" + Integer.toBinaryString(newData));
		int nd = (int) (newData & 0xFFFFFFFFL);
		if(toWaterLevel(nd) != toWaterLevel(getWaterLevelAt(chunk, x, y, z))){
			chunk.setData(nd, x,y,z);
			chunk.setNeedsUpsate(true);
		} else {
			chunk.setData(nd, x,y,z);
		}
	}
	
	public static void setResistanceAt(WaterChunk chunk, int x, int y, int z, int value){
		if(value > maxLevel){
			LivelyWorld.getInstance().getLogger().info("DEBUG: Level was too high! (" + value + ">" + maxLevel + ")");
			value = (int) maxLevel;
		} else if(value < 0){
			LivelyWorld.getInstance().getLogger().info("DEBUG: Level was too low! (" + value + "<0)");
			value = 0;
		}
		//LivelyWorld.getInstance().getLogger().info("DEBUG:");
		//LivelyWorld.getInstance().getLogger().info("Start:" + Integer.toBinaryString(getData()));
		//LivelyWorld.getInstance().getLogger().info(Integer.toBinaryString((getData() & (~(maxLevel << moistureCode)))) + " | " + Integer.toBinaryString((Utils.constrainTo(value, 0, maxLevel) << moistureCode)));
		long newData = ((chunk.getData(x,y,z) & 0xFFFFFFFFL) & (~(maxResistance << resistanceCode))) | (((long) value) << resistanceCode);
		//LivelyWorld.getInstance().getLogger().info("Finish:" + Integer.toBinaryString(newData));
		int nd = (int) (newData & 0xFFFFFFFFL);
		if(chunk.getData(x,y,z) != nd){
			chunk.setData(nd, x,y,z);
		}
	}
	
	public static int getWaterResistanceAt(World world, int x, int y, int z){
		WaterChunk wc = WaterChunk.get(world, x >> 4, z >> 4);
		return getWaterResistanceAt(wc, Math.floorMod(x, 16), y, Math.floorMod(z, 16));
	}
	
	public static int getWaterResistanceAt(WaterChunk chunk, int x, int y, int z){
		return (int) ((chunk.getData(x, y, z) >>> resistanceCode) & maxResistance);
	}
	
	public Block getBlock(){
		return chunk.getWorld().getBlockAt(getX(), getY(), getZ());
	}
	
	public static int toWaterLevel(int level){
		if(level >= maxLevel * 0.9) return 8;
		return (int) (8.0 * (((double)level)/ (double) maxLevel));
	}
	
	public void sendChangedEvent(){
		if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
		BukkitRunnable br = new VisualUpdateTask(getLevel(), getResistance());
		br.runTask(LivelyWorld.getInstance());
	}

	public static boolean canReplace(Material mat){
		if(mat == Material.WATER) return true;
		if(mat == Material.STATIONARY_WATER) return true;
		if(mat == Material.AIR) return true;
		if(mat == Material.LONG_GRASS) return true;
		if(mat == Material.VINE) return true;
		if(mat == Material.TORCH) return true;
		if(mat == Material.SNOW) return true;
		if(mat == Material.SNOW_BLOCK) return true;
		return false;
	}
	
	public static boolean isDropable(Material mat){
		if(mat == Material.VINE) return true;
		if(mat == Material.TORCH) return true;
		if(mat == Material.LONG_GRASS) return true;
		return false;
	}
	
	public void moveWaterDown() {
		WaterData down = getRelative(BlockFace.DOWN);
		if(down != null){
			int level = getLevel();
			if(level < 1) return; 
			int max = down.getMaxQuantity();
			if(max < 245){
				max = (int) (max * chunk.tickRandom);
			}
			int transfer = Math.min(max - down.getLevel(), level);
			if (transfer <= 0) return;
			setLevelUnchecked(level - transfer);
			down.setLevelUnchecked(down.getLevel() + transfer);
		}
	}

	public void moveWaterHorizontally(boolean loadChunks) {
		if(!loadChunks){
			if(x == 0 && !chunk.isRelativeLoaded(-1, 0))
				return;
			if(x == 15 && !chunk.isRelativeLoaded(1, 0))
				return;
			if(z == 0 && !chunk.isRelativeLoaded(0, -1))
				return;
			if(z == 15 && !chunk.isRelativeLoaded(0, 1))
				return;
		}
		WaterData[] relatives = new WaterData[4];
		
		int level = 0;
		// Gets a random offset number for the order in which surrounding blocks will be checked.
		//int rdm = Utils.fastRandomInt(4);
		// Populates the surrounding blocks.
		
			relatives[0] = getRelative(order[0]);
			relatives[1] = getRelative(order[1]);
			relatives[2] = getRelative(order[2]);
			relatives[3] = getRelative(order[3]);
			
		// Do the calculations for each potential block.
		level = getLevel();
		int[] levels = new int[4];
		int[] max = new int[4];
		for(int i = 0; i < 4; i++){
			levels[i] = relatives[i].getLevel();
			max[i] = relatives[i].getMaxQuantity();
			if(max[i] < 245){
				max[i] = (int) (max[i] * chunk.tickRandom);
			}
		}
		int[] diff;
		int minDiff;
		int columnsToFill;
		for(int i = 0; i < 4; i++){
			diff = new int[4];
			minDiff = -1;
			columnsToFill = 0;
			
			// Populates the differences array and checks which one is the smallest one.
			for(int m = 0; m < 4; m++){
				//if(levels[m] == -1) continue;
				// Calculates the difference, and caps it to the difference between the target's max level and its current level.
				diff[m] = Math.min((level) - levels[m], max[m] - levels[m]);
				// If there is a positive difference.
				if(diff[m] > 1){
					// Adds one to the number of columns to transfer water to.
					columnsToFill ++;
					// Updates the minDifference if needed.
					if(minDiff != -1){
						if(diff[m] < diff[minDiff]){
							minDiff = m;
						}
					} else {
						minDiff = m;
					}
					//minDiff = (minDiff == -1 ? m : (diff[m] < diff[minDiff] ? m : minDiff));
				}
			}
			
			// Fills up all columns if possible.
			if(columnsToFill > 0){
				// Calculates the amount of water to move to each column for equilibrium.
				int transfer = 0;
				if(columnsToFill == 3) transfer = diff[minDiff] >> 2;
				else if(columnsToFill == 1) transfer = diff[minDiff] >> 1;
				else transfer = Math.floorDiv(diff[minDiff], columnsToFill + 1);
				// If there's at least 1 level to transfer to each column.
				if(transfer >= 1){
					// Go through each column.
					for(int i2 = 0; i2 < 4; i2++){
						//if(levels[i2] == -1) continue;
						// If the column can be filled.
						if(diff[i2] > 1){
							// Moves water level down in source.
							level -= transfer;
							// Moves water level up in target column.
							levels[i2] = (levels[i2] + transfer);
						}
					}
				} else {
					// Moves one unit of water to the column with the minimum difference.
					// Moves water level down in source.
					level -= 1;
					// Moves water level up in target column.
					levels[minDiff] = levels[minDiff] + 1;
				}
				// Removes the column with the minimum difference from the next calculations.
				//relatives[minDiff].setLevel(levels[minDiff]);
			} else {
				// There was no column to fill, process can stop.
				break;
			}
		}
		this.setLevelUnchecked(level);
		for(int i = 0; i < 4; i++){
			if(relatives[i].chunk == chunk){
				relatives[i].setLevelUnchecked(levels[i]);
			} else {
				relatives[i].setLevel(levels[i]);
			}
		}
	}

	 public void setLevelUnchecked(int value) {
		 long newData = (getData() & (~maxLevel)) | ((long) value);
		if(toWaterLevel(value) != toWaterLevel(getLevel())){
			setDataUnchecked(newData);
			chunk.setNeedsUpsate(true);
		} else {
			setDataUnchecked(newData);
		}
	}
	 
	public class VisualUpdateTask extends BukkitRunnable{

		final int level;
		final int resistance;
		
		public VisualUpdateTask(int level, int resistance){
			this.level = level;
			this.resistance = resistance;
		}
		
		@Override
		public void run() {
			if(!chunk.isLoaded() || !chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ()))
				return;
			Block b = getBlock();
			if(canReplace(b.getType())){
				int waterLevel = WaterData.toWaterLevel(level);
				if(waterLevel != Utils.getWaterHeight(b)){
					if(waterLevel > 0 && isDropable(b.getType())){
						b.breakNaturally();
					}
					Utils.setWaterHeight(b, waterLevel, true);
				}
				if(WaterData.getResistanceFor(b.getType()) != resistance){
					setResistance(WaterData.getResistanceFor(b.getType()));
				}
			} else if(b.getRelative(BlockFace.DOWN).getType() == Material.AIR && b.getType() != Material.AIR && getLevel() > 0) {
				chunk.getWorld().spawnParticle(Particle.DRIP_WATER, b.getX() + Math.random(), b.getY() - 0.01, b.getZ() + Math.random(), 1);
			}
		}
		
	}*/
}