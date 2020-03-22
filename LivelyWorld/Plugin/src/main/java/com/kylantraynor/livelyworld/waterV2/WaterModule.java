package com.kylantraynor.livelyworld.waterV2;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WaterModule {

    Map<String, WaterWorld> worlds;

    private WaterListener listener;
    private final WaterChunkLoaderThread loaderThread;

    private boolean realisticSimulation = true;
    public boolean usePermeability = true;
    private boolean debug = true;

    /**
     * TODO
     * @return
     */
    public boolean isEnabled(){return true;}

    public WaterModule(LivelyWorld plugin){
        worlds = new ConcurrentHashMap<>();

        listener = new WaterListener(this);
        Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(plugin, () -> {
            for(WaterWorld w : worlds.values()){
                w.update();
            }
        }, 30L, 20L);

        loaderThread = new WaterChunkLoaderThread();
        loaderThread.start();
    }

    public void disable(){
        loaderThread.interrupt();
    }

    public void unloadAll(){
        for(WaterWorld world : worlds.values()){
            world.unloadAll();
        }
    }

    public void onCommand(CommandSender sender, Command cmd, String label,
                          String[] args) {
        if (args.length >= 2) {
            switch (args[1].toUpperCase()) {
                case "INFO":
                    sender.sendMessage("Loaded worlds: " + worlds.size());
                    for(String key : worlds.keySet().toArray(new String[0])){
                        sender.sendMessage(key);
                        sender.sendMessage("  Loaded chunks: " + worlds.get(key).loadedChunks.size());
                        sender.sendMessage("  Chunks pending load: " + worlds.get(key).pendingLoadChunks.size());
                        sender.sendMessage("  Chunks pending unload: " + worlds.get(key).pendingUnloadChunks.size());
                    }
                    /*sender.sendMessage("Timings: ");
                    sender.sendMessage("Ocean Level: " + Utils.fitTimings(WaterChunk.total[0] / WaterChunk.samples[0]) + " (" + WaterChunk.samples[0] + ")");
                    sender.sendMessage("Pressure Update: " + Utils.fitTimings(WaterChunk.total[1] / WaterChunk.samples[1]) + " (" + WaterChunk.samples[1] + ")");
                    sender.sendMessage("Chunk Refresh: " + Utils.fitTimings(WaterChunk.total[3] / WaterChunk.samples[3]) + " (" + WaterChunk.samples[3] + ")");
                    sender.sendMessage("Water Update: " + Utils.fitTimings(WaterChunk.total[2] / WaterChunk.samples[2]) + " (" + WaterChunk.samples[2] + ")");
                    sender.sendMessage("BALANCE:  Flow: " + WaterChunk.delta[0] + " Evaporation: " + WaterChunk.delta[1] + " Rain: " + WaterChunk.delta[2] + " Plants: " + WaterChunk.delta[3]);*/
                    break;
                case "STOPWATERTHREAD":
                    /*sender.sendMessage("Stopping water thread.");
                    stopWaterChunksThread();*/
                    break;
                case "DRAINCHUNK":
                    /*sender.sendMessage("Draining Chunk.");
                    Player player = (Player) sender;
                    Chunk c = player.getLocation().getChunk();
                    if(c != null) {
                        BukkitRunnable br = new BukkitRunnable(){
                            @Override
                            public void run() {
                                WaterChunk.get(c.getWorld(), c.getX(), c.getZ()).drain();
                            }
                        };
                        br.runTaskAsynchronously(plugin);
                    }*/
                    break;
                case "SATURATEBLOCK":
                    {
                        sender.sendMessage("Saturating Block.");
                        Player player0 = (Player) sender;
                        Location l = player0.getLocation();
                        WaterWorld ww = worlds.get(l.getWorld().getName());
                        if(ww != null){

                            int chunkX = l.getBlockX() >> 4;
                            int chunkZ = l.getBlockZ() >> 4;

                            int x = Utils.floorMod2(l.getBlockX(), 4);
                            int z = Utils.floorMod2(l.getBlockZ(), 4);

                            int left = 32;

                            if(ww.isChunkLoaded(chunkX, chunkZ)){
                                sender.sendMessage("Adding water at " + x + "," + l.getBlockY() + "," + z + " (Chunk "+chunkX + "," + chunkZ+").");
                                left = ww.getChunk(chunkX, chunkZ).DEBUGAddWaterIn(new BlockLocation(x, l.getBlockY(), z), left);
                            } else {
                                sender.sendMessage("Chunk " + chunkX + "," + chunkZ + " is not loaded.");
                            }

                            //int left = ww.addWaterAt(l.getBlockX(), l.getBlockY(), l.getBlockZ(), 32);
                            if(left > 0){
                                sender.sendMessage(left + " out of 32 could not be added.");
                            }
                        }
                    }
                    break;
                case "DRAINBLOCK":
                    {
                        sender.sendMessage("Draining Block.");
                        Player player0 = (Player) sender;
                        Location l = player0.getLocation();
                        WaterWorld ww = worlds.get(l.getWorld().getName());
                        if(ww != null){
                            int left = ww.removeWaterAt(l.getBlockX(), l.getBlockY(), l.getBlockZ(), 32);
                            if(left > 0){
                                sender.sendMessage(left + " out of 32 could not be removed.");
                            }
                        }
                    }
                    break;
                case "UPDATECHUNK":
                    /*sender.sendMessage("Updating chunk.");
                    Player player1 = (Player) sender;
                    Chunk c1 = player1.getLocation().getChunk();
                    WaterChunk.get(c1.getWorld(), c1.getX(), c1.getZ()).updateVisually();*/
                    break;
                case "GET":
                    if(args.length >= 3){
                        Player p = (Player) sender;
                        WaterWorld w = getWorld(p.getWorld());
                        if(w == null) {p.sendMessage("This is not a Lively World."); return;}
                        switch(args[2].toUpperCase()) {
                            case "MOISTURE":
                                WaterChunk wc = w.getChunk(p.getLocation().getBlockX() >> 4, p.getLocation().getBlockZ() >> 4);
                                int y = p.getLocation().getBlockY();
                                int x = Utils.floorMod2(p.getLocation().getBlockX(), 4);
                                int z = Utils.floorMod2(p.getLocation().getBlockZ(), 4);
                                int wa = wc.getBlockWaterAmount(new BlockLocation(x,y,z));
                                Permeability per = wc.permeabilities[x][y][z];
                                p.sendMessage("Closest water block: Y" + y + ".");
                                p.sendMessage("DEBUG : Water Amount: " + wa + ", Data Permeability: " + (per == null ? "null" : per.toString()));
                                break;
                            case "CLOSEST":
                                WaterChunk wc2 = w.getChunk(p.getLocation().getBlockX() >> 4, p.getLocation().getBlockZ() >> 4);
                                int wa2 = 0;
                                Permeability per2 = null;
                                int y2 = p.getLocation().getBlockY();
                                int x2 = Utils.floorMod2(p.getLocation().getBlockX(), 4);
                                int z2 = Utils.floorMod2(p.getLocation().getBlockZ(), 4);
                                do{
                                    wa = wc2.getBlockWaterAmount(new BlockLocation(x2, y2, z2));
                                    per = wc2.permeabilities[x2][y2][z2];
                                } while (wa2 == 0 && y2-- > 0);
                                y2++;
                                p.sendMessage("Closest water block: Y" + y2 + ".");
                                p.sendMessage("DEBUG : Water Amount: " + wa2 + ", Data Permeability: " + (per2 == null ? "null" : per2.toString()));
                                break;
                        }
                    }
                    break;
                case "TOGGLE":
                    if (args.length >= 3) {
                        switch (args[2].toUpperCase()) {
                            case "SIMULATION":
                                realisticSimulation = !realisticSimulation;
                                sender.sendMessage("Realistic water simulation is set to: " + realisticSimulation);
                                break;
                            case "PERMEABILITY":
                                usePermeability = !usePermeability;
                                sender.sendMessage("Using permeability: " + usePermeability);
                                break;
                            case "WAVES":
                                /*if (ignoredPlayers.contains((Player) sender)) {
                                    ignoredPlayers.remove((Player) sender);
                                    sender.sendMessage("Waves turned on.");
                                } else {
                                    ignoredPlayers.add((Player) sender);
                                    sender.sendMessage("Waves turned off.");
                                }*/
                                break;
                            case "BEACHESREGRESSION":
                                /*if (sender.isOp()) {
                                    beachRegression = !beachRegression;
                                    if (beachRegression) {
                                        sender.sendMessage("Beaches Regression turned on.");
                                    } else {
                                        sender.sendMessage("Beaches Regression turned off.");
                                    }
                                }*/
                                break;
                            case "DEBUG":
                                if (sender.isOp()) {
                                    debug = !debug;
                                    if (debug) {
                                        sender.sendMessage("Debug for water module turned on.");
                                    } else {
                                        sender.sendMessage("Debug for water module turned off.");
                                    }
                                }
                                break;
                        }
                    }
            }
        }
    }

    public boolean isRealisticSimulation(){
        return realisticSimulation;
    }

    public WaterWorld getWorld(World world){
        if(!world.getName().equalsIgnoreCase("world")) return null;

        if(worlds.containsKey(world.getName())) return worlds.get(world.getName());
        worlds.put(world.getName(), new WaterWorld(world));
        return worlds.get(world.getName());
    }

}
