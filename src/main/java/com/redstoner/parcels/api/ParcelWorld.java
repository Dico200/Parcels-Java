package com.redstoner.parcels.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.redstoner.parcels.api.list.PlayerMap;
import com.redstoner.parcels.api.schematic.ParcelSchematic;
import com.redstoner.parcels.api.schematic.Schematic;
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
				return Optional.ofNullable(parcels.getParcelAt(px, pz));
		}
		return Optional.empty();
	}
	
	public Optional<Parcel> getParcelAt(Location loc) {
		return getParcelAt(loc.getBlockX(), loc.getBlockZ());
	}
	
	public Optional<Parcel> getParcelAt(Block b) {
		return getParcelAt(b.getX(), b.getZ());
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
		ParcelSchematic.load(this, parcel1).swapWith(ParcelSchematic.load(this, parcel2));
		
		UUID owner1 = parcel1.getOwner().orElse(null);
		parcel1.setOwner(parcel2.getOwner().orElse(null));
		parcel2.setOwner(owner1);
		
		PlayerMap<Boolean> added1 = parcel1.getAdded();
		PlayerMap<Boolean> added2 = parcel2.getAdded();
		Map<UUID, Boolean> map1 = new HashMap<>(added1.getMap());
		added1.clear();
		added2.getMap().forEach((player, value) -> added1.add(player, value));
		added2.clear();
		map1.forEach((player, value) -> added2.add(player, value));
		
	}
	
	private Stream<Entity> getEntities(Parcel parcel) {
		World world = getWorld();
		Coord NW = getBottomCoord(parcel);
		int parcelDistance = settings.parcelSize - 1;
		return Schematic.getContainedEntities(world, NW.getX(), 0, NW.getZ(), NW.getX() + parcelDistance, 255, NW.getZ() + parcelDistance);
	}
	
	public void removeEntities(Parcel parcel) {
		getEntities(parcel).filter(entity -> entity.getType() != EntityType.PLAYER).forEach(Entity::remove);
	}

}
