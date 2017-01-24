package com.redstoner.parcels.api;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.blockvisitor.VerticalRangeVisitor;
import com.redstoner.parcels.api.list.PlayerMap;
import com.redstoner.parcels.api.schematic.Schematic;
import io.dico.dicore.util.block.BlockPos;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class BlockOperations {

    private BlockOperations() {
        throw new UnsupportedOperationException();
    }

    private static void teleportRelative(Collection<Entity> entities, int dx, int dz) {
        for (Entity entity : entities) {
            if (entity.getType() != EntityType.PLAYER) {
                entity.teleport(entity.getLocation().add(dx, 0, dz), PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }
    }

    public static void clear(Parcel parcel, Runnable onFinish) {
        ParcelWorld world = parcel.getWorld();

        for (Entity entity : world.getEntities(parcel)) {
            if (entity.getType() != EntityType.PLAYER) {
                entity.remove();
            }
        }

        ParcelWorldSettings settings = world.getSettings();
        short fillId = settings.fillType.getId();
        byte fillData = settings.fillType.getData();
        short floorId = settings.floorType.getId();
        byte floorData = settings.floorType.getData();
        int floorHeight = settings.floorHeight;

        new VerticalRangeVisitor(parcel, 0, 256) {
            @Override
            protected boolean process(Block block) {
                int y = block.getY();
                if (y > floorHeight) {
                    block.setTypeIdAndData(0, (byte) 0, false);
                } else if (y < floorHeight) {
                    block.setTypeIdAndData(fillId, fillData, false);
                } else {
                    block.setTypeIdAndData(floorId, floorData, false);
                }
                return true;
            }

            @Override
            protected void finished(boolean early) {
                onFinish.run();
            }
        }.start();
    }

    /*
    public static void swap(Parcel parcel1, Parcel parcel2, Runnable onFinish) {
        ParcelWorld pworld = parcel1.getWorld();
        if (pworld != parcel2.getWorld()) {
            throw new IllegalArgumentException("Parcels not of same world");
        }

        World world = pworld.getWorld();
        final int dx = (parcel2.getX() - parcel1.getX()) * pworld.getSettings().sectionSize;
        final int dz = (parcel2.getZ() - parcel1.getZ()) * pworld.getSettings().sectionSize;
        if (dx == 0 && dz == 0) {
            return;
        }

        Map<SchematicBlock, Boolean> attachables = new LinkedHashMap<>();

        parcel2.incrementBlockVisitors();
        new BlockVisitor(parcel1) {
            @Override
            protected boolean process(Block first) {
                Block second = first.getRelative(dx, 0, dz);
                SchematicBlock firstSchem = new SchematicBlock(first);
                SchematicBlock secondSchem = new SchematicBlock(second);

                if (isAttachable(first)) {
                    attachables.put(firstSchem, true);
                    if (isAttachable(second)) {
                        attachables.put(secondSchem, false);
                    } else {
                        secondSchem.paste(world, -dx, 0, -dz);
                    }
                } else {
                    if (isAttachable(second)) {
                        attachables.put(secondSchem, false);
                    } else {
                        secondSchem.paste(world, -dx, 0, -dz);
                    }
                    firstSchem.paste(world, dx, 0, dz);
                }
                return true;
            }

            @Override
            protected void finished(boolean early) {
                parcel1.incrementBlockVisitors();
                Collection<Entity> entities1 = pworld.getEntities(parcel1);
                Collection<Entity> entities2 = pworld.getEntities(parcel2);
                teleportRelative(entities1, dx, dz);
                teleportRelative(entities2, -dx, -dz);
                ParcelsPlugin.getInstance().getParcelListener().entitiesSwapped(entities1, entities2, parcel1, parcel2);

                new IteratorTask<Map.Entry<SchematicBlock, Boolean>>(attachables.entrySet()) {
                    @Override
                    protected boolean process(Map.Entry<SchematicBlock, Boolean> entry) {
                        if (entry.getValue()) {
                            entry.getKey().paste(world, dx, 0, dz);
                        } else {
                            entry.getKey().paste(world, -dx, 0, -dz);
                        }
                        return true;
                    }

                    @Override
                    protected void onFinish(boolean early) {
                        parcel1.decrementBlockVisitors();
                        parcel2.decrementBlockVisitors();
                        onFinish.run();
                    }
                }.start(ParcelsPlugin.getInstance(), getPause(), getPause(), getWorkTime());
            }
        }.start();

        swapParcelInfo(parcel1, parcel2);
    }

    public static void coolSwap(Parcel parcel1, Parcel parcel2, Runnable onFinish) {
        ParcelWorld pworld = parcel1.getWorld();
        if (pworld != parcel2.getWorld()) {
            throw new IllegalArgumentException("Parcels not of same world");
        }

        final int parcelSize = pworld.getSettings().parcelSize;
        final int sectionSize = pworld.getSettings().sectionSize;
        final int dx = (parcel2.getX() - parcel1.getX()) * sectionSize;
        final int dz = (parcel2.getZ() - parcel1.getZ()) * sectionSize;
        if (dx == 0 && dz == 0) {
            onFinish.run();
            return;
        }

        SchematicBlock2 uselessInstance = SchematicBlock2.uselessInstance();
        World world = pworld.getWorld();
        SchematicBlock2[][][] cache1 = new SchematicBlock2[3][parcelSize][parcelSize];
        SchematicBlock2[][][] cache2 = new SchematicBlock2[3][parcelSize][parcelSize];
        Map<Material, SchematicBlock2> defaults = new EnumMap<>(Material.class);
        Map<BlockPos, SchematicBlock2> attachable = new LinkedHashMap<>();

        parcel2.incrementBlockVisitors();
        new VerticalRangePosVisitor(parcel1, -3, 256) {
            int previousY;

            @Override
            protected boolean process(BlockPos pos) {
                int x = pworld.toParcelSectionX(pos.getX());
                int y = pos.getY();
                int z = pworld.toParcelSectionZ(pos.getZ());
                if (y > -1) {
                    cache1[0][x][z].paste(world.getBlockAt(pos.getX() + dx, y, pos.getZ() + dz));
                    cache2[0][x][z].paste(world.getBlockAt(pos.getX(), y, pos.getZ()));
                }
                if (previousY != y) {
                    previousY = y;
                    rotateCache();
                }
                if (y < 253) {
                    Block above1 = pos.addY(3).getBlock();
                    Block above2 = above1.getRelative(dx, 0, dz);
                    if (Schematic.ATTACHABLE_MATERIALS.contains(above1.getType())) {
                        cache1[2][x][z] = uselessInstance;
                        attachable.put(new BlockPos(true, above1).add(dx, 0, dz).makeImmutable(), of(above1));
                    } else {
                        cache1[2][x][z] = of(above1);
                    }
                    if (Schematic.ATTACHABLE_MATERIALS.contains(above2.getType())) {
                        cache2[2][x][z] = uselessInstance;
                        attachable.put(new BlockPos(true, above2).add(-dx, 0, -dz).makeImmutable(), of(above2));
                    } else {
                        cache2[2][x][z] = of(above2);
                    }
                    pos.addY(-3);
                }
                return true;
            }

            private void rotateCache() {
                SchematicBlock2[][] temp = cache1[0];
                cache1[0] = cache1[1];
                cache1[1] = cache1[2];
                cache1[2] = temp;
                temp = cache2[0];
                cache2[0] = cache2[1];
                cache2[1] = cache2[2];
                cache2[2] = temp;
            }

            private SchematicBlock2 of(Block block) {
                if (block.getData() != 0) {
                    return SchematicBlock2.copy(block);
                }
                SchematicBlock2 result = defaults.get(block.getType());
                if (result == null) {
                    result = SchematicBlock2.copy(block);
                    if (result.hasMeta()) {
                        return result;
                    }
                    defaults.put(block.getType(), result);
                }
                return result;
            }

            @Override
            protected void finished(boolean early) {
                Collection<Entity> entities1 = pworld.getEntities(parcel1);
                Collection<Entity> entities2 = pworld.getEntities(parcel2);
                teleportRelative(entities1, dx, dz);
                teleportRelative(entities2, -dx, -dz);
                ParcelsPlugin.getInstance().getParcelListener().entitiesSwapped(entities1, entities2, parcel1, parcel2);

                new ParcelVisitor<Map.Entry<BlockPos, SchematicBlock2>>(parcel1) {
                    {
                        refresh(attachable.entrySet(), false);
                    }

                    @Override
                    protected boolean process(Map.Entry<BlockPos, SchematicBlock2> entry) {
                        entry.getValue().paste(entry.getKey().getBlock());
                        return true;
                    }

                    @Override
                    protected void finished(boolean early) {
                        parcel2.decrementBlockVisitors();
                        onFinish.run();
                    }
                }.start();

            }
        }.start();
        swapParcelInfo(parcel1, parcel2);
    }
    */

    public static void swapParcelInfo(Parcel parcel1, Parcel parcel2) {
        UUID owner1 = parcel1.getOwner().orElse(null);
        parcel1.setOwner(parcel2.getOwner().orElse(null));
        parcel2.setOwner(owner1);

        PlayerMap<Boolean> added1 = parcel1.getAdded();
        PlayerMap<Boolean> added2 = parcel2.getAdded();
        Map<UUID, Boolean> map1 = new HashMap<>(added1.getMap());
        added1.clear();
        added2.getMap().forEach(added1::add);
        added2.clear();
        map1.forEach(added2::add);

        ParcelSettings settings1 = parcel1.getSettings();
        ParcelSettings settings2 = parcel2.getSettings();
        boolean allowsInteractLever = settings1.allowsInteractInputs();
        boolean allowsInteractInventory = settings1.allowsInteractInventory();
        settings1.setAllowsInteractInputs(settings2.allowsInteractInputs());
        settings1.setAllowsInteractInventory(settings2.allowsInteractInventory());
        settings2.setAllowsInteractInputs(allowsInteractLever);
        settings2.setAllowsInteractInventory(allowsInteractInventory);
    }

    public static void newSwap(Parcel parcel1, Parcel parcel2, Runnable onFinish) {
        if (parcel1.getWorld() != parcel2.getWorld()) {
            throw new IllegalArgumentException();
        }
        parcel1.incrementBlockVisitors();
        parcel2.incrementBlockVisitors();
        Schematic schem1 = parcel1.initSchematic();
        Schematic schem2 = parcel2.initSchematic();

        BiConsumer<Schematic, Runnable> pasteConsumer = (schem, runnable) -> {
            BlockPos target = (schem == schem1 ? schem2 : schem1).getOrigin();
            schem.paste(target, runnable);
        };

        waitFinish(Schematic::load, schem1, schem2, () -> waitFinish(pasteConsumer, schem1, schem2, () -> {
            swapParcelInfo(parcel1, parcel2);
            swapEntities(parcel1, parcel2);
            parcel1.decrementBlockVisitors();
            parcel2.decrementBlockVisitors();
            onFinish.run();
        }));
    }

    private static void swapEntities(Parcel parcel1, Parcel parcel2) {
        ParcelWorld pworld = parcel1.getWorld();
        if (pworld != parcel2.getWorld()) {
            throw new IllegalArgumentException();
        }

        final int sectionSize = pworld.getSettings().sectionSize;
        final int dx = (parcel2.getX() - parcel1.getX()) * sectionSize;
        final int dz = (parcel2.getZ() - parcel1.getZ()) * sectionSize;
        if (dx == 0 && dz == 0) {
            return;
        }

        Collection<Entity> entities1 = parcel1.getWorld().getEntities(parcel1);
        Collection<Entity> entities2 = parcel1.getWorld().getEntities(parcel2);
        teleportRelative(entities1, dx, dz);
        teleportRelative(entities2, -dx, -dz);
        ParcelsPlugin.getInstance().getParcelListener().entitiesSwapped(entities1, entities2, parcel1, parcel2);
    }

    // run the consumer for both instances, and call onFinish when the last one exits
    private static <T> void waitFinish(BiConsumer<T, Runnable> consumer, T inst1, T inst2, Runnable onFinish) {
        Runnable fixer = new Runnable() {
            boolean firstFinished = false;

            @Override
            public void run() {
                if (!firstFinished) {
                    firstFinished = true;
                    return;
                }
                onFinish.run();
            }
        };
        consumer.accept(inst1, fixer);
        consumer.accept(inst2, fixer);
    }

}
