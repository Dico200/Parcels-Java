package com.redstoner.parcels.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import com.redstoner.parcels.ParcelsPlugin;

class ParcelContainer implements Serializable {
	private static final long serialVersionUID = -2960464067977890792L;

	static ParcelContainer resize(ParcelContainer container, String world, int newLimit) {
		
		ParcelContainer result = new ParcelContainer(world, newLimit);
		container.stream().forEach(parcel -> {
			int x = parcel.getX();
			int z = parcel.getZ();
			if (result.isWithinBoundaryAt(x, z))
					result.setParcelAt(x, z, parcel);
			else
				ParcelsPlugin.log( "WARN: " + parcel.toString() + " could not be fitted in the new size");
		});
		
		return result;
		
	}
	
	public void print() {
		for (int i = parcels.length - 1; i >= 0; i--) {
			Parcel[] row = parcels[i];
			ParcelsPlugin.debug(String.join(" ", (CharSequence[])Arrays.stream(row)
					.map(parcel -> parcel.isClaimed()? "D" : "x")
					.toArray(size -> new String[size])));
		}
	}
	
	protected ParcelContainer(String world, int axisLimit) {
		count = axisLimit;
		parcels = IntStream.rangeClosed(-axisLimit, axisLimit)
				.mapToObj(x -> IntStream.rangeClosed(-axisLimit, axisLimit)
						.mapToObj(z -> new Parcel(world, x, z))
						.toArray(size -> new Parcel[size])
				).toArray(size -> new Parcel[size][]);
	}
	
	final private int count;
	private Parcel[][] parcels;
	
	private <T> T atX(T[] array, int x) {
		return array[count + x];
	}
	
	private boolean isWithinBoundaryAt(int x) {
		return ((x<0)?-x:x) <= count;
	}
	
	protected Parcel getParcelAt(int x, int z) {
		return isWithinBoundaryAt(x, z)? atX(atX(parcels, x), z) : null;
	}
	
	protected boolean isClaimedAt(int x, int z) {
		return isWithinBoundaryAt(x, z)? getParcelAt(x, z).isClaimed() : false;
	}
	
	protected boolean isWithinBoundaryAt(int x, int z) {
		return isWithinBoundaryAt(x) && isWithinBoundaryAt(z);
	}
	
	protected Parcel[] getAll() {
		return stream().toArray(size -> new Parcel[size]);
	}
	
	protected Stream<Parcel> stream() {
		Builder<Parcel> builder = Stream.builder();
		for (Parcel[] row : parcels)
			for (Parcel parcel : row)
				builder.accept(parcel);
		return builder.build();
	}
	
	protected Parcel nextUnclaimed() {
		for (int distance = 0; distance <= count; distance++) {
			int inner = distance - 1;
			for (int x = -distance; x < distance + 1; x++) {
				for (int z = -distance; z < distance + 1; z++) {
					if (((x<0)?-x:x) <= inner && ((z<0)?-z:z) <= inner) {
						continue;
					}
					Parcel p = getParcelAt(x, z);
					if (!p.isClaimed()) {
						return p;
					}
				}
			}
		}
		return null;
	}
	
	protected int getAxisLimit() {
		return count;
	}
	
	private void setParcelAt(int x, int z, Parcel parcel) {
		atX(parcels, x)[count + z] = parcel;
	}
	
}