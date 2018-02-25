package com.kylantraynor.livelyworld.gravity;

public class GravityProperties {

	int radius = 1;
	int stability = 1;

	GravityType type = GravityType.BASIC;

	public GravityProperties() {
		this(1, 1);
	}

	public GravityProperties(int stability, int radius) {
		this(GravityType.BASIC, stability, radius);
	}

	public GravityProperties(GravityType type, int stability, int radius) {
		this.type = type;
		this.stability = stability;
		this.radius = radius;
	}

	public GravityType getType() {
		return this.type;
	}

	public int getStability(){
		return this.stability;
	}
	
	public void setStability(int stability){
		this.stability = stability;
	}
	
	public int getRadius() {
		return this.radius;
	}

	public void setType(GravityType type) {
		this.type = type;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public static GravityProperties sandlike() {
		GravityProperties gp = new GravityProperties();
		gp.setType(GravityType.SANDLIKE);
		gp.setStability(1);
		gp.setRadius(0);
		return gp;
	}
}
