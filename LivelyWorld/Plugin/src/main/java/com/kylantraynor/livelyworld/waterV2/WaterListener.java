package com.kylantraynor.livelyworld.waterV2;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;

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
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event){
        if(!LivelyWorld.getInstance().getWaterModule().isEnabled()) return;
        Block b = event.getBlockClicked().getRelative(event.getBlockFace());
        Chunk c = b.getChunk();

        ItemStack is = event.getPlayer().getInventory().getItemInMainHand();
        String info = Utils.getLoreInfo(is, "Level");

        final int cX = Utils.floorMod2(b.getX(), 4);
        final int cZ = Utils.floorMod2(b.getZ(), 4);
        final int level = (info != null ? Utils.keepBetween(0, Integer.parseInt(info),32) : 32);

        WaterWorld w = module.getWorld(b.getWorld());
        if(w == null) return;
        WaterChunk wc = w.getChunk(c.getX(), c.getZ());
        if(wc == null) return;
        wc.addWaterIn(new BlockLocation(cX, b.getY(), cZ), level);
    }

    @EventHandler
    public void onFluidLevelChange(FluidLevelChangeEvent event){
        if(!Utils.isWater(event.getBlock())) return;
        if(!event.getBlock().getWorld().getName().equals("world")) return;
        if(!LivelyWorld.getInstance().getWaterModule().isRealisticSimulation()) return;

        event.setCancelled(true);
    }

}
