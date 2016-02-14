package com.redstoner.parcels.api;

import com.redstoner.utils.CastingMap;
import com.redstoner.utils.DuoObject.BlockType;

public class ParcelWorldSettings {
	
	public BlockType wall, floor, fill, pathMain, pathEdge;
	public int parcelSize, floorHeight, xOffset, zOffset, sectionSize, pathOffset;
	
	public ParcelWorldSettings(BlockType wall, BlockType floor, BlockType fill, BlockType pathMain, BlockType pathEdge, 
			int parcelSize, int pathSize, int floorHeight, int offsetX, int offsetZ) {	
		this.wall = wall;
		this.floor = floor;
		this.fill = fill;
		this.pathMain = pathMain;
		this.pathEdge = pathEdge;
		
		this.parcelSize = parcelSize;
		this.floorHeight = floorHeight;
		this.xOffset = offsetX;
		this.zOffset = offsetZ;
		
		this.sectionSize = parcelSize + pathSize;
		this.pathOffset = ((pathSize % 2 == 0)? pathSize + 2 : pathSize + 1) / 2;
	}
	
	public ParcelWorldSettings(CastingMap<String, Object> settings) {
		this(
			settings.getCasted("wall-type"), 
			settings.getCasted("floor-type"), 
			settings.getCasted("fill-type"), 
			settings.getCasted("path-main-type"), 
			settings.getCasted("path-edge-type"), 
			settings.getCasted("parcel-size"), 
			settings.getCasted("path-size"), 
			settings.getCasted("floor-height"),
			settings.getCasted("offset-x"),
			settings.getCasted("offset-z")
		);
		 
	}

}
