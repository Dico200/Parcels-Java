package com.redstoner.parcels.api;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.redstoner.parcels.ParcelsPlugin;

public class Permissions {
	
	public static final String
			PARCEL_COMMAND = "parcels.command",
			PARCEL_HOME_OTHERS = "parcels.command.home.others",
			ADMIN_BUILDANYWHERE = "parcels.admin.buildanywhere",
			ADMIN_BYPASS = "parcels.admin.bypass.ban",
			ADMIN_BYPASS_GAMEMODE = "parcels.admin.bypass.gamemode",
			ADMIN_MANAGE = "parcels.admin.manage",
			PARCEL_LIMIT = "parcels.limit.";
	
	private static final int DEFAULT_PARCEL_LIMIT = 1;
	
	public static int getParcelLimit(Player user) {
		for (PermissionAttachmentInfo info : user.getEffectivePermissions()) {
			String perm = info.getPermission();
			
			if (perm.startsWith(PARCEL_LIMIT)) {
				return parseSuffix(user, perm, perm.substring(PARCEL_LIMIT.length()));
			}

			/*
			for (String permission : info.getAttachment().getPermissions().keySet()) {
				if (permission.startsWith(PARCEL_LIMIT)) {
					String suffix = permission.substring(PARCEL_LIMIT.length());
					return parseSuffix(user, permission, suffix);
				}
			}
			*/
		}
		return DEFAULT_PARCEL_LIMIT;
	}
	
	private static int parseSuffix(Player user, String permission, String suffix) {
		if (suffix.equals("*")) {
			return Integer.MAX_VALUE;
		}
		try {
			return Integer.parseInt(suffix);
		} catch (NumberFormatException e) {
			ParcelsPlugin.log(String.format("[ERROR] %s has permission '%s'. The suffix "
					+ "can not be parsed to an integer (or *).", user.getName(), permission));
			return DEFAULT_PARCEL_LIMIT;
		}
	}

}
