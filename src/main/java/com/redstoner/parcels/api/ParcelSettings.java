package com.redstoner.parcels.api;

public class ParcelSettings {
	
	ParcelSettings(Parcel parcel) {
		this.parcel = parcel;
	}
	
	private final Parcel parcel;
	private boolean allowsInteractLever = false;
	private boolean allowsInteractInventory = false;
	
	public boolean allowsInteractLever() {
		return allowsInteractLever;
	}
	
	public boolean allowsInteractInventory() {
		return allowsInteractInventory;
	}
	
	public boolean setAllowsInteractLever(boolean enabled) {
		if (enabled != this.allowsInteractLever) {
			this.allowsInteractLever = enabled;
			SqlManager.setAllowInteractLever(parcel.getWorld(), parcel.getX(), parcel.getZ(), enabled);
			return true;
		}
		return false;
	}
	
	public boolean setAllowsInteractInventory(boolean enabled) {
		if (enabled != this.allowsInteractInventory) {
			this.allowsInteractInventory = enabled;
			SqlManager.setAllowInteractInventory(parcel.getWorld(), parcel.getX(), parcel.getZ(), enabled);
			return true;
		}
		return false;
	}
	
	public void setAllowsInteractLeverIgnoreSQL(boolean enabled) {
		this.allowsInteractLever = enabled;
	}
	
	public void setAllowsInteractInventoryIgnoreSQL(boolean enabled) {
		this.allowsInteractInventory = enabled;
	}
	
}
