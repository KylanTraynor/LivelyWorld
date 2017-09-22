package com.kylantraynor.livelyworld.water;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;

public class WaterData {
	
	private WaterChunk chunk = null;
	private int x = 0;
	private int y = 0;
	private int z = 0;
	
	public static int moistureCode = 0;
	private static int inCurrentCode = 3;
	private static int outCurrentCode = 9;
	private static int inStrengthCode = 6;
	private static int outStrengthCode = 12;
	private static int saltCode = 15;
	
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
	
	public int getData(){
		return chunk.getData(x, y, z);
	}
	
	public void setData(int value){
		chunk.setData(value, x, y, z);
	}
	
	public int getLevel(){
		return (getData() & (7 << moistureCode)) >> moistureCode;
	}
	
	public void setLevel(int value){
		int newData = (getData() & (~(7 << moistureCode))) + (Utils.constrainTo(value, 0, 7) << moistureCode);
		if(newData != getData()){
			setData(newData);
			//if(Math.random() < 0.1) sendChangedEvent();
		}
	}
	
	public int getInCurrentDirection(){
		return (getData() & (7 << inCurrentCode)) >> inCurrentCode;
	}
	
	public void setInCurrentDirection(int value){
		setData((getData() & (~(7 << inCurrentCode))) + (Utils.constrainTo(value, 0, 7) << inCurrentCode));
	}
	
	public int getOutCurrentDirection(){
		return (getData() & (7 << outCurrentCode)) >> outCurrentCode;
	}
	
	public void setOutCurrentDirection(int value){
		setData((getData() & (~(7 << outCurrentCode))) + (Utils.constrainTo(value, 0, 7) << outCurrentCode));
	}
	
	public int getInCurrentStrength(){
		return (getData() & (7 << inStrengthCode)) >> inStrengthCode;
	}
	
	public void setInCurrentStrength(int value){
		setData((getData() & (~(7 << inStrengthCode))) + (Utils.constrainTo(value, 0, 7) << inStrengthCode));
	}
	
	public int getOutCurrentStrength(){
		return (getData() & (7 << outStrengthCode)) >> outStrengthCode;
	}
	
	public void setOutCurrentStrength(int value){
		setData((getData() & (~(7 << outStrengthCode))) + (Utils.constrainTo(value, 0, 7) << outStrengthCode));
	}
	
	public int getSalt(){
		return (getData() & (7 << saltCode)) >> saltCode;
	}
	
	public void setSalt(int value){
		setData((getData() & (~(7 << saltCode))) + (Utils.constrainTo(value, 0, 7) << saltCode));
	}
	
	public boolean isSalted(){
		return getSalt() > 0;
	}

	public void tick(boolean loadChunks) {
		if(!chunk.isLoaded()) return;
		int level = this.getLevel();
		if(level == 0) return;
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
			if(down.getLevel() < 7 && Math.random() <= down.getPermeability()){
				down.setLevel(down.getLevel() + 1);
				this.setLevel(level - 1);
				return;
			}
		}
		if(level <= 1) return;
		double rdm = Math.random() * 4;
		BlockFace[] order = new BlockFace[0];
		if(rdm > 3){
			order = new BlockFace[] {BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST};
		} else if(rdm > 2){
			order = new BlockFace[] {BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH};
		} else if(rdm > 1){
			order = new BlockFace[] {BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST};
		} else {
			order = new BlockFace[] {BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH};
		}
		for(BlockFace bf : order){
			WaterData target = getRelative(bf);
			if(target.getLevel() < level && Math.random() <= target.getPermeability()){
				target.setLevel(down.getLevel() + 1);
				this.setLevel(level - 1);
				return;
			}
		}
	}
	
	public double getPermeability(){
		BlockState bs = getBlockState();
		if(bs == null) return 0;
		switch (bs.getType()){
		case WATER: case STATIONARY_WATER: case AIR:
			return 1;
		case FENCE: case SPRUCE_FENCE: case DARK_OAK_FENCE: case JUNGLE_FENCE: case BIRCH_FENCE:
			return 0.9;
		case SAND:
			return 0.4;
		case DIRT: case GRASS_PATH:
			return 0.2;
		case COBBLESTONE:
			return 0.1;
		default:
			return 0;
		}
	}
	
	public BlockState getBlockState(){
		if(!chunk.isLoaded()) return null;
		if(!WaterChunkThread.isChunkLoaded(chunk.getWorld(), chunk.getX(), chunk.getZ())) return null;
		try{
			return chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ()).getBlock(getChunkX(), y, getChunkZ()).getState();
		} catch (Exception e){
			LivelyWorld.getInstance().getLogger().warning("Error while getting blockstate at [" + getChunkX() + "," + y + "," + getChunkZ() + "] in chunk " + chunk.getX() + "," + chunk.getZ());
			e.printStackTrace();
		}
		return null;
	}
	
	public static int getWaterLevelAt(World world, int x, int y, int z){
		WaterChunk wc = WaterChunk.get(world, x >> 4, z >> 4);
		return getWaterLevelAt(wc, Math.floorMod(x, 16), y, Math.floorMod(z, 16));
	}
	
	public static int getWaterLevelAt(WaterChunk chunk, int x, int y, int z){
		int d = chunk.getData(x, y, z);
		return (d & (7 << moistureCode)) >> moistureCode;
	}
	
	public void sendChangedEvent(){
		BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				BlockState s = getBlockState();
				if(s == null) return;
				Block b = getBlockState().getBlock();
				if(getPermeability() >= 1 && getRelative(BlockFace.DOWN).getBlockState().getType() != Material.AIR){
					if(Utils.getWaterHeight(b) != getLevel()){
						Utils.setWaterHeight(b, getLevel(), false);
					}
				} else if(getLevel() > 0) {
					chunk.getWorld().spawnParticle(Particle.DRIP_WATER, b.getX() + Math.random(), b.getY(), b.getZ() + Math.random(), 1);
				}
				//BlockWaterChangedEvent e = new BlockWaterChangedEvent(b, getData());
				//Bukkit.getPluginManager().callEvent(e);
			}
		};
		br.runTask(LivelyWorld.getInstance());
	}
}
