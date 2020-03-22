package com.kylantraynor.livelyworld.burn;

import com.kylantraynor.livelyworld.Utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
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
		case GRASS_BLOCK:
			block.setType(Material.DIRT);
			break;
		case DIRT:
			block.setType(Material.COARSE_DIRT);
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
			case OAK_LOG: case DARK_OAK_LOG:
			case ACACIA_LOG: case SPRUCE_LOG:
			case BIRCH_LOG: case JUNGLE_LOG:
			case COAL_BLOCK:
				BukkitRunnable n = new BukkitRunnable() {

					@Override
					public void run() {
						block.setType(Material.COAL_BLOCK, false);
					}

				};
				n.runTaskLater(plugin, 10L);
				break;
			default:
				break;
		}
		if(Utils.isLeaves(block.getType())){
			final Material mat = block.getType();
			BukkitRunnable n1 = new BukkitRunnable() {

				@Override
				public void run() {
					switch(mat){
						case OAK_LEAVES: block.setType(Material.OAK_SAPLING); break;
						case DARK_OAK_LEAVES: block.setType(Material.DARK_OAK_SAPLING); break;
					}
				}

			};
			n1.runTaskLater(plugin, 10L);
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
