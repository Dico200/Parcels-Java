package com.redstoner.parcels.api.blockvisitor;

import com.redstoner.parcels.api.Parcel;
import com.redstoner.utils.DuoObject;
import io.dico.dicore.util.BlockPos;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class VerticalRangePosVisitor extends ParcelVisitor<BlockPos> {

    public VerticalRangePosVisitor(Parcel parcel, int y0, int y1) {
        super(parcel);
        if (y0 == y1) {
            throw new IllegalArgumentException("Invalid range");
        }
        DuoObject.Coord bottom = parcel.getWorld().getBottomCoord(parcel);
        int x0 = bottom.getX();
        int z0 = bottom.getZ();
        int parcelSize = parcel.getWorld().getSettings().parcelSize;
        int count = parcelSize * parcelSize * Math.abs(y1 - y0);
        BlockPos current = new BlockPos(true, parcel.getWorld().getWorld(), 0, 0, 0);

        if (y0 < y1) {
            refresh(new Iterator<BlockPos>() {
                int blockId;

                @Override
                public boolean hasNext() {
                    return blockId < count;
                }

                @Override
                public BlockPos next() {
                    if (this.blockId == count) {
                        throw new NoSuchElementException();
                    }
                    int blockId = this.blockId++;
                    int x = x0 + blockId % parcelSize;
                    blockId /= parcelSize;
                    int z = z0 + blockId % parcelSize;
                    blockId /= parcelSize;
                    int y = y0 + blockId;
                    return current.with(x, y, z);
                }
            });
        } else {
            refresh(new Iterator<BlockPos>() {
                int blockId = count - 1;

                @Override
                public boolean hasNext() {
                    return blockId < count;
                }

                @Override
                public BlockPos next() {
                    if (this.blockId == -1) {
                        throw new NoSuchElementException();
                    }
                    int blockId = this.blockId--;
                    int x = x0 + blockId % parcelSize;
                    blockId /= parcelSize;
                    int z = z0 + blockId % parcelSize;
                    blockId /= parcelSize;
                    int y = y0 + blockId;
                    return current.with(x, y, z);
                }
            });
        }


    }

}
