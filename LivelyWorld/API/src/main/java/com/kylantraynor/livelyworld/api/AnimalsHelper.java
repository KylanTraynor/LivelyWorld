package com.kylantraynor.livelyworld.api;

import org.bukkit.Location;
import org.bukkit.entity.Animals;

public interface AnimalsHelper {
	public boolean isInLoveMode(Animals animal);

	public void moveTo(Animals animal, Location location, double speed);
	public void moveTowardOthers(Animals animal);
	public void moveAwayFromOthers(Animals animal);
    public void startLoveMode(Animals animal);
}
