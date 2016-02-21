package com.redstoner.parcels.api;

import org.bukkit.entity.Player;

public class Permissions {
	
	public static final String
			PARCEL_COMMAND = "parcels.command",
			PARCEL_HOME_OTHERS = "parcels.command.home.others",
			ADMIN_BUILDANYWHERE = "parcels.admin.buildanywhere",
			ADMIN_BYPASS = "parcels.admin.bypass",
			ADMIN_MANAGE = "parcels.admin.manage";
	
	private static final String
			PARCEL_LIMIT = "parcels.limit.";
	
	public static int getParcelLimit(Player user) {
		if (user.hasPermission(PARCEL_LIMIT + '*'))
			return Integer.MAX_VALUE;
		for (int i = 0; i < 256; i++)
			if (user.hasPermission(PARCEL_LIMIT + i))
				return i;
		return 0;
	}

}
