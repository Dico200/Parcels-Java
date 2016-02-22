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

import com.redstoner.parcels.ParcelsPlugin;

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
	
	@SuppressWarnings("unused")
	private static final Comparator<Block> ATTACHABLE;
	
	private static final Set<Class<? extends MaterialData>> ATTACHABLES;
	
	private final World world;
	private final int x0, y0, z0;
	private final SchematicBlock[] blocks1, blocks2;
	private final List<Entity> entities;
	
	public Schematic(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
		
		this.world = world;
		
		this.x0 = minimum(x1, x2);
		this.y0 = minimum(y1, y2);
		this.z0 = minimum(z1, z2);
		
		int xmax = x0 + absolute(x2 - x1);
		int ymax = y0 + absolute(y2 - y1);
		int zmax = z0 + absolute(z2 - z1);
		
		Builder<Block> blocks1 = Stream.builder();
		Builder<Block> blocks2 = Stream.builder();
		
		int x, z, y;
		Block block;
		for (x = x0; x <= xmax; x++) {
			for (z = z0; z <= zmax; z++) {
				for (y = y0; y <= ymax; y++) {
					block = world.getBlockAt(x, y, z);
					if (ATTACHABLES.contains(block.getType().getData())) {
						blocks2.accept(block);
					} else {
						blocks1.accept(block);
					}
				}
			}
		}
		
		this.blocks1 = blocks1.build()./*sorted(ATTACHABLE).*/map(SchematicBlock::new).toArray(size -> new SchematicBlock[size]);
		this.blocks2 = blocks2.build().map(SchematicBlock::new).toArray(size -> new SchematicBlock[size]);
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
	
	public void pasteAt(int x, int y, int z, boolean teleportEntities) {
		int dx = x - x0;
		int dy = y - y0;
		int dz = z - z0;
		
		ParcelsPlugin.debug("--Pasting schematic");
		ParcelsPlugin.debug("Pasting unattachable blocks");
		long time1 = System.currentTimeMillis();
		for (SchematicBlock block : blocks1) {
			block.paste(world, dx, dy, dz);
		}
		ParcelsPlugin.debug(String.format("  %.2fs elapsed,", (System.currentTimeMillis() - time1) / 1000.0));
		
		ParcelsPlugin.debug("Pasting attachable blocks");
		long time2 = System.currentTimeMillis();
		for (SchematicBlock block : blocks2) {
			block.paste(world, dx, dy, dz);
		}
		ParcelsPlugin.debug(String.format("  %.2fs elapsed,", (System.currentTimeMillis() - time2) / 1000.0));
		
		if (teleportEntities) {
			long time3 = System.currentTimeMillis();
			ParcelsPlugin.debug("Teleporting entities");
			entities.forEach(e -> {
				Location loc = e.getLocation();
				e.teleport(new Location(world, loc.getX() + dx, loc.getY() + dy, loc.getZ() + dz, loc.getYaw(), loc.getPitch()));
			});
			ParcelsPlugin.debug(String.format("  %.2fs elapsed,", (System.currentTimeMillis() - time3) / 1000.0));
		}
		ParcelsPlugin.debug(String.format("Total time of %.2fs elapsed,", (System.currentTimeMillis() - time1) / 1000.0));
	}
	
	static {
		
		ATTACHABLES = new HashSet<>();	
		ATTACHABLES.add(org.bukkit.material.Banner.class);
		ATTACHABLES.add(org.bukkit.material.Cake.class);
		ATTACHABLES.add(org.bukkit.material.CocoaPlant.class);
		ATTACHABLES.add(org.bukkit.material.Crops.class);
		ATTACHABLES.add(org.bukkit.material.Diode.class);
		ATTACHABLES.add(org.bukkit.material.Door.class);
		ATTACHABLES.add(org.bukkit.material.FlowerPot.class);
		ATTACHABLES.add(org.bukkit.material.LongGrass.class);
		ATTACHABLES.add(org.bukkit.material.Mushroom.class);
		ATTACHABLES.add(org.bukkit.material.NetherWarts.class);
		ATTACHABLES.add(org.bukkit.material.PressurePlate.class);
		ATTACHABLES.add(org.bukkit.material.Rails.class);
		ATTACHABLES.add(org.bukkit.material.RedstoneWire.class);
		ATTACHABLES.add(org.bukkit.material.Sign.class);
		ATTACHABLES.add(org.bukkit.material.SimpleAttachableMaterialData.class);
		ATTACHABLES.add(org.bukkit.material.Tripwire.class);
		ATTACHABLES.add(org.bukkit.material.Vine.class);
		
		ATTACHABLE = new Comparator<Block>() {
			
			private int priority(Material material) {
				// The higher the priority, the later the block is placed.
				return ATTACHABLES.contains(material.getData())? 1 : 0;
			}

			@Override
			public int compare(Block b1, Block b2) {
				return priority(b1.getType()) - priority(b2.getType());
			}

		};
		
	}
}

