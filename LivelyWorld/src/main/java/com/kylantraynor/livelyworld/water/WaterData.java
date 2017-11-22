package com.kylantraynor.livelyworld.water;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

public class WaterData {
	
	private WaterChunk chunk = null;
	private int x = 0;
	private int y = 0;
	private int z = 0;
	//private boolean needsVisualUpdate = false;
	
	public static long maxLevel = 0xffL;
	public static int moistureCode = 0; // 255 (1 byte) 0000 0000 0000 0000 0000 0000 1111 1111
	public static long maxResistance = 0xffL;
	public static int resistanceCode = 8;
	/*private static int outCurrentCode = 9;
	private static int outStrengthCode = 12;*/
	private static long maxSalt = 0xfL;
	private static int saltCode = 24; // 15 (4 bits) 0000 1111 0000 0000 0000 0000 0000 0000*/ 
	private static int stableCode = 31; // 1 (1 bit) 1000 0000 0000 0000 0000 0000 0000 0000*/ 
	
	public WaterData(WaterChunk chunk, int x, int y, int z){
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
	
	@Nullable
	public WaterData getRelative(int x, int y, int z){
		if(this.y + y < 0 || this.y + y > 255){
			return null;
		}
		if(this.x + x < 0 || this.x + x > 15 || this.z + z < 0 || this.z + z > 15){
			return new WaterData(chunk.getWorld(), this.getX() + x, this.getY() + y, this.getZ() + z);
		}
		return new WaterData(chunk, this.x + x, this.y + y, this.z + z);
	}
	
	public WaterData getRelative(@Nonnull BlockFace bf){
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
	
	public int getLevel(){
		return (int) (getData() & maxLevel);
	}
	
	public void setLevel(int value){
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
		long newData = (getData() & (~maxLevel)) | ((long) value);
		//LivelyWorld.getInstance().getLogger().info("Finish:" + Integer.toBinaryString(newData));
		if(toWaterLevel(value) != toWaterLevel(getLevel())){
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
	/*
	public int getInCurrentDirection(){
		return (getData() & (7 << inCurrentCode)) >> inCurrentCode;
	}
	
	public void setInCurrentDirection(int value){
		setData((getData() & (~(7 << inCurrentCode))) + (Utils.constrainTo(value, 0, 7) << inCurrentCode));
	}*/
	/*
	public int getOutCurrentDirection(){
		return (getData() & (15 << outCurrentCode)) >> outCurrentCode;
	}
	
	public void setOutCurrentDirection(int value){
		setData((getData() & (~(15 << outCurrentCode))) + (Utils.constrainTo(value, 0, 15) << outCurrentCode));
	}*/
	/*
	public int getInCurrentStrength(){
		return (getData() & (7 << inStrengthCode)) >> inStrengthCode;
	}
	
	public void setInCurrentStrength(int value){
		setData((getData() & (~(7 << inStrengthCode))) + (Utils.constrainTo(value, 0, 7) << inStrengthCode));
	}*/
	/*
	public int getOutCurrentStrength(){
		return (getData() & (15 << outStrengthCode)) >> outStrengthCode;
	}
	
	public void setOutCurrentStrength(int value){
		setData((getData() & (~(15 << outStrengthCode))) + (Utils.constrainTo(value, 0, 15) << outStrengthCode));
	}
	*/
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
	
	public boolean isStable(){
		return (getData() & (1 << stableCode)) >>> stableCode == 1;
	}
	
	
	public void setStable(boolean value){
		setData((getData() & (~(1L << stableCode))) | ((value ? 1L : 0L) << stableCode));
	}
	
	public boolean isSalted(){
		return getSalt() > 0;
	}

	private static BlockFace[] order = new BlockFace[] {BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST};
	
	public void updateResistance(){
		if(!chunk.isLoaded() || !WaterChunkThread.isChunkLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ()))
			return;
		
		int id = chunk.getWorld().getBlockTypeIdAt(getX(), getY(), getZ());
		setResistance(getResistanceFor(Material.getMaterial(id)));
	}
	
	public int getMaxQuantity(){
		int resistance = getResistance();
		if(resistance == 0 || Utils.fastRandomDouble() < 0.01) {
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
	
	public static int getResistanceFor(@Nonnull Material material){
		/*if(material == null){
			if(!chunk.isLoaded() || !WaterChunkThread.isChunkLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ()))
				return 0;
			int id = chunk.getWorld().getBlockTypeIdAt(getX(), getY(), getZ());
			material = Material.getMaterial(id);
		}*/
		if(material == null) return 0;
		switch (material){
		case WATER: case STATIONARY_WATER: case AIR:
			return 1;
		case LADDER: case RAILS: case TORCH:
			return 2;
		case TRAP_DOOR:
		case WOODEN_DOOR: case WOOD_DOOR:
		case SPRUCE_DOOR: case DARK_OAK_DOOR: case JUNGLE_DOOR: case BIRCH_DOOR:
		case FENCE_GATE: case  SPRUCE_FENCE_GATE: case DARK_OAK_FENCE_GATE: case JUNGLE_FENCE_GATE: case BIRCH_FENCE_GATE:
		case FENCE: case SPRUCE_FENCE: case DARK_OAK_FENCE: case JUNGLE_FENCE: case BIRCH_FENCE: case IRON_FENCE:
			return 20;
		case LONG_GRASS: case DOUBLE_PLANT: case RED_ROSE: case YELLOW_FLOWER:
		case LEAVES: case LEAVES_2:
			return 30;
		case SAND: case GRAVEL:
			return 200;
		case DIRT: case GRASS_PATH: case GRASS: case SOIL: case CLAY:
			return 215;
		case COBBLESTONE:
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
	
	public static void setLevel(WaterChunk chunk, int x, int y, int z, int value){
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
		long newData = ((((long) chunk.getData(x, y, z)) & 0xFFFFFFFFL) & (~maxLevel)) | ((long) value);
		//LivelyWorld.getInstance().getLogger().info("Finish:" + Integer.toBinaryString(newData));
		if(toWaterLevel(value) != toWaterLevel(getWaterLevelAt(chunk, x, y, z))){
			chunk.setData((int) (value & 0xFFFFFFFFL), x,y,z);
			chunk.setNeedsUpsate(true);
		} else {
			chunk.setData((int) (value & 0xFFFFFFFFL), x,y,z);
		}
	}
	
	public static int getWaterResistanceAt(World world, int x, int y, int z){
		WaterChunk wc = WaterChunk.get(world, x >> 4, z >> 4);
		return getWaterResistanceAt(wc, Math.floorMod(x, 16), y, Math.floorMod(z, 16));
	}
	
	public static int getWaterResistanceAt(WaterChunk chunk, int x, int y, int z){
		return (int) ((chunk.getData(x, y, z) >>> resistanceCode) & maxResistance);
	}
	
	/**
	 * Gets the block corresponding to this water data.
	 * This function is NOT thread-safe, and should only be called from the main thread.
	 * @return Block
	 */
	public Block getBlock(){
		return chunk.getWorld().getBlockAt(getX(), getY(), getZ());
	}
	
	public static int toWaterLevel(int level){
		if(level >= maxLevel * 0.9) return 8;
		return (int) (8.0 * (((double)level)/ (double) maxLevel));
	}
	
	public void sendChangedEvent(){
		//needsVisualUpdate = false;
		if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
		BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				if(!chunk.isLoaded() || !chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ()))
					return;
				Block b = getBlock();
				if(getResistance() == 1 && b.getRelative(BlockFace.DOWN).getType() != Material.AIR && (b.getY() > 48 && !Utils.isOcean(chunk.getWorld().getBiome(getX(), getZ())))){
					if(Utils.getWaterHeight(b) != toWaterLevel(getLevel())){
						Utils.setWaterHeight(b, toWaterLevel(getLevel()), true);
					}
				} else if(b.getRelative(BlockFace.DOWN).getType() == Material.AIR && b.getType() != Material.AIR && getLevel() > 0) {
					chunk.getWorld().spawnParticle(Particle.DRIP_WATER, b.getX() + Math.random(), b.getY() - 0.01, b.getZ() + Math.random(), 1);
				}
				//BlockWaterChangedEvent e = new BlockWaterChangedEvent(b, getData());
				//Bukkit.getPluginManager().callEvent(e);
			}
		};
		br.runTask(LivelyWorld.getInstance());
	}

	public void moveWaterDown() {
		WaterData down = getRelative(BlockFace.DOWN);
		if(down != null){
			int level = getLevel();
			int transfer = Math.min(down.getMaxQuantity() - down.getLevel(), level);
			if (transfer < 0) transfer = 0;
			setLevel(level - transfer);
			down.setLevel(down.getLevel() + transfer);
			/*for(int i = 1; i <= level; i++){
				if(down.getLevel() < down.getMaxQuantity() && getLevel() > 0){
					down.setLevel(down.getLevel() + 1);
					setLevel(getLevel() - 1);
				} else {
					break;
				}
			}*/
		}
	}

	public void moveWaterHorizontally(boolean loadChunks) {
		if(!loadChunks){
			if(x == 0 && !chunk.getRelative(-1, 0).isLoaded())
				return;
			if(x == 15 && !chunk.getRelative(1, 0).isLoaded())
				return;
			if(z == 0 && !chunk.getRelative(0, -1).isLoaded())
				return;
			if(z == 15 && !chunk.getRelative(0, 1).isLoaded())
				return;
		}
		//WaterData[] relatives = new WaterData[4];
		int level = 0;
		// Gets a random offset number for the order in which surrounding blocks will be checked.
		//int rdm = Utils.fastRandomInt(4);
		// Populates the surrounding blocks.
		/*switch(rdm){
		case 0:
			relatives[0] = getRelative(order[0]);
			relatives[1] = getRelative(order[1]);
			relatives[2] = getRelative(order[2]);
			relatives[3] = getRelative(order[3]);
			break;
		case 1:
			relatives[0] = getRelative(order[1]);
			relatives[1] = getRelative(order[2]);
			relatives[2] = getRelative(order[3]);
			relatives[3] = getRelative(order[0]);
			break;
		case 2:
			relatives[0] = getRelative(order[2]);
			relatives[1] = getRelative(order[3]);
			relatives[2] = getRelative(order[0]);
			relatives[3] = getRelative(order[1]);
			break;
		case 3:
			relatives[0] = getRelative(order[3]);
			relatives[1] = getRelative(order[0]);
			relatives[2] = getRelative(order[1]);
			relatives[3] = getRelative(order[2]);
			break;
		}*/
		// Do the calculations for each potential block.
		level = getLevel();
		int cx = getChunkX();
		int cz = getChunkZ();
		int[] levels = new int[4];
		levels[0] = WaterData.getWaterLevelAt(chunk, cx - 1, y, cz);
		levels[1] = WaterData.getWaterLevelAt(chunk, cx + 1, y, cz);
		levels[2] = WaterData.getWaterLevelAt(chunk, cx, y, cz - 1);
		levels[3] = WaterData.getWaterLevelAt(chunk, cx, y, cz + 1);
		int[] max = new int[4];
		max[0] = (int) WaterData.maxLevel - WaterData.getWaterResistanceAt(chunk, cx - 1, y, cz);
		max[1] = (int) WaterData.maxLevel - WaterData.getWaterResistanceAt(chunk, cx + 1, y, cz);
		max[2] = (int) WaterData.maxLevel - WaterData.getWaterResistanceAt(chunk, cx, y, cz - 1);
		max[3] = (int) WaterData.maxLevel - WaterData.getWaterResistanceAt(chunk, cx, y, cz + 1);
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
				diff[m] = Math.min(level - levels[m], max[m] - levels[m]);
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
		this.setLevel(level);
		WaterData.setLevel(chunk, cx - 1, y, cz, levels[0]);
		WaterData.setLevel(chunk, cx + 1, y, cz, levels[1]);
		WaterData.setLevel(chunk, cx, y, cz - 1, levels[2]);
		WaterData.setLevel(chunk, cx, y, cz + 1, levels[3]);
	}
	
	/*public boolean needsVisualUpdate(){
		return needsVisualUpdate;
	}

	public void setNeedsVisualUpdate(boolean b) {
		needsVisualUpdate = b;
	}*/
}
