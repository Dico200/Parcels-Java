package com.redstoner.parcels.api;

import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.redstoner.event.ChangeListener;
import com.redstoner.event.Event;
import com.redstoner.event.Order;
import com.redstoner.event.Field;
import com.redstoner.utils.DuoObject.Coord;

public class Parcel {
	
	private static Field<OfflinePlayer, Parcel> owner = new Field<OfflinePlayer, Parcel>();
	private List<UUID> friends;
	private int x, z;
	
	public Parcel() {
		owner.initialise(this);
		this.x = 0;
		this.z = 0;
	}
	
	public String getId() {
		return String.format("%d:%d", x, z);
	}
	
	public OfflinePlayer getOwner() {
		return owner.get(this);
	}
	
	public static Field<OfflinePlayer, Parcel> owner() {
		return owner;
	}
	
	public boolean setOwner(OfflinePlayer newOwner, Player sender) {
		return owner.set(this, newOwner, sender);
	}
	
	public boolean canBuild(Player user) {
		return user.getUniqueId().equals(getOwner().getUniqueId()) || friends.contains(user);
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
	
	static {
		owner().addListener(new ChangeListener<OfflinePlayer>(Order.LATE) {

			@Override
			public void change(Event<OfflinePlayer> event) {
				Player cause = Field.cast(Player.class, event.getCause(), true);
				if (cause.hasPermission("parcels.command.parcel.setowner.any"))
					return;
				if (event.oldValue().getUniqueId().equals(cause.getUniqueId()))
					return;
				event.setCancelled(true);
			}
			
		});
	}

}
