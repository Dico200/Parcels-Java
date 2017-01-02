package com.redstoner.parcels;

import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.Permissions;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.DuoObject.Coord;
import io.dico.dicore.command.Formatting;
import io.dico.dicore.command.Messaging;
import io.dico.dicore.util.Registrator;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

import static org.bukkit.event.EventPriority.HIGHEST;
import static org.bukkit.event.EventPriority.NORMAL;

public final class ParcelListener {
    private int ignoreWeatherChanges;
    private final Map<Entity, Parcel> entities;

    public ParcelListener() {
        ignoreWeatherChanges = 0;
        entities = new HashMap<>();
    }

    public void register(Registrator r) {
        r.registerListener(PlayerMoveEvent.class, NORMAL, true, this::onPlayerMove);
        r.registerListener(BlockBreakEvent.class, NORMAL, true, this::onBlockBreak);
        r.registerListener(BlockPlaceEvent.class, NORMAL, true, this::onBlockPlace);
        r.registerListener(BlockPistonExtendEvent.class, NORMAL, true, this::onBlockPistonExtend);
        r.registerListener(BlockPistonRetractEvent.class, NORMAL, true, this::onBlockPistonRetract);
        r.registerListener(ExplosionPrimeEvent.class, NORMAL, true, this::onExplosionPrime);
        r.registerListener(EntityExplodeEvent.class, NORMAL, true, this::onEntityExplode);
        r.registerListener(BlockFromToEvent.class, NORMAL, true, this::onBlockFromTo);
        r.registerListener(PlayerInteractEvent.class, NORMAL, true, this::onPlayerInteract);
        r.registerListener(PlayerInteractEntityEvent.class, NORMAL, true, this::onPlayerInteractEntity);
        r.registerListener(EntityChangeBlockEvent.class, NORMAL, true, this::onEntityChangeBlock);
        r.registerListener(EntityCreatePortalEvent.class, NORMAL, true, this::onEntityCreatePortal);
        r.registerListener(PlayerDropItemEvent.class, NORMAL, true, this::onPlayerDropItem);
        r.registerListener(PlayerPickupItemEvent.class, NORMAL, true, this::onPlayerPickupItem);
        r.registerListener(InventoryClickEvent.class, NORMAL, true, this::onInventoryClick);
        r.registerListener(WeatherChangeEvent.class, NORMAL, this::onWeatherChange);
        r.registerListener(WorldLoadEvent.class, NORMAL, true, this::onWorldLoad);
        r.registerListener(EntitySpawnEvent.class, NORMAL, true, this::onEntitySpawn);
        r.registerListener(VehicleMoveEvent.class, NORMAL, this::onVehicleMove);
        r.registerListener(EntityDamageByEntityEvent.class, NORMAL, this::onEntityDamageByEntity);
        r.registerListener(HangingBreakEvent.class, NORMAL, true, this::onHangingBreak);
        r.registerListener(HangingBreakByEntityEvent.class, NORMAL, true, this::onHangingBreakByEntity);
        r.registerListener(HangingPlaceEvent.class, NORMAL, true, this::onHangingPlace);
        r.registerListener(StructureGrowEvent.class, NORMAL, true, this::onStructureGrow);
        r.registerListener(BlockDispenseEvent.class, NORMAL, this::onBlockDispense);
        r.registerListener(ItemSpawnEvent.class, HIGHEST, true, this::onItemSpawn);
        r.registerListener(EntityTeleportEvent.class, NORMAL, true, this::onEntityTeleport);
        r.registerListener(ProjectileLaunchEvent.class, NORMAL, true, this::onProjectileLaunch);
        r.registerListener(EntityDeathEvent.class, NORMAL, this::onEntityDeath);
        r.registerListener(PlayerChangedWorldEvent.class, NORMAL, this::onPlayerChangedWorld);
    }

