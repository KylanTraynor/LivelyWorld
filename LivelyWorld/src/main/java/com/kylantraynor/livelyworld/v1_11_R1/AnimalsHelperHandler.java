package com.kylantraynor.livelyworld.v1_11_R1;

import java.lang.reflect.Field;

import org.bukkit.entity.Animals;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftAnimals;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftEntity;

import com.kylantraynor.livelyworld.creatures.AnimalsHelper;

import net.minecraft.server.v1_11_R1.EntityAnimal;
import net.minecraft.server.v1_11_R1.EntityHuman;

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
}
