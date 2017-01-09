package com.kylantraynor.livelyworld.gravity;

public class GravityProperties {

	int radius = 1;

	GravityType type = GravityType.BASIC;

	public GravityProperties() {
		this(1);
	}

	public GravityProperties(int radius) {
		this(GravityType.BASIC, radius);
	}

	public GravityProperties(GravityType type, int radius) {
		this.type = type;
		this.radius = radius;
	}

	public GravityType getType() {
		return this.type;
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
		gp.setRadius(0);
		return gp;
	}
}
