package com.kylantraynor.livelyworld.events;

import com.kylantraynor.livelyworld.deterioration.DeteriorationCause;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.material.MaterialData;

public class BlockDeteriorateEvent extends BlockEvent implements Cancellable{

	private boolean cancelled = false;
	private DeteriorationCause cause = null;
	private Material target;
	private static final HandlerList handlers = new HandlerList();
	
	public BlockDeteriorateEvent(Block theBlock, DeteriorationCause cause, Material target) {
		super(theBlock);
		this.setCause(cause);
		this.target = target;
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

	public DeteriorationCause getCause() {
		return cause;
	}

	public void setCause(DeteriorationCause cause) {
		this.cause = cause;
	}

	public Material getTarget() {
		return target;
	}
}
