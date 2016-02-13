package com.redstoner.parcels.api;

import java.util.Map;

public class ParcelWorldSettings {
	
	public short wall, floor, fill, pathMain, pathEdge;
	public int parcelSize, pathSize, floorHeight;
	public int sectionSize, xOffset, zOffset;
	
	public ParcelWorldSettings(int wall, int floor, int fill, int pathMain, int pathEdge, int parcelSize, int pathSize, int floorHeight, int offsetX, int offsetZ) {	
		this.wall = (short) wall;
		this.floor = (short) floor;
		this.fill = (short) fill;
		this.pathMain = (short) pathMain;
		this.pathEdge = (short) pathEdge;
		
		this.parcelSize = parcelSize;
		this.pathSize = pathSize;
		this.floorHeight = floorHeight;
		
		this.sectionSize = parcelSize + pathSize;
		int pathOffset = ((pathSize % 2 == 0)? pathSize + 2 : pathSize + 1) / 2;
		this.xOffset = pathOffset + offsetX;
		this.zOffset = pathOffset + offsetZ;
	}
	
	public ParcelWorldSettings(Map<String, Integer> settings) {
		this(
			settings.get("wall-type"), 
			settings.get("floor-type"), 
			settings.get("fill-type"), 
			settings.get("path-main-type"), 
			settings.get("path-edge-type"), 
			settings.get("parcel-size"), 
			settings.get("path-size"), 
			settings.get("floor-height"),
			settings.get("offset-x"),
			settings.get("offset-z")
		);
		 
	}

}
