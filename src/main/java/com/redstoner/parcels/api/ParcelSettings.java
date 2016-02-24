package com.redstoner.parcels.api;

public class ParcelSettings {
	
	ParcelSettings(Parcel parcel) {
		this.parcel = parcel;
	}
	
	private final Parcel parcel;
	private boolean allowsInteractInputs = false;
	private boolean allowsInteractInventory = false;
	
	public boolean allowsInteractInputs() {
		return allowsInteractInputs;
	}
	
	public boolean allowsInteractInventory() {
		return allowsInteractInventory;
	}
	
	public boolean setAllowsInteractInputs(boolean enabled) {
		if (enabled != this.allowsInteractInputs) {
			this.allowsInteractInputs = enabled;
			SqlManager.setAllowInteractInputs(parcel.getWorld(), parcel.getX(), parcel.getZ(), enabled);
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
	
	public void setAllowsInteractInputsIgnoreSQL(boolean enabled) {
		this.allowsInteractInputs = enabled;
	}
	
	public void setAllowsInteractInventoryIgnoreSQL(boolean enabled) {
		this.allowsInteractInventory = enabled;
	}
	
}
