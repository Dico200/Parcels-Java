package com.redstoner.parcels.api;

import java.io.Serializable;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.redstoner.parcels.api.list.PlayerMap;
import com.redstoner.parcels.api.list.SerialPlayerMap;
import com.redstoner.parcels.api.list.SqlPlayerMap;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Optional;

public class Parcel implements Serializable {
	private static final long serialVersionUID = -7252413358120772747L;
	
	private String world;
	private Optional<OfflinePlayer> owner;
	
	//Players added to the parcel. If true: they can build. If false: They are banned.
	private PlayerMap<Boolean> added;
	private int x, z;
	
	Parcel(String world, int x, int z) {
		this.world = world;
		this.owner = Optional.empty();
		this.x = x;
		this.z = z;
		if (StorageManager.useMySQL) {
			this.added = new SqlPlayerMap<Boolean>(true) {

				@Override
				public void addToSQL(OfflinePlayer toAdd, Boolean value) {
					SqlManager.addPlayer(world, x, z, toAdd, value);
				}

				@Override
				public void removeFromSQL(OfflinePlayer toRemove) {
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
	
	public String getId() {
		return String.format("%d:%d", x, z);
	}
	
	public Optional<OfflinePlayer> getOwner() {
		return owner;
	}
	
	boolean setOwnerIgnoreSQL(OfflinePlayer owner) {
		if (this.owner.equals(owner))
			return false;
		this.owner = Optional.ofNullable(owner);
		return true;
	}
	
	public boolean setOwner(OfflinePlayer owner) {
		boolean result = setOwnerIgnoreSQL(owner);
		if (StorageManager.useMySQL)
			this.owner.ifPresentOrElse(player -> {
				SqlManager.setOwner(world, x, z, player);
			}, () -> {
				SqlManager.delOwner(world, x, z);
			});
		return result;
	}
	
	public boolean isOwner(OfflinePlayer toCheck) {
		return owner.filter(owner -> owner == toCheck).isPresent();
	}
	
	public boolean canBuild(Player user) {
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
		return getOwner().isPresent();
	}
	
	public boolean isAllowed(OfflinePlayer user) {
		return added.is(user, true);
	}
	
	public boolean isBanned(OfflinePlayer user) {
		return added.is(user, false);
	}
	
	public PlayerMap<Boolean> getAdded() {
		return added;
	}
	
	public String toString() {
		return String.format("parcel at (%s)", getId());
	}
	
	public String getInfo() {
		return String.format("&bID: (&e%s&b) Owner: &e%s&b\nAllowed: &e%s&b\nBanned: &e%s", 
				getId(), getOwner().map(OfflinePlayer::getName).orElse(""), 
				added.toString(true), added.toString(false));
	}

}
