package com.redstoner.parcels.api.schematic;

import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public abstract class SpecialType<T> {

    public static Consumer<BlockState> getConverter(BlockState state) {
        for (SpecialType<?> special : SPECIAL) {
            if (special.isInstance(state)) {
                return special.createConverter(state);
            }
        }
        return null;
    }

    private static final SpecialType<?>[] SPECIAL;

    private Class<T> type;

    private SpecialType(Class<T> type) {
        this.type = type;
    }

    private boolean isInstance(BlockState state) {
        return type.isInstance(state);
    }

    Consumer<BlockState> createConverter(BlockState origin) {
        return createConverter(type.cast(origin));
    }

    abstract Consumer<BlockState> createConverter(T origin);

    static {

        SPECIAL = new SpecialType[]{

                new SpecialType<InventoryHolder>(InventoryHolder.class) {

                    private ItemStack copyStack(ItemStack stack) {
                        return stack == null ? null : new ItemStack(stack);
                    }

                    @Override
                    Consumer<BlockState> createConverter(InventoryHolder origin) {
                        ItemStack[] contents = Arrays.stream(origin.getInventory().getContents()).map(this::copyStack).toArray(ItemStack[]::new);
                        return state -> ((InventoryHolder) state).getInventory().setContents(contents);
                    }

                },

                new SpecialType<Sign>(Sign.class) {

                    @Override
                    Consumer<BlockState> createConverter(Sign origin) {
                        String[] lines = origin.getLines();
                        return state -> {
                            Sign sign = (Sign) state;
                            for (int i = 0; i < 4; i++)
                                sign.setLine(i, lines[i]);
                        };
                    }

                },

                new SpecialType<Skull>(Skull.class) {

                    @Override
                    Consumer<BlockState> createConverter(Skull origin) {
                        String owner = origin.getOwner();
                        return state -> ((Skull) state).setOwner(owner);
                    }

                },

                new SpecialType<NoteBlock>(NoteBlock.class) {

                    @Override
                    Consumer<BlockState> createConverter(NoteBlock origin) {
                        Note note = origin.getNote();
                        return state -> ((NoteBlock) state).setNote(note);
                    }

                },

                new SpecialType<Jukebox>(Jukebox.class) {

                    @Override
                    Consumer<BlockState> createConverter(Jukebox origin) {
                        Material played = origin.getPlaying();
                        return state -> ((Jukebox) state).setPlaying(played);
                    }

                },

                new SpecialType<Banner>(Banner.class) {

                    @Override
                    Consumer<BlockState> createConverter(Banner origin) {
                        List<Pattern> patterns = new ArrayList<>(origin.getPatterns());
                        return state -> ((Banner) state).setPatterns(patterns);
                    }

                },

                new SpecialType<CommandBlock>(CommandBlock.class) {

                    @Override
                    Consumer<BlockState> createConverter(CommandBlock origin) {
                        String name = origin.getName();
                        String command = origin.getCommand();
                        return state -> {
                            CommandBlock block = (CommandBlock) state;
                            block.setName(name);
                            block.setCommand(command);
                        };
                    }

                },

                new SpecialType<CreatureSpawner>(CreatureSpawner.class) {

                    @Override
                    Consumer<BlockState> createConverter(CreatureSpawner origin) {
                        EntityType spawned = origin.getSpawnedType();
                        int delay = origin.getDelay();
                        return state -> {
                            CreatureSpawner spawner = (CreatureSpawner) state;
                            spawner.setSpawnedType(spawned);
                            spawner.setDelay(delay);
                        };
                    }

                },
        };
    }

}

