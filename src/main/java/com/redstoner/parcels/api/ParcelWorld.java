package com.redstoner.parcels.api;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.redstoner.parcels.api.schematic.ParcelSchematic;
import com.redstoner.parcels.generation.ParcelGenerator;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Optional;
import com.redstoner.utils.Values;

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
	
	public World getWorld() {
		World result = Bukkit.getWorld(name);
		if (result == null)
			throw new NullPointerException("World name not found in bukkit");
		return result;
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
		Coord home = getHomeCoord(parcel);
		user.teleport(new Location(Bukkit.getWorld(name), home.getX(), settings.floorHeight + 1, home.getZ(), -90, 0));
	}
	
	public Coord getHomeCoord(Parcel parcel) {
		Coord NW = getBottomCoord(parcel);
		return Coord.of(NW.getX() - 2, NW.getZ() + (settings.parcelSize - 1) / 2);
	}
	
	public Coord getBottomCoord(Parcel parcel) {
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
	
	public void reset(Parcel parcel) {
		parcel.setOwner(null);
		parcel.getAdded().clear();
		clearBlocks(parcel);
		removeEntities(parcel);
	}
	
	public Stream<Block> getBlocks(Parcel parcel) {
		Builder<Block> builder = Stream.builder();
		
		World world = getWorld();
		
		Coord NW = getBottomCoord(parcel);
		int x0 = NW.getX();
		int z0 = NW.getZ();
		
		int x, z, y;
		for (x = x0; x < x0 + settings.parcelSize; x++) {
			for (z = z0; z < z0 + settings.parcelSize; z++) {
				for (y = 0; y < 256; y++) {
					builder.accept(world.getBlockAt(x, y, z));
				}
			}
		}
		
		return builder.build();
	}
	
	@SuppressWarnings("deprecation")
	public void clearBlocks(Parcel parcel) {
		
		short fillId = settings.fill.getId();
		byte fillData = settings.fill.getData();
		short floorId = settings.floor.getId();
		byte floorData = settings.floor.getData();
		int floorHeight = settings.floorHeight;
		
		getBlocks(parcel).forEach(block -> {
			int y = block.getY();
			if (y < floorHeight) {
				block.setTypeId(fillId);
				block.setData(fillData);
			} else if (y == floorHeight) {
				block.setTypeId(floorId);
				block.setData(floorData);
			} else if (y > floorHeight) {
				block.setTypeId(0);
				block.setData((byte)0);
			}
		});
	}
	
	public void swap(Parcel parcel1, Parcel parcel2) {
		ParcelSchematic schem1 = new ParcelSchematic(this, parcel1);
		ParcelSchematic schem2 = new ParcelSchematic(this, parcel2);
		schem1.pasteAt(this, parcel2);
		schem2.pasteAt(this, parcel1);
	}
	
	/*
	public PyGenerator<Block> getBlocksGenerator(Parcel parcel) {
		
		return new PyGenerator<Block>() {

			@Override
			protected void run() throws InterruptedException {
				World world = Bukkit.getWorld(name);
				if (world == null)
					return;
				
				Coord NW = getNWCoord(parcel);
				int x0 = NW.getX();
				int z0 = NW.getZ();
				
				int x, z, y;
				for (x = x0; x < x0 + settings.parcelSize; x++) {
					for (z = z0; z < z0 + settings.parcelSize; z++) {
						for (y = 0; y < 256; y++) {
							yield(world.getBlockAt(x, y, z));
						}
					}
				}
			}
		};
	}
	*/
	
	public List<Entity> getEntities(Parcel parcel) {
		World world = getWorld();
		Coord NW = getBottomCoord(parcel);
		int halfParcel = settings.parcelSize / 2; //floored	
		ArmorStand stand = (ArmorStand) world.spawnEntity(new Location(world, NW.getX() + halfParcel, 128, NW.getZ() + halfParcel), EntityType.ARMOR_STAND);
		List<Entity> entities = stand.getNearbyEntities(halfParcel, 128, halfParcel);
		stand.remove();
		return entities;
	}
	
	public void removeEntities(Parcel parcel) {
		getEntities(parcel).stream().filter(entity -> entity.getType() != EntityType.PLAYER).forEach(Entity::remove);
	}

}
