package com.kylantraynor.livelyworld.water;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.Utils.SmallChunkData;
import com.kylantraynor.livelyworld.api.BoatHelper;
import com.kylantraynor.livelyworld.events.BlockWaterLevelChangeEvent;

public class WaterListener implements Listener{
	
	@EventHandler
	public void onVehicleMove(VehicleMoveEvent event) {
		if (event.getVehicle().getType() == EntityType.BOAT) {
			Boat boat = (Boat) event.getVehicle();
			BoatHelper helper = LivelyWorld.getInstance().getWaterModule().getBoatHelper();
			if(helper != null){
				/*if(boat.getPassengers().size() > 0){
					boat.getPassengers().get(0).sendMessage("[DEBUG] Is Underwater? " + helper.isUnderwater(boat));
				}*/
				if(helper.isUnderwater(boat)){
					Vector v = helper.getMotionVector(boat);
					boat.teleport(boat.getLocation().clone().add(0,1/6.0,0));
					v.setY(v.getY() < 0 ? -v.getY() + 0.2 : 0.4);
					helper.setMotionVector(boat, v);
				}
			} else {
				if((event.getTo().getBlock().getBiome() == Biome.BEACHES || 
						event.getTo().getBlock().getBiome() == Biome.STONE_BEACH || 
						event.getTo().getBlock().getBiome() == Biome.OCEAN ||
						event.getTo().getBlock().getBiome() == Biome.DEEP_OCEAN) &&
						(event.getTo().getY() > LivelyWorld.getInstance().getOceanY() - 1 &&
						event.getTo().getY() < LivelyWorld.getInstance().getOceanY() + 1))
				{
					if ((event.getTo().clone().add(0, 1, 0).getBlock().isLiquid() || event
							.getFrom().clone().add(0, 0, 0).getBlock().isLiquid())
							&& event.getVehicle().getPassenger() != null) {
						if (event.getVehicle().getPassenger().getVelocity().getY() < 0) {
							event.getVehicle().setVelocity(
									event.getVehicle()
											.getPassenger()
											.getVelocity()
											.add(new Vector(0d, Math.abs(event
													.getVehicle().getPassenger()
													.getVelocity().getY() + 0.5), 0d))
											.add(event
													.getVehicle()
													.getPassenger()
													.getLocation()
													.getDirection()
													.setY(0)
													.add(event.getVehicle()
															.getPassenger()
															.getLocation()
															.getDirection().setY(0)
															.multiply(0.2))));
						} else {
						}
					}
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockWaterLevelChange(BlockWaterLevelChangeEvent event){
		/*for(Player p : Bukkit.getOnlinePlayers()){
			if(p.getLocation().distanceSquared(event.getBlock().getLocation()) < (100*100)){
				if(event.getNewLevel() > 0){
					BukkitRunnable br = new BukkitRunnable(){
						public void run(){
							p.sendBlockChange(event.getBlock().getLocation(), Material.WATER, (byte)(7 - event.getNewLevel()));
						}
					};
					br.runTaskLater(LivelyWorld.getInstance(), 1);
				} else {
					BukkitRunnable br = new BukkitRunnable(){
						public void run(){
							p.sendBlockChange(event.getBlock().getLocation(), Material.AIR, (byte)(0));
						}
					};
					br.runTaskLater(LivelyWorld.getInstance(), 1);
				}
			}
		}*/
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event){
		if(!Utils.isWater(event.getBlock())) return;
		if(!event.getBlock().getWorld().getName().equals("world")) return;
		if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
		event.getBlock().setTypeIdAndData(Material.STATIONARY_WATER.getId(), event.getBlock().getData(), false);
		event.setCancelled(true);
		/*Biome fromBiome = event.getBlock().getBiome();
		switch(fromBiome){
		case OCEAN:
			if(event.getBlock().getY() <= LivelyWorld.getInstance().getOceanY()){
				event.getToBlock().setType(event.getBlock().getType());
				event.setCancelled(true);
				return;
			}
		default:
		}*/
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){
		if(event.getFrom().getChunk() != event.getTo().getChunk()){
			if(WaterChunk.disabled) return;
			if(!LivelyWorld.getInstance().getWaterModule().isEnabled()) return;
			Chunk c = event.getTo().getChunk();
			if(!c.getWorld().getName().equals("world")) return;
			LivelyWorld.getInstance().getWaterModule().getWaterThread().addLoadedChunk(c);
			LivelyWorld.getInstance().getWaterModule().getWaterThread().updateOnlinePlayer(event.getPlayer());
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		LivelyWorld.getInstance().getWaterModule().getWaterThread().updateOnlinePlayer(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){
		LivelyWorld.getInstance().getWaterModule().getWaterThread().removeOnlinePlayer(event.getPlayer());
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event){
		if(!LivelyWorld.getInstance().getWaterModule().isEnabled()) return;
		Block b = event.getBlockClicked().getRelative(event.getBlockFace());
		Chunk c = b.getChunk();
		
		ItemStack is = event.getPlayer().getInventory().getItemInMainHand();
		String info = Utils.getLoreInfo(is, "Level");
		
		final int level = (int) (info != null ? Utils.keepBetween(0, Integer.parseInt(info), (int) WaterData.maxLevel) : WaterData.maxLevel);
		
		if(!c.getWorld().getName().equals("world")) return;
		BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				WaterChunk wc = WaterChunk.get(c.getWorld(), c.getX(), c.getZ());
				wc.addWaterAt(Math.floorMod(b.getX(), 16), b.getY(), Math.floorMod(b.getZ(), 16), level);
			}
		};
		br.runTaskAsynchronously(LivelyWorld.getInstance());
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent event){
		if(!LivelyWorld.getInstance().getWaterModule().isEnabled()) return;
		Block b = event.getBlockClicked().getRelative(event.getBlockFace());
		Chunk c = b.getChunk();
		if(!c.getWorld().getName().equals("world")) return;
		BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				WaterChunk wc = WaterChunk.get(c.getWorld(), c.getX(), c.getZ());
				WaterData d = wc.getAt(Math.floorMod(b.getX(), 16), b.getY(), Math.floorMod(b.getZ(), 16));
				Utils.setLoreInfo(event.getItemStack(), "Level", "" + d.getLevel());
				d.level = 0;
			}
		};
		br.runTaskAsynchronously(LivelyWorld.getInstance());
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event){
		if(!event.getBlock().getWorld().getName().equals("world")) return;
		if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
		/*BukkitRunnable bk = new WaterDataUpdate(event.getBlock());
		bk.runTaskLaterAsynchronously(LivelyWorld.getInstance(), 1);*/
		SmallChunkData scd = WaterChunkThread.getChunkData(event.getBlock().getWorld(), event.getBlock().getChunk().getX(), event.getBlock().getChunk().getZ());
		if(scd != null){
			int x = Utils.floorMod2(event.getBlock().getX(), 4);
			int z = Utils.floorMod2(event.getBlock().getZ(), 4);
			scd.getState(x, event.getBlock().getY(), z).setData(new MaterialData(Material.AIR));
		} else {
			scd = LivelyWorld.getInstance().getWaterModule().getWaterThread().addLoadedChunk(event.getBlock().getChunk());
			int x = Utils.floorMod2(event.getBlock().getX(), 4);
			int z = Utils.floorMod2(event.getBlock().getZ(), 4);
			scd.getState(x, event.getBlock().getY(), z).setData(new MaterialData(Material.AIR));
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event){
		if(!event.getBlock().getWorld().getName().equals("world")) return;
		if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
		/*BukkitRunnable bk = new WaterDataUpdate(event.getBlock());
		bk.runTaskLaterAsynchronously(LivelyWorld.getInstance(), 1);*/
		SmallChunkData scd = WaterChunkThread.getChunkData(event.getBlock().getWorld(), event.getBlock().getChunk().getX(), event.getBlock().getChunk().getZ());
		if(scd != null){
			int x = Utils.floorMod2(event.getBlock().getX(), 4);
			int z = Utils.floorMod2(event.getBlock().getZ(), 4);
			scd.getState(x, event.getBlock().getY(), z).setData(event.getBlockPlaced().getState().getData());
		} else {
			scd = LivelyWorld.getInstance().getWaterModule().getWaterThread().addLoadedChunk(event.getBlock().getChunk());
			int x = Utils.floorMod2(event.getBlock().getX(), 4);
			int z = Utils.floorMod2(event.getBlock().getZ(), 4);
			scd.getState(x, event.getBlock().getY(), z).setData(event.getBlockPlaced().getState().getData());
		}
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event){
		if(WaterChunk.disabled) return;
		if(!LivelyWorld.getInstance().getWaterModule().isEnabled()) return;
		Chunk c = event.getChunk();
		if(!c.getWorld().getName().equals("world")) return;
		LivelyWorld.getInstance().getWaterModule().getWaterThread().addLoadedChunk(c);
		/*BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				WaterChunk.get(c.getWorld(), c.getX(), c.getZ());
			}
		};
		br.runTaskAsynchronously(LivelyWorld.getInstance());*/
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event){
		if(WaterChunk.disabled) return;
		Chunk c = event.getChunk();
		if(!c.getWorld().getName().equals("world")) return;
		LivelyWorld.getInstance().getWaterModule().getWaterThread().removeLoadedChunk(c);
		/*BukkitRunnable br = new BukkitRunnable(){
			@Override
			public void run() {
				WaterChunk wc = WaterChunk.get(c.getWorld(), c.getX(), c.getZ());
				wc.unload();
			}
		};
		br.runTaskLaterAsynchronously(LivelyWorld.getInstance(), 20 * 5);*/
	}

}
