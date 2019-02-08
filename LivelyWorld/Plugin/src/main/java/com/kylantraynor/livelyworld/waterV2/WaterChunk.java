package com.kylantraynor.livelyworld.waterV2;

import com.kylantraynor.livelyworld.LivelyWorld;
import com.kylantraynor.livelyworld.Utils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockDamageEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WaterChunk {

    public boolean needsRefresh = true;
    public long lastUpdate;
    /**
     * Set the number of cell updates allowed per iteration.
     */
    private static final int updateNumber = 2000;
    /**
     * Set the number of blocks in each dimension.
     */
    static final int yBlocks = 256;
    /**
     * Calculate the length of each dimension of the water array.
     */
    static final int xLength = 32;
    static final int yLength = yBlocks*8;
    static final int zLength = 32;
    /**
     * Calculate the length of each dimension of the obstacles array.
     */
    private static final int xObstaclesLength = 32;
    private static final int yObstaclesLength = yBlocks * 2;
    private static final int zObstaclesLength = 32;
    /**
     * Initiates the arrays.
     */
    final boolean[][][] water;
    final boolean[][][] obstacles;
    final Permeability[][][] permeabilities;
    /**
     * Update lists.
     */
    final Queue<WaterLocation> updateQueue;
    public final Set<BlockLocation> updateSet;
    /**
     * Coordinates of the chunk.
     */
    public final WaterWorld world;
    public final WaterChunkCoords coords;

    /**
     * Constructor.
     * @param coords {@link WaterChunkCoords} of the chunk
     */
    public WaterChunk(WaterWorld world, WaterChunkCoords coords){
        this.world = world;
        this.coords = coords;

        updateQueue = new ConcurrentLinkedQueue<>();
        updateSet = ConcurrentHashMap.newKeySet();

        water = new boolean[xLength][yLength][zLength];
        obstacles = new boolean[xObstaclesLength][yObstaclesLength][zObstaclesLength];
        permeabilities = new Permeability[16][yBlocks][16];
        lastUpdate = System.currentTimeMillis();
    }

    public void refreshChunkBlocks(Chunk c){
        for(int x = 0; x < 16; x++){
            for(int z = 0; z < 16; z++){
                boolean underOcean = false;
                for(int y = yBlocks - 1; y >= 0; y--){
                    BlockData d = c.getBlock(x,y,z).getBlockData();
                    Material m = d.getMaterial();
                    Permeability p = WaterUtils.materialToPermeability(m);
                    boolean[] obstacles = new boolean[8];
                    if(p != null){
                        obstacles = WaterUtils.dataToObstacles(d);
                    }
                    onBlockChange(x,y,z, p, obstacles, false);
                    if(p == Permeability.NONE){
                        underOcean = false;
                    }
                    if(Utils.getWaterHeight(d) == 8 || underOcean) {
                        // Force fill blocks
                        boolean avoidObstacles = p == Permeability.NONE;
                        for(int wy = y << 3; wy < (y + 1) << 3;wy++)
                            for (int wx = x << 1; wx < (x + 1) << 1; wx++)
                                for (int wz = z << 1; wz < (z + 1) << 1; wz++)
                                {
                                    if(!(obstacle(wx,wy,wz) && avoidObstacles)) water[wx][wy][wz] = true;
                                }
                        if(Utils.isOcean(c.getBlock(x,y,z).getBiome())){
                            underOcean = true;
                        }
                    }
                }
            }
        }
        needsRefresh = false;
    }

    /**
     * Process all the elements of the queue.
     * @return the number of elements updated
     */
    public int update(){
        if(needsRefresh) return 0;
        WaterLocation current;
        int i = 0;
        int s = 0;
        int maxS = updateQueue.size() * 2;
        while(updateQueue.peek() != null && i < updateNumber && s < maxS){
            current = updateQueue.poll();
            if(updateCell(current)) {
                updateSet.add(current.toBlockLocation());
                i++;
            }
            s++;
        }
        if(i==0){
            randomUpdate();
        }
        return i;
    }

    public void randomUpdate(){
        int x = Utils.fastRandomInt(xLength);
        int y = Utils.fastRandomInt(yLength);
        int z = Utils.fastRandomInt(zLength);
        requestUpdate(new WaterLocation(x,y,z));
    }

    /**
     * Update the {@link Chunk} to show the flow.
     * @param chunk {@link Chunk} to update
     */
    public void updateBukkitChunk(Chunk chunk){
        for(BlockLocation bl : updateSet){
            Block b = chunk.getBlock(bl.x, bl.y, bl.z);
            if(WaterUtils.isReplaceable(b.getType())){
                Utils.setWaterHeight(b, getBlockLevel(bl), false, false);
            }
        }
        updateSet.clear();
        lastUpdate = System.currentTimeMillis();
    }

    /**
     * Returns the amount of water in the block in the range of [0-32].
     * @param c {@link BlockLocation}
     * @return {@code int} between 0 and 8 included
     */
    public int getBlockWaterAmount(BlockLocation c){
        int count = 0;
        for(int x = (c.x << 1); x < (c.x << 1) + 2; x++){
            for(int z = (c.z << 1); z < (c.z << 1) + 2; z++){
                for(int y = (c.y << 3); y < (c.y << 3) + 8; y++){
                    if(hasWater(x, y, z)) count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns the amount of water in the block in the range of [0-8].
     * @param c {@link BlockLocation}
     * @return {@code int} between 0 and 8 included
     */
    public int getBlockLevel(BlockLocation c){
        return getBlockWaterAmount(c) >> 2;
    }

    /**
     * Process one element of the water array.
     * @param c {@link WaterLocation} of the cell
     * @return  {@code true} if water moved during this update, {@code false} otherwise
     */
    private boolean updateCell(WaterLocation c){
        if(!water[c.x][c.y][c.z]) return false;
        boolean canPotentiallyMoveDown = canPotentiallyMoveTo(c.x, c.y-1, c.z);
        boolean canPotentiallyMoveWest = canPotentiallyMoveTo(c.x-1, c.y, c.z);
        boolean canPotentiallyMoveEast = canPotentiallyMoveTo(c.x+1, c.y, c.z);
        boolean canPotentiallyMoveNorth = canPotentiallyMoveTo(c.x, c.y, c.z-1);
        boolean canPotentiallyMoveSouth = canPotentiallyMoveTo(c.x, c.y, c.z+1);
        boolean canPotentiallyMove = (canPotentiallyMoveDown || canPotentiallyMoveEast || canPotentiallyMoveWest || canPotentiallyMoveNorth || canPotentiallyMoveSouth);
        if(canPotentiallyMove){

            int rdm = Utils.fastRandomInt(4);
            boolean canMoveDown = canMoveTo(c.x, c.y-1, c.z) && !bordersUnloadedChunk(c.x, c.y-1, c.z);
            boolean canMoveWest = canMoveTo(c.x-1, c.y, c.z) && rdm == 0;
            boolean canMoveEast = canMoveTo(c.x+1, c.y, c.z) && rdm == 1;
            boolean canMoveNorth = canMoveTo(c.x, c.y, c.z-1) && rdm == 2;
            boolean canMoveSouth = canMoveTo(c.x, c.y, c.z+1) && rdm == 3;
            if(canMoveDown || canMoveWest || canMoveEast || canMoveNorth ||canMoveSouth){

                if(canMoveDown){
                    water[c.x][c.y-1][c.z] = true;
                    water[c.x][c.y][c.z] = false;
                    updateNeighbours(c);
                    return true;
                } else if(canMoveWest){
                    if(c.x == 0){
                        getRelative(-1,0).setWaterAt(xLength - 1,c.y,c.z, false);
                    } else {
                        setWaterAt(c.x-1,c.y,c.z, false);
                    }
                    removeWaterAt(c.x,c.y,c.z, false);
                    updateNeighbours(c);
                    return true;
                } else if(canMoveEast){
                    if(c.x == xLength - 1){
                        getRelative(1,0).setWaterAt(0,c.y,c.z, false);
                    } else {
                        setWaterAt(c.x+1,c.y,c.z, false);
                    }
                    removeWaterAt(c.x,c.y,c.z, false);
                    updateNeighbours(c);
                    return true;
                } else if(canMoveNorth){
                    if(c.z == 0){
                        getRelative(0,-1).setWaterAt(c.x,c.y,zLength-1, false);
                    } else {
                        setWaterAt(c.x,c.y,c.z-1, false);
                    }
                    removeWaterAt(c.x,c.y,c.z, false);
                    updateNeighbours(c);
                    return true;
                } else if(canMoveSouth){
                    if(c.z == zLength - 1){
                        getRelative(0,1).setWaterAt(c.x,c.y,0, false);
                    } else {
                        setWaterAt(c.x,c.y,c.z+1, false);
                    }
                    removeWaterAt(c.x,c.y,c.z, false);
                    updateNeighbours(c);
                    return true;
                }
            }
            // Did not move, but could potentially do so.
            updateQueue.add(c);
            return false;
        } else {
            // Can't ever move.
            return false;
        }
    }

    /**
     * Check if water can move to the cell at the given water grid location.
     * @param x
     * @param y
     * @param z
     * @return {@code true} if the given location is valid to move to, {@code false} otherwise
     */
    private boolean canMoveTo(int x, int y, int z){
        if(x >= 0 && y >= 0 && z >= 0 && x <= xLength - 1 && y <= yLength - 1 && z <= zLength - 1){
            return (!water[x][y][z] && !(obstacle(x, y, z) && !testPermeability(x,y,z)));
        }
        if(x < 0 && isRelativeLoaded(-1,0)){
            return getRelative(-1,0).canMoveTo(xLength - 1, y,z);
        }
        if(x> xLength - 1 && isRelativeLoaded(1,0)){
            return getRelative(1,0).canMoveTo(0,y,z);
        }
        if(z< 0 && isRelativeLoaded(0,-1)){
            return getRelative(0,-1).canMoveTo(x,y,zLength -1);
        }
        if(z>zLength - 1 && isRelativeLoaded(0, 1)){
            return getRelative(0,1).canMoveTo(x,y,0);
        }
        return false;
    }

    private boolean bordersUnloadedChunk(int x, int y, int z){
        if(x == 0 && !isRelativeLoaded(-1, 0)) return true;
        if(x == xLength - 1 && !isRelativeLoaded(1, 0)) return true;
        if(z == 0 && !isRelativeLoaded(0,-1)) return true;
        if(z == zLength - 1 && !isRelativeLoaded(0, 1)) return true;
        return false;
    }

    /**
     * Check if water can potentially move to the cell at the given water grid location.
     * @param x
     * @param y
     * @param z
     * @return {@code true} if the given location is valid to eventually move to, {@code false} otherwise
     */
    private boolean canPotentiallyMoveTo(int x, int y, int z){
        if(x >= 0 && y >= 0 && z >= 0 && x <= xLength - 1 && y <= yLength - 1 && z <= zLength - 1){
            return (!water[x][y][z] && !(obstacle(x, y, z) && (permeability(x,y,z) == Permeability.NONE)));
        }
        if(x < 0 && isRelativeLoaded(-1,0)){
            return getRelative(-1,0).canPotentiallyMoveTo(xLength - 1, y,z);
        }
        if(x> xLength - 1 && isRelativeLoaded(1,0)){
            return getRelative(1,0).canPotentiallyMoveTo(0,y,z);
        }
        if(z< 0 && isRelativeLoaded(0,-1)){
            return getRelative(0,-1).canPotentiallyMoveTo(x,y,zLength -1);
        }
        if(z>zLength - 1 && isRelativeLoaded(0, 1)){
            return getRelative(0,1).canPotentiallyMoveTo(x,y,0);
        }
        return false;
    }

    /**
     * Check if there is an obstacle at the given water grid location.
     * @param waterX
     * @param waterY
     * @param waterZ
     * @return {@code true} if the given location has an obstacle, {@code false} otherwise
     */
    public boolean obstacle(int waterX, int waterY, int waterZ){
        int ox = waterX;      //Math.floorDiv(waterX, xLength / xObstaclesLength);
        int oy = waterY >> 2; //Math.floorDiv(waterY, yLength / yObstaclesLength);
        int oz = waterZ;      //Math.floorDiv(waterZ, zLength / zObstaclesLength);

        return obstacles[ox][oy][oz];
    }

    /**
     * Get the permeability of the block material at the given location.
     * @param waterX {@code int} location of the water cell
     * @param waterY {@code int} location of the water cell
     * @param waterZ {@code int} location of the water cell
     * @return {@link Permeability} of the block, or {@code null} if it isn't set
     */
    public Permeability permeability(int waterX, int waterY, int waterZ){
        int px = waterX >> 1;
        int py = waterY >> 3;  //Math.floorDiv(waterY, yLength / yBlocks);
        int pz = waterZ >> 1;

        return permeabilities[px][py][pz];
    }

    /**
     * Test the permeability of the block material by using a random value.
     * @param waterX {@code int} location of the water cell
     * @param waterY {@code int} location of the water cell
     * @param waterZ {@code int} location of the water cell
     * @return {@code true} if water can flow into it, {@code false} otherwise
     */
    public boolean testPermeability(int waterX, int waterY, int waterZ){
        Permeability p = permeability(waterX, waterY, waterZ);
        return p == null || Utils.fastRandomFloat() < p.getProbability();
    }

    /**
     * Add all the neighbours of the given location to the update queue.
     * @param c
     */
    private void updateNeighbours(WaterLocation c){
        if(c.x > 0) requestUpdate(new WaterLocation(c.x - 1, c.y, c.z));
        else if(isRelativeLoaded(-1,0)){
            getRelative(-1, 0).requestUpdate(new WaterLocation(WaterChunk.xLength - 1, c.y, c.z));
        }
        if(c.x < xLength - 1) requestUpdate(new WaterLocation(c.x + 1, c.y, c.z));
        else if(isRelativeLoaded(+1, 0)){
            getRelative(+1,0).requestUpdate(new WaterLocation(0, c.y, c.z));
        }
        if(c.z > 0) updateQueue.add(new WaterLocation(c.x, c.y, c.z-1));
        else if(isRelativeLoaded(0,-1)){
            getRelative(0, -1).requestUpdate(new WaterLocation(c.x, c.y, WaterChunk.zLength - 1));
        }
        if(c.z < zLength - 1) updateQueue.add(new WaterLocation(c.x, c.y, c.z+1));
        else if(isRelativeLoaded(0, +1)){
            getRelative(0,+1).requestUpdate(new WaterLocation(c.x, c.y, 0));
        }
        if(c.y > 0) requestUpdate(new WaterLocation(c.x, c.y - 1, c.z));
        if(c.y < yLength - 1) requestUpdate(new WaterLocation(c.x, c.y + 1, c.z));
    }

    /**
     * Add water at the given location and register it for an update.
     * @param location {@link WaterLocation} in the water chunk
     */
    public void setWaterAt(WaterLocation location){
        if(location.x == 0 && !isRelativeLoaded(-1,0)) return;
        if(location.x == xLength-1 && !isRelativeLoaded(1,0)) return;
        if(location.z == 0 && !isRelativeLoaded(0,-1)) return;
        if(location.z == zLength-1 && !isRelativeLoaded(0,1)) return;
        water[location.x][location.y][location.z] = true;
        updateQueue.add(location);
    }

    /**
     * Add water at the given location and register it for an update.
     * @param x {@code int} location
     * @param y {@code int} location
     * @param z {@code int} location
     * @param update {@code boolean} indicating whether it should request an update
     */
    public void setWaterAt(int x, int y, int z, boolean update){
        if(x == 0 && !isRelativeLoaded(-1,0)) return;
        if(x == xLength-1 && !isRelativeLoaded(1,0)) return;
        if(z == 0 && !isRelativeLoaded(0,-1)) return;
        if(z == zLength-1 && !isRelativeLoaded(0,1)) return;
        water[x][y][z] = true;
        if(update) updateQueue.add(new WaterLocation(x,y,z));
    }

    /**
     * Remove water at the given location and register its neighbours for an update.
     * @param location {@link WaterLocation} in the water chunk
     */
    public void removeWaterAt(WaterLocation location){
        if(location.x == 0 && !isRelativeLoaded(-1,0)) return;
        if(location.x == xLength-1 && !isRelativeLoaded(1,0)) return;
        if(location.z == 0 && !isRelativeLoaded(0,-1)) return;
        if(location.z == zLength-1 && !isRelativeLoaded(0,1)) return;
        water[location.x][location.y][location.z] = false;
        updateNeighbours(location);
    }

    /**
     * Remove water at the given location and register its neighbours for an update.
     * @param x {@code int} location
     * @param y {@code int} location
     * @param z {@code int} location
     * @param update {@code boolean} indicating whether it should request an update
     */
    public void removeWaterAt(int x, int y, int z, boolean update){
        if(x == 0 && !isRelativeLoaded(-1,0)) return;
        if(x == xLength-1 && !isRelativeLoaded(1,0)) return;
        if(z == 0 && !isRelativeLoaded(0,-1)) return;
        if(z == zLength-1 && !isRelativeLoaded(0,1)) return;
        water[x][y][z] = false;
        if(update) updateNeighbours(new WaterLocation(x,y,z));
    }

    /**
     * Check if there is water at the given location.
     * @param location {@link WaterLocation}
     * @return {@code true} if there is water, {@code false} otherwise
     */
    public boolean hasWater(WaterLocation location){
        return hasWater(location.x, location.y, location.z);
    }

    /**
     * Check if there is water at the given location.
     * @param x {@link int} coordinate
     * @param y {@link int} coordinate
     * @param z {@link int} coordinate
     * @return {@code true} if there is water, {@code false} otherwise
     */
    public boolean hasWater(int x, int y, int z){
        return water[x][y][z];
    }

    /**
     * Check if the {@link WaterChunk} at the given offset is loaded.
     * @param offsetX {@code int} offset
     * @param offsetZ {@code int} osset
     * @return {@code true} if it is loaded, {@code false} otherwise
     */
    public boolean isRelativeLoaded(int offsetX, int offsetZ){
        return world.isChunkLoaded(coords.x + offsetX, coords.z + offsetZ);
    }

    /**
     * Get the {@link WaterChunk} at the given offset if it is loaded.
     * @param offsetX {@code int} offset
     * @param offsetZ {@code int} offset
     * @return {@link WaterChunk} if it is loaded, {@code null} otherwise
     */
    public WaterChunk getRelative(int offsetX, int offsetZ){
        return world.getChunk(coords.x + offsetX, coords.z + offsetZ);
    }

    /**
     * Request the update of the cell at the given {@link WaterLocation}.
     * @param location {@link WaterLocation} of the cell to update
     */
    public void requestUpdate(WaterLocation location){
        if(water[location.x][location.y][location.z]){
            if(!updateQueue.contains(location)){
                updateQueue.add(location);
            }
        }
    }

    /**
     * Change the permeability at the block's location and updates the obstacles.
     * @param blockX {@code int} location of the block
     * @param blockY {@code int} location of the block
     * @param blockZ {@code int} location of the block
     * @param p new {@link Permeability} of the block
     * @param values {@code boolean array} of obstacle values. Order: z then x then y
     */
    public void onBlockChange(int blockX, int blockY, int blockZ, Permeability p, boolean[] values, boolean update){
        permeabilities[blockX][blockY][blockZ] = p;
        if(p == null){
            removeObstaclesIn(blockX, blockY, blockZ, update);
        } else {
            setObstaclesIn(blockX, blockY, blockZ, values, update);
        }
    }

    private void fillBlock(int blockX, int blockY, int blockZ){
        boolean avoidObstacles = permeabilities[blockX][blockY][blockZ] == Permeability.NONE;
        for(int y = blockY << 3; y < (blockY + 1) << 3;y++)
            for (int x = blockX << 1; x < (blockX + 1) << 1; x++)
                for (int z = blockZ << 1; z < (blockZ + 1) << 1; z++)
                    if(!(obstacle(x,y,z) && avoidObstacles)) setWaterAt(x,y,z, false);
    }

    public int addWaterIn(BlockLocation location, int amount){
        for(int y = location.y << 3; y >= (location.y + 1) << 3;y++){
            for (int x = location.x << 1; x < (location.x + 1) << 1; x++){
                for (int z = location.z << 1; z < (location.z + 1) << 1; z++){
                    if(!water[x][y][z]){
                        setWaterAt(x,y,z,true);
                        if(--amount <= 0) return 0;
                    }
                }
            }
        }
        return amount;
    }

    public int removeWaterIn(BlockLocation location, int amount){
        for(int y = location.y << 3; y >= (location.y + 1) << 3;y++){
            for (int x = location.x << 1; x < (location.x + 1) << 1; x++){
                for (int z = location.z << 1; z < (location.z + 1) << 1; z++){
                    if(water[x][y][z]){
                        removeWaterAt(x,y,z, true);
                        if(--amount <= 0) return 0;
                    }
                }
            }
        }
        return amount;
    }

    private void removeObstaclesIn(int blockX, int blockY, int blockZ, boolean update){
        for(int x = blockX << 1; x < (blockX + 1) << 1;x++)
            for (int z = blockZ << 1; z < (blockZ + 1) << 1; z++)
                for (int y = blockY << 1; y < (blockY + 1) << 1; y++)
                {
                    obstacles[x][y][z] = false;
                    if(update){
                        updateNeighbours(new WaterLocation(x,y*4,z));
                        updateNeighbours(new WaterLocation(x,y*4 + 1,z));
                        updateNeighbours(new WaterLocation(x,y*4 + 2,z));
                        updateNeighbours(new WaterLocation(x,y*4 + 3,z));
                    }
                }
    }

    private void setObstaclesIn(int blockX, int blockY, int blockZ, boolean[] values, boolean update){
        int i = 0;
        for(int y = blockY << 1; y < (blockY + 1) << 1;y++)
            for (int x = blockX << 1; x < (blockX + 1) << 1; x++)
                for (int z = blockZ << 1; z < (blockZ + 1) << 1; z++)
                {
                    obstacles[x][y][z] = values[i++];
                    if(update){
                        updateNeighbours(new WaterLocation(x,y*4,z));
                        updateNeighbours(new WaterLocation(x,y*4 + 1,z));
                        updateNeighbours(new WaterLocation(x,y*4 + 2,z));
                        updateNeighbours(new WaterLocation(x,y*4 + 3,z));
                    }
                }
    }

    public File getFile(){
        File dir1 = new File(LivelyWorld.getInstance().getDataFolder(), "BlockData");
        if(!dir1.exists()){
            dir1.mkdir();
        }
        File dir2 = new File(dir1, world.bukkitWorld.getName());
        if(!dir2.exists()){
            dir2.mkdir();
        }
        File f = new File(dir2, "" + (coords.x >> 5) + "_" + (coords.z >> 5) + ".rbd");
        if(!f.exists()){
            try {
                LivelyWorld.getInstance().getLogger().info("Creating region file " + f.getName() +".");
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return f;
    }
}
