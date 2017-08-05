package com.kylantraynor.livelyworld.climate;

public class WindVector {
	
	public static final WindVector ZERO = new WindVector(0,0,0,0);
	
	private double x = 0;
	private double y = 0;
	private double z = 0;
	private double speed = 0;
	
	public WindVector(double x, double y, double z, double speed){
		this.x = x;
		this.y = y;
		this.z = z;
		this.speed = speed;
	}
	
	public WindVector normalize(){
		double total = x * x + y * y + z * z;
		total = Math.sqrt(total);
		return new WindVector(x / total, y / total, z / total, speed);
	}
	
	@Override
	public String toString(){
		return "" + getX() + "," + getY() + "," + getZ() + "," + getSpeed();
	}
	
	public double getX(){
		return x;
	}
	
	public double getY(){
		return y;
	}
	
	public double getZ(){
		return z;
	}
	
	public double getSpeed(){
		return speed;
	}
	
	public void setX(double x){
		this.x = x;
	}
	
	public void setY(double y){
		this.y = y;
	}
	
	public void setZ(double z){
		this.z = z;
	}
	
	public void setSpeed(double speed){
		this.speed = speed;
	}
	
	public double getRadAngle(){
		if(speed == 0) return Double.NaN;
		return Math.atan2(y, x);
	}
}
