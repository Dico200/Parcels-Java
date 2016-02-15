package com.redstoner.parcels.api;

import java.util.List;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.generation.ParcelGenerator;
import com.redstoner.utils.Bool;
import com.redstoner.utils.Calc;
import com.redstoner.utils.CastingMap;
import com.redstoner.utils.DuoObject;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Optional;

public class ParcelWorld {
	
	private ParcelContainer parcels;
	private ParcelGenerator generator;
	private ParcelWorldSettings settings;
	private String world;
	
	public ParcelWorld(String world, CastingMap<String, Object> settings) {
		this(world, new ParcelWorldSettings(settings), settings.getCasted("parcel-axis-limit"));
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
	
	public Optional<Parcel> getParcelAt(int absX, int absZ) {
		int sectionSize = settings.sectionSize;
		int parcelSize = settings.parcelSize;
		absX -= settings.xOffset + settings.pathOffset;
		absZ -= settings.zOffset + settings.pathOffset;
		int modX = Calc.posModulo(absX, sectionSize);
		int modZ = Calc.posModulo(absZ, sectionSize);
		if (Bool.inRange(modX, 0, parcelSize) && Bool.inRange(modZ, 0, parcelSize)) {
			int px = (absX - modX) / sectionSize;
			int pz = (absZ - modZ) / sectionSize;
			if (parcels.isWithinBoundaryAt(px, pz))
				return Optional.of(parcels.getParcelAt(px, pz));
		}
		return Optional.empty();
	}
	
	public boolean isInParcel(int absX, int absZ, int px, int pz) {
		int sectionSize = settings.sectionSize;
		int parcelSize = settings.parcelSize;
		absX -= settings.xOffset + settings.pathOffset + px*sectionSize;
		absZ -= settings.zOffset + settings.pathOffset + pz*sectionSize;
		int modX = Calc.posModulo(absX, sectionSize);
		int modZ = Calc.posModulo(absZ, sectionSize);
		return Bool.inRange(modX, 0, parcelSize) && Bool.inRange(modZ, 0, parcelSize);
	}

}
