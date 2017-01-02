package com.redstoner.parcels.api.schematic.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public abstract class StateBlock<T extends BlockState> extends BaseBlock {

    public StateBlock(T state) {
        super(state.getTypeId(), state.getRawData());
    }

    protected abstract void paste(T state);

    @Override
    public void paste(Block block) {
        super.paste(block);
        T state;
        try {
            state = (T) block.getState();
        } catch (ClassCastException ex) {
            return;
        }
        paste(state);
    }

    @Override
    public abstract Type getType();

}
