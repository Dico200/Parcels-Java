package com.redstoner.parcels.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.redstoner.parcels.generation.ParcelGenerator;
import com.redstoner.utils.Values;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Optional;

public class ParcelWorld {
	
	private ParcelContainer parcels;
	private ParcelGenerator generator;
	private ParcelWorldSettings settings;
	private String name;
	
	public ParcelWorld(String name, ParcelWorldSettings settings) {
		this.parcels = new ParcelContainer(name, settings.axisLimit);
		this.generator = new ParcelGenerator(settings);
		this.settings = settings;
		this.name = name;
	}
	
	public ParcelWorldSettings getSettings() {
		return settings;
	}
	
	public ParcelGenerator getGenerator() {
		return generator;
	}
	
	public String getName() {
		return name;
	}
	
	public Optional<Parcel> getParcelAt(int absX, int absZ) {
		int sectionSize = settings.sectionSize;
		absX -= settings.xOffset + settings.pathOffset;
		absZ -= settings.zOffset + settings.pathOffset;
		int modX = Values.posModulo(absX, sectionSize);
		int modZ = Values.posModulo(absZ, sectionSize);
		if (isOriginParcel(modX, modZ)) {
			int px = (absX - modX) / sectionSize;
			int pz = (absZ - modZ) / sectionSize;
			if (parcels.isWithinBoundaryAt(px, pz))
				return Optional.of(parcels.getParcelAt(px, pz));
		}
		return Optional.empty();
	}
	
	public Optional<Parcel> getParcelAtID(int px, int pz) {
		return parcels.isWithinBoundaryAt(px, pz)? Optional.of(getParcelByID(px, pz)) : Optional.empty();
	}
	
	public Parcel getParcelByID(int px, int pz) {
		return parcels.getParcelAt(px, pz);
	}
	
	private boolean isOriginParcel(int x, int z) {
		return Values.inRange(x, 0, settings.parcelSize) && Values.inRange(z, 0, settings.parcelSize);
	}
	
	public boolean isInParcel(int absX, int absZ, int px, int pz) {
		int sectionSize = settings.sectionSize;
		absX -= settings.xOffset + settings.pathOffset + px*sectionSize;
		absZ -= settings.zOffset + settings.pathOffset + pz*sectionSize;
		int modX = Values.posModulo(absX, sectionSize);
		int modZ = Values.posModulo(absZ, sectionSize);
		return isOriginParcel(modX, modZ);
	}
	
	public void teleport(Player user, Parcel parcel) {
		Coord home = toHomeCoord(parcel);
		user.teleport(new Location(Bukkit.getWorld(name), home.getX(), settings.floorHeight + 1, home.getZ(), -90, 0));
	}
	
	private Coord toHomeCoord(Parcel parcel) {
		Coord NW = toNWCoord(parcel);
		return Coord.of(NW.getX() - 2, NW.getZ() + (settings.parcelSize - 1) / 2);
	}
	
	private Coord toNWCoord(Parcel parcel) {
		return Coord.of(settings.sectionSize * parcel.getX() + settings.pathOffset + settings.xOffset,
						settings.sectionSize * parcel.getZ() + settings.pathOffset + settings.zOffset);
	}
	
	@SuppressWarnings("unused")
	private Parcel fromNWCoord(Coord coord) {
		return parcels.getParcelAt((coord.getX() - settings.pathOffset - settings.xOffset)/settings.sectionSize,
								   (coord.getZ() - settings.pathOffset - settings.zOffset)/settings.sectionSize);
	}
	
	public Parcel[] getOwned(OfflinePlayer user) {
		return parcels.stream().filter(p -> p.isOwner(user)).toArray(size -> new Parcel[size]);
	}
	
	public Optional<Parcel> getNextUnclaimed() {
		return Optional.ofNullable(parcels.nextUnclaimed());
	}
	
	ParcelContainer getParcels() {
		return parcels;
	}
	
	void setParcels(ParcelContainer parcels) {
		if (this.parcels.getAxisLimit() != parcels.getAxisLimit()) {
			this.parcels = ParcelContainer.resize(parcels, name, this.parcels.getAxisLimit());
		} else {
			this.parcels = parcels;
		}
	}

}
