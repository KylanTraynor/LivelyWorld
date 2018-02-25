package com.kylantraynor.livelyworld.climate;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import com.kylantraynor.livelyworld.water.WaterChunk;

public class WeatherEffectsRunnable extends BukkitRunnable {

	final int effects;
	
	public WeatherEffectsRunnable(int effects){
		this.effects = effects;
	}
	
	@Override
	public void run() {
		Collection<? extends Player> plist = Bukkit.getOnlinePlayers();
		if(plist.size() > 0){
			Player p = plist.toArray(new Player[plist.size()])[(int) Utils.fastRandomInt(plist.size())];//.toArray(new Player[Bu]);
			ClimateCell c = LivelyWorld.getInstance().getClimateModule().getClimateCellFor(p);
			if(c == null) return;
			int mostDist = (int) 300;
			int doubleMostDist = mostDist << 1;
			
			for(int i = 0; i < effects; i++){
				int random_x = (int) (Utils.fastRandomInt(doubleMostDist) - mostDist);
				int random_z = (int) (Utils.fastRandomInt(doubleMostDist) - mostDist);
				int x = p.getLocation().getBlockX() + random_x;
				int z = p.getLocation().getBlockZ() + random_z;
				int chunkX = x >> 4; // /16
				int chunkZ = z >> 4; // /16
				if(!p.getWorld().isChunkLoaded(chunkX, chunkZ)){
					continue;
				}
				Block b = p.getWorld().getHighestBlockAt(x, z);
				while(b.getType() == Material.AIR && b.getY() > 1){
					b = b.getRelative(BlockFace.DOWN);
				}
				ClimateCell cell = ClimateUtils.getClimateCellAt(b.getLocation(), c);
				if(cell == null) continue;
				LivelyWorld.getInstance().getClimateModule().updateBiome(b, cell);
				switch(cell.getWeather()){
				case CLEAR:
					double tdiff = ClimateUtils.getAltitudeWeightedTriangleTemperature(cell, b.getLocation()).getValue() - Temperature.fromCelsius(5).getValue();
					if(Utils.fastRandomDouble() < 0.1 * (tdiff / 2)){
						int effectAmount = (int) Math.floor(tdiff/6);
						if(effectAmount > 0){
							if(Utils.isWater(b)){
								final Block fb = b;
								final int evaporation = effectAmount;
								BukkitRunnable br = new BukkitRunnable(){
									@Override
									public void run() {
										WaterChunk wc = WaterChunk.get(fb.getWorld(), chunkX, chunkZ);
										if(wc.isLoaded()){
											int xc = Utils.floorMod2(fb.getX(), 4);
											int zc = Utils.floorMod2(fb.getZ(), 4);
											wc.setLevel(xc, fb.getY(), zc, wc.getLevel(xc, fb.getY(), zc) - evaporation);
										}
									}
								};
								br.runTaskAsynchronously(LivelyWorld.getInstance());
								b.getWorld().spawnParticle(Particle.CLOUD, b.getLocation().add(0.5,0.5,0.5), evaporation, 0.5, 0.5, 0.5, 0.05);
							} else {
								while((b.getRelative(BlockFace.DOWN).getType() == Material.AIR ||
										b.getRelative(BlockFace.DOWN).getType() == Material.LEAVES ||
										b.getRelative(BlockFace.DOWN).getType() == Material.LEAVES_2) &&
										b.getY() > 1){
									b = b.getRelative(BlockFace.DOWN);
								}
								b = Utils.getHighestSnowBlockAround(b, 3);
								final Block fb = b;
								final int amount = ClimateUtils.melt(b, effectAmount);
								BukkitRunnable br = new BukkitRunnable(){
									@Override
									public void run() {
										WaterChunk wc = WaterChunk.get(fb.getWorld(), chunkX, chunkZ);
										if(wc.isLoaded()){
											wc.addWaterAt(Utils.floorMod2(fb.getX(), 4), fb.getY(), Utils.floorMod2(fb.getZ(), 4), (int) (amount * 0xFF) / 8);
										}
									}
								};
								br.runTaskAsynchronously(LivelyWorld.getInstance());
							}
						}
					}
					break;
				case OVERCAST:
					break;
				case RAIN:
				case SNOW:
					double tdiff1 = Temperature.fromCelsius(5).getValue() - ClimateUtils.getAltitudeWeightedTriangleTemperature(cell, b.getLocation()).getValue();
					if(Utils.fastRandomDouble() < 0.5 * (tdiff1 / 2)){
						SnowFallTask task = new SnowFallTask(LivelyWorld.getInstance().getClimateModule(), cell, b.getX(), b.getY() + 1, b.getZ());
						task.runTaskLater(LivelyWorld.getInstance(), 1);
					} if (Utils.fastRandomDouble() < 1.0 * (-tdiff1 / 2)){
						/*while(b.getRelative(BlockFace.DOWN).getType() == Material.WATER || b.getRelative(BlockFace.DOWN).getType() == Material.STATIONARY_WATER){
							b = b.getRelative(BlockFace.DOWN);
						}*/
						final Block fb = b;
						BukkitRunnable br = new BukkitRunnable(){
							@Override
							public void run() {
								WaterChunk wc = WaterChunk.get(fb.getWorld(), chunkX, chunkZ);
								if(wc.isLoaded()){
									wc.addWaterAt(Utils.floorMod2(fb.getX(), 4), fb.getY(), Utils.floorMod2(fb.getZ(), 4), 2);
								}
							}
						};
						br.runTaskAsynchronously(LivelyWorld.getInstance());
					}
					break;
				case STORM:
				case SNOWSTORM:
					double tdiff2 = Temperature.fromCelsius(5).getValue() - ClimateUtils.getAltitudeWeightedTriangleTemperature(cell, b.getLocation()).getValue();
					if(Utils.fastRandomDouble() < 1.0 * (tdiff2 / 2)){
						SnowFallTask task = new SnowFallTask(LivelyWorld.getInstance().getClimateModule(), cell, b.getX(), b.getY() + 1, b.getZ());
						task.runTaskLater(LivelyWorld.getInstance(), 1);
					} if (Utils.fastRandomDouble() < 1.0 * (-tdiff2 / 2)){
						/*while(b.getRelative(BlockFace.DOWN).getType() == Material.WATER || b.getRelative(BlockFace.DOWN).getType() == Material.STATIONARY_WATER){
							b = b.getRelative(BlockFace.DOWN);
						}*/
						final Block fb = b;
						BukkitRunnable br = new BukkitRunnable(){
							@Override
							public void run() {
								WaterChunk wc = WaterChunk.get(fb.getWorld(), chunkX, chunkZ);
								if(wc.isLoaded()){
									wc.addWaterAt(Utils.floorMod2(fb.getX(), 4), fb.getY(), Utils.floorMod2(fb.getZ(), 4), 4);
								}
							}
						};
						br.runTaskAsynchronously(LivelyWorld.getInstance());
					}
					break;
				case THUNDERSTORM:
					if(Utils.fastRandomDouble() < 0.05 / effects){
						Utils.spawnLightning(b.getRelative(BlockFace.UP));
					}
					break;
				default:
					break;
				
				}
			}
		} else {
			
		}
	}

}
