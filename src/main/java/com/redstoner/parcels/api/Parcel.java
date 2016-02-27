package com.redstoner.parcels.api;

import java.io.Serializable;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.redstoner.parcels.api.list.PlayerMap;
import com.redstoner.parcels.api.list.SerialPlayerMap;
import com.redstoner.parcels.api.list.SqlPlayerMap;
import com.redstoner.parcels.api.storage.SqlManager;
import com.redstoner.parcels.api.storage.StorageManager;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Optional;

public class Parcel implements Serializable {
	private static final long serialVersionUID = -7252413358120772747L;
	
	private final String world;
	private Optional<UUID> owner;
	
	//Players added to the parcel. If true: they can build. If false: They are banned.
	private final PlayerMap<Boolean> added;
	private final ParcelSettings settings;
	private final int x, z;
	
	public Parcel(String world, int x, int z) {
		this.world = world;
		this.owner = Optional.empty();
		this.x = x;
		this.z = z;
		this.settings = new ParcelSettings(this);
		if (StorageManager.useMySQL) {
			this.added = new SqlPlayerMap<Boolean>(true) {

				@Override
				public void addToSQL(UUID toAdd, Boolean value) {
					SqlManager.addPlayer(world, x, z, toAdd, value);
				}

				@Override
				public void removeFromSQL(UUID toRemove) {
					SqlManager.removePlayer(world, x, z, toRemove);
				}

				@Override
				protected void clearSQL() {
					SqlManager.removeAllPlayers(world, x, z);
				}
				
			};
		} else {
			this.added = new SerialPlayerMap<Boolean>(true);
		}
	}
	
	public String getWorld() {
		return world;
	}
	
	public String getId() {
		return String.format("%d:%d", x, z);
	}
	
	public Optional<UUID> getOwner() {
		return owner;
	}
	
	public boolean setOwnerIgnoreSQL(UUID owner) {
		if (this.owner.equals(owner))
			return false;
		this.owner = Optional.ofNullable(owner);
		return true;
	}
	
	public boolean setOwner(UUID owner) {
		if (setOwnerIgnoreSQL(owner)) {
			if (StorageManager.useMySQL) {
				SqlManager.setOwner(world, x, z, owner);
			}
			return true;
		}
		return false;
	}
	
	public boolean isOwner(OfflinePlayer toCheck) {
		return owner.filter(owner -> owner.equals(toCheck.getUniqueId())).isPresent();
	}
	
	public boolean canBuild(OfflinePlayer user) {
		return isOwner(user) || isAllowed(user);
	}
	
	public int getX() {
		return x;
	}
	
	public int getZ() {
		return z;
	}
	
	public Coord getCoord() {
		return Coord.of(x, z);
	}
	
	public boolean isClaimed() {
		return owner.isPresent();
	}
	
	public boolean isAllowed(OfflinePlayer user) {
		return added.is(user.getUniqueId(), true);
	}
	
	public boolean isBanned(OfflinePlayer user) {
		return added.is(user.getUniqueId(), false);
	}
	
	public ParcelSettings getSettings() {
		return settings;
	}
	
	public PlayerMap<Boolean> getAdded() {
		return added;
	}
	
	public void dispose() {
		setOwner(null);
		added.clear();
		settings.setAllowsInteractInputs(false);
		settings.setAllowsInteractInventory(false);
	}
	
	public String toString() {
		return String.format("parcel at (%s)", getId());
	}
	
	public String getInfo() {
		return String.format("&bID: (&e%s&b) Owner: &e%s&b\nAllowed: &e%s&b\nBanned: &e%s", 
				getId(), getOwner().map(player -> Bukkit.getOfflinePlayer(player).getName()).orElse(""), 
				added.toString(true), added.toString(false));
	}

}
