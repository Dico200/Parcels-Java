package com.redstoner.parcels.api.schematic.block;

import org.bukkit.block.Sign;

import java.util.Arrays;

public class SignBlock extends StateBlock<Sign> {
    private String[] lines;

    public SignBlock(Sign state) {
        super(state);
        lines = Arrays.copyOf(state.getLines(), 4);
    }

    public String[] getLines() {
        return lines;
    }

    @Override
    public Type getType() {
        return Type.SIGN;
    }

    @Override
    protected void paste(Sign state) {
        for (int i = 0; i < lines.length; i++) {
            state.setLine(i, lines[i]);
        }
    }

}
