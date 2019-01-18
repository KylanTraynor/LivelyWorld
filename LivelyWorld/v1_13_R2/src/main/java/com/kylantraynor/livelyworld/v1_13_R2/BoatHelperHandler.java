package com.kylantraynor.livelyworld.v1_13_R2;

import com.kylantraynor.livelyworld.api.BoatHelper;
import net.minecraft.server.v1_13_R2.EntityBoat;
import net.minecraft.server.v1_13_R2.EntityBoat.EnumStatus;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftBoat;
import org.bukkit.entity.Boat;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;

public class BoatHelperHandler implements BoatHelper {
	public EntityBoat getEntityBoat(Boat boat){
		return ((CraftBoat) boat).getHandle();
	}

	@Override
	public Vector getMotionVector(Boat boat) {
		EntityBoat b = getEntityBoat(boat);
		return new Vector(b.motX, b.motY, b.motZ);
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
