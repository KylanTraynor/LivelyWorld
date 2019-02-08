package com.kylantraynor.livelyworld.waterV2;

public class WaterChunkCoords {
    public int x;
    public int z;

    public WaterChunkCoords(int x, int z){
        this.x = x;
        this.z = z;
    }

    /**
     * Check if this {@link WaterChunkCoords) and the given {@link Object) are the same.
     * @param o {@link Object} to test equality with
     * @return {@code true} if the two objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o){
        if(!(o instanceof WaterChunkCoords)) return false;
        WaterChunkCoords c = (WaterChunkCoords)o;

        return c.x == x && c.z == z;
    }
    /**
     * Get the hash code.
     * @return {@int} hash code
     */
    @Override
    public int hashCode(){
        return x * 31 + z;
    }
}
