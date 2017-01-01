package com.redstoner.parcels.api;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.list.PlayerMap;
import com.redstoner.parcels.api.schematic.Schematic;
import com.redstoner.parcels.api.schematic.SchematicBlock;
import io.dico.dicore.util.task.IteratorTask;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public final class BlockOperations {

    private BlockOperations() {
        throw new UnsupportedOperationException();
    }

    private static boolean isAttachable(Block block) {
        return Schematic.ATTACHABLE_MATERIALS.contains(block.getType());
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

        new BlockVisitor(parcel) {
            @Override
            protected boolean process(Block block) {
                int y = block.getY();
                if (y < floorHeight) {
                    block.setTypeId(fillId);
                    block.setData(fillData);
                } else if (y == floorHeight) {
                    block.setTypeId(floorId);
                    block.setData(floorData);
                } else if (y > floorHeight) {
                    block.setTypeId(0);
                    block.setData((byte) 0);
                }
                return true;
            }

            @Override
            protected void finished(boolean early) {
                onFinish.run();
            }
        }.start();
    }

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

}
