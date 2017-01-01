package com.redstoner.parcels;

import com.redstoner.parcels.api.BlockVisitor;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.parcels.api.storage.StorageManager;
import com.redstoner.parcels.command.ConfirmableRequest;
import com.redstoner.parcels.command.ParcelCommands;
import io.dico.dicore.DicoPlugin;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

public class ParcelsPlugin extends DicoPlugin {
    private static ParcelsPlugin instance = null;

    public static ParcelsPlugin getInstance() {
        return instance;
    }

    private ParcelListener parcelListener;

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String world, String id) {
        return WorldManager.getWorld(world).map(ParcelWorld::getGenerator).orElse(null);
    }

    @Override
    protected void enable() {
        instance = this;
        setDebugging(true);
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);

        WorldManager.loadSettingsFromConfig();
        StorageManager.initialise();
        new ParcelCommands(getName()).register();
        ConfirmableRequest.registerListener(getName(), getRegistrator());

        parcelListener = new ParcelListener();
        parcelListener.register(getRegistrator());
        getServer().getWorlds().forEach(parcelListener::enforceWorldSettingsIfApplicable);

        BlockVisitor.loadSettingsFromConfig(getConfig().getConfigurationSection("block-visitor"));

        Plugin worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEdit != null) {
            WorldEditListener.register(worldEdit);
        }

        startTicking(25, 5);
    }

    @Override
    protected void tick() {
        parcelListener.tick();
    }

    public ParcelListener getParcelListener() {
        return parcelListener;
    }

}
