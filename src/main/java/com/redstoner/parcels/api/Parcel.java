package com.redstoner.parcels.api;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.redstoner.event.ChangeListener;
import com.redstoner.event.Event;
import com.redstoner.event.Order;
import com.redstoner.event.Field;
import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Optional;

public class Parcel {
	
	private static Field<OfflinePlayer, Parcel> owner = new Field<>(null);
	private List<OfflinePlayer> friends;
	private int x, z;
	
	Parcel(int x, int z) {
		owner.initialise(this);
		this.x = x;
		this.z = z;
		friends = new ArrayList<>();
		System.out.println("New " + toString());
	}
	
	public String getId() {
		return String.format("%d:%d", x, z);
	}
	
	public Optional<OfflinePlayer> getOwner() {
		return owner.get(this);
	}
	
	public static Field<OfflinePlayer, Parcel> owner() {
		return owner;
	}
	
	public boolean setOwner(OfflinePlayer newOwner, Player sender) {
		return owner.set(this, newOwner, sender);
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
		return new Coord(x, z);
	}
	
	public boolean isClaimed() {
		boolean pres = getOwner().isPresent();
		//System.out.println(toString() + " claimed: " + pres);
		return pres;
	}
	
	public String toString() {
		return String.format("parcel at (%s)", getId());
	}
	
	static {
		owner().addListener(new ChangeListener<OfflinePlayer>(Order.LATE) {

			@Override
			public void change(Event<OfflinePlayer> event) {
				ParcelsPlugin.debug("Owner change event for " + event.<Parcel>getHolder().toString());
				Player cause = event.getCause();
				if (cause.hasPermission("parcels.command.parcel.setowner.any"))
					return;
				if (event.oldValue().map(OfflinePlayer::getUniqueId).map(uuid -> uuid.equals(cause.getUniqueId())).orElse(false))
					return;
				ParcelsPlugin.debug("Owner change event for " + event.<Parcel>getHolder().toString() + " failed");
				event.setCancelled(true);
			}
			
		});
	}
	
	public boolean addFriend(OfflinePlayer friend) {
		if (friends.contains(friend))
			return false;
		return friends.add(friend);
	}
	
	public boolean removeFriend(OfflinePlayer friend) {
		if (!friends.contains(friend))
			return false;
		return friends.remove(friend);
	}
	
	public boolean isFriend(Player toCheck) {
		return friends.contains(toCheck);
	}
	
	public boolean isOwner(OfflinePlayer toCheck) {
		return getOwner().map(OfflinePlayer::getUniqueId).map(uuid -> uuid.equals(toCheck.getUniqueId())).orElse(false);
	}
	
	public String getInfo() {
		return String.format("&4ID: (&e%s&4) Owner: &e%s&4\nHelpers: &e%s", 
				getId(), getOwner().map(OfflinePlayer::getName).orElse(""), 
				String.join(", ", (CharSequence[])friends.stream().map(OfflinePlayer::getName).toArray(size -> new String[size])));
	}

}
