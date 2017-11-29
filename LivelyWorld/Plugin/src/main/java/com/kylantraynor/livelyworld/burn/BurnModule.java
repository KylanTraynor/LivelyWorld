package com.kylantraynor.livelyworld.burn;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.material.Leaves;
import org.bukkit.scheduler.BukkitRunnable;

import com.kylantraynor.livelyworld.LivelyWorld;

public class BurnModule implements Listener {

	private LivelyWorld plugin;

	public BurnModule(LivelyWorld plugin) {
		this.setPlugin(plugin);
	}

	public void onEnable() {
	}

	public void onDisable() {

	}

	public void onBlockIgnite(BlockIgniteEvent event) {
		if (event.getCause() == IgniteCause.SPREAD
				&& event.getIgnitingBlock() != null) {
			Block igniter = event.getIgnitingBlock();
			if (igniter.getType() == Material.COAL_BLOCK) {
				event.setCancelled(true);
				return;
			}
		}
		Block block = event.getBlock().getRelative(BlockFace.DOWN);
		// plugin.log(Level.INFO, "Block ignited: " +
		// block.getType().toString());;
		switch (block.getType()) {
		case GRASS:
			block.setType(Material.DIRT);
			break;
		case DIRT:
			block.setData((byte) 1);
			break;
		default:
			break;
		}
	}

	public void onBlockBurn(BlockBurnEvent event) {
		Block block = event.getBlock();
		// plugin.log(Level.INFO, "Block burning: " +
		// block.getType().toString());;
		switch (block.getType()) {
		case LOG:
		case LOG_2:
		case COAL_BLOCK:
			BukkitRunnable n = new BukkitRunnable() {

				@Override
				public void run() {
					block.setType(Material.COAL_BLOCK);
				}

			};
			n.runTaskLater(plugin, 10L);
			break;
		case LEAVES:
		case LEAVES_2:
			BukkitRunnable n1 = new BukkitRunnable() {

				@Override
				public void run() {
					BlockState state = block.getState();
					if (state != null && state instanceof Leaves) {
						Leaves b = (Leaves) state.getData();
						block.setType(Material.SAPLING);
						block.setData(b.getSpecies().getData());
					}
				}

			};
			n1.runTaskLater(plugin, 10L);
			break;
		default:
			break;
		}
	}

	public LivelyWorld getPlugin() {
		return plugin;
	}

	public void setPlugin(LivelyWorld plugin) {
		this.plugin = plugin;
	}

	public void onBlockUpdate(Block b, Player p) {
		// TODO Auto-generated method stub
	}
}
