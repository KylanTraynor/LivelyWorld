package com.kylantraynor.livelyworld.deterioration;

import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.kylantraynor.livelyworld.LivelyWorld;

public class DeteriorationModule {

	private LivelyWorld plugin;

	// Default probability for stonebrick -> Cracked
	public double stonebrick_cracked = 0.002;
	// Default probability for the polished stones -> regular
	public double smoothgranite_granite = 0.002;
	public double smoothandesite_andesite = 0.002;
	public double smoothdiorite_diorite = 0.002;

	private boolean debug;

	public void onEnable(LivelyWorld basePlugin) {
		setPlugin(basePlugin);
		loadConfigValues();
	}

	// Loads data from the config, and write default values
	public void loadConfigValues() {
		// Get configuration File (config.yml)
		YamlConfiguration cfg = (YamlConfiguration) plugin.getConfig();
		// Same, but for stonebrick to cracked probability
		if (cfg.contains("deteriorate.probabilities.stonebrick-cracked")) {
			stonebrick_cracked = cfg
					.getDouble("deteriorate.probabilities.stonebrick-cracked");
		} else {
			plugin.getConfig().set(
					"deteriorate.probabilities.stonebrick-cracked",
					stonebrick_cracked);
		}
		// Same, but for polished to regular probability
		if (cfg.contains("deteriorate.probabilities.smoothgranite-granite")) {
			smoothgranite_granite = cfg
					.getDouble("deteriorate.probabilities.smoothgranite-granite");
		} else {
			plugin.getConfig().set(
					"deteriorate.probabilities.smoothgranite-granite",
					smoothgranite_granite);
		}
		if (cfg.contains("deteriorate.probabilities.smoothandesite-andesite")) {
			smoothandesite_andesite = cfg
					.getDouble("deteriorate.probabilities.smoothandesite-andesite");
		} else {
			plugin.getConfig().set(
					"deteriorate.probabilities.smoothandesite-andesite",
					smoothandesite_andesite);
		}
		if (cfg.contains("deteriorate.probabilities.smoothdiorite-diorite")) {
			smoothdiorite_diorite = cfg
					.getDouble("deteriorate.probabilities.smoothdiorite-diorite");
		} else {
			plugin.getConfig().set(
					"deteriorate.probabilities.smoothdiorite-diorite",
					smoothdiorite_diorite);
		}
		// Actually writes the changes in config.yml
		plugin.saveConfig();
	}

	public void onBlockUpdate(Block b, Player p) {
		if (b.getType() == Material.AIR) {
			if (isConfinedSpace(b))
				trySpawnCobWeb(b);
		} else {

		}
	}

	private void trySpawnCobWeb(Block b) {
		if (debug)
			this.plugin.log(Level.INFO,
					"Trying to spawn Cobweb at " + b.getLocation());
		if (b.getLightFromSky() >= 5)
			return;
		if (Math.random() * 100 <= 25 && b.getType() == Material.AIR) {
			b.setType(Material.WEB);
			if (debug)
				this.plugin.log(Level.INFO, "Succesfully spawned Cobweb.");
		}
	}

	public boolean isConfinedSpace(Block b) {
		int count = 0;
		if (isSolidBlock(b.getRelative(BlockFace.DOWN)))
			count++;
		if (isSolidBlock(b.getRelative(BlockFace.UP)))
			count++;
		if (isSolidBlock(b.getRelative(BlockFace.SOUTH)))
			count++;
		if (isSolidBlock(b.getRelative(BlockFace.NORTH)))
			count++;
		if (isSolidBlock(b.getRelative(BlockFace.EAST)))
			count++;
		if (isSolidBlock(b.getRelative(BlockFace.WEST)))
			count++;
		if (count >= 3) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isSolidBlock(Block b) {
		if (b.getType() == Material.AIR)
			return false;
		if (b.getType() == Material.STONE)
			return true;
		if (b.getType() == Material.BRICK)
			return true;
		if (b.getType() == Material.SMOOTH_BRICK)
			return true;
		if (b.getType() == Material.BOOKSHELF)
			return true;
		if (b.getType() == Material.CAULDRON)
			return true;
		if (b.getType() == Material.CHEST)
			return true;
		if (b.getType() == Material.COBBLESTONE)
			return true;
		if (b.getType() == Material.DOUBLE_STEP)
			return true;
		if (b.getType() == Material.FURNACE)
			return true;
		if (b.getType() == Material.LOG)
			return true;
		if (b.getType() == Material.LOG_2)
			return true;
		if (b.getType() == Material.HARD_CLAY)
			return true;
		if (b.getType() == Material.NETHER_BRICK)
			return true;
		if (b.getType() == Material.PRISMARINE)
			return true;
		if (b.getType() == Material.QUARTZ_BLOCK)
			return true;
		if (b.getType() == Material.SANDSTONE)
			return true;
		if (b.getType() == Material.ANVIL)
			return true;
		if (b.getType() == Material.ARMOR_STAND)
			return true;
		if (b.getType().isSolid())
			return true;
		return false;
	}

	public LivelyWorld getPlugin() {
		return plugin;
	}

	public void setPlugin(LivelyWorld plugin) {
		this.plugin = plugin;
	}

	public void onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (args.length >= 2) {
			switch (args[1].toUpperCase()) {
			case "TOGGLE":
				if (args.length >= 3) {
					switch (args[2].toUpperCase()) {
					case "DEBUG":
						this.debug = !this.debug;
						if (this.debug) {
							sender.sendMessage("Debug is now turned on for the Deterioration Module!");
						} else {
							sender.sendMessage("Debug is now turned off for the Deterioration Module!");
						}
					}
				}
			}
		}
	}
}
