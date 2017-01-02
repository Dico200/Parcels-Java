package com.redstoner.parcels.api.schematic.block;

import org.bukkit.Material;
import org.bukkit.block.Jukebox;

public class JukeboxBlock extends StateBlock<Jukebox> {
    private Material disc;

    public JukeboxBlock(Jukebox state) {
        super(state);
        disc = state.getPlaying();
    }

    public Material getDisc() {
        return disc;
    }

    @Override
    public Type getType() {
        return Type.JUKEBOX;
    }

    @Override
    protected void paste(Jukebox state) {
        state.setPlaying(disc);
    }

}
