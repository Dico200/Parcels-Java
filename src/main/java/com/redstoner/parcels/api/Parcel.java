package com.redstoner.parcels.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.redstoner.event.ChangeListener;
import com.redstoner.event.Event;
import com.redstoner.event.Order;
import com.redstoner.event.Field;
import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.DuoObject.Coord;

public class Parcel {
	
	private static Field<OfflinePlayer, Parcel> owner = new Field<>(null);
	private List<OfflinePlayer> friends;
	private int x, z;
	
	public Parcel(int x, int z) {
		owner.initialise(this);
		this.x = 0;
		this.z = 0;
		friends = new ArrayList<>();
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
		return user.getUniqueId().equals(getOwner().map(OfflinePlayer::getUniqueId)) || friends.contains(user);
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
		return getOwner().isPresent();
	}
	
	public String toString() {
		return String.format("parcel at (%s, %s)", x, z);
	}
	
	static {
		owner().addListener(new ChangeListener<OfflinePlayer>(Order.LATE) {

			@Override
			public void change(Event<OfflinePlayer> event) {
				ParcelsPlugin.debug("Owner change event for " + event.<Parcel>getHolder().toString());
				Player cause = Field.cast(Player.class, event.getCause(), true);
				if (cause.hasPermission("parcels.command.parcel.setowner.any"))
					return;
				if (event.oldValue().map(OfflinePlayer::getUniqueId).map(uuid -> uuid.equals(cause.getUniqueId())).orElse(false))
					return;
				event.setCancelled(true);
			}
			
		});
	}

}