    /*
     * Tracks entities. If the entity is dead, they are removed from the list.
     * If the entity is found to have left the parcel it was created in, it will be removed from the world and from the list.
     * If it is still in the parcel it was created in, and it is on the ground, it is removed from the list.
     *
     * Start after 5 seconds, run every 0.25 seconds
     */
    public void tick() {
        Iterator<Map.Entry<Entity, Parcel>> iterator = entities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, Parcel> entry = iterator.next();
            Entity entity = entry.getKey();
            if (entity.isDead() || entity.isOnGround()) {
                iterator.remove();
            } else {
                Parcel origin = entry.getValue();
                if (origin != null && origin.hasBlockVisitors()) {
                    iterator.remove();
                }
                Parcel parcel = WorldManager.getParcelAt(entity.getLocation()).orElse(null);
                if (!parcel.hasBlockVisitors() && parcel != entry.getValue()) {
                    entity.remove();
                    iterator.remove();
                }
            }
        }
    }

    public void entitiesSwapped(Collection<Entity> entities1, Collection<Entity> entities2, Parcel parcel1, Parcel parcel2) {
        for (Entity entity : entities1) {
            entities.computeIfPresent(entity, (key, parcel) -> parcel == parcel1 ? parcel2 : parcel);
        }
        for (Entity entity : entities2) {
            entities.computeIfPresent(entity, (key, parcel) -> parcel == parcel2 ? parcel1 : parcel);
        }
    }

    private static void checkPistonAction(BlockPistonEvent event, List<Block> affectedBlocks) {
        Optional<ParcelWorld> maybeWorld = WorldManager.getWorld(event.getBlock().getWorld());
        if (!maybeWorld.isPresent()) {
            return;
        }

        BlockFace direction = event.getDirection();
        Set<Coord> affectedColumns = new HashSet<>();
        for (Block block : affectedBlocks) {
            affectedColumns.add(Coord.of(block.getX(), block.getZ()));
            block = block.getRelative(direction);
            affectedColumns.add(Coord.of(block.getX(), block.getZ()));
        }

        ParcelWorld world = maybeWorld.get();
        for (Coord coord : affectedColumns) {
            Optional<Parcel> mparcel = world.getParcelAt(coord.getX(), coord.getZ());
            if (!mparcel.isPresent() || mparcel.get().hasBlockVisitors()) {
                event.setCancelled(true);
                return;
            }
        }

    }

    /*
     * Prevents players from entering plots they are banned from
     */
    private void onPlayerMove(PlayerMoveEvent event) {
        Player user = event.getPlayer();
        if (!user.hasPermission(Permissions.ADMIN_BYPASS)) {
            Location to = event.getTo();
            WorldManager.getWorld(to.getWorld()).ifPresent(world -> {
                if (world.getParcelAt(to.getBlockX(), to.getBlockZ()).filter(parcel -> parcel.isBanned(user)).isPresent()) {
                    Location from = event.getFrom();
                    Optional<Parcel> optParcel = world.getParcelAt(from.getBlockX(), from.getBlockZ());
                    if (optParcel.isPresent()) {
                        world.teleport(user, optParcel.get());
                        Messaging.send(user, "Parcels", Formatting.YELLOW, "You are banned from this parcel");
                    } else {
                        event.setTo(event.getFrom());
                    }
                }
            });
        }

    }

    /*
     * Prevents players from breaking blocks outside of their parcels
     * Prevents containers from dropping their contents when broken, if configured
     */
    private void onBlockBreak(BlockBreakEvent event) {
        WorldManager.ifWorldPresent(event.getBlock(), (world, maybeParcel) -> {
            Player user = event.getPlayer();

            if (!user.hasPermission(Permissions.ADMIN_BUILDANYWHERE) && !maybeParcel.filter(p -> p.canBuild(user)).isPresent()) {
                event.setCancelled(true);
            } else if (!world.getSettings().dropEntityItems) {
                BlockState state = event.getBlock().getState();
                if (state instanceof InventoryHolder) {
                    ((InventoryHolder) state).getInventory().clear();
                    state.update();
                }
            }

        });
    }

    /*
     * Prevents players from placing blocks outside of their parcels
     */
    private void onBlockPlace(BlockPlaceEvent event) {
        WorldManager.ifWorldPresent(event.getBlock(), (world, maybeParcel) -> {
            Player user = event.getPlayer();

            if (!user.hasPermission(Permissions.ADMIN_BUILDANYWHERE) && !maybeParcel.filter(p -> p.canBuild(user)).isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Prevents pistons from touching blocks outside parcels
     */
    private void onBlockPistonExtend(BlockPistonExtendEvent event) {
        checkPistonAction(event, event.getBlocks());
    }

    /*
     * Prevents pistons from touching blocks outside parcels
     */
    private void onBlockPistonRetract(BlockPistonRetractEvent event) {
        checkPistonAction(event, event.getBlocks());
    }

    /*
     * Prevents explosions if enabled by the configs for that world
     */
    private void onExplosionPrime(ExplosionPrimeEvent event) {
        Location loc = event.getEntity().getLocation();
        WorldManager.getWorld(loc.getWorld()).ifPresent(world -> {
            if (world.getParcelAt(loc).filter(Parcel::hasBlockVisitors).isPresent()) {
                event.setRadius(0);
                event.setCancelled(true);
            } else if (world.getSettings().disableExplosions) {
                event.setRadius(0);
            }
        });
    }

    /*
     * Prevents creepers and tnt minecarts from exploding if explosions are disabled
     */
    private void onEntityExplode(EntityExplodeEvent event) {
        entities.remove(event.getEntity());
        Location loc = event.getEntity().getLocation();
        WorldManager.getWorld(loc.getWorld()).ifPresent(world -> {
            if (world.getSettings().disableExplosions || world.getParcelAt(loc).filter(Parcel::hasBlockVisitors).isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Prevents liquids from flowing outside of parcels
     */
    private void onBlockFromTo(BlockFromToEvent event) {
        WorldManager.getWorld(event.getToBlock().getWorld()).ifPresent(world -> {
            Optional<Parcel> mparcel = world.getParcelAt(event.getToBlock());
            if (!mparcel.isPresent() || mparcel.get().hasBlockVisitors()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Prevents players from placing liquids, using flint and steel, changing redstone components,
     * using inputs (unless allowed by the plot),
     * and using items disabled in the configuration for that world.
     * Prevents player from using beds in HELL or SKY biomes if explosions are disabled.
     */
    @SuppressWarnings("deprecation")
    private void onPlayerInteract(PlayerInteractEvent event) {
        Player user = event.getPlayer();
        WorldManager.getWorld(user.getWorld()).ifPresent(world -> {

            boolean hasAdminPerm = user.hasPermission(Permissions.ADMIN_BUILDANYWHERE);
            Block clickedB = event.getClickedBlock();
            Optional<Parcel> clickedP = clickedB == null ? Optional.empty() : world.getParcelAt(clickedB.getX(), clickedB.getZ());

            if (clickedP.filter(p -> p.isBanned(user)).isPresent()) {
                Messaging.send(user, ParcelsPlugin.getInstance().getName(), Formatting.RED, "You cannot interact with parcels you're banned from");
                event.setCancelled(true);
                return;
            }

            Action action = event.getAction();
            switch (action) {
                case RIGHT_CLICK_BLOCK:

                    Material type = clickedB.getType();
                    switch (type) {
                        case DIODE_BLOCK_ON:
                        case DIODE_BLOCK_OFF:
                        case REDSTONE_COMPARATOR_OFF:
                        case REDSTONE_COMPARATOR_ON:
                            if (!hasAdminPerm && !clickedP.filter(p -> p.canBuild(user)).isPresent()) {
                                event.setCancelled(true);
                                return;
                            }
                            break;
                        case LEVER:
                        case STONE_BUTTON:
                        case WOOD_BUTTON:
                        case FENCE_GATE:
                        case WOODEN_DOOR:
                        case ANVIL:
                        case TRAP_DOOR:
                        case TRAPPED_CHEST:
                            //case REDSTONE_ORE:
                            if (!hasAdminPerm && !clickedP.filter(p -> p.canBuild(user) || p.getSettings().allowsInteractInputs()).isPresent()) {
                                Messaging.send(user, ParcelsPlugin.getInstance().getName(), Formatting.YELLOW, "You cannot use inputs in this parcel");
                                event.setCancelled(true);
                                return;
                            }
                            break;
                        case BED_BLOCK:

                            if (world.getSettings().disableExplosions) {
                                Block bedHead;
                                switch (clickedB.getData()) {
                                    case 0:
                                    case 4:
                                        bedHead = clickedB.getRelative(BlockFace.SOUTH);
                                        break;
                                    case 1:
                                    case 5:
                                        bedHead = clickedB.getRelative(BlockFace.WEST);
                                        break;
                                    case 2:
                                    case 6:
                                        bedHead = clickedB.getRelative(BlockFace.NORTH);
                                        break;
                                    case 3:
                                    case 7:
                                        bedHead = clickedB.getRelative(BlockFace.EAST);
                                        break;
                                    default:
                                        bedHead = clickedB;
                                        break;
                                }

                                if (bedHead.getType() == Material.BED_BLOCK && bedHead.getData() > 7 && (bedHead.getBiome() == Biome.HELL || bedHead.getBiome() == Biome.SKY)) {
                                    event.setCancelled(true);
                                    Messaging.send(user, ParcelsPlugin.getInstance().getName(), Formatting.YELLOW, "You cannot use this bed because it would explode");
                                    return;
                                }
                            }
                            break;
                        default:
                            break;
                    }
                    // no break

                case RIGHT_CLICK_AIR:

                    if (event.hasItem()) {
                        Material item = event.getItem().getType();
                        if (world.getSettings().blockedItems.contains(event.getItem().getType())) {
                            Messaging.send(user, ParcelsPlugin.getInstance().getName(), Formatting.YELLOW, "That item is disabled in this world");
                            event.setCancelled(true);
                            return;
                        } else if (!hasAdminPerm && !clickedP.filter(p -> p.canBuild(user)).isPresent()) {
                            switch (item) {
                                case LAVA_BUCKET:
                                case WATER_BUCKET:
                                case BUCKET:
                                case FLINT_AND_STEEL:
                                    event.setCancelled(true);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    break;

                case PHYSICAL:

                    if (!hasAdminPerm && !clickedP.filter(p -> p.canBuild(user) || p.getSettings().allowsInteractInputs()).isPresent()) {
                        event.setCancelled(true);
                        return;
                    }
                    break;

                default:
                    break;
            }
        });
    }

    /*
     * Prevents players from breeding mobs, entering or opening boats/minecarts,
     * rotating item frames, doing stuff with leashes, and putting stuff on armor stands.
     */
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player user = event.getPlayer();
        if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE)) {
            return;
        }

        WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
            if (world.getParcelAt(user.getLocation()).filter(p -> p.canBuild(user)).isPresent()) {
                return;
            }

            switch (event.getRightClicked().getType()) {
                case BOAT:
                case MINECART:
                case MINECART_CHEST:
                case MINECART_COMMAND:
                case MINECART_FURNACE:
                case MINECART_HOPPER:
                case MINECART_MOB_SPAWNER:
                case MINECART_TNT:

                case ARMOR_STAND:
                case PAINTING:
                case ITEM_FRAME:
                case LEASH_HITCH:

                case CHICKEN:
                case COW:
                case HORSE:
                case SHEEP:
                case VILLAGER:
                case WOLF:
                    event.setCancelled(true);
                    break;
                default:
                    break;
            }
        });
    }

    /*
     * Prevents endermen from griefing.
     */
    private void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Entity e = event.getEntity();
        WorldManager.getWorld(e.getWorld()).ifPresent(world -> {
            if (e.getType() == EntityType.ENDERMAN || world.getParcelAt(e.getLocation()).filter(Parcel::hasBlockVisitors).isPresent()) {
                event.setCancelled(true);
            } else if (e.getType() == EntityType.FALLING_BLOCK) {
                entities.put(e, WorldManager.getParcelAt(e.getLocation()).orElse(null));
            }
        });
    }

    /*
     * Prevents portals from being created if set so in the configs for that world
     */
    private void onEntityCreatePortal(EntityCreatePortalEvent event) {
        if (WorldManager.getWorld(event.getEntity().getWorld()).filter(world -> world.getSettings().blockPortalCreation).isPresent()) {
            event.setCancelled(true);
        }
    }

    /*
     * Prevents players from dropping items
     */
    private void onPlayerDropItem(PlayerDropItemEvent event) {
        Player user = event.getPlayer();
        if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE)) {
            return;
        }

        if (!WorldManager.isInOtherWorldOrInParcel(event.getItemDrop().getLocation(), p -> p.canBuild(user) || p.getSettings().allowsInteractInventory())) {
            event.setCancelled(true);
        }
    }

    /*
     * Prevents players from picking up items
     */
    private void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player user = event.getPlayer();
        if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE)) {
            return;
        }

        WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
            if (!world.getParcelAt(user.getLocation()).filter(p -> p.canBuild(user)).isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Prevents players from editing inventories
     */
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getWhoClicked().hasPermission(Permissions.ADMIN_BUILDANYWHERE)) {
            return;
        }

        Player user = (Player) event.getWhoClicked();
        WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
            Inventory inv = event.getInventory();
            if (inv == null) {
                // if hotbar, returns null
                return;
            }

            InventoryHolder holder = inv.getHolder();
            if (holder == user) {
                return;
            }

            Location loc = inv.getLocation();
            if (loc == null) {
                return;
            }

            if (!world.getParcelAt(loc).filter(p -> p.canBuild(user) || p.getSettings().allowsInteractInventory()).isPresent()) {
                event.setResult(Event.Result.DENY);
            }
        });
    }

    /*
     * Cancels weather changes and sets the weather to sunny if requested by the config for that world.
     */
    private void resetWeather(World w) {
        ignoreWeatherChanges++;
        w.setStorm(false);
        w.setThundering(false);
        w.setWeatherDuration(Integer.MAX_VALUE);
    }

    private void onWeatherChange(WeatherChangeEvent event) {
        WorldManager.getWorld(event.getWorld()).filter(world -> world.getSettings().staticWeatherClear).ifPresent(world -> {
            if (ignoreWeatherChanges > 0) {
                ignoreWeatherChanges--;
            } else {
                event.setCancelled(true);
                resetWeather(world.getWorld());
            }
        });
    }

    /*
     * Sets time to day and doDayLightCycle gamerule if requested by the config for that world
     * Sets the weather to sunny if requested by the config for that world.
     */
    private void onWorldLoad(WorldLoadEvent event) {
        enforceWorldSettingsIfApplicable(event.getWorld());
    }

    public void enforceWorldSettingsIfApplicable(World w) {
        WorldManager.getWorld(w).ifPresent(world -> {

            if (world.getSettings().staticTimeDay) {
                w.setGameRuleValue("doDaylightCycle", "false");
                w.setTime(6000);
            }

            if (world.getSettings().staticWeatherClear) {
                resetWeather(w);
            }

            w.setGameRuleValue("doTileDrops", Boolean.toString(world.getSettings().doTileDrops));
        });
    }

    /*
     * Prevents mobs (living entities) from spawning if that is disabled for that world in the config.
     */
    private void onEntitySpawn(EntitySpawnEvent event) {
        Location loc = event.getEntity().getLocation();
        WorldManager.getWorld(loc.getWorld()).ifPresent(world -> {
            if (event.getEntity() instanceof Creature && world.getSettings().blockMobSpawning) {
                event.setCancelled(true);
            } else if (world.getParcelAt(loc).filter(Parcel::hasBlockVisitors).isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Prevents minecarts/boats from moving outside a plot
     */
    private void onVehicleMove(VehicleMoveEvent event) {
        WorldManager.getWorld(event.getTo().getWorld()).ifPresent(world -> {
            Optional<Parcel> mparcel = world.getParcelAt(event.getTo());
            if (!mparcel.isPresent()) {
                Vehicle vehicle = event.getVehicle();
                Entity passenger = vehicle.getPassenger();
                if (passenger != null) {
                    vehicle.eject();
                    if (passenger.getType() == EntityType.PLAYER) {
                        vehicle.eject();
                        Messaging.send(passenger, ParcelsPlugin.getInstance().getName(), Formatting.RED, "Your ride ends here");
                    } else {
                        passenger.remove();
                    }
                }
                vehicle.remove();
            } else if (mparcel.get().hasBlockVisitors()) {
                // attempt to cancel the event?
                event.getTo().add(event.getFrom()).subtract(event.getTo());
            }

        });
    }

    /*
     * Prevents players from removing items from item frames
     * Prevents TNT Minecarts and creepers from destroying entities (This event is called BEFORE EntityExplodeEvent GG)
     * Actually doesn't prevent this because the entities are destroyed anyway, even though the code works?
     */
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        WorldManager.getWorld(damaged.getWorld()).ifPresent(world -> {
            Entity damager = event.getDamager();

            if (world.getSettings().disableExplosions && damager instanceof ExplosiveMinecart || damager instanceof Creeper) {
                event.setCancelled(true);
                return;
            }

            Player user;
            if (damager instanceof Player) {
                user = (Player) damager;
            } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
                user = (Player) ((Projectile) damager).getShooter();
            } else {
                return;
            }

            if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE)) {
                return;
            }

            if (!world.getParcelAt(damaged.getLocation()).filter(p -> p.canBuild(user)).isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    private void onHangingBreak(HangingBreakEvent event) {
        Location loc = event.getEntity().getLocation();
        WorldManager.getWorld(loc.getWorld()).ifPresent(world -> {
            if ((event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION && world.getSettings().disableExplosions)
                    || world.getParcelAt(loc).filter(Parcel::hasBlockVisitors).isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Prevents players from deleting paintings and item frames
     * This appears to take care of shooting with a bow, throwing snowballs or throwing ender pearls.
     */
    private void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        Entity hanging = event.getEntity();
        WorldManager.getWorld(hanging.getWorld()).ifPresent(world -> {

            Entity remover = event.getRemover();
            if (remover instanceof Player) {
                Player user = (Player) remover;

                if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE)) {
                    return;
                }

                if (!world.getParcelAt(hanging.getLocation()).filter(p -> p.canBuild(user)).isPresent()) {
                    event.setCancelled(true);
                }
            }

        });
    }

    /*
     * Prevents players from placing paintings and item frames
     */
    private void onHangingPlace(HangingPlaceEvent event) {
        Player user = event.getPlayer();
        if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
            return;

        WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
            Block b = event.getBlock().getRelative(event.getBlockFace());
            if (!world.getParcelAt(b).filter(p -> p.canBuild(user)).isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Prevents stuff from growing outside of plots
     */
    private void onStructureGrow(StructureGrowEvent event) {
        WorldManager.getWorld(event.getLocation().getWorld()).ifPresent(world -> {

            Optional<Parcel> optParcel = world.getParcelAt(event.getLocation());
            if (optParcel.isPresent()) {
                Parcel parcel = optParcel.get();
                Player user = event.getPlayer();
                if (!user.hasPermission(Permissions.ADMIN_BUILDANYWHERE) && !parcel.canBuild(user)) {
                    event.setCancelled(true);
                    return;
                }

                List<BlockState> blocks = event.getBlocks();
                for (BlockState block : new ArrayList<>(blocks)) {
                    if (!world.getParcelAt(block.getBlock()).filter(p -> p == parcel).isPresent()) {
                        blocks.remove(block);
                    }
                }

            } else {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Prevents dispensers/droppers from dispensing out of parcels
     */
    private static BlockFace getDispenserFace(byte data) {
        switch (data) {
            case 0:
            case 6:
            case 8:
            case 14:
                return BlockFace.DOWN;
            case 1:
            case 7:
            case 9:
            case 15:
                return BlockFace.UP;
            case 2:
            case 10:
                return BlockFace.NORTH;
            case 3:
            case 11:
                return BlockFace.SOUTH;
            case 4:
            case 12:
                return BlockFace.WEST;
            case 5:
            case 13:
                return BlockFace.EAST;
            default:
                return null;
        }
    }

    @SuppressWarnings("deprecation")
    private void onBlockDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DISPENSER && block.getType() != Material.DROPPER)
            return;

        WorldManager.getWorld(block.getWorld()).ifPresent(world -> {

            if (!world.getParcelAt(block.getRelative(getDispenserFace(block.getData()))).isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Puts spawned items into the entities list, making sure they don't leave the parcel.
     */
    private void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        WorldManager.getWorld(item.getWorld()).ifPresent(world -> {

            Parcel parcelFrom = world.getParcelAt(item.getLocation()).orElse(null);
            if (parcelFrom == null) {
                event.setCancelled(true);
            } else {
                entities.put(item, parcelFrom);
            }
        });
    }

    /*
     * Prevents endermen and endermite from teleporting outside their parcel
     */
    private void onEntityTeleport(EntityTeleportEvent event) {
        Location from = event.getFrom();
        WorldManager.getParcelAt(from).ifPresent(parcelFrom -> {
            if (parcelFrom.hasBlockVisitors()) {
                return;
            }
            if (!parcelFrom.getWorld().getParcelAt(event.getTo()).filter(parcelTo -> parcelTo == parcelFrom).isPresent()) {
                event.setCancelled(true);
            }
        });
    }

    /*
     * Prevents projectiles from flying out of parcels
     * Prevents players from firing projectiles if they cannot build
     */
    private void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile arrow = event.getEntity();
        WorldManager.getWorld(arrow.getWorld()).ifPresent(world -> {

            Parcel firedFrom = world.getParcelAt(arrow.getLocation()).orElse(null);
            if (firedFrom == null || (arrow.getShooter() instanceof Player && !firedFrom.canBuild((Player) arrow.getShooter()))) {
                event.setCancelled(true);
            } else {
                entities.put(arrow, firedFrom);
            }
        });
    }

    /*
     * Prevents entities from dropping items upon death, if configured that way
     */
    private void onEntityDeath(EntityDeathEvent event) {
        entities.remove(event.getEntity());
        if (WorldManager.getWorld(event.getEntity().getWorld()).filter(world -> !world.getSettings().dropEntityItems).isPresent()) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    /*
     * Assigns players their default game mode upon entering the world
     */
    private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player user = event.getPlayer();
        if (user.hasPermission(Permissions.ADMIN_BYPASS_GAMEMODE)) {
            return;
        }

        WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
            GameMode defaultMode = world.getSettings().gameMode;
            if (defaultMode != null) {
                user.setGameMode(defaultMode);
            }
        });
    }
}
