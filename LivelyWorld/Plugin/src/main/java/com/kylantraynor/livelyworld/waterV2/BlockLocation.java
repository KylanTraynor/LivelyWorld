package com.kylantraynor.livelyworld.waterV2;

public class BlockLocation {
    public int x;
    public int y;
    public int z;

    public BlockLocation(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object loc){
        if(!(loc instanceof BlockLocation)) return false;
        BlockLocation location = (BlockLocation) loc;

        return location.x == x && location.y == y && location.z == z;
    }

    @Override
    public int hashCode(){
        return y * 16 * 16 + z * 16 + x;
    }

    public static BlockLocation parse(int code){
        int x = Math.floorMod(code, 16);
        int z = Math.floorMod(code - x, 16 * 16) / 256;
        int y = (code - x - z * 16) / (16 * 16);
        return new BlockLocation(x,y,z);
    }
}
