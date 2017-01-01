package com.redstoner.parcels.api.schematic;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

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

    public static Collection<Entity> getContainedEntities(World w, int x1, int y1, int z1, int x2, int y2, int z2) {
        return w.getNearbyEntities(new Location(w, average(x1, x2), average(y1, y2), average(z1, z2)),
                absolute(x2 - x1) / 2.0, absolute(y2 - y1) / 2.0, absolute(z2 - z1) / 2.0);
    }

    private static final Comparator<Block> ATTACHABLE;
    public static final Set<Material> ATTACHABLE_MATERIALS;

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
        this.entities = getContainedEntities(world, x1, y1, z1, x2, y2, z2).stream().filter(e -> e.getType() != EntityType.PLAYER).collect(Collectors.toList());
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

        ATTACHABLE_MATERIALS = EnumSet.of(
                Material.ACACIA_DOOR,
                Material.ACTIVATOR_RAIL,
                Material.BIRCH_DOOR,
                Material.BROWN_MUSHROOM,
                Material.CACTUS,
                Material.CAKE_BLOCK,
                Material.CARPET,
                Material.CARROT,
                Material.COCOA,
                Material.CROPS,
                Material.DARK_OAK_DOOR,
                Material.DEAD_BUSH,
                Material.DETECTOR_RAIL,
                Material.DIODE_BLOCK_OFF,
                Material.DIODE_BLOCK_ON,
                Material.DOUBLE_PLANT,
                Material.DRAGON_EGG,
                Material.FIRE,
                Material.FLOWER_POT,
                Material.GOLD_PLATE,
                Material.IRON_DOOR_BLOCK,
                Material.IRON_PLATE,
                Material.IRON_TRAPDOOR,
                Material.JUNGLE_DOOR,
                Material.LADDER,
                Material.LEVER,
                Material.LONG_GRASS,
                Material.MELON_STEM,
                Material.NETHER_WARTS,
                //Material.PISTON_BASE,
                //Material.PISTON_EXTENSION,
                //Material.PISTON_MOVING_PIECE,
                //Material.PISTON_STICKY_BASE,
                Material.PORTAL,
                Material.POTATO,
                Material.POWERED_RAIL,
                Material.PUMPKIN_STEM,
                Material.RAILS,
                Material.REDSTONE_COMPARATOR_OFF,
                Material.REDSTONE_COMPARATOR_ON,
                Material.REDSTONE_TORCH_OFF,
                Material.REDSTONE_TORCH_ON,
                Material.REDSTONE_WIRE,
                Material.RED_MUSHROOM,
                Material.RED_ROSE,
                Material.SAPLING,
                Material.SIGN_POST,
                Material.SNOW,
                Material.SPRUCE_DOOR,
                Material.STANDING_BANNER,
                Material.STONE_BUTTON,
                Material.STONE_PLATE,
                Material.SUGAR_CANE_BLOCK,
                Material.TORCH,
                Material.TRAP_DOOR,
                Material.TRIPWIRE,
                Material.TRIPWIRE_HOOK,
                Material.VINE,
                Material.WALL_BANNER,
                Material.WALL_SIGN,
                Material.WATER_LILY,
                Material.WOOD_BUTTON,
                Material.WOODEN_DOOR, //The item is WOOD_DOOR
                Material.WOOD_PLATE,
                Material.YELLOW_FLOWER
        );

        ATTACHABLE = (b1, b2) -> {
            boolean c1 = ATTACHABLE_MATERIALS.contains(b1.getType());
            return c1 == ATTACHABLE_MATERIALS.contains(b2.getType()) ? 0 : c1 ? 1 : -1;
        };
    }
}

