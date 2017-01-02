package com.redstoner.parcels.api.schematic.block;

import org.bukkit.block.Beacon;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BeaconBlock extends StateBlock<Beacon> {
    private PotionEffectType primary, secondary;

    public BeaconBlock(Beacon state) {
        super(state);
        primary = typeOf(state.getPrimaryEffect());
        secondary = typeOf(state.getSecondaryEffect());
    }

    private static PotionEffectType typeOf(PotionEffect effect) {
        return effect == null ? null : effect.getType();
    }

    public PotionEffectType getPrimary() {
        return primary;
    }

    public PotionEffectType getSecondary() {
        return secondary;
    }

    @Override
    public Type getType() {
        return Type.BEACON;
    }

    @Override
    protected void paste(Beacon state) {
        state.setPrimaryEffect(primary);
        state.setSecondaryEffect(secondary);
    }

}
