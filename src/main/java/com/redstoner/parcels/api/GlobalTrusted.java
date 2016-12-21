package com.redstoner.parcels.api;

import com.redstoner.parcels.api.list.PlayerMap;
import com.redstoner.parcels.api.list.SqlPlayerMap;
import com.redstoner.parcels.api.storage.SqlManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GlobalTrusted {

    private static final Map<UUID, PlayerMap<Boolean>> trusted = new HashMap<>();
    private static final PlayerMap<Boolean> empty = new PlayerMap<Boolean>(true);

    private static final PlayerMap<Boolean> newMap(UUID player) {
        return new SqlPlayerMap<Boolean>(true) {

            @Override
            protected void addToSQL(UUID toAdd, Boolean value) {
                SqlManager.addGlobalPlayer(player, toAdd, value);
            }

            @Override
            protected void removeFromSQL(UUID toRemove) {
                SqlManager.removeGlobalPlayer(player, toRemove);
            }

            @Override
            protected void clearSQL() {
                SqlManager.removeAllGlobalPlayers(player);
            }

        };
    }

    private static void ensureKey(UUID player) {
        if (!trusted.containsKey(player)) {
            trusted.put(player, newMap(player));
        }
    }

    public static PlayerMap<Boolean> getAdded(UUID player) {
        return trusted.getOrDefault(player, empty);
    }

    public static void addPlayerIgnoreSQL(String playerId, String addedId, boolean allowed) {
        UUID player = UUID.fromString(playerId);
        UUID added = UUID.fromString(addedId);
        ensureKey(player);
        getAdded(player).getMap().put(added, allowed);
    }

    public static boolean addPlayer(UUID player, UUID added, boolean allowed) {
        ensureKey(player);
        return getAdded(player).add(added, allowed);
    }

    public static boolean removePlayer(UUID player, UUID removed, boolean allowed) {
        return getAdded(player).remove(removed, allowed);
    }

    private GlobalTrusted() {
    }

}
