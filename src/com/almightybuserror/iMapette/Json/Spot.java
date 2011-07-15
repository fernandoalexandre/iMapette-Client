package com.almightybuserror.iMapette.Json;

/**
 * Represents a JSon Spot.
 * 
 * @author Fernando Alexandre
 *
 */
public class Spot {

	private String user_id;
	private float latd;
	private float longd;
	private float accuracy;
	private long timestamp;
	private float altitude;
	private float speed;
	
	public Spot() {}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	public float getLatd() {
		return latd;
	}

	public void setLatd(float latd) {
		this.latd = latd;
	}

	public float getLongd() {
		return longd;
	}

	public void setLongd(float longd) {
		this.longd = longd;
	}

	public float getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public float getAltitude() {
		return altitude;
	}

	public void setAltitude(float altitude) {
		this.altitude = altitude;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}
}
