package com.kylantraynor.livelyworld.gravity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GravityTask extends BukkitRunnable {

	private GravityModule module;
	private World world;
	private int x;
	private int y;
	private int z;

	public GravityTask(GravityModule module, World w, int x, int y, int z) {
		this.module = module;
		this.world = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public void run() {
		doGravityCheck();
	}

	public Block getBlock() {
		return world.getBlockAt(x, y, z);
	}
	
	private Vector hasAirAround(){
		int rdm = (int) Math.floor(Math.random() * 4);
		switch(rdm){
		case 0:
			if(!module.isSolidBlock(world.getBlockAt(x + 1, y, z)))
				if(!module.isSolidBlock(world.getBlockAt(x + 1, y - 1, z))) return new Vector(1, 0, 0);
			if(!module.isSolidBlock(world.getBlockAt(x - 1, y, z)))
				if(!module.isSolidBlock(world.getBlockAt(x - 1, y - 1, z))) return new Vector(-1, 0, 0);
			if(!module.isSolidBlock(world.getBlockAt(x, y, z + 1)))
				if(!module.isSolidBlock(world.getBlockAt(x, y - 1, z + 1))) return new Vector(0, 0, 1);
			if(!module.isSolidBlock(world.getBlockAt(x, y, z - 1)))
				if(!module.isSolidBlock(world.getBlockAt(x, y - 1, z - 1))) return new Vector(0, 0, -1);
			break;
		case 1:
			if(!module.isSolidBlock(world.getBlockAt(x - 1, y, z)))
				if(!module.isSolidBlock(world.getBlockAt(x - 1, y - 1, z))) return new Vector(-1, 0, 0);
			if(!module.isSolidBlock(world.getBlockAt(x + 1, y, z)))
				if(!module.isSolidBlock(world.getBlockAt(x + 1, y - 1, z))) return new Vector(1, 0, 0);
			if(!module.isSolidBlock(world.getBlockAt(x, y, z - 1)))
				if(!module.isSolidBlock(world.getBlockAt(x, y - 1, z - 1))) return new Vector(0, 0, -1);
			if(!module.isSolidBlock(world.getBlockAt(x, y, z + 1)))
				if(!module.isSolidBlock(world.getBlockAt(x, y - 1, z + 1))) return new Vector(0, 0, 1);
			break;
		case 2:
			if(!module.isSolidBlock(world.getBlockAt(x, y, z + 1)))
				if(!module.isSolidBlock(world.getBlockAt(x, y - 1, z + 1))) return new Vector(0, 0, 1);
			if(!module.isSolidBlock(world.getBlockAt(x, y, z - 1)))
				if(!module.isSolidBlock(world.getBlockAt(x, y - 1, z - 1))) return new Vector(0, 0, -1);
			if(!module.isSolidBlock(world.getBlockAt(x + 1, y, z)))
				if(!module.isSolidBlock(world.getBlockAt(x + 1, y - 1, z))) return new Vector(1, 0, 0);
			if(!module.isSolidBlock(world.getBlockAt(x - 1, y, z)))
				if(!module.isSolidBlock(world.getBlockAt(x - 1, y - 1, z))) return new Vector(-1, 0, 0);
			break;
		case 3:
			if(!module.isSolidBlock(world.getBlockAt(x, y, z - 1)))
				if(!module.isSolidBlock(world.getBlockAt(x, y - 1, z - 1))) return new Vector(0, 0, -1);
			if(!module.isSolidBlock(world.getBlockAt(x, y, z + 1)))
				if(!module.isSolidBlock(world.getBlockAt(x, y - 1, z + 1))) return new Vector(0, 0, 1);
			if(!module.isSolidBlock(world.getBlockAt(x - 1, y, z)))
				if(!module.isSolidBlock(world.getBlockAt(x - 1, y - 1, z))) return new Vector(-1, 0, 0);
			if(!module.isSolidBlock(world.getBlockAt(x + 1, y, z)))
				if(!module.isSolidBlock(world.getBlockAt(x + 1, y - 1, z))) return new Vector(1, 0, 0);
			break;
		}
		/*
		for(int x = -1; x <= 1; x++){
			for(int z = -1; z <= 1; z++){
				if(!module.isSolidBlock(world.getBlockAt(this.x + x, y, this.z + z))){
					if(!module.isSolidBlock(world.getBlockAt(this.x  + x, y - 1 , this.z + z)))
						return new Vector(x, 0, z);
				}
			}
		}*/
		return null;
	}

	private void doGravityCheck() {
		if (!getBlock().getChunk().isLoaded())
			return;
		boolean hasAirBelow = !module.isSolidBlock(world
				.getBlockAt(x, y - 1, z));
		if (!hasAirBelow) {
			if (Math.random() < 0.5) {
				GravityTask task = new GravityTask(module, world, x, y - 1, z);
				task.runTaskLater(module.getPlugin(), 10);
			}
			Vector airDirection = hasAirAround();
			if(airDirection != null){
				GravityProperties gp = module.getBlockProperties(getBlock());
				if(gp != null){
					if(gp.getType() == GravityType.SANDLIKE){
						spawnFallingBlock(airDirection);
					}
				}
				return;
			} else {
				return;
			}
		}
		GravityProperties gp = module.getBlockProperties(getBlock());
		if (gp != null) {
			switch (gp.getType()) {
			case SANDLIKE:
				break;
			case BASIC:
				if (hasSupport(world, x, y, z, gp.getRadius(), true)) {
					return;
				}
				break;
			}
		} else {
			switch (getBlock().getType()) {
			case CLAY:
			case SNOW_BLOCK:
				module.setBlockProperties(getBlock().getType(),
						GravityProperties.sandlike());
				break;
			case GRASS:
			case DIRT:
			case GRASS_PATH:
				module.setBlockProperties(getBlock().getType(),
						new GravityProperties(1));
				if (hasSupport(world, x, y, z, 1, true)) {
					return;
				}
				break;
			case COBBLESTONE:
			case COBBLESTONE_STAIRS:
				module.setBlockProperties(getBlock().getType(),
						new GravityProperties(1));
				if (hasSupport(world, x, y, z, 1, true)) {
					return;
				}
				break;
			case SMOOTH_BRICK:
				if (getBlock().getData() < 2) {
					if (hasSupport(world, x, y, z, 5, true)) {
						if (!hasSupport(world, x, y, z, 4, false)) {
							if (Math.random() < 0.1) {
								getBlock().setData((byte) 2);
							}
						}
						return;
					}
				} else {
					if (hasSupport(world, x, y, z, 4, true)) {
						return;
					}
				}
				getBlock().setData((byte) 2);
				break;
			case SMOOTH_STAIRS:
				module.setBlockProperties(getBlock().getType(),
						new GravityProperties(4));
				if (hasSupport(world, x, y, z, 4, true)) {
					return;
				}
				break;
			case BRICK:
			case BRICK_STAIRS:
				module.setBlockProperties(getBlock().getType(),
						new GravityProperties(4));
				if (hasSupport(world, x, y, z, 4, true)) {
					return;
				}
				break;
			default:
				return;
			}
		}

		if(spawnFallingBlock(null)){
			for (int x = this.x - 1; x <= this.x + 1; x++) {
				for (int y = this.y; y <= this.y + 1; y++) {
					for (int z = this.z - 1; z <= this.z + 1; z++) {
						if (Math.random() < 0.5) {
							GravityTask task = new GravityTask(module, world,
									x, y, z);
							task.runTaskLater(module.getPlugin(), 10);
						}
					}
				}
			}
		}

	}
	
	@SuppressWarnings("deprecation")
	public boolean spawnFallingBlock(Vector velocity){
		if (getBlock().getChunk().getEntities().length < Bukkit.getServer()
				.getAmbientSpawnLimit()) {
			if (module.isDebug())
				Bukkit.getServer().getLogger().info("Spawning falling block.");
			if(velocity != null){
				FallingBlock fb = world.spawnFallingBlock(getBlock().getLocation().add(0.5, 0.5, 0.5).add(velocity),
						getBlock().getType(), getBlock().getData());
				//fb.setVelocity(velocity);
			} else {
				FallingBlock fb = world.spawnFallingBlock(getBlock().getLocation().add(0.5,0.5,0.5),
						getBlock().getType(), getBlock().getData());
			}
			getBlock().setType(Material.AIR);
			world.playSound(getBlock().getLocation(), Sound.BLOCK_GRAVEL_FALL,
					1, 1);
			
			return true;
		}
		return false;
	}

	public boolean hasSupport(World w, int baseX, int y, int baseZ, int range,
			boolean spread) {
		if (range <= 0)
			return true;
		if (y <= 1)
			return true;

		for (int x = baseX - 1; x <= baseX + 1; x++) {
			for (int z = baseZ - 1; z <= baseZ + 1; z++) {
				if (x == baseX && z == baseZ)
					continue;
				if (module.isSolidBlock(w.getBlockAt(x, y - 1, z))) {
					if (spread) {
						if (Math.random() < 0.5) {
							GravityTask task = new GravityTask(module, w, x,
									y - 1, z);
							task.runTaskLater(module.getPlugin(), 10);
						}
					}
					return true;
				} else if (module.isSolidBlock(w.getBlockAt(x, y, z))) {
					if (isSupportBeam(w.getBlockAt(x, y, z))) {
						return true;
					} else {
						if (range > 1) {
							if (hasSupport(w, x, y, z, range - 1, spread))
								return true;
						}
					}
				}
			}
		}

		return false;
	}

	public boolean isSupportBeam(Block b) {
		if (b.getType() == Material.LOG)
			return true;
		if (b.getType() == Material.LOG_2)
			return true;
		if (b.getType() == Material.IRON_BLOCK)
			return true;
		return false;
	}
	/*
	 * public boolean hasSupport2(Location l, int range, boolean spread){
	 * Location current = l.clone(); int y = l.getBlockY() - 1; if (y <= 0) {
	 * return true; } current.setY(y); for (int x = l.getBlockX() - range; x <=
	 * l.getBlockX() + range; x++) { for (int z = l.getBlockZ() - range; z <=
	 * l.getBlockZ() + range; z++) { current.setX(x); current.setZ(z); if
	 * (module.isSolidBlock(current.getBlock())) { if(spread){ if(Math.random()
	 * < 0.5){ GravityTask task = new GravityTask(module, world,
	 * current.getBlockX(), current.getBlockY(), current.getBlockZ());
	 * task.runTaskLater(module.getPlugin(), 10); } } return true; } } } return
	 * false; }
	 */
}
