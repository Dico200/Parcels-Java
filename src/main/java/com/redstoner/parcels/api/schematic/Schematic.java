package com.redstoner.parcels.api.schematic;

import com.redstoner.parcels.api.blockvisitor.BlockVisitor;
import com.redstoner.parcels.api.schematic.block.BaseBlock;
import io.dico.dicore.util.block.BlockPos;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public final class Schematic {
    public static final Set<Material> attachable;
    private BlockPos origin, size;
    private BaseBlock[] data;
    private long[] attachables;
    private transient int state;
    private transient DataIterator dataIterator = new DataIterator();
    private transient WorldIterator worldIterator = new WorldIterator();

    public Schematic(BlockPos origin, BlockPos size) {
        this.origin = origin.cloneIfMutable().makeImmutable();
        this.size = size.cloneIfMutable().makeImmutable();
        data = new BaseBlock[size.getX() * size.getY() * size.getZ()];
        attachables = new long[(data.length + 63) / 64];
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public BlockPos getSize() {
        return size;
    }

    private boolean isAttachable(int index) {
        return attachables[index & 0x3F] >> (index >> 6) != 0;
    }

    private void setAttachable(int index) {
        attachables[index & 0x3F] |= 1 << (index >> 6);
    }

    public BaseBlock getBlockAt(BlockPos pos) {
        return getBlockAt(pos.getX(), pos.getY(), pos.getZ());
    }

    public BaseBlock getBlockAt(int x, int y, int z) {
        return getRelativeBlock(x - origin.getX(), y - origin.getY(), z - origin.getZ());
    }

    public BaseBlock getRelativeBlock(int x, int y, int z) {
        return data[indexFor(x, y, z)];
    }

    private int indexFor(Block block) {
        return indexFor(block.getX() - origin.getX(), block.getY() - origin.getY(), block.getZ() - origin.getZ());
    }

    private int indexFor(int x, int y, int z) {
        return z + size.getX() * (x + size.getY() * y);
    }

    private void requireState(int state) {
        if (this.state != state) {
            throw new IllegalStateException();
        }
    }

    public void load() {
        requireState(0);
        state = 2;
        worldIterator.reset();
        while (worldIterator.hasNext()) {
            Block block = worldIterator.next();
            int index = worldIterator.index;
            data[index] = BaseBlock.copy(block);
            if (attachable.contains(block.getType())) {
                setAttachable(index);
            }
        }
        state = 1;
    }

    public void load(Runnable onFinish) {
        requireState(0);
        state = 2;
        worldIterator.reset();
        new BlockVisitor<Block>(worldIterator) {
            @Override
            protected boolean process(Block block) {
                int index = worldIterator.index;
                data[index] = BaseBlock.copy(block);
                if (attachable.contains(block.getType())) {
                    setAttachable(index);
                }
                return true;
            }

            @Override
            protected void onFinish(boolean early) {
                state = 1;
                // we don't want to keep like 4096 instances of BaseBlock (in worst case).
                BaseBlock.clearCache();
                onFinish.run();
            }
        }.start();
    }

    public void paste(BlockPos pos) {
        requireState(1);
        paste(pos, false);
        paste(pos, true);
    }

    private void paste(BlockPos pos, boolean attachables) {
        for (int y = 0; y < size.getY(); y++) {
            for (int x = 0; x < size.getX(); x++) {
                for (int z = 0; z < size.getZ(); z++) {
                    int index = indexFor(x, y, z);
                    if (isAttachable(index) == attachables) {
                        data[index].paste(pos.add(x, y, z).getBlock());
                    }
                }
            }
        }
    }

    public void paste(BlockPos pos, Runnable onFinish) {
        pos.makeImmutable();
        paste(pos, false, () -> paste(pos, true, onFinish));
    }

    private void paste(BlockPos pos, boolean attachables, Runnable onFinish) {
        requireState(1);
        state = 2;
        dataIterator.reset();
        new BlockVisitor<BaseBlock>(dataIterator) {
            @Override
            protected boolean process(BaseBlock block) {
                int index = dataIterator.index;
                if (isAttachable(index) == attachables) {
                    int z = index % size.getZ();
                    index /= size.getZ();
                    int x = index % size.getX();
                    index /= size.getX();
                    int y = index;
                    block.paste(pos.add(x, y, z).getBlock());
                }
                return true;
            }

            @Override
            protected void onFinish(boolean early) {
                state = 1;
                onFinish.run();
            }
        }.start();
    }

    private final class DataIterator implements Iterator<BaseBlock> {
        int index = -1;

        void reset() {
            index = -1;
        }

        @Override
        public boolean hasNext() {
            return index < data.length - 1;
        }

        @Override
        public BaseBlock next() {
            try {
                return data[++index];
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new NoSuchElementException();
            }
        }
    }

    private final class WorldIterator implements Iterator<Block> {
        int index = -1;

        void reset() {
            index = -1;
        }

        @Override
        public boolean hasNext() {
            return index < data.length - 1;
        }

        @Override
        public Block next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int index = ++this.index;
            int z = origin.getZ() + index % size.getZ();
            index /= size.getZ();
            int x = origin.getX() + index % size.getX();
            index /= size.getX();
            int y = origin.getY() + index;
            return origin.getWorld().getBlockAt(x, y, z);
        }
    }

    static {
        attachable = EnumSet.of(
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
                Material.PISTON_BASE,
                //Material.PISTON_EXTENSION,
                //Material.PISTON_MOVING_PIECE,
                Material.PISTON_STICKY_BASE,
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
    }

}
