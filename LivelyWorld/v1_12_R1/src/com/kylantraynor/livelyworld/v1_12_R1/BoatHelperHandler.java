package com.kylantraynor.livelyworld.v1_12_R1;

import java.lang.reflect.Field;

import org.bukkit.craftbukkit.v1_12_R1.entity.CraftBoat;
import org.bukkit.entity.Boat;
import org.bukkit.util.Vector;

import com.kylantraynor.livelyworld.api.BoatHelper;

import net.minecraft.server.v1_12_R1.EntityAnimal;
import net.minecraft.server.v1_12_R1.EntityBoat;
import net.minecraft.server.v1_12_R1.EntityBoat.EnumStatus;

public class BoatHelperHandler implements BoatHelper {
	public EntityBoat getEntityBoat(Boat boat){
		return ((CraftBoat) boat).getHandle();
	}

	@Override
	public Vector getMotionVector(Boat boat) {
		EntityBoat b = getEntityBoat(boat);
		Vector v = new Vector(b.motX, b.motY, b.motZ);
		return v;
	}

	@Override
	public void setMotionVector(Boat boat, Vector v) {
		EntityBoat b = getEntityBoat(boat);
		b.motX = v.getX();
		b.motY = v.getY();
		b.motZ = v.getZ();
	}

	@Override
	public boolean isUnderwater(Boat boat) {
		EntityBoat b = getEntityBoat(boat);
		EnumStatus status = null;
		try{
			Class<?> c = EntityBoat.class;
            Field aG = c.getDeclaredField("aG");
            aG.setAccessible(true);
            status = (EnumStatus) aG.get(b);
		} catch (Exception e){
			e.printStackTrace();
		}
		if(status == null) return false;
		return status == EnumStatus.UNDER_WATER || status == EnumStatus.UNDER_FLOWING_WATER;
	}
	
	
}
