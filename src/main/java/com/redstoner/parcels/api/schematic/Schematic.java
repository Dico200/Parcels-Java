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
		
		Builder<Block> blocks = Stream.builder();
		
		int x, z, y;
		for (x = x0; x <= xmax; x++) {
			for (z = z0; z <= zmax; z++) {
				for (y = y0; y <= ymax; y++) {
					blocks.accept(world.getBlockAt(x, y, z));
				}
			}
		}
		
		this.blocks = blocks.build().sorted(ATTACHABLE).map(SchematicBlock::new).toArray(SchematicBlock[]::new);
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
	
	static {
		
		ATTACHABLE = new Comparator<Block>() {
			
			private final Set<Material> attachables;

			@Override
			public int compare(Block b1, Block b2) {
				boolean c1 = attachables.contains(b1.getType());
				return c1 == attachables.contains(b2.getType()) ? 0 : c1? 1 : -1;
			}
			
			{
				attachables = new HashSet<Material>() {
					private static final long serialVersionUID = 1L;
					{
						add(Material.ACACIA_DOOR);
						add(Material.ACTIVATOR_RAIL);
						add(Material.BIRCH_DOOR);
						add(Material.BROWN_MUSHROOM);
						add(Material.CACTUS);
						add(Material.CAKE_BLOCK);
						add(Material.CARPET);
						add(Material.CARROT);
						add(Material.COCOA);
						add(Material.CROPS);
						add(Material.DARK_OAK_DOOR);
						add(Material.DEAD_BUSH);
						add(Material.DETECTOR_RAIL);
						add(Material.DIODE_BLOCK_OFF);
						add(Material.DIODE_BLOCK_ON);
						add(Material.DOUBLE_PLANT);
						add(Material.DRAGON_EGG);
						add(Material.FIRE);
						add(Material.FLOWER_POT);
						add(Material.GOLD_PLATE);
						add(Material.IRON_DOOR_BLOCK);
						add(Material.IRON_PLATE);
						add(Material.IRON_TRAPDOOR);
						add(Material.JUNGLE_DOOR);
						add(Material.LADDER);
						add(Material.LEVER);
						add(Material.LONG_GRASS);
						add(Material.MELON_STEM);
						add(Material.NETHER_WARTS);
						//add(Material.PISTON_BASE);
						//add(Material.PISTON_EXTENSION);
						//add(Material.PISTON_MOVING_PIECE);
						//add(Material.PISTON_STICKY_BASE);
						add(Material.PORTAL);
						add(Material.POTATO);
						add(Material.POWERED_RAIL);
						add(Material.PUMPKIN_STEM);
						add(Material.RAILS);
						add(Material.REDSTONE_COMPARATOR_OFF);
						add(Material.REDSTONE_COMPARATOR_ON);
						add(Material.REDSTONE_TORCH_OFF);
						add(Material.REDSTONE_TORCH_ON);
						add(Material.REDSTONE_WIRE);
						add(Material.RED_MUSHROOM);
						add(Material.RED_ROSE);
						add(Material.SAPLING);
						add(Material.SIGN_POST);
						add(Material.SNOW);
						add(Material.SPRUCE_DOOR);
						add(Material.STANDING_BANNER);
						add(Material.STONE_BUTTON);
						add(Material.STONE_PLATE);
						add(Material.SUGAR_CANE_BLOCK);
						add(Material.TORCH);
						add(Material.TRAP_DOOR);
						add(Material.TRIPWIRE);
						add(Material.TRIPWIRE_HOOK);
						add(Material.VINE);
						add(Material.WALL_BANNER);
						add(Material.WALL_SIGN);
						add(Material.WATER_LILY);
						add(Material.WOOD_BUTTON);
						add(Material.WOODEN_DOOR); //The item is WOOD_DOOR
						add(Material.WOOD_PLATE);
						add(Material.YELLOW_FLOWER);
					}
				};
			}
		};
	}
}

