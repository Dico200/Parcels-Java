package com.redstoner.parcels.api.schematic.block;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.bukkit.Material;
import org.bukkit.block.*;

public class BaseBlock {
    private static final TIntObjectMap<BaseBlock> cache = new TIntObjectHashMap<>(16, .75F, -1);
    private static final BaseBlock uselessInstance;
    private int typeId;

    private BaseBlock(int typeId) {
        this.typeId = typeId;
    }

    public BaseBlock(int typeId, byte data) {
        this.typeId = (typeId << 8) | data;
    }

    protected BaseBlock(Block block) {
        this(block.getTypeId(), block.getData());
    }

    public BaseBlock(Material type, byte data) {
        this(type.getId(), data);
    }

    public int getTypeId() {
        return typeId >> 8;
    }

    public int getData() {
        return typeId & 0xF;
    }

    public void paste(Block block) {
        block.setTypeIdAndData(getTypeId(), (byte) getData(), false);
    }

    public Type getType() {
        return Type.DEFAULT;
    }

    public static void clearCache() {
        cache.clear();
    }

    public static BaseBlock copy(Block block) {
        Material type = block.getType();
        switch (type) {
            case BANNER:
                return new BannerBlock((Banner) block.getState());
            case BEACON:
                return new BeaconBlock((Beacon) block.getState());
            case COMMAND:
                return new CommandBlockBlock((CommandBlock) block.getState());
            case MOB_SPAWNER:
                return new CreatureSpawnerBlock((CreatureSpawner) block.getState());
            case CHEST:
            case TRAPPED_CHEST:
                return new InventoryHolderBlock<>((Chest) block.getState());
            case DISPENSER:
                return new InventoryHolderBlock<>((Dispenser) block.getState());
            case DROPPER:
                return new InventoryHolderBlock<>((Dropper) block.getState());
            case HOPPER:
                return new InventoryHolderBlock<>((Hopper) block.getState());
            case BREWING_STAND:
                return new InventoryHolderBlock<>((BrewingStand) block.getState());
            case FURNACE:
            case BURNING_FURNACE:
                return new InventoryHolderBlock<>((Furnace) block.getState());
            case JUKEBOX:
                return new JukeboxBlock((Jukebox) block.getState());
            case NOTE_BLOCK:
                return new NoteBlockBlock((NoteBlock) block.getState());
            case SIGN_POST:
            case WALL_SIGN:
                return new SignBlock((Sign) block.getState());
            case SKULL:
                return new SkullBlock((Skull) block.getState());
            default: {
                int typeId = (type.getId() << 8) | block.getData();
                BaseBlock result = cache.get(typeId);
                if (result == null) {
                    result = new BaseBlock(typeId);
                    cache.put(typeId, result);
                }
                return result;
            }
        }
    }

    public enum Type {
        DEFAULT,
        BANNER,
        BEACON,
        COMMAND_BLOCK,
        CREATURE_SPAWNER,
        INVENTORY_HOLDER,
        JUKEBOX,
        NOTE_BLOCK,
        SIGN,
        SKULL;
    }

    static {
        uselessInstance = new BaseBlock(Material.AIR, (byte) 0) {
            @Override
            public void paste(Block block) {
                // do nothing
            }
        };
    }

}
