package com.redstoner.parcels.api.blockvisitor;

import com.redstoner.parcels.api.Parcel;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public abstract class ParcelVisitor<T> extends BlockVisitor<T> {
    private final Parcel parcel;

    public ParcelVisitor(Parcel parcel) {
        this.parcel = Objects.requireNonNull(parcel);
    }

    @Override
    public void start(Plugin plugin, int delay, int period, long workTime) {
        super.start(plugin, delay, period, workTime);
        parcel.incrementBlockVisitors();
    }

    @Override
    protected final void onFinish(boolean early) {
        parcel.decrementBlockVisitors();
        finished(early);
    }

    protected void finished(boolean early) {
    }

}
