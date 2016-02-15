package com.redstoner.parcels.command;

import java.util.function.BiPredicate;

import org.bukkit.entity.Player;

import com.redstoner.command.Validate;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.utils.DuoObject;
import com.redstoner.utils.Optional;

public enum ParcelRequirement {
	
	NONE		(null, 		(loc, user) -> true, 										""),
	IN_WORLD	(null, 		(loc, user) -> loc.getWorld().isPresent(), 					"You have to be in a parcel world to use that command"),
	IN_PARCEL	(IN_WORLD, 	(loc, user) -> loc.getParcel().isPresent(), 				"You have to be on a parcel to use that command"),
	IN_OWNED	(IN_PARCEL, (loc, user) -> loc.getParcel().get().isOwner(user) || user.hasPermission("parcels.admin.manage"), 
			"You must own this parcel to use that command"),
	IS_ADMIN	(IN_PARCEL, (loc, user) -> user.hasPermission("parcels.admin.manage"), 	"You must have admin rights to use that command");
	
	private Optional<ParcelRequirement> checkOther;
	private BiPredicate<Loc, Player> tester;
	private String message;
	
	private ParcelRequirement(ParcelRequirement checkOther, BiPredicate<Loc, Player> tester, String message) {
		this.checkOther = Optional.ofNullable(checkOther);
		this.tester = tester;
		this.message = message;
	}
	
	private void performTest(Player user, Loc loc) {
		Validate.isTrue(tester.test(loc, user), message);
	}
	
	public void check(Player user, Loc loc) {
		checkOther.ifPresent(req -> req.check(user, loc));
		performTest(user, loc);
	}
	
	public void check(Player user, Optional<ParcelWorld> world, Optional<Parcel> parcel) {
		check(user, new Loc(world, parcel));
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
