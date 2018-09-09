package com.kylantraynor.livelyworld.climate;

import java.util.Collection;
import java.lang.ref.WeakReference;

import org.bukkit.*;
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
	    World world = Bukkit.getWorld("world");
        Chunk[] chunks = world.getLoadedChunks();
        int l = chunks.length / 4;
		for(int i = 0; i < l; i++){
		    WeakReference<Chunk> chunkRef = new WeakReference<>(chunks[Utils.fastRandomInt(chunks.length)]);
		    final int random_x = Utils.fastRandomInt(16);
		    final int random_z = Utils.fastRandomInt(16);
		    int y = 0;
            while(chunkRef.get() != null & chunkRef.get().getBlock(random_x, y, random_z).getType() != Material.AIR && y < 255){
                y++;
            }
            final int fy = y;
            if(chunkRef.get() == null) continue;
            final int chunkX = chunkRef.get().getX();
            final int chunkZ = chunkRef.get().getZ();
            final Block b = chunkRef.get().getBlock(random_x, y, random_z);

            int x = (chunkX << 4) + random_x;
            int z = (chunkZ << 4) + random_z;

            ClimateCell c = ClimateUtils.getClimateCellAt(world, x, z);

			if(c == null) return;

            LivelyWorld.getInstance().getClimateModule().updateBiome(b, c);
            switch(c.getWeather()){
                case CLEAR:
                    double tdiff = ClimateUtils.getAltitudeWeightedTemperature(b.getLocation()).getValue() - Temperature.fromCelsius(5).getValue();
                    if(Utils.fastRandomDouble() < 0.1 * (tdiff / 2)){
                        int effectAmount = (int) Math.floor(tdiff/6);
                        if(effectAmount > 0){
                            if(Utils.isWater(b)){
                                final int evaporation = (int) (effectAmount * (1-(c.getRelativeHumidity() * 0.01)));
                                BukkitRunnable br = new BukkitRunnable(){
                                    @Override
                                    public void run() {
                                        WaterChunk wc = WaterChunk.get(b.getWorld(), chunkX, chunkZ);
                                        if(wc.isLoaded()){
                                            int oldLevel = wc.getLevel(random_x, fy, random_z);
                                            wc.setLevel(random_x, fy, random_z, oldLevel - evaporation);
                                            WaterChunk.delta[1] -= (oldLevel-evaporation < 0 ? oldLevel : evaporation);
                                        }
                                    }
                                };
                                br.runTaskAsynchronously(LivelyWorld.getInstance());
                                b.getWorld().spawnParticle(Particle.CLOUD, b.getLocation().add(0.5,0.5,0.5), evaporation, 0.5, 0.5, 0.5, 0.05);
                            } else {
                                Block sb = b;
                                while((sb.getRelative(BlockFace.DOWN).getType() == Material.AIR ||
                                        sb.getRelative(BlockFace.DOWN).getType() == Material.LEAVES ||
                                        sb.getRelative(BlockFace.DOWN).getType() == Material.LEAVES_2) &&
                                        sb.getY() > 1){
                                    sb = sb.getRelative(BlockFace.DOWN);
                                }
                                final Block fb = Utils.getHighestSnowBlockAround(sb, 3);
                                final int amount = ClimateUtils.melt(b, effectAmount);
                                BukkitRunnable br = new BukkitRunnable(){
                                    @Override
                                    public void run() {
                                        WaterChunk wc = WaterChunk.get(fb.getWorld(), chunkX, chunkZ);
                                        if(wc.isLoaded()){
                                            int am = (int) (amount * 0xFF) / 8;
                                            wc.addWaterAt(random_x, fy, random_z, am);
                                            WaterChunk.delta[2] += am;
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
                    double tdiff1 = Temperature.fromCelsius(5).getValue() - ClimateUtils.getAltitudeWeightedTemperature(b.getLocation()).getValue();
                    if(Utils.fastRandomDouble() < 0.5 * (tdiff1 / 2)){
                        SnowFallTask task = new SnowFallTask(LivelyWorld.getInstance().getClimateModule(), c, b.getX(), b.getY() + 1, b.getZ());
                        task.runTaskLater(LivelyWorld.getInstance(), 1);
                    } if (Utils.fastRandomDouble() < 1.0 * (-tdiff1 / 2)){
						/*while(b.getRelative(BlockFace.DOWN).getType() == Material.WATER || b.getRelative(BlockFace.DOWN).getType() == Material.STATIONARY_WATER){
							b = b.getRelative(BlockFace.DOWN);
						}*/
                    BukkitRunnable br = new BukkitRunnable(){
                        @Override
                        public void run() {
                            WaterChunk wc = WaterChunk.get(b.getWorld(), chunkX, chunkZ);
                            if(wc.isLoaded()){
                                wc.addWaterAt(random_x, fy, random_z, 2);
                                WaterChunk.delta[2] += 2;
                            }
                        }
                    };
                    br.runTaskAsynchronously(LivelyWorld.getInstance());
                }
                    break;
                case STORM:
                case SNOWSTORM:
                    double tdiff2 = Temperature.fromCelsius(5).getValue() - ClimateUtils.getAltitudeWeightedTemperature(b.getLocation()).getValue();
                    if(Utils.fastRandomDouble() < 1.0 * (tdiff2 / 2)){
                        SnowFallTask task = new SnowFallTask(LivelyWorld.getInstance().getClimateModule(), c, b.getX(), b.getY() + 1, b.getZ());
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
                                wc.addWaterAt(random_x, fy, random_z, 4);
                                WaterChunk.delta[2] += 4;
                            }
                        }
                    };
                    br.runTaskAsynchronously(LivelyWorld.getInstance());
                }
                    break;
                case THUNDERSTORM:
                    if(Utils.fastRandomDouble() < 0.05 / effects){
                        Utils.spawnLightning(b);
                    }
                    break;
                default:
                    break;

            }
		}
	}

}
