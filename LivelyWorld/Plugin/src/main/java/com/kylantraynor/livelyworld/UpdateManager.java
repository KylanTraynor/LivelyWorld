package com.kylantraynor.livelyworld;

import java.time.Instant;
import java.time.temporal.ChronoField;

import com.kylantraynor.livelyworld.hooks.HookManager;
import com.kylantraynor.livelyworld.waterV2.BlockLocation;
import com.kylantraynor.livelyworld.waterV2.WaterChunk;
import com.kylantraynor.livelyworld.waterV2.WaterWorld;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;

public class UpdateManager {
	
	private static Instant lastBlockUpdate = Instant.now();
	private static long blockUpdatePeriod = 5L;

	public static void processRandomChunkUpdate(World world){
		
		if (Instant.now().get(ChronoField.MILLI_OF_SECOND)
				- lastBlockUpdate.get(ChronoField.MILLI_OF_SECOND) > (ChronoField.MILLI_OF_SECOND
				.range().getMaximum() * (blockUpdatePeriod + 1) / 20.0)) {
			lastBlockUpdate = Instant.now();
			return;
		} else {
			lastBlockUpdate = Instant.now();
		}
		
		Chunk[] chunksToUpdate = new Chunk[getOnlinePlayersInWorld(world)];
		for(int i = 0; i < chunksToUpdate.length; i++){
			chunksToUpdate[i] = world.getLoadedChunks()[Utils.fastRandomInt(world.getLoadedChunks().length)];
		}
		if(chunksToUpdate.length == 0){
			int randomX = 0;
			int randomZ = 0;
			if(HookManager.hasWorldBorder()){
				Location worldCenter = HookManager.getWorldBorder().getWorldCenter(world);
				double worldRadiusX = HookManager.getWorldBorder().getWorldRadiusX(world);
				double worldRadiusZ = HookManager.getWorldBorder().getWorldRadiusZ(world);
				if(worldRadiusX == 0 || worldRadiusZ == 0) return;
					randomX = (int) (Utils.fastRandomInt((int) (worldRadiusX * 2)) - worldRadiusX);
				randomZ = (int) (Utils.fastRandomInt((int) (worldRadiusZ * 2)) - worldRadiusZ);
				randomX += worldCenter.getBlockX();
				randomZ += worldCenter.getBlockZ();
			} else {
				return;
			}
			randomX = randomX >> 4;
			randomZ = randomZ >> 4;
			chunksToUpdate = new Chunk[]{world.getChunkAt(randomX, randomZ)};
		}
		processChunksUpdate(chunksToUpdate);
	}
	
	private static void processChunksUpdate(Chunk[] chunksToUpdate) {
		for(Chunk c : chunksToUpdate){
			processChunkUpdate(c);
		}
	}

	private static void processChunkUpdate(Chunk c) {
		for(int y = 0; y < 256; y++){
			for(int x = 0; x < 16; x++){
				for(int z = 0; z < 16; z++){
					processBlockUpdate(c.getBlock(x, y, z));
				}
			}
		}
	}

	private static void processBlockUpdate(Block block) {
		if(LivelyWorld.getInstance().getWaterModule().isEnabled() && Utils.fastRandomDouble() < 0.01){
			processWaterUpdate(block);
		}
		if(LivelyWorld.getInstance().getGravityModule().isEnabled() && Utils.fastRandomDouble() < 0.0001){
			LivelyWorld.getInstance().getGravityModule().checkGravityOn(block);
		}
		if(LivelyWorld.getInstance().getVegetationModule().isEnabled() && Utils.fastRandomDouble() < 0.001){
			LivelyWorld.getInstance().getVegetationModule().onBlockUpdate(block, null);
		}
	}

	private static void processWaterUpdate(Block block) {
		if(block.getType() == Material.FARMLAND){
			WaterWorld w = LivelyWorld.getInstance().getWaterModule().getWorld(block.getWorld());
			if(w == null) return;
			WaterChunk wc = w.getChunk(block.getX() >> 4, block.getZ() >> 4);
			if(wc == null) return;
			int xc = Utils.floorMod2(block.getX(), 4);
			int zc = Utils.floorMod2(block.getZ(), 4);
			Farmland frld = (Farmland) block.getBlockData();
			int moisture = frld.getMoisture();
			int level = wc.getBlockWaterAmount(new BlockLocation(xc, block.getY(), zc));
			int aboveLevel = 0;
			if(block.getY() < 255){
				aboveLevel = wc.getBlockWaterAmount(new BlockLocation(xc, block.getY() + 1, zc));
			}
			if(level > 0 || aboveLevel > 0){
				frld.setMoisture(7);
				if(Utils.isCrop(block.getRelative(BlockFace.UP).getType())){
					if(Utils.fastRandomDouble() > 0.9){
					    wc.removeWaterIn(new BlockLocation(xc, block.getY(), zc), 1);
					}
				}
				block.setBlockData(frld, false);
			} else if(moisture > 0){
				frld.setMoisture(moisture - 1);
				block.setBlockData(frld, false);
			}
		}
	}

	public static int getOnlinePlayersInWorld(World world){
		int count = 0;
		for(Player p : Bukkit.getOnlinePlayers()){
			if(p.getWorld() == world){
				count++;
			}
		}
		return count;
	}
	
}
