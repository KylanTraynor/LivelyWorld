package com.kylantraynor.livelyworld.waterV2;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;

public class WaterListener implements Listener {

    private WaterModule module;

    public WaterListener(WaterModule module){
        this.module = module;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event){
        if(!event.getBlock().getWorld().getName().equals("world")) return;
        if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
        WaterWorld w = module.getWorld(event.getBlock().getWorld());
        if (w == null) return;
        WaterChunk wc = w.getChunk(event.getBlock().getChunk().getX(), event.getBlock().getChunk().getZ());
        if(wc == null) return;
        int x = Utils.floorMod2(event.getBlock().getX(), 4);
        int z = Utils.floorMod2(event.getBlock().getZ(), 4);
        wc.onBlockChange(x,event.getBlock().getY(), z, null, new boolean[8], true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event){
        if(!event.getBlock().getWorld().getName().equals("world")) return;
        if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;
        WaterWorld w = module.getWorld(event.getBlock().getWorld());
        if (w == null) return;
        WaterChunk wc = w.getChunk(event.getBlock().getChunk().getX(), event.getBlock().getChunk().getZ());
        if(wc == null) return;
        int x = Utils.floorMod2(event.getBlock().getX(), 4);
        int z = Utils.floorMod2(event.getBlock().getZ(), 4);
        BlockData bd = event.getBlockPlaced().getBlockData();
        Permeability perm = WaterUtils.materialToPermeability(bd.getMaterial());
        boolean[] obstacles = new boolean[8];
        if(perm != null){
            obstacles = WaterUtils.dataToObstacles(bd);
        }
        wc.onBlockChange(x,event.getBlock().getY(), z, perm, obstacles, true);
    }

    @EventHandler
    public void onFluidLevelChange(FluidLevelChangeEvent event){
        if(!Utils.isWater(event.getBlock())) return;
        if(!event.getBlock().getWorld().getName().equals("world")) return;
        if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;

        event.setCancelled(true);
    }

}
