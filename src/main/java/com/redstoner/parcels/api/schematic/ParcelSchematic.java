package com.redstoner.parcels.api.schematic;

import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.utils.DuoObject.Coord;
import org.bukkit.World;

public class ParcelSchematic {

    public static ParcelSchematic load(ParcelWorld world, Parcel parcel) {
        return new ParcelSchematic(world, parcel);
    }

    private final Schematic parcel;

    private ParcelSchematic(ParcelWorld world, Parcel parcel) {
        World w = world.getWorld();
        int parcelDistance = world.getSettings().parcelSize - 1;

        Coord bottom = world.getBottomCoord(parcel);
        this.parcel = new Schematic(w, bottom.getX(), 0, bottom.getZ(), bottom.getX() + parcelDistance, 255, bottom.getZ() + parcelDistance);
    }

    public void swapWith(ParcelSchematic other) {
        parcel.pasteAt(other.parcel.getX0(), other.parcel.getY0(), other.parcel.getZ0(), true);
        other.parcel.pasteAt(parcel.getX0(), parcel.getY0(), parcel.getZ0(), true);
    }

}
