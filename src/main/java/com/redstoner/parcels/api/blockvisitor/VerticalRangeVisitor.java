package com.redstoner.parcels.api.blockvisitor;

import com.redstoner.parcels.api.Parcel;
import com.redstoner.utils.DuoObject;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class VerticalRangeVisitor extends ParcelVisitor<Block> {

    public VerticalRangeVisitor(Parcel parcel, int y0, int y1) {
        super(parcel);
        if (y0 == y1) {
            throw new IllegalArgumentException("Invalid range");
        }
        DuoObject.Coord bottom = parcel.getWorld().getBottomCoord(parcel);
        int x0 = bottom.getX();
        int z0 = bottom.getZ();
        int parcelSize = parcel.getWorld().getSettings().parcelSize;
        int count = parcelSize * parcelSize * Math.abs(y1 - y0);
        World world = parcel.getWorld().getWorld();

        Iterator<Block> iterator;
        if (y0 < y1) {
            iterator = new Iterator<Block>() {
                int blockId;

                @Override
                public boolean hasNext() {
                    return blockId < count;
                }

                @Override
                public Block next() {
                    if (this.blockId == count) {
                        throw new NoSuchElementException();
                    }
                    int blockId = this.blockId++;
                    int x = x0 + blockId % parcelSize;
                    blockId /= parcelSize;
                    int z = z0 + blockId % parcelSize;
                    blockId /= parcelSize;
                    int y = y0 + blockId;
                    return world.getBlockAt(x, y, z);
                }
            };
        } else {
            iterator = new Iterator<Block>() {
                int blockId = count - 1;

                @Override
                public boolean hasNext() {
                    return blockId >= 0;
                }

                @Override
                public Block next() {
                    if (this.blockId == -1) {
                        throw new NoSuchElementException();
                    }
                    int blockId = this.blockId--;
                    int x = x0 + blockId % parcelSize;
                    blockId /= parcelSize;
                    int z = z0 + blockId % parcelSize;
                    blockId /= parcelSize;
                    int y = y0 + blockId;
                    return world.getBlockAt(x, y, z);
                }
            };
        }
        refresh(iterator);
    }

}
