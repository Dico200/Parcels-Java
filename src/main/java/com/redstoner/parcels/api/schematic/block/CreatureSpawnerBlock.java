package com.redstoner.parcels.api.schematic.block;

import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;

public class CreatureSpawnerBlock extends StateBlock<CreatureSpawner> {
    private EntityType spawnedType;

    public CreatureSpawnerBlock(CreatureSpawner state) {
        super(state);
        spawnedType = state.getSpawnedType();
    }

    public EntityType getSpawnedType() {
        return spawnedType;
    }

    @Override
    public Type getType() {
        return Type.CREATURE_SPAWNER;
    }

    @Override
    protected void paste(CreatureSpawner state) {
        state.setSpawnedType(spawnedType);
    }

}
