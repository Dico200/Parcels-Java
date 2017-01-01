package com.redstoner.parcels.command;

import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.Permissions;
import io.dico.dicore.command.Validate;
import org.bukkit.entity.Player;

enum ParcelRequirement {
    NONE,
    IN_WORLD {
        @Override
        public void test(Player user, ParcelWorld world, Parcel parcel) {
            Validate.isTrue(world != null, "You have to be in a parcel world to use that command");
        }
    },
    IN_PARCEL {
        @Override
        public void test(Player user, ParcelWorld world, Parcel parcel) {
            IN_WORLD.test(user, world, parcel);
            Validate.isTrue(parcel != null, "You have to be in a parcel to use that command");
        }
    },
    IN_OWNED {
        @Override
        public void test(Player user, ParcelWorld world, Parcel parcel) {
            IN_PARCEL.test(user, world, parcel);
            Validate.isTrue(parcel.isOwner(user) || user.hasPermission(Permissions.ADMIN_MANAGE),
                    "You must own this parcel to use that command");
        }
    },
    IS_ADMIN {
        @Override
        public void test(Player user, ParcelWorld world, Parcel parcel) {
            Validate.isAuthorized(user, Permissions.ADMIN_MANAGE, "You must have admin rights to use that command");
        }
    },
    IS_ADMIN_IN_PARCEL {
        @Override
        public void test(Player user, ParcelWorld world, Parcel parcel) {
            IS_ADMIN.test(user, world, parcel);
            IN_PARCEL.test(user, world, parcel);
        }
    };

    public void test(Player user, ParcelWorld world, Parcel parcel) {

    }
}
