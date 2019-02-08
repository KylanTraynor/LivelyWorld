package com.kylantraynor.livelyworld.waterV2;

public enum Permeability{
    NONE(0f, 0b0001),
    LOW(0.0001f, 0b0010),
    MEDIUM(0.001f, 0b0011),
    HIGH(0.01f, 0b0100);

    private final float probability;
    private final int serializedValue;

    Permeability(float probability, int serializedValue){
        this.probability = probability;
        this.serializedValue = serializedValue;
    }

    public float getProbability(){
        return probability;
    }

    public int getSerializedValue(){
        return serializedValue;
    }

    public static Permeability parse(int b){
        for(Permeability p : values()){
            if(b == p.serializedValue) return p;
        }
        return null;
    }
}
