package com.redstoner.parcels.api.schematic;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.material.MaterialData;

public class Schematic {
	
	private static int absolute(int i) {
		return i < 0 ? -i : i;
	}
	
	private static double average(int x, int y) {
		return (x + y) / 2.0;
	}
	
	private static int minimum(int x, int y) {
		return y < x ? y : x;
	}
	
	public static Stream<Entity> getContainedEntities(World w, int x1, int y1, int z1, int x2, int y2, int z2) {
		return w.getNearbyEntities(new Location(w, average(x1, x2), average(y1, y2), average(z1, z2)), 
				absolute(x2 - x1)/2.0, absolute(y2 - y1)/2.0, absolute(z2 - z1)/2.0).stream();	
	}
	
	private static final Comparator<Block> ATTACHABLE;
	
	private final World world;
	private final int x0, y0, z0;
	private final SchematicBlock[] blocks;
	private final List<Entity> entities;
	
	public Schematic(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
		
		this.world = world;
		
		this.x0 = minimum(x1, x2);
		this.y0 = minimum(y1, y2);
		this.z0 = minimum(z1, z2);
		
		int xmax = x0 + absolute(x2 - x1);
		int ymax = y0 + absolute(y2 - y1);
		int zmax = z0 + absolute(z2 - z1);
		
		Builder<Block> stream = Stream.builder();
		
		for (int x = x0; x <= xmax; x++) {
			for (int z = z0; z <= zmax; z++) {
				for (int y = y0; y <= ymax; y++) {
					stream.accept(world.getBlockAt(x, y, z));
				}
			}
		}
		
		this.blocks = stream.build().sorted(ATTACHABLE).map(SchematicBlock::new).toArray(size -> new SchematicBlock[size]);
		this.entities = getContainedEntities(world, x1, y1, z1, x2, y2, z2).filter(e -> e.getType() != EntityType.PLAYER).collect(Collectors.toList());
	}
	
	public int getX0() {
		return x0;
	}

	public int getY0() {
		return y0;
	}

	public int getZ0() {
		return z0;
	}
	
	public void pasteAt(int x, int y, int z, List<Entity> keep, boolean teleportEntities) {
		int dx = x - x0;
		int dy = y - y0;
		int dz = z - z0;
		for (SchematicBlock block : blocks) {
			block.paste(world, dx, dy, dz);
		}
		
		if (teleportEntities) {
			entities.forEach(e -> {
				Location loc = e.getLocation();
				e.teleport(new Location(world, loc.getX() + dx, loc.getY() + dy, loc.getZ() + dz, loc.getYaw(), loc.getPitch()));
			});
		}
	}
	
	public List<Entity> entitiesInOrigin() {
		return entities;
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

