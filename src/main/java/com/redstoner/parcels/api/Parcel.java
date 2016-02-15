package com.redstoner.parcels.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Optional;

public class Parcel {
	private Optional<OfflinePlayer> owner;
	private PlayerList friends, denied;
	private int x, z;
	
	Parcel(int x, int z) {
		this.owner = Optional.empty();
		this.x = x;
		this.z = z;
		this.friends = new PlayerList();
		this.denied = new PlayerList();
	}
	
	public String getId() {
		return String.format("%d:%d", x, z);
	}
	
	public Optional<OfflinePlayer> getOwner() {
		return owner;
	}
	
	public boolean setOwner(OfflinePlayer owner) {
		if (this.owner == owner)
			return false;
		this.owner = Optional.ofNullable(owner);
		return true;
	}
	
	public boolean setOwner(Optional<OfflinePlayer> owner) {
		return setOwner(owner.orElse(null));
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
		return String.format("&4ID: (&e%s&4) Owner: &e%s&4\nHelpers: &e%s", 
				getId(), getOwner().map(OfflinePlayer::getName).orElse(""), 
				String.join(", ", (CharSequence[])friends.stream().map(OfflinePlayer::getName).toArray(size -> new String[size])));
	}

}
