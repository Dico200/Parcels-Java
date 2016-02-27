package com.redstoner.utils;

import java.util.UUID;

import org.bukkit.Bukkit;

public class UUIDUtil {
	
	public static String getName(UUID player) {
		String result = Bukkit.getOfflinePlayer(player).getName();
		return result == null ? ":unknownName:" : result;
	}

}
