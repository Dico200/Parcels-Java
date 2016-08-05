package com.redstoner.parcels.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

import com.redstoner.parcels.api.list.PlayerMap;
import com.redstoner.parcels.api.list.SqlPlayerMap;
import com.redstoner.parcels.api.storage.SqlManager;
import com.redstoner.parcels.api.storage.StorageManager;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Optional;
import com.redstoner.utils.UUIDUtil;

public class Parcel implements Serializable {
	private static final long serialVersionUID = -7252413358120772747L;
	
	private final ParcelWorld world;
	private Optional<UUID> owner;
	
	//Players added to the parcel. If true: they can build. If false: They are banned.
	private final PlayerMap<Boolean> added;
	private final ParcelSettings settings;
	private final int x, z;
	
	public Parcel(ParcelWorld world, int x, int z) {
		this.world = world;
		this.owner = Optional.empty();
		this.x = x;
		this.z = z;
		this.settings = new ParcelSettings(this);
		
		String worldName = world.getName();
		this.added = new SqlPlayerMap<Boolean>(true) {

			@Override
			public void addToSQL(UUID toAdd, Boolean value) {
				SqlManager.addPlayer(worldName, x, z, toAdd, value);
			}

			@Override
			public void removeFromSQL(UUID toRemove) {
				SqlManager.removePlayer(worldName, x, z, toRemove);
			}

			@Override
			protected void clearSQL() {
				SqlManager.removeAllPlayers(worldName, x, z);
			}
			
		};
	}
	
	public ParcelWorld getWorld() {
		return world;
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
				SqlManager.setOwner(world.getName(), x, z, owner);
			}
			world.setOwnerSign(this);
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
		return added.is(user.getUniqueId(), true) || getGloballyAdded().filter(map -> map.is(user.getUniqueId(), true)).isPresent();
	}
	
	public boolean isBanned(OfflinePlayer user) {
		return added.is(user.getUniqueId(), false) || getGloballyAdded().filter(map -> map.is(user.getUniqueId(), false)).isPresent();
	}
	
	public ParcelSettings getSettings() {
		return settings;
	}
	
	public PlayerMap<Boolean> getAdded() {
		return added;
	}
	
	public Optional<PlayerMap<Boolean>> getGloballyAdded() {
		return owner.map(GlobalTrusted::getAdded);
	}
	
	public void dispose() {
		setOwner(null);
		added.clear();
		settings.setAllowsInteractInputs(false);
		settings.setAllowsInteractInventory(false);
	}
	
	public String getId() {
		return String.format("%d:%d", x, z);
	}
	
	@Override
	public String toString() {
		return String.format("parcel at (%s)", getId());
	}
	
	public String getInfo() {
		
		// Key: The player, Value: Whether allowed or banned.
		Map<UUID, Boolean> global = getGloballyAdded().map(PlayerMap::getMap).orElse(new HashMap<>());
		Map<UUID, Boolean> local = getAdded().getMap();
		
		// Key: The player, Value: The name.
		Map<UUID, String> allowedPlayers = new LinkedHashMap<>();
		Map<UUID, String> bannedPlayers = new LinkedHashMap<>();
		
		local.forEach((uuid, allowed) -> {
			(allowed ? allowedPlayers : bannedPlayers).put(uuid, UUIDUtil.getName(uuid)); 
		});
		
		global.forEach((uuid, allowed) -> {
			(allowed ? allowedPlayers : bannedPlayers).put(uuid, "&a(G)&e" + UUIDUtil.getName(uuid));
		});
		
		String allowedList = String.join("&b, &e", allowedPlayers.values());
		String bannedList = String.join("&b, &e", bannedPlayers.values());
		
		return String.format("&bID: (&e%s&b) Owner: &e%s&b\nAllowed: &e%s&b\nBanned: &e%s", 
				getId(), owner.map(UUIDUtil::getName).orElse(""), allowedList, bannedList);
	}

}
