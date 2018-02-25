package com.kylantraynor.livelyworld.water;

/**
 * Must be used run on the main thread only.
 * @author Baptiste
 *
 */
/*
public class WaterChunkUpdateRunnable extends BukkitRunnable {

	public enum UpdateType{
		LEVEL,
		RESISTANCE;
	}
	
	private final WaterChunk chunk;
	private final UpdateType updateType;
	
	public WaterChunkUpdateRunnable(WaterChunk chunk, UpdateType type){
		this.chunk = chunk;
		this.updateType = type;
	}
	
	public int getPCS(){
		int pc = Bukkit.getOnlinePlayers().size();
		return pc * pc;
	}
	
	@Override
	public void run() {
		if(!chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ())) return;
		Chunk c = chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ());
		int level = 0;
		int waterLevel = 0;
		WaterData wd = null;
		Block currentBlock = null;
		if(updateType == UpdateType.LEVEL){
			for(int y = 0; y < 256; y++){
				for(int x = 0; x < 16; x++){
					for(int z = 0; z < 16; z++){
						wd = chunk.getAt(x, y, z);
						//if(!wd.needsUpdate()) continue;
						level = wd.getLevel();
						currentBlock = c.getBlock(x, y, z);
						if(wd.getResistance() != WaterData.getResistanceFor(currentBlock.getType())){
							new WaterDataUpdate(currentBlock).runTaskLaterAsynchronously(LivelyWorld.getInstance(), 1);
						}
						if(WaterData.canReplace(currentBlock.getType())){
							waterLevel = WaterData.toWaterLevel(level);
							if(waterLevel != Utils.getWaterHeight(currentBlock)){
								if(waterLevel > 0 && WaterData.isDropable(currentBlock.getType())){
									currentBlock.breakNaturally();
								}
								Utils.setWaterHeight(currentBlock, waterLevel, true);
							}
						}
					}
				}
			}
		} else if(updateType == UpdateType.RESISTANCE){
			int res = 0;
			for(int y = 0; y < 256; y++){
				for(int x = 0; x < 16; x++){
					for(int z = 0; z < 16; z++){
						currentBlock = c.getBlock(x, y, z);
						res = WaterData.getResistanceFor(currentBlock.getType());
						wd = chunk.getAt(x, y, z);
						if(wd.getResistance() != res){
							BukkitRunnable br = new WaterResistanceUpdate(wd, res);
							br.runTaskAsynchronously(LivelyWorld.getInstance());
						}
					}
				}
			}
		}
	}
}*/
