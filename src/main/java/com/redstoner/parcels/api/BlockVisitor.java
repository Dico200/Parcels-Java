package com.redstoner.parcels.api;

import com.redstoner.parcels.ParcelsPlugin;
import io.dico.dicore.util.task.IteratorTask;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public abstract class BlockVisitor extends IteratorTask<Block> {
    private static int pause = 2;
    private static long workTime = 30;

    public static void loadSettingsFromConfig(ConfigurationSection section) {
        if (section != null) {
            pause = Math.max(1, Math.min(100, section.getInt("pause-ticks", 2)));
            workTime = Math.max(15000L, Math.min(3, section.getLong("work-millis", 30)));
        }
    }

    public static int getPause() {
        return pause;
    }

    public static long getWorkTime() {
        return workTime;
    }

    private final Parcel parcel;

    public BlockVisitor(Parcel parcel) {
        super(parcel.getWorld().getAllBlocks(parcel));
        this.parcel = parcel;
    }

    @Override
    public void start(Plugin plugin, long delay, long period, long workTime) {
        parcel.incrementBlockVisitors();
        super.start(plugin, delay, period, workTime);
    }

    public void start(int pause, long workTime) {
        start(ParcelsPlugin.getInstance(), 0, pause, workTime);
    }

    public void start() {
        start(pause, workTime);
    }

    @Override
    protected final void onFinish(boolean early) {
        finished(early);
        parcel.decrementBlockVisitors();
    }

    protected void finished(boolean early) {
    }

}
