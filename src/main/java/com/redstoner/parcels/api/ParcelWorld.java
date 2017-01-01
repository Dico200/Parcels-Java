package com.redstoner.parcels.api;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.list.PlayerMap;
import com.redstoner.parcels.api.schematic.Schematic;
import com.redstoner.parcels.api.schematic.SchematicBlock;
import com.redstoner.parcels.generation.ParcelGenerator;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.UUIDUtil;
import com.redstoner.utils.Values;
import io.dico.dicore.util.generator.Generator;
import io.dico.dicore.util.generator.SimpleGenerator;
import io.dico.dicore.util.task.IteratorTask;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

public class ParcelWorld {
    private World world;
    private ParcelContainer parcels;
    private ParcelGenerator generator;
    private ParcelWorldSettings settings;
    private String name;

    public ParcelWorld(String name, ParcelWorldSettings settings) {
        this.generator = new ParcelGenerator(settings);
        this.settings = settings;
        this.name = name;
        this.parcels = new ParcelContainer(this, settings.axisLimit);
    }

    public ParcelWorldSettings getSettings() {
        return settings;
    }

    public ParcelGenerator getGenerator() {
        return generator;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        World result = world;
        if (result == null) {
            result = world = Bukkit.getWorld(name);
            if (result == null) {
                throw new NullPointerException("World " + name + " does not appear to be loaded");
            }
        }
        return result;
    }

    public Optional<Parcel> getParcelAt(int absX, int absZ) {
        int sectionSize = settings.sectionSize;
        absX -= settings.offsetX + settings.pathOffset;
        absZ -= settings.offsetZ + settings.pathOffset;
        int modX = Values.posModulo(absX, sectionSize);
        int modZ = Values.posModulo(absZ, sectionSize);
        return isOriginParcel(modX, modZ) ? Optional.ofNullable(parcels.getParcelAt((absX - modX) / sectionSize, (absZ - modZ) / sectionSize)) : Optional.empty();
    }

    public Optional<Parcel> getParcelAt(Location loc) {
        return getParcelAt(loc.getBlockX(), loc.getBlockZ());
    }

    public Optional<Parcel> getParcelAt(Block b) {
        return getParcelAt(b.getX(), b.getZ());
    }

    public Optional<Parcel> getParcelAtID(int px, int pz) {
        return parcels.isWithinBoundaryAt(px, pz) ? Optional.of(getParcelByID(px, pz)) : Optional.empty();
    }

    public Parcel getParcelByID(int px, int pz) {
        return parcels.getParcelAt(px, pz);
    }

    private boolean isOriginParcel(int x, int z) {
        return Values.inRange(x, 0, settings.parcelSize) && Values.inRange(z, 0, settings.parcelSize);
    }

    public boolean isInParcel(int absX, int absZ, int px, int pz) {
        int sectionSize = settings.sectionSize;
        absX -= settings.offsetX + settings.pathOffset + px * sectionSize;
        absZ -= settings.offsetZ + settings.pathOffset + pz * sectionSize;
        int modX = Values.posModulo(absX, sectionSize);
        int modZ = Values.posModulo(absZ, sectionSize);
        return isOriginParcel(modX, modZ);
    }

    public void teleport(Player user, Parcel parcel) {
        Coord home = getHomeCoord(parcel);
        user.teleport(new Location(getWorld(), home.getX(), settings.floorHeight + 1, home.getZ(), -90, 0));
    }

    public Coord getHomeCoord(Parcel parcel) {
        Coord NW = getBottomCoord(parcel);
        return Coord.of(NW.getX(), NW.getZ() + (settings.parcelSize - 1) / 2);
    }

    public Coord getBottomCoord(Parcel parcel) {
        return Coord.of(settings.sectionSize * parcel.getX() + settings.pathOffset + settings.offsetX,
                settings.sectionSize * parcel.getZ() + settings.pathOffset + settings.offsetZ);
    }

    @SuppressWarnings("unused")
    private Parcel fromNWCoord(Coord coord) {
        return parcels.getParcelAt((coord.getX() - settings.pathOffset - settings.offsetX) / settings.sectionSize,
                (coord.getZ() - settings.pathOffset - settings.offsetZ) / settings.sectionSize);
    }

    public Parcel[] getOwned(OfflinePlayer user) {
        return parcels.stream().filter(p -> p.isOwner(user)).toArray(Parcel[]::new);
    }

    public Optional<Parcel> getNextUnclaimed() {
        return Optional.ofNullable(parcels.nextUnclaimed());
    }

    public ParcelContainer getParcels() {
        return parcels;
    }

    //Resizes to current, old size in config
    public void setParcels(ParcelContainer parcels) {
        if (this.parcels.getAxisLimit() != parcels.getAxisLimit()) {
            this.parcels = ParcelContainer.resize(parcels, this, this.parcels.getAxisLimit());
        } else {
            this.parcels = parcels;
        }
    }

    public void refreshParcels() {
        this.parcels = new ParcelContainer(this, settings.axisLimit);
    }

