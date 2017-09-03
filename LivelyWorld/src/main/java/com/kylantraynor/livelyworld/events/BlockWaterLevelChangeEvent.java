package com.kylantraynor.livelyworld.events;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.material.MaterialData;

import com.kylantraynor.livelyworld.deterioration.DeteriorationCause;

public class BlockWaterLevelChangeEvent extends BlockEvent implements Cancellable{
	private boolean cancelled = false;
	private DeteriorationCause cause = null;
	private MaterialData target = null;
	private int newLevel;
	private Block block;
	private static final HandlerList handlers = new HandlerList();
	
	public BlockWaterLevelChangeEvent(Block theBlock, int newlevel) {
		super(theBlock);
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
	    return handlers;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean newValue) {
		cancelled = newValue;
	}
	
	public int getNewLevel() {
		return newLevel;
	}

	public void setNewLevel(int newLevel) {
		this.newLevel = newLevel;
	}
}
