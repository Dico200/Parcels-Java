package com.redstoner.parcels.api.blockvisitor;

import com.redstoner.parcels.ParcelsPlugin;
import io.dico.dicore.util.task.IteratorTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.Iterator;

public abstract class BlockVisitor<T> extends IteratorTask<T> {
    private static int pause = 1;
    private static long workTime = 40;
    private static int running = 0;

    public static void loadSettingsFromConfig(ConfigurationSection section) {
        if (section != null) {
            pause = Math.max(1, Math.min(100, section.getInt("pause-ticks", 1)));
            workTime = Math.max(3L, Math.min(15000L, section.getLong("work-millis", 40L)));
        }
    }

    public BlockVisitor() {
    }

    public BlockVisitor(Iterator<? extends T> iterator) {
        super(iterator);
    }

    public void setIterator(Iterator<? extends T> iterator) {
        refresh(iterator);
    }

    public static int getPause() {
        return pause;
    }

    @Override
    protected long getWorkTime() {
        return workTime / Math.min(1, running);
    }

    public static long workTime() {
        return workTime / Math.min(1, running);
    }

    @Override
    protected void onFinish(boolean early) {
        if (running > 0) {
            running--;
        }
    }

    @Override
    public void start(Plugin plugin) {
        running++;
        start(plugin, 0, pause, getWorkTime());
    }

    public void start() {
        start(ParcelsPlugin.getInstance());
    }

}
