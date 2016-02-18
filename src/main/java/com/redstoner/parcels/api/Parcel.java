package com.redstoner.parcels.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Optional;

public class Parcel {
	
	private String world;
	private Optional<OfflinePlayer> owner;
	private PlayerList friends, denied;
	private int x, z;
	
	Parcel(String world, int x, int z) {
		this.world = world;
		this.owner = Optional.empty();
		this.x = x;
		this.z = z;
		if (StorageManager.useMySQL) {
			this.friends = new SqlPlayerList() {
	
				@Override
				protected void removeFromSQL(OfflinePlayer toRemove) {
					SqlManager.removeFriend(SqlManager.getId(world, x, z), toRemove.getUniqueId().toString());
				}
	
				@Override
				protected void addToSQL(OfflinePlayer toAdd) {
					SqlManager.addFriend(SqlManager.getId(world, x, z), toAdd.getUniqueId().toString());
				}
				
			};
			
			this.denied = new SqlPlayerList() {

				@Override
				protected void removeFromSQL(OfflinePlayer toRemove) {
					SqlManager.removeDenied(SqlManager.getId(world, x, z), toRemove.getUniqueId().toString());
				}

				@Override
				protected void addToSQL(OfflinePlayer toAdd) {
					SqlManager.addDenied(SqlManager.getId(world, x, z), toAdd.getUniqueId().toString());
				}
				
			};
		} else {
			this.friends = new PlayerList();
			this.denied = new PlayerList();
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
		this.owner.ifPresentOrElse(player -> {
			ParcelsPlugin.debug("Setting owner");
			SqlManager.setOwner(SqlManager.getId(world, x, z), player.getUniqueId().toString());
		}, () -> {
			ParcelsPlugin.debug("Removing owner");
			SqlManager.delOwner(SqlManager.getId(world, x, z));
		});
		return result;
	}
	
	public boolean isOwner(OfflinePlayer toCheck) {
		return owner.isPresent()? owner.get().getUniqueId().equals(toCheck.getUniqueId()) : false;
	}
	
	public boolean canBuild(Player user) {
		return user.getUniqueId().equals(getOwner().map(OfflinePlayer::getUniqueId).orElse(null)) || friends.contains(user);
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
	
	public PlayerList getFriends() {
		return friends;
	}
	
	public PlayerList getDenied() {
		return denied;
	}
	
	public String toString() {
		return String.format("parcel at (%s)", getId());
	}
	
	public String getInfo() {
		return String.format("&4ID: (&e%s&4) Owner: &e%s&4\nHelpers: &e%s&4\nDenied: &e%s", 
				getId(), getOwner().map(OfflinePlayer::getName).orElse(""), 
				String.join("&4, &e", (CharSequence[])friends.stream().map(OfflinePlayer::getName).toArray(size -> new String[size])),
				String.join("&4, &e", (CharSequence[])denied.stream().map(OfflinePlayer::getName).toArray(size -> new String[size])));
	}

}
