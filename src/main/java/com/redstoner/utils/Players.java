package com.redstoner.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Players {

    public static String toUuid(Player user) {
        return user.getUniqueId().toString();
    }

    public static Player toPlayer(String uuid) {
        return Bukkit.getPlayer(UUID.fromString(uuid));
    }

    public static OfflinePlayer toOfflinePlayer(String uuid) {
        return Bukkit.getOfflinePlayer(UUID.fromString(uuid));
    }

}