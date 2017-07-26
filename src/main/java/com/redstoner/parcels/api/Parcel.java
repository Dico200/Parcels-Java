package com.redstoner.parcels.api;

import com.redstoner.parcels.api.list.PlayerMap;
import com.redstoner.parcels.api.list.SqlPlayerMap;
import com.redstoner.parcels.api.schematic.Schematic;
import com.redstoner.parcels.api.storage.SqlManager;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.UUIDUtil;
import io.dico.dicore.util.block.BlockPos;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class Parcel {
    private final ParcelWorld world;
    private final int x, z;
    private final ParcelOwnerData owner;
    private final PlayerMap<Boolean> added;
    private final ParcelSettings settings;
    
    // called and used only by the SQL manager
    private transient int uniqueId = -1;
    // if the parcel is being cleared or swapped, this number is non-zero
    private transient int blockVisitors = 0;

    public Parcel(ParcelWorld world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
        this.owner = new ParcelOwnerData(this);
        this.settings = new ParcelSettings(this);
        this.added = new SqlPlayerMap<Boolean>(true) {

            @Override
            public void addToSQL(UUID toAdd, Boolean value) {
                SqlManager.addPlayer(Parcel.this, toAdd, value);
            }

            @Override
            public void removeFromSQL(UUID toRemove) {
                SqlManager.removePlayer(Parcel.this, toRemove);
            }

            @Override
            protected void clearSQL() {
                SqlManager.removeAllPlayers(Parcel.this);
            }

        };
    }

    public void incrementBlockVisitors() {
        blockVisitors++;
    }

    public void decrementBlockVisitors() {
        if (blockVisitors > 0) {
            blockVisitors--;
        } else {
            blockVisitors = 0;
        }
    }

    public boolean hasBlockVisitors() {
        return blockVisitors != 0;
    }

    public ParcelWorld getWorld() {
        return world;
    }

    public ParcelOwnerData getOwner() {
        return owner;
    }

    /*
    public boolean setOwnerIgnoreSQL(UUID owner) {
        if (Objects.equals(this.owner, owner)) {
            return false;
        }
        
        this.owner = owner;
        this.ownerName = UUIDUtil.getName(owner);
        return true;
    }

    public boolean setOwner(UUID owner) {
        if (setOwnerIgnoreSQL(owner)) {
            if (StorageManager.useMySQL) {
                SqlManager.setOwner(this, owner);
            }
            world.setOwnerSign(this);
            return true;
        }
        return false;
    }
    */

    /*
    public boolean isOwner(OfflinePlayer toCheck) {
        return owner != null && owner.equals(toCheck.getUniqueId());
    }
    */

    public boolean canBuild(OfflinePlayer user) {
        return !hasBlockVisitors() && (owner.matches(user) || isAllowed(user));
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

    public Schematic initSchematic() {
        Coord bottom = world.getBottomCoord(this);
        BlockPos origin = new BlockPos(world.getWorld(), bottom.getX(), 0, bottom.getZ());
        int parcelSize = world.getSettings().parcelSize;
        BlockPos size = new BlockPos(world.getWorld(), parcelSize, 256, parcelSize);
        return new Schematic(origin, size);
    }

    public PlayerMap<Boolean> getAdded() {
        return added;
    }

    public Optional<PlayerMap<Boolean>> getGloballyAdded() {
        return !owner.hasUniqueId() ? Optional.empty() : Optional.ofNullable(GlobalTrusted.getAdded(owner.getUniqueId()));
    }

    public void dispose() {
        owner.setUniqueId(null);
        added.clear();
        settings.setAllowsInteractInputs(false);
        settings.setAllowsInteractInventory(false);
    }

    public String getTextId() {
        return String.format("%d:%d", x, z);
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public String toString() {
        return String.format("parcel at (%s)", getTextId());
    }

    public String getInfo() {

        // Key: The player, Value: The name.
        Map<UUID, String> allowedPlayers = new LinkedHashMap<>();
        Map<UUID, String> bannedPlayers = new LinkedHashMap<>();

        getAdded().getMap().forEach((uuid, allowed) -> {
            (allowed ? allowedPlayers : bannedPlayers).put(uuid, UUIDUtil.getName(uuid));
        });

        getGloballyAdded().map(PlayerMap::getMap).orElse(new HashMap<>()).forEach((uuid, allowed) -> {
            (allowed ? allowedPlayers : bannedPlayers).put(uuid, "&a(G)&e" + UUIDUtil.getName(uuid));
        });

        String allowedList;
        if (allowedPlayers.isEmpty()) {
            allowedList = "";
        } else {
            allowedList = "&b\nAllowed: &e" + String.join("&b, &e", allowedPlayers.values());
        }

        String bannedList;
        if (bannedPlayers.isEmpty()) {
            bannedList = "";
        } else {
            bannedList = "&b\nBanned: &e" + String.join("&b, &e", bannedPlayers.values());
        }

        return String.format("&bID: (&e%s&b) Owner: &e%s%s%s",
                getTextId(), owner.getNameOr(""), allowedList, bannedList);
    }

}
