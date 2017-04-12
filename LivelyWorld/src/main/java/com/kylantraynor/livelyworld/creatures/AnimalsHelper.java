package com.kylantraynor.livelyworld.creatures;

import org.bukkit.Location;
import org.bukkit.entity.Animals;

public interface AnimalsHelper {
	public boolean isInLoveMode(Animals animal);

	public void moveTo(Animals animal, Location location, double speed);
    public void startLoveMode(Animals animal);
}
