package com.kylantraynor.livelyworld.waterV2;

public class WaterLocation {
    public int x;
    public int y;
    public int z;

    public WaterLocation(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object loc){
        if(!(loc instanceof WaterLocation)) return false;
        WaterLocation location = (WaterLocation) loc;

        return location.x == x && location.y == y && location.z == z;
    }

    @Override
    public int hashCode(){
        return y * WaterChunk.xLength * WaterChunk.zLength + z * WaterChunk.xLength + x;
    }

    public BlockLocation toBlockLocation(){
        int bX = x >> 1;
        int bZ = z >> 1;
        int bY = y >> 3;
        return new BlockLocation(bX, bY, bZ);
    }

    public static WaterLocation parse(int code){
        int x = Math.floorMod(code, WaterChunk.xLength);
        int z = Math.floorMod(code - x, WaterChunk.xLength * WaterChunk.zLength) / WaterChunk.xLength;
        int y = (code - x - z * WaterChunk.xLength) / (WaterChunk.xLength * WaterChunk.zLength);
        return new WaterLocation(x,y,z);
    }
}