    public Stream<Block> getBlocks(Parcel parcel) {
        Builder<Block> builder = Stream.builder();

        World world = getWorld();

        Coord NW = getBottomCoord(parcel);
        int x0 = NW.getX();
        int z0 = NW.getZ();

        int x, z, y;
        for (x = x0; x < x0 + settings.parcelSize; x++) {
            for (z = z0; z < z0 + settings.parcelSize; z++) {
                for (y = 0; y < 256; y++) {
                    builder.accept(world.getBlockAt(x, y, z));
                }
            }
        }

        return builder.build();
    }

    public Generator<Block> getAllBlocks(Parcel parcel) {
        // this queries the world for blocks "asynchronously" but getBlockAt is thread safe
        // because it simply returns a new instance of Block
        return new SimpleGenerator<Block>() {
            @Override
            protected void run() {
                World world = getWorld();

                Coord NW = getBottomCoord(parcel);
                int x0 = NW.getX();
                int z0 = NW.getZ();

                int x, z, y;
                for (x = x0; x < x0 + settings.parcelSize; x++) {
                    for (z = z0; z < z0 + settings.parcelSize; z++) {
                        for (y = 0; y < 256; y++) {
                            yield(world.getBlockAt(x, y, z));
                        }
                    }
                }

            }
        };
    }

    public void swap(Parcel parcel1, Parcel parcel2, Runnable onFinish) {
        if (parcel1.getWorld() != this || parcel2.getWorld() != this) {
            throw new IllegalArgumentException("Invalid world");
        }
        final int dx = (parcel2.getX() - parcel1.getX()) * settings.sectionSize;
        final int dz = (parcel2.getZ() - parcel1.getZ()) * settings.sectionSize;
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

                if (Schematic.ATTACHABLE_MATERIALS.contains(first.getType())) {
                    attachables.put(firstSchem, true);
                    if (Schematic.ATTACHABLE_MATERIALS.contains(second.getType())) {
                        attachables.put(secondSchem, false);
                    } else {
                        secondSchem.paste(world, -dx, 0, -dz);
                    }
                } else {
                    if (Schematic.ATTACHABLE_MATERIALS.contains(second.getType())) {
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
                parcel2.decrementBlockVisitors();
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
                        onFinish.run();
                    }
                }.start(ParcelsPlugin.getInstance(), BlockVisitor.getPause(), BlockVisitor.getPause(), BlockVisitor.getWorkTime());
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

    private void processAttachables(World world, Set<SchematicBlock> attachables, int dx, int dz, Runnable onFinish) {
        new IteratorTask<SchematicBlock>(attachables) {
            @Override
            protected boolean process(SchematicBlock block) {
                block.paste(world, dx, 0, dz);
                return true;
            }

            @Override
            protected void onFinish(boolean early) {
                onFinish.run();
            }
        }.start(ParcelsPlugin.getInstance(), BlockVisitor.getPause(), BlockVisitor.getPause(), BlockVisitor.getWorkTime());
    }

    public Collection<Entity> getEntities(Parcel parcel) {
        World world = getWorld();
        Coord NW = getBottomCoord(parcel);
        return Schematic.getContainedEntities(world, NW.getX(), 0, NW.getZ(), NW.getX() + settings.parcelSize, 255, NW.getZ() + settings.parcelSize);
    }

    @SuppressWarnings("deprecation")
    public void setOwnerSign(Parcel parcel) {
        World world = getWorld();
        Coord bottom = getBottomCoord(parcel);
        int bx = bottom.getX();
        int bz = bottom.getZ();

        Block wallBlock = world.getBlockAt(bx - 1, settings.floorHeight + 1, bz - 1);
        Block signBlock = world.getBlockAt(bx - 2, settings.floorHeight + 1, bz - 1);
        Block skullBlock = world.getBlockAt(bx - 1, settings.floorHeight + 2, bz - 1);
        Bukkit.getScheduler().runTask(ParcelsPlugin.getInstance(), () -> {

            Optional<UUID> optOwner = parcel.getOwner();
            if (optOwner.isPresent()) {
                UUID owner = optOwner.get();
                String ownerName = UUIDUtil.getName(owner);

                wallBlock.setTypeIdAndData(settings.ownerWallBlockType.getId(), settings.ownerWallBlockType.getData(), false);

                signBlock.setType(Material.WALL_SIGN);
                signBlock.setData((byte) 4);
                Sign sign = (Sign) signBlock.getState();
                sign.setLine(0, "ID: " + parcel.getId());
                sign.setLine(2, "Owner:");
                sign.setLine(3, ownerName);
                sign.update();

                skullBlock.setType(Material.SKULL);
                skullBlock.setData((byte) 1);
                Skull skull = (Skull) skullBlock.getState();
                skull.setOwner(ownerName);
                skull.setRotation(BlockFace.WEST);
                skull.update();

            } else {
                wallBlock.setTypeIdAndData(settings.wallType.getId(), settings.wallType.getData(), false);
                signBlock.setTypeIdAndData(0, (byte) 0, false);
                skullBlock.setTypeIdAndData(0, (byte) 0, false);
            }
        });
    }

    public void setBiome(Parcel parcel, Biome biome) {
        World world = getWorld();
        Coord bottom = getBottomCoord(parcel);
        int bx = bottom.getX();
        int bz = bottom.getZ();

        for (int x = bx; x < bx + settings.parcelSize; x++) {
            for (int z = bz; z < bz + settings.parcelSize; z++) {
                world.setBiome(x, z, biome);
            }
        }

    }

}
