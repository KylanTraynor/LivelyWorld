package com.kylantraynor.livelyworld.events;

import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.material.MaterialData;

import com.kylantraynor.livelyworld.deterioration.DeteriorationCause;
import com.kylantraynor.livelyworld.water.WaterData;

public class BlockWaterChangedEvent extends BlockEvent{
	private boolean cancelled = false;
	private DeteriorationCause cause = null;
	private MaterialData target = null;
	private long data;
	private Block block;
	private static final HandlerList handlers = new HandlerList();
	
	public BlockWaterChangedEvent(Block theBlock, long data) {
		super(theBlock);
		this.data = data;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
	    return handlers;
	}
	
	public int getNewLevel() {
		return 0;//(int) (data & (WaterData.maxLevel << WaterData.moistureCode)) >>> WaterData.moistureCode;
	}
}
