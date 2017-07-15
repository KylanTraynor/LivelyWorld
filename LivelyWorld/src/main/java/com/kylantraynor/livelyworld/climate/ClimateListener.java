package com.kylantraynor.livelyworld.climate;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.kylantraynor.livelyworld.LivelyWorld;

public class ClimateListener implements Listener{
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e){
		if(e.isCancelled()) return;
		if(e.getTo().getBlockX() != e.getFrom().getBlockX() || e.getTo().getBlockZ() != e.getFrom().getBlockZ()){
			LivelyWorld.getInstance().getClimateModule().updatePlayerCell(e.getPlayer());
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e){
		LivelyWorld.getInstance().getClimateModule().getPlayerCache().remove(e.getPlayer().getUniqueId());
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e){
		LivelyWorld.getInstance().getClimateModule().updatePlayerCell(e.getPlayer());
	}
}
