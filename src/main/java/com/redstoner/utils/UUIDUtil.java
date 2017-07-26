package com.redstoner.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.nio.ByteBuffer;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

public class UUIDUtil {

    public static String UUIDToString(UUID uuid) {
        if (uuid == null) return null;
        return uuid.toString().replaceAll("-", "");
    }

    public static String addUUIDDashes(String string) {
        char[] orig = string.toLowerCase().toCharArray();
        if (orig.length != 32) {
            throw new IllegalArgumentException();
        }
        char[] ret = new char[36];
        int dashCount = 0;
        for (int i = 0; i < orig.length; i++) {
            if (i == 8 || i == 12 || i == 16 || i == 20) {
                ret[i + dashCount] = '-';
                dashCount++;
            }
            ret[i + dashCount] = orig[i];
        }
        return String.valueOf(ret);
    }

    public static UUID UUIDFromString(String string) {
        if (string == null) return null;
        if (string.length() != 32)
            throw new IllegalArgumentException("Invalid hex string length for UUID (must be 32): " + string.length() + " - " + string);
        return UUID.fromString(addUUIDDashes(string));
    }

    public static byte[] UUIDToBytes(UUID uuid) {
        if (uuid == null) return null;
        return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    }

    public static UUID UUIDFromBytes(byte[] array) {
        if (array == null) return null;
        ByteBuffer buf = ByteBuffer.wrap(array);
        return new UUID(buf.getLong(), buf.getLong());
    }

    public static String getName(UUID player) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        return offlinePlayer.hasPlayedBefore() ? ":unknownName:" : offlinePlayer.getName();
    }

    public static byte[] toByteArray(UUID uuid) {
        return uuid == null ? null : ByteBuffer.wrap(new byte[16]).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    }

    public static String bytesToString(byte[] array) {
        if (array == null) return null;
        return new String(ByteBuffer.wrap(array).asCharBuffer().array());
    }

    public static String toBytesAsString(UUID uuid) {
        return bytesToString(toByteArray(uuid));
    }

    public static UUID fromBytesAsString(String string) {
        checkArgument(string.length() == 16, "Input length must be 16 to convert to UUID: " + string);
        ByteBuffer buf = ByteBuffer.allocate(16);
        for (char c : string.toCharArray()) {
            int code = Character.digit(c, 256);
            if (code == -1) {
                throw new IllegalArgumentException("Character not of UTF-8 format: " + c);
            }
            buf.put((byte) code);
        }
        return new UUID(buf.getLong(0), buf.getLong(8));
    }

}
