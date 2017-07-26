package com.redstoner.parcels.api;

import com.redstoner.parcels.api.storage.SqlManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Objects;
import java.util.UUID;

public final class ParcelOwnerData {
    private final Parcel parcel;
    private Object ref;
    
    public ParcelOwnerData(Parcel parcel) {
        this.parcel = parcel;
    }
    
    public boolean isPresent() {
        return ref != null;
    }
    
    public boolean matches(OfflinePlayer target) {
        return target == null ? ref == null : hasUniqueId() && getUniqueId().equals(target.getUniqueId());
    }
    
    public boolean hasUniqueId() {
        return ref instanceof UUID;
    }
    
    public UUID getUniqueId() {
        return hasUniqueId() ? (UUID) ref : null;
    }
    
    public boolean hasName() {
        return ref instanceof String;
    }
    
    public String getName() {
        if (hasName()) {
            return (String) ref;
        }
        
        OfflinePlayer owner = Bukkit.getOfflinePlayer(getUniqueId());
        return owner.hasPlayedBefore() ? owner.getName() : null;
    }
    
    public String getUsableName() {
        return getNameOr(":unknownName");
    }
    
    public String getNameOr(String alternative) {
        String name = getName();
        return name == null ? alternative : name;
    }
    
    public boolean updateSQLIf(boolean b) {
        if (b) {
            SqlManager.setOwner(parcel, getUniqueId(), getName());
        }
        return b;
    }
    
    public boolean setUniqueId(UUID uuid) {
        return updateSQLIf(setUniqueIdIgnoreSQL(uuid));
    }
    
    public boolean setUniqueIdIgnoreSQL(UUID uuid) {
        if (!Objects.equals(this.ref, uuid)) {
            this.ref = uuid;
            return true;
        }
        return false;
    }
    
    public boolean setName(String name) {
        return updateSQLIf(setNameIgnoreSQL(name));
    }
    
    public boolean setNameIgnoreSQL(String name) {
        if (!Objects.equals(this.ref, name)) {
            this.ref = name;
            return true;
        }
        return false;
    }
    
    private static OfflinePlayer getOfflinePlayerByName(String name) {
        OfflinePlayer out = Bukkit.getPlayer(name);
        if (out != null) {
            return out;
        }
        
        out = Bukkit.getOfflinePlayer(name);
        /*
        if (out.getUniqueId().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8)))) {
            return null;
        }
        */
        
        if (!out.hasPlayedBefore()) {
            return null;
        }
        
        return out;
    }
    
}
