package com.redstoner.parcels.command;

import java.util.Arrays;
import java.util.function.BiPredicate;

import com.redstoner.command.Validate;
import com.redstoner.utils.DuoObject;
import com.redstoner.utils.Optional;

import org.bukkit.entity.Player;

import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;

public enum ParcelRequirement {
	
	NONE,
	
	IN_WORLD (
			(user, loc) -> loc.getWorld().isPresent(), 
			"You have to be in a parcel world to use that command"
	),
	
	IN_PARCEL (
			(user, loc) -> loc.getParcel().isPresent(),
			"You have to be in a parcel to use that command",
			IN_WORLD
	),
	
	IN_OWNED (
			(user, loc) -> loc.getParcel().get().isOwner(user) || user.hasPermission("parcels.admin.manage"), 
			"You must own this parcel to use that command",
			IN_PARCEL
	),
	
	IS_ADMIN (
			(user, loc) -> user.hasPermission("parcels.admin.manage"),
			"You must have admin rights to use that command"
	),
	
	IS_ADMIN_IN_PARCEL (
			IS_ADMIN, IN_PARCEL
	);
	
	private final BiPredicate<Player, Loc> tester;
	private final String message;
	
	private ParcelRequirement(BiPredicate<Player, Loc> tester, String message, ParcelRequirement... checkOthers) {
		this.tester = (user, loc) -> {
			Arrays.stream(checkOthers).forEach(requirement -> requirement.test(user, loc));
			return tester.test(user, loc);
		};
		this.message = message;
	}
	
	private ParcelRequirement(ParcelRequirement... checkOthers) {
		this((user, loc) -> true, null, checkOthers);
	}
	
	public void test(Player user, Loc loc) {
		Validate.isTrue(tester.test(user, loc), message);
	}
	
	public void test(Player user, Optional<ParcelWorld> world, Optional<Parcel> parcel) {
		test(user, new Loc(world, parcel));
	}

}

class Loc extends DuoObject<Optional<ParcelWorld>, Optional<Parcel>> {

	public Loc(Optional<ParcelWorld> v1, Optional<Parcel> v2) {
		super(v1, v2);
	}
	
	public Optional<ParcelWorld> getWorld() {
		return v1;
	}
	
	public Optional<Parcel> getParcel() {
		return v2;
	}
	
}
