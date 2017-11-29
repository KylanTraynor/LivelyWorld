package com.kylantraynor.livelyworld.api;

import org.bukkit.entity.Boat;
import org.bukkit.util.Vector;

public interface BoatHelper {
	public Vector getMotionVector(Boat boat);
	
	public void setMotionVector(Boat boat, Vector v);
	
	public boolean isUnderwater(Boat boat);
}
