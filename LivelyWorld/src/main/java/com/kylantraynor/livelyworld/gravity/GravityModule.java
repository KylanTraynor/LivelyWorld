package com.kylantraynor.livelyworld.gravity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.kylantraynor.livelyworld.LivelyWorld;

public class GravityModule {
  private LivelyWorld plugin;
  private boolean debug;
  final Map<Material, GravityProperties> blockProperties = new HashMap<Material, GravityProperties>();
  
  public GravityModule(LivelyWorld livelyWorld) {
    this.setPlugin(livelyWorld);
  }
  
  public void enable() {}
  
  public void onBlockUpdate(Block b, Player p) {
	  GravityTask t = new GravityTask(this, b.getWorld(), b.getX(), b.getY(), b.getZ());
	  t.runTaskLater(plugin, 1);
  }
  
  public void reloadProperties(FileConfiguration fileConfiguration){
	  blockProperties.clear();
	  ConfigurationSection cs = fileConfiguration.getConfigurationSection("gravity.blocks");
	  if(cs != null){
		  for(String s : cs.getKeys(false)){
			  GravityProperties gp = new GravityProperties();
			  gp.setType(GravityType.valueOf(fileConfiguration.getString("gravity.blocks."+s+".type")));
			  gp.setRadius(fileConfiguration.getInt("gravity.blocks."+s+".radius"));
			  blockProperties.put(Material.getMaterial(s), gp);
		  }
	  }
  }
  
  public FileConfiguration setProperties(FileConfiguration fileConfiguration){
	  fileConfiguration.set("gravity.blocks", null);
	  for(Entry<Material, GravityProperties> e : blockProperties.entrySet()){
		  fileConfiguration.set("gravity.blocks." + e.getKey().toString() + ".type", e.getValue().getType().toString());
		  fileConfiguration.set("gravity.blocks." + e.getKey().toString() + ".radius", e.getValue().getRadius());
	  }
	  return fileConfiguration;
  }
  
  public void onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if(args.length >= 2){
			switch(args[1].toUpperCase()){
			case "TOGGLE":
				if(args.length >= 3){
					switch(args[2].toUpperCase()){
					case "DEBUG":
						this.setDebug(!this.isDebug());
						if(this.isDebug()){
							sender.sendMessage("Debug is now turned on for the Gravity Module!");
						} else {
							sender.sendMessage("Debug is now turned off for the Gravity Module!");
						}
					}
				}
				break;
			case "SET":
				if(args.length >= 5 && sender.hasPermission("livelyworld.gravity.admin")){
					Material m = Material.getMaterial(args[2].toUpperCase());
					if(m != null){
						if(args[3].equalsIgnoreCase("BASIC")){
							int r = Integer.parseInt(args[4]);
							GravityProperties gp = new GravityProperties(r);
							blockProperties.put(m, gp);
							sender.sendMessage("Blocks of type " + m.toString() + " now have " + gp.getType().toString() + " gravity with " + gp.getRadius() + " radius of support.");
							return;
						} else if(args[3].equalsIgnoreCase("SANDLIKE")){
							blockProperties.put(m, GravityProperties.sandlike());
							sender.sendMessage("Blocks of type " + m.toString() + " now have SANDLIKE gravity.");
							return;
						}
					}
				}
				sender.sendMessage("/lw gravity set <Material> <GravityType> <SupportRadius>");
				break;
			}
		}
	}

  public boolean isSolidBlock(Block b){
	  if(b.getType() == Material.AIR) return false;
	  if(b.getType() == Material.TORCH) return false;
	  if(b.getType() == Material.LADDER) return false;
	  if(b.isLiquid()) return false;
	  return true;
  }
  
  
public boolean isDebug() {
	return debug;
}

public void setDebug(boolean debug) {
	this.debug = debug;
}

public LivelyWorld getPlugin() {
	return plugin;
}

public void setPlugin(LivelyWorld plugin) {
	this.plugin = plugin;
}

public GravityProperties getBlockProperties(Block block) {
	return blockProperties.get(block.getType());
}


public void setBlockProperties(Material type, GravityProperties properties) {
	blockProperties.put(type, properties);
}
}
