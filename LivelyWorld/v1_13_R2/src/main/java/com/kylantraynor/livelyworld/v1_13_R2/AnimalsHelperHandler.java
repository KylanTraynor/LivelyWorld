package com.kylantraynor.livelyworld.v1_13_R2;

import com.kylantraynor.livelyworld.api.AnimalsHelper;
import net.minecraft.server.v1_13_R2.EntityAnimal;
import net.minecraft.server.v1_13_R2.EntityHuman;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;

public class AnimalsHelperHandler implements AnimalsHelper {
	private EntityAnimal getEntityAnimal(Animals animal) {
        return (EntityAnimal) ((CraftEntity) animal).getHandle();
    }

    @Override
    public boolean isInLoveMode(Animals animal) {
        EntityAnimal entity = getEntityAnimal(animal);
        return entity.isInLove();
    }
    
    public void moveTo(Animals animal, Location location, double speed) {
    	EntityAnimal entity = getEntityAnimal(animal);
    	try{
    		entity.getNavigation().a(location.getX(), location.getY(), location.getZ(), speed);
    	} catch (Exception x){
    		throw new RuntimeException(x.toString());
    	}
    }

    public void startLoveMode(Animals animal) {
        EntityAnimal entity = getEntityAnimal(animal);
        EntityHuman human = null;

        try {
            Class<?> c = EntityAnimal.class;
            Field bx = c.getDeclaredField("bC");
            bx.setAccessible(true);
            bx.setInt(entity, 600);

            Field by = c.getDeclaredField("breedCause");
            by.setAccessible(true);
            by.set(entity, human.getUniqueID());

            entity.breedItem = null;
            entity.world.broadcastEntityEffect(entity, (byte)18);
        } catch (Exception x) {
            throw new RuntimeException(x.toString());
        }
    }

	@Override
	public void moveTowardOthers(Animals animal) {
		double dx = 0;
		double dy = 0;
		double dz = 0;
		for(Entity e : animal.getNearbyEntities(16, 16, 16)){
			if(e instanceof Animals){
				dx += e.getLocation().getX() - animal.getLocation().getX();
				dy += e.getLocation().getY() - animal.getLocation().getY();
				dz += e.getLocation().getZ() - animal.getLocation().getZ();
			}
		}
		Vector herdDir = new Vector(dx, dy, dz);
		herdDir.normalize();
		moveTo(animal, animal.getLocation().clone().add(herdDir), 1);
	}

	@Override
	public void moveAwayFromOthers(Animals animal) {
		double dx = 0;
		double dy = 0;
		double dz = 0;
		for(Entity e : animal.getNearbyEntities(16, 16, 16)){
			if(e instanceof Animals){
				dx += e.getLocation().getX() - animal.getLocation().getX();
				dy += e.getLocation().getY() - animal.getLocation().getY();
				dz += e.getLocation().getZ() - animal.getLocation().getZ();
			}
		}
		Vector herdDir = new Vector(dx, dy, dz);
		herdDir.normalize();
		moveTo(animal, animal.getLocation().clone().add(herdDir.multiply(-1.0)), 1);
	}
}
