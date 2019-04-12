package com.kylantraynor.livelyworld.climate;

import java.util.ArrayList;
import java.util.Collection;
import java.lang.ref.WeakReference;
import java.util.List;

import com.kylantraynor.livelyworld.waterV2.BlockLocation;
import com.kylantraynor.livelyworld.waterV2.WaterChunk;
import com.kylantraynor.livelyworld.waterV2.WaterWorld;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;

public class WeatherEffectsRunnable extends BukkitRunnable {

    private final World world;
	private final int effects;
	
	WeatherEffectsRunnable(World world, int effects){
	    this.world = world;
	    this.effects = effects;
	}

    public int closestPlayerDistance(int x, int z){
        int closestDistance = 15;
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            if(p.getWorld() == world){
                int d = Utils.manhattanDistance(x,z, p.getLocation().getBlockX() >> 4, p.getLocation().getBlockZ() >> 4);
                closestDistance = d < closestDistance ? d : closestDistance;
            }
        }
        return closestDistance;
    }

    public int playersInWorld(){
	    int count = 0;
	    for(Player p : Bukkit.getServer().getOnlinePlayers()){
	        if(p.getWorld() == world){
	            count++;
            }
        }
        return count;
    }

    public Chunk[] getValidChunks(Chunk[] chunks){
	    ArrayList<Chunk> result = new ArrayList<>();
	    for(Chunk c : chunks){
	        if(closestPlayerDistance(c.getX(), c.getZ()) < 8){
	            result.add(c);
            }
        }
        return result.toArray(new Chunk[0]);
    }

	@Override
	public void run() {
	    final int playersInWorld = playersInWorld();
	    if(playersInWorld == 0) return;
        Chunk[] chunks = getValidChunks(world.getLoadedChunks());
        if(chunks.length == 0) return;
        final int l = (playersInWorld * 10);

		for(int i = 0; i < l; i++){
		    Chunk chunk = chunks[Utils.fastRandomInt(chunks.length)];
		    final int random_x = Utils.fastRandomInt(16);
		    final int random_z = Utils.fastRandomInt(16);
		    int y = 0;
            while(chunk.getBlock(random_x, y, random_z).getType() != Material.AIR && y < 255){
                y++;
            }
            final int fy = y;
            final int chunkX = chunk.getX();
            final int chunkZ = chunk.getZ();
            final Block b = chunk.getBlock(random_x, y, random_z);

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
                                WaterWorld w = LivelyWorld.getInstance().getWaterModule().getWorld(b.getWorld());
                                if(w == null) continue;
                                w.removeWaterAt(random_x, y, random_z, evaporation);

                                b.getWorld().spawnParticle(Particle.CLOUD, b.getLocation().add(0.5,0.5,0.5), evaporation, 0.5, 0.5, 0.5, 0.05);
                            } else {
                                Block sb = b;
                                while((sb.getRelative(BlockFace.DOWN).getType() == Material.AIR ||
                                        Utils.isLeaves(sb.getRelative(BlockFace.DOWN).getType())) &&
                                        sb.getY() > 1){
                                    sb = sb.getRelative(BlockFace.DOWN);
                                }
                                final Block fb = Utils.getHighestSnowBlockAround(sb, 3);
                                if(fb == null) continue;
                                final int amount = ClimateUtils.melt(b, effectAmount);
                                WaterWorld w = LivelyWorld.getInstance().getWaterModule().getWorld(b.getWorld());
                                if(w == null) continue;
                                w.addWaterAt(Utils.floorMod2(fb.getX(), 4), fb.getY(), Utils.floorMod2(fb.getZ(), 4), amount);
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
                        WaterWorld w = LivelyWorld.getInstance().getWaterModule().getWorld(b.getWorld());
                        if(w == null) continue;
                        w.addWaterAt(random_x, y, random_z, 2);
                    }
                    break;
                case STORM:
                case SNOWSTORM:
                    double tdiff2 = Temperature.fromCelsius(5).getValue() - ClimateUtils.getAltitudeWeightedTemperature(b.getLocation()).getValue();
                    if(Utils.fastRandomDouble() < 1.0 * (tdiff2 / 2)){
                        SnowFallTask task = new SnowFallTask(LivelyWorld.getInstance().getClimateModule(), c, b.getX(), b.getY() + 1, b.getZ());
                        task.runTaskLater(LivelyWorld.getInstance(), 1);
                    } if (Utils.fastRandomDouble() < 1.0 * (-tdiff2 / 2)){
                        WaterWorld w = LivelyWorld.getInstance().getWaterModule().getWorld(b.getWorld());
                        if(w == null) continue;
                        w.addWaterAt(random_x, y, random_z, 4);
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
