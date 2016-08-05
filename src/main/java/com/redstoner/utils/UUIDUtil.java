package com.redstoner.utils;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.bukkit.Bukkit;

public class UUIDUtil {
	
	public static String getName(UUID player) {
		String result = Bukkit.getOfflinePlayer(player).getName();
		return result == null ? ":unknownName:" : result;
	}
	
	public static byte[] toByteArray(UUID uuid) {
		return ByteBuffer.wrap(new byte[16]).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
	}
	
	public static UUID fromByteArray(byte[] array) {
		checkArgument(array.length == 16, "byte[] length must be 16");
		ByteBuffer buffer = ByteBuffer.wrap(array);
		return new UUID(buffer.getLong(0), buffer.getLong(8));
	}

}
