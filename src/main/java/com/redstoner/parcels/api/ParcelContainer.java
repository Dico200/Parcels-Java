package com.redstoner.parcels.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.DuoObject;
import com.redstoner.utils.DuoObject.Coord;

class ParcelContainer {
	
	public void print() {
		for (Parcel[] row : parcels) {
			ParcelsPlugin.debug(String.join(" ", (CharSequence[])Arrays.stream(row)
					.map(parcel -> parcel.isClaimed()? "D" : "x")
					.toArray(size -> new String[size])));
		}
	}
	
	protected static DuoObject<ParcelContainer, List<Parcel>> resize(ParcelContainer old, int axisLimit) {
		ParcelContainer container = new ParcelContainer(axisLimit);
		List<Parcel> notTransferred = new ArrayList<>();
		Arrays.stream(old.getAll()).forEach(parcel -> {
			int x = parcel.getX();
			int z = parcel.getZ();
			if (container.isWithinBoundaryAt(x, z)) {
				container.setParcelAt(x, z, parcel);
			} else {
				notTransferred.add(parcel);
			}
		});
		return new DuoObject<ParcelContainer, List<Parcel>>(container, notTransferred);
	}
	
	protected ParcelContainer(int axisLimit) {
		count = axisLimit;
		int total = 2*axisLimit + 1;
		parcels = IntStream.range(0, total)
				.mapToObj(x -> IntStream.range(0, total)
						.mapToObj(z -> new Parcel(x, z))
						.toArray(size -> new Parcel[size])
				).toArray(size -> new Parcel[size][]);
		ParcelsPlugin.debug("Parcelcontainer: ");
		print();
	}
	
	private int count;
	private Parcel[][] parcels;
	
	private <T> T atX(T[] array, int x) {
		return array[count + x];
	}
	
	private <T> void insertX(T[] array, int x, T obj) {
		array[count + x] = obj;
	}
	
	private Parcel[] parcelsAtX(int x) {
		return atX(parcels, x);
	}
	
	private boolean isWithinBoundaryAt(int x) {
		return ((x<0)?-x:x) <= count;
	}
	
	protected Parcel getParcelAt(int x, int z) {
		return atX(parcelsAtX(x), z);
	}
	
	protected void setParcelAt(int x, int z, Parcel parcel) {
		insertX(parcelsAtX(x), z, parcel);
	}
	
	protected boolean isClaimedAt(int x, int z) {
		return getParcelAt(x, z).isClaimed();
	}
	
	protected boolean isWithinBoundaryAt(int x, int z) {
		return isWithinBoundaryAt(x) && isWithinBoundaryAt(z);
	}
	
	protected Parcel[] getAll() {
		List<Parcel> all = new ArrayList<>();
		IntStream.rangeClosed(-count, count).forEach(x -> {
			Parcel[] atX = parcelsAtX(x);
			IntStream.rangeClosed(-count, count).forEach(z -> {
				Parcel atXZ = atX(atX, z);
				if (atXZ != null)
					all.add(atXZ);
			});
		});
		
		return all.stream().toArray(size -> new Parcel[size]);
	}
	
	protected Coord nextUnclaimed() {
		for (int distance = 0; distance <= count; distance++) {
			int inner = distance - 1;
			for (int x = -distance; x < distance + 1; x++) {
				for (int z = -distance; z < distance + 1; z++) {
					if (((x<0)?-x:x) <= inner && ((z<0)?-z:z) <= inner) {
						continue;
					}
					if (!isClaimedAt(x, z)) {
						return new Coord(x, z);
					}
				}
			}
		}
		return null;
	}
	
}