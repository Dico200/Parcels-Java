package com.redstoner.parcels.api;

import java.util.List;
import java.util.Map;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.generation.ParcelGenerator;
import com.redstoner.utils.Bool;
import com.redstoner.utils.Calc;
import com.redstoner.utils.DuoObject;
import com.redstoner.utils.DuoObject.Coord;

public class ParcelWorld {
	
	private ParcelContainer parcels;
	private ParcelGenerator generator;
	private ParcelWorldSettings settings;
	private String world;
	
	public ParcelWorld(String world, Map<String, Integer> settings) {
		this(world, new ParcelWorldSettings(settings), settings.get("parcel-axis-limit"));
	}
	
	public ParcelWorld(String world, ParcelWorldSettings settings, int axisLimit) {
		this.parcels = new ParcelContainer(axisLimit);
		this.generator = new ParcelGenerator(settings);
		this.settings = settings;
	}
	
	public void resize(int axisLimit) {
		DuoObject<ParcelContainer, List<Parcel>> output = ParcelContainer.resize(parcels, axisLimit);
		this.parcels = output.v1();
		output.v2().stream().forEach(parcel -> {
			Coord coord = parcels.nextUnclaimed();
			if (coord == null) {
				ParcelsPlugin.log("Error: resize made it impossible to fit all previous parcels! Parcel removed.");
			} else {
				parcels.setParcelAt(coord.getX(), coord.getZ(), parcel);
			}
		});
	}
	
	public ParcelWorldSettings getSettings() {
		return settings;
	}
	
	public ParcelContainer getParcels() {
		return parcels;
	}
	
	public ParcelGenerator getGenerator() {
		return generator;
	}
	
	public String getWorld() {
		return world;
	}
	
	public Parcel getParcelAt(int absX, int absZ) {
		int sectionSize = settings.sectionSize;
		int parcelSize = settings.parcelSize;
		absX -= settings.xOffset;
		absZ -= settings.zOffset;
		int modX = Calc.posModulo(absX, sectionSize);
		int modZ = Calc.posModulo(absZ, sectionSize);
		if (Bool.inRange(modX, 0, parcelSize) && Bool.inRange(modZ, 0, parcelSize)) {
			return parcels.getParcelAt((absX - modX) / sectionSize, (absZ - modZ) / sectionSize);
		}
		return null;
	}

}
