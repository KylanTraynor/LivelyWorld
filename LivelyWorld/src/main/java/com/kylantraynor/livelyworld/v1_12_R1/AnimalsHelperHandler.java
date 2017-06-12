package com.kylantraynor.livelyworld.v1_12_R1;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftAnimals;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;

import com.kylantraynor.livelyworld.creatures.AnimalsHelper;

import net.minecraft.server.v1_12_R1.EntityAnimal;
import net.minecraft.server.v1_12_R1.EntityHuman;

public class AnimalsHelperHandler implements AnimalsHelper {
	private EntityAnimal getEntityAnimal(Animals animal) {
        EntityAnimal entity = (EntityAnimal) ((CraftEntity) ((CraftAnimals) animal)).getHandle();
        return entity;
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

            Field bx = c.getDeclaredField("bw");
            bx.setAccessible(true);
            bx.setInt(entity, 600);

            Field by = c.getDeclaredField("bx");
            by.setAccessible(true);
            by.set(entity, human);

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
