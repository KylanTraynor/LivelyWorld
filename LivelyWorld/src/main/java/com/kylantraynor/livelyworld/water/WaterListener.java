package com.kylantraynor.livelyworld.water;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;

public class WaterListener implements Listener{
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event){
		if(!Utils.isWater(event.getBlock())) return;
		if(!event.getBlock().getWorld().getName().equals("world")) return;
		Biome fromBiome = event.getBlock().getBiome();
		switch(fromBiome){
		case OCEAN:
			if(event.getBlock().getY() <= LivelyWorld.getInstance().getOceanY()){
				event.getToBlock().setType(event.getBlock().getType());
				event.setCancelled(true);
				return;
			}
		default:
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event){
		if(!LivelyWorld.getInstance().getWaterModule().isEnabled()) return;
		Block b = event.getBlockClicked().getRelative(event.getBlockFace());
		Chunk c = b.getChunk();
		if(!c.getWorld().getName().equals("world")) return;
		BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				WaterChunk wc = WaterChunk.get(c.getWorld(), c.getX(), c.getZ());
				wc.addWaterAt(Math.floorMod(b.getX(), 16), b.getY(), Math.floorMod(b.getZ(), 16), 7);
			}
		};
		br.runTaskAsynchronously(LivelyWorld.getInstance());
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent event){
		Block b = event.getBlockClicked();
		Biome clickedBiome = b.getBiome();
		switch(clickedBiome){
		case OCEAN:
			if(b.getY() > LivelyWorld.getInstance().getOceanY()) return;
		case RIVER:
			event.getItemStack().setType(Material.WATER_BUCKET);
			event.setCancelled(true);
			return;
		default:
		}
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event){
		/*if(WaterChunk.disabled) return;
		if(!LivelyWorld.getInstance().getWaterModule().isEnabled()) return;
		Chunk c = event.getChunk();
		if(!c.getWorld().getName().equals("world")) return;
		BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				WaterChunk.get(c.getWorld(), c.getX(), c.getZ());
			}
		};
		br.runTaskAsynchronously(LivelyWorld.getInstance());*/
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event){
		/*if(WaterChunk.disabled) return;
		Chunk c = event.getChunk();
		if(!c.getWorld().getName().equals("world")) return;
		BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				WaterChunk wc = WaterChunk.get(c.getWorld(), c.getX(), c.getZ());
				wc.unload();
			}
		};
		br.runTaskLaterAsynchronously(LivelyWorld.getInstance(), 20 * 5);*/
	}

}
