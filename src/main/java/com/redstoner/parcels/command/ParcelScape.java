package com.redstoner.parcels.command;

import com.redstoner.command.CommandScape;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.Optional;
import org.bukkit.entity.Player;

class ParcelScape extends CommandScape {

    public ParcelScape(Player user, CommandScape scape, ParcelRequirement requirement) {
        super(scape);
        this.in = WorldManager.getWorld(user.getWorld());
        this.at = in.flatMap(w -> w.getParcelAt(user.getLocation()));
        requirement.test(user, in, at);
    }

    private Optional<Parcel> at;
    private Optional<ParcelWorld> in;

    public Parcel getParcel() {
        return at.get();
    }

    public Optional<Parcel> getMaybeParcel() {
        return at;
    }

    public ParcelWorld getWorld() {
        return in.get();
    }

    public Optional<ParcelWorld> getMaybeWorld() {
        return in;
    }


}
