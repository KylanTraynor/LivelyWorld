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
	private boolean needsVisualUpdate = false;
	
	public static long maxLevel = 0xffL;
	public static int moistureCode = 0; // 255 (1 byte) 0000 0000 0000 0000 0000 0000 1111 1111
	public static long maxResistance = 0xffL;
	public static int resistanceCode = 8;
	/*private static int outCurrentCode = 9;
	private static int outStrengthCode = 12;*/
	private static long maxSalt = 0xfL;
	private static int saltCode = 28; // 15 (4 bits) 1111 0000 0000 0000 0000 0000 0000 0000*/ 
	
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
		if(value != getLevel()){
			setData(newData);
			needsVisualUpdate = true;
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
	
	public boolean isSalted(){
		return getSalt() > 0;
	}

	private static BlockFace[] order = new BlockFace[] {BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST};
	
	public void tick(boolean loadChunks) {
		if(!chunk.isLoaded()) return;
		if(getLevel() == 0) return;
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
		WaterData down = getRelative(BlockFace.DOWN);
		if(down != null){
			int level = getLevel();
			for(int i = 1; i <= level; i++){
				if(down.getLevel() >= maxLevel || getLevel() == 0){
					break;
				}
				if(down.getLevel() < down.getMaxQuantity()){
				//if(Math.random() < down.getPermeability()){
					down.setLevel(down.getLevel() + 1);
					setLevel(getLevel() - 1);
				}
			}
			/*if(down.getLevel() < maxLevel){
				int transfer = (int) maxLevel - down.getLevel();
				transfer = Math.min((int)Math.floor(transfer * down.getPermeability()), getLevel());
				down.setLevel(down.getLevel() + transfer);
				this.setLevel(getLevel() - transfer);
			}*/
		}
		if(getLevel() <= 1) return;
		double rdm = Math.random() * 4;
		int level = getLevel();
		for(int i = 0; i < level; i++){
			WaterData target = getRelative(order[(i + (int) rdm) % 4]);
			//if(target.getLevel() < getLevel() - 1 && Math.random() < target.getPermeability()){
			if(target.getLevel() < getLevel() - 1 && target.getLevel() < target.getMaxQuantity()) {
				target.setLevel(target.getLevel() + 1);
				this.setLevel(getLevel() - 1);
			}
		}
		/*for(BlockFace bf : order){
			WaterData target = getRelative(bf);
			if(target.getLevel() < getLevel() - 1){
				int transfer = (getLevel() - 1) - target.getLevel();
				transfer = (int) Math.floor(transfer * target.getPermeability());
				target.setLevel(target.getLevel() + transfer);
				this.setLevel(getLevel() - transfer);
			}
		}*/
	}
	
	public void updateResistance(){
		if(!chunk.isLoaded() || !WaterChunkThread.isChunkLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ()))
			return;
		int id = chunk.getWorld().getBlockTypeIdAt(getX(), getY(), getZ());
		setResistance(getResistanceFor(Material.getMaterial(id)));
	}
	
	public int getMaxQuantity(){
		int resistance = getResistance();
		if(resistance == 0 || Math.random() < 0.01) {
			updateResistance();
			resistance = getResistance();
		}
		return (int) (maxLevel - resistance);
	}
	
	public double getPermeability(){
		int resistance = getResistance();
		if(resistance == 0 || Math.random() < 0.01) {
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
		case WATER: case STATIONARY_WATER: case LONG_GRASS: case AIR:
			return 1;
		case FENCE: case SPRUCE_FENCE: case DARK_OAK_FENCE: case JUNGLE_FENCE: case BIRCH_FENCE: case IRON_FENCE:
			return 20;
		case LEAVES: case LEAVES_2:
			return 30;
		case SAND: case GRAVEL:
			return 100;
		case DIRT: case GRASS_PATH: case GRASS:
			return 150;
		case COBBLESTONE:
			return 200;
		default:
			return 255;
		}
	}
	
	public static int getWaterLevelAt(World world, int x, int y, int z){
		WaterChunk wc = WaterChunk.get(world, x >> 4, z >> 4);
		return getWaterLevelAt(wc, Math.floorMod(x, 16), y, Math.floorMod(z, 16));
	}
	
	public static int getWaterLevelAt(WaterChunk chunk, int x, int y, int z){
		long d = (long) chunk.getData(x, y, z) & 0xFFFFFFFFL;
		return (int) (d & (maxLevel << moistureCode)) >> moistureCode;
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
		needsVisualUpdate = false;
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
			for(int i = 1; i <= level; i++){
				if(down.getLevel() < down.getMaxQuantity() && getLevel() > 0){
					down.setLevel(down.getLevel() + 1);
					setLevel(getLevel() - 1);
				} else {
					break;
				}
			}
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
		double rdm = Math.random() * 4;
		int level = getLevel();
		for(int i = 0; i < level; i++){
			WaterData target = getRelative(order[(i + (int) rdm) % 4]);
			//if(target.getLevel() < getLevel() - 1 && Math.random() < target.getPermeability()){
			if(target.getLevel() < getLevel() - 1 && target.getLevel() < target.getMaxQuantity()) {
				target.setLevel(target.getLevel() + 1);
				this.setLevel(getLevel() - 1);
			}
		}
	}
	
	public boolean needsVisualUpdate(){
		return needsVisualUpdate;
	}

	public void setNeedsVisualUpdate(boolean b) {
		needsVisualUpdate = b;
	}
}
