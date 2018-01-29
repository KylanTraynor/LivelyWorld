package com.kylantraynor.livelyworld.water;

import com.kylantraynor.livelyworld.Utils;

public class WaterData {
	
	final static int resistanceCode = 8;
	final static int solidCode = 28;
	public static final int maxLevel = 0xff;
	
	public WaterData(int data, int pressure, int x, int y, int z) {
		this(Utils.toByteArray(data), pressure, x, y, z);
	}
	
	public WaterData(byte[] data, int pressure, int x, int y, int z) {
		level = data[0];
		resistance = data[1];
		isSolid = ((data[3] & 0x10) >> 4) == 1;
		pressure = 0;
		this.x = (byte) x;
		this.y = (byte) y;
		this.z = (byte) z;
	}
	
	public byte x;
	public byte y;
	public byte z;
	public byte level;
	public int pressure;
	public byte resistance;
	public boolean isSolid;
	public int lastDirection = 0;
	
	public int getX(){
		return Byte.toUnsignedInt(x);
	}
	
	public int getZ(){
		return Byte.toUnsignedInt(z);
	}
	
	public int getY(){
		return Byte.toUnsignedInt(y);
	}
	
	public int getLevel(){
		return Byte.toUnsignedInt(level);
	}
	
	public int getResistance(){
		return Byte.toUnsignedInt(resistance);
	}
	
	public int getSalt(){
		return 0;
	}
	
	public int getMaxQuantity(){
		return 0xFF - getResistance();
	}
	
	public int getMaxQuantityRDM(){
		if(!isSolid) return getMaxQuantity();
		if(getResistance() == 255) return 0;
		return Utils.fastRandomInt(getMaxQuantity() + 1);
	}
	
	public int getData(){
		byte b2 = 0;
		byte b3 = (byte) (isSolid ? 0x10 : 0x00);
		return Utils.toInt(level, resistance, b2, b3);
	}

	public byte[] getByteArray() {
		byte b2 = 0;
		byte b3 = (byte) (isSolid ? 0x10 : 0x00);
		return new byte[] {level, resistance, b2, b3};
	}
}
