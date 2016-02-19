package com.redstoner.parcels.api.schematic;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.material.MaterialData;

import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.utils.DuoObject.Coord;

public class ParcelSchematic {
	
	private static final Comparator<Block> ATTACHABLE;

	private final int x, z;
	private final Stream<SchematicBlock> blocks;
	private final Stream<Entity> entities;
	
	public ParcelSchematic(ParcelWorld world, Parcel parcel) {
		Coord NW = world.getBottomCoord(parcel);
		this.x = NW.getX();
		this.z = NW.getZ();
		this.blocks = world.getBlocks(parcel).sorted(ATTACHABLE).map(block -> new SchematicBlock(block));
		this.entities = world.getEntities(parcel).stream().filter(entity -> entity.getType() != EntityType.PLAYER);
	}
	
    public void pasteAt(ParcelWorld pworld, Parcel parcel) {
		World world = pworld.getWorld();
		Coord NW = pworld.getBottomCoord(parcel);
		int relativeX = NW.getX() - x;
		int relativeY = 0;
		int relativeZ = NW.getZ() - z;
		blocks.forEach(schem -> {
		    schem.paste(world, relativeX, relativeY, relativeZ);
		});

		entities.forEach(entity -> {
			Location loc = entity.getLocation();
			entity.teleport(new Location(world, loc.getX() + relativeX, loc.getY(), loc.getZ() + relativeZ, loc.getYaw(), loc.getPitch()));
		});
	}

	static {
		
		ATTACHABLE = new Comparator<Block>() {
			
			private Set<Class<? extends MaterialData>> attachables;
			
			private int priority(Material material) {
				// The higher the priority, the later the block is placed.
				return attachables.contains(material.getData())? 1 : 0;
			}

			@Override
			public int compare(Block b1, Block b2) {
				return priority(b2.getType()) - priority(b1.getType());
			}

			{
				attachables = new HashSet<>();	
				attachables.add(org.bukkit.material.Banner.class);
				attachables.add(org.bukkit.material.Cake.class);
				attachables.add(org.bukkit.material.CocoaPlant.class);
				attachables.add(org.bukkit.material.Crops.class);
				attachables.add(org.bukkit.material.Diode.class);
				attachables.add(org.bukkit.material.Door.class);
				attachables.add(org.bukkit.material.FlowerPot.class);
				attachables.add(org.bukkit.material.LongGrass.class);
				attachables.add(org.bukkit.material.Mushroom.class);
				attachables.add(org.bukkit.material.NetherWarts.class);
				attachables.add(org.bukkit.material.PressurePlate.class);
				attachables.add(org.bukkit.material.Rails.class);
				attachables.add(org.bukkit.material.RedstoneWire.class);
				attachables.add(org.bukkit.material.Sign.class);
				attachables.add(org.bukkit.material.SimpleAttachableMaterialData.class);
				attachables.add(org.bukkit.material.Tripwire.class);
				attachables.add(org.bukkit.material.Vine.class);
			}
		};
		
	}
	
}
