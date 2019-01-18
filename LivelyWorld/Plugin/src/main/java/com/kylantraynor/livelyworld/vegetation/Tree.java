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
		switch(type){
			case OAK_LOG:
			case DARK_OAK_LOG:
			case ACACIA_LOG:
			case SPRUCE_LOG:
			case JUNGLE_LOG:
			case BIRCH_LOG:
				return true;
		}
		return false;
	}

	private static boolean isBranch(Material type) {
		switch(type){
			case OAK_FENCE:
			case SPRUCE_FENCE:
			case ACACIA_FENCE:
			case BIRCH_FENCE:
			case DARK_OAK_FENCE:
			case JUNGLE_FENCE:
				return true;
		}
		return false;
	}

	private static boolean isLeaf(Material type) {
		switch(type){
			case OAK_LEAVES:
			case DARK_OAK_LEAVES:
			case ACACIA_LEAVES:
			case BIRCH_LEAVES:
			case SPRUCE_LEAVES:
			case JUNGLE_LEAVES:
				return true;
		}
		return false;
	}

	public Tree() {

	}
}