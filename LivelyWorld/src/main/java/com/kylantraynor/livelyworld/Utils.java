package com.kylantraynor.livelyworld;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class Utils {
	
	public int addSnow(Block snowBlock, int amount){
		if(snowBlock.getType() == Material.SNOW){
			return 0;
		} else if(snowBlock.getType() == Material.SNOW_BLOCK){
			return 0;
		} else {
			return 0;
		}
	}
	
}
