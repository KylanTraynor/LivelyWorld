package com.kylantraynor.livelyworld.vegetation;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class Tree {
	List<Block> blocks;
	List<Block> leaves;
	List<Block> roots;

	static Tree getAt(Location l) {
		if (isLog(l.getBlock().getType()) || isBranch(l.getBlock().getType())) {

		}
		return null;
	}

	private static boolean isLog(Material type) {
		if (type == Material.LOG)
			return true;
		if (type == Material.LOG_2)
			return true;
		return false;
	}

	private static boolean isBranch(Material type) {
		if (type == Material.FENCE)
			return true;
		if (type == Material.SPRUCE_FENCE)
			return true;
		if (type == Material.ACACIA_FENCE)
			return true;
		if (type == Material.BIRCH_FENCE)
			return true;
		if(type == Material.DARK_OAK_FENCE)
			return true;
		return false;
	}

	private static boolean isLeaf(Material type) {
		if (type == Material.LEAVES)
			return true;
		if (type == Material.LEAVES_2)
			return true;
		return false;
	}

	public Tree() {

	}
}