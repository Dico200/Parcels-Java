package com.redstoner.parcels.command;

import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.WorldManager;
import io.dico.dicore.command.CommandScape;
import org.bukkit.entity.Player;

import java.util.Optional;

class ParcelScape extends CommandScape {

    public ParcelScape(Player user, CommandScape scape, ParcelRequirement requirement) {
        super(scape);
        this.in = WorldManager.getWorld(user.getWorld());
        this.at = in.flatMap(w -> w.getParcelAt(user.getLocation()));
        requirement.test(user, in.orElse(null), at.orElse(null));
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
