package com.redstoner.parcels.api.schematic.block;

import org.bukkit.OfflinePlayer;
import org.bukkit.SkullType;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;

public class SkullBlock extends StateBlock<Skull> {
    private BlockFace rotation;
    private SkullType skullType;
    private OfflinePlayer owningPlayer;

    public SkullBlock(Skull state) {
        super(state);
        rotation = state.getRotation();
        skullType = state.getSkullType();
        if (skullType == SkullType.PLAYER) {
            owningPlayer = state.getOwningPlayer();
        }
    }

    public BlockFace getRotation() {
        return rotation;
    }

    public SkullType getSkullType() {
        return skullType;
    }

    public OfflinePlayer getOwningPlayer() {
        return owningPlayer;
    }

    @Override
    public Type getType() {
        return Type.SKULL;
    }

    @Override
    protected void paste(Skull state) {
        state.setRotation(rotation);
        state.setSkullType(skullType);
        if (skullType == SkullType.PLAYER && owningPlayer != null) {
            state.setOwningPlayer(owningPlayer);
        }
    }

}
