package com.redstoner.parcels.api;

import com.redstoner.parcels.api.storage.SqlManager;

public class ParcelSettings {

    ParcelSettings(Parcel parcel) {
        this.parcel = parcel;
    }

    private final Parcel parcel;
    private boolean allowsInteractInputs = false;
    private boolean allowsInteractInventory = false;

    public boolean allowsInteractInputs() {
        return allowsInteractInputs && !parcel.hasBlockVisitors();
    }

    public boolean allowsInteractInventory() {
        return allowsInteractInventory && !parcel.hasBlockVisitors();
    }

    public boolean setAllowsInteractInputs(boolean enabled) {
        if (enabled != this.allowsInteractInputs) {
            this.allowsInteractInputs = enabled;
            SqlManager.setAllowInteractInputs(parcel, enabled);
            return true;
        }
        return false;
    }

    public boolean setAllowsInteractInventory(boolean enabled) {
        if (enabled != this.allowsInteractInventory) {
            this.allowsInteractInventory = enabled;
            SqlManager.setAllowInteractInventory(parcel, enabled);
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
