package com.redstoner.parcels;

import com.redstoner.command.Messaging;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.Permissions;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.Formatting;
import com.redstoner.utils.Optional;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class ParcelListener implements Listener {

    public static void register() {
        Bukkit.getPluginManager().registerEvents(new ParcelListener(), ParcelsPlugin.getInstance());
    }

    private int ignoreWeatherChanges;
    private final ConcurrentHashMap<Entity, Parcel> entities;

    private ParcelListener() {
        ignoreWeatherChanges = 0;
        entities = new ConcurrentHashMap<>();

		/*\
		 * Tracks entities. If the entity is dead, they are removed from the list.
		 * If the entity is found to have left the parcel it was created in, it will be removed from the world and from the list.
		 * If it is still in the parcel it was created in, and it is on the ground, it is removed from the list.
		 * 
		 * Start after 5 seconds, run every 0.25 seconds
		 */
        Bukkit.getScheduler().scheduleSyncRepeatingTask(ParcelsPlugin.getInstance(), () -> {
            entities.forEach((entity, firedFrom) -> {
                if (entity.isDead()) {
                    entities.remove(entity);
                } else if (WorldManager.getParcelAt(entity.getLocation()).orElse(null) != firedFrom) {
                    entity.remove();
                    entities.remove(entity);
                } else if (entity.isOnGround()) {
                    entities.remove(entity);
                }
            });
        }, 100, 5);

        ParcelsPlugin.getInstance().getServer().getWorlds().forEach(this::enforceWorldSettingsIfApplicable);
    }

    private static Location inventoryLocation(Inventory inv) {
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof BlockState) {
            return ((BlockState) holder).getLocation();
        } else if (inv instanceof Entity) {
            return ((Entity) inv).getLocation();
        } else {
            return null;
        }
    }

    private static void checkPistonAction(BlockPistonEvent event, List<Block> affectedBlocks) {
        Optional<ParcelWorld> maybeWorld = WorldManager.getWorld(event.getBlock().getWorld());
        if (!maybeWorld.isPresent())
            return;

        BlockFace direction = event.getDirection();
        Set<Coord> affectedColumns = new HashSet<>();
        for (Block block : affectedBlocks) {
            affectedColumns.add(Coord.of(block.getX(), block.getZ()));
            block = block.getRelative(direction);
            affectedColumns.add(Coord.of(block.getX(), block.getZ()));
        }

        ParcelWorld world = maybeWorld.get();

        for (Coord coord : affectedColumns) {
            if (!world.getParcelAt(coord.getX(), coord.getZ()).isPresent()) {
                event.setCancelled(true);
            }
        }

    }

    /*
     * Prevents players from entering plots they are banned from
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {

        Player user = event.getPlayer();
        if (!user.hasPermission(Permissions.ADMIN_BYPASS)) {
            Location to = event.getTo();
            WorldManager.getWorld(to.getWorld()).ifPresent(world -> {
                world.getParcelAt(to.getBlockX(), to.getBlockZ()).filter(parcel -> parcel.isBanned(user)).ifPresent(() -> {
                    Location from = event.getFrom();
                    world.getParcelAt(from.getBlockX(), from.getBlockZ()).ifPresentOrElse(parcel -> {
                        world.teleport(user, parcel);
                        Messaging.send(user, "Parcels", Formatting.YELLOW, "You are banned from this parcel");
                    }, () -> {
                        event.setTo(event.getFrom());
                    });
                });
            });
        }
    }

    /*
     * Prevents players from breaking blocks outside of their parcels
     * Prevents containers from dropping their contents when broken, if configured
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

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
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

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
    @EventHandler(ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {

        checkPistonAction(event, event.getBlocks());
    }

    /*
     * Prevents pistons from touching blocks outside parcels
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {

        checkPistonAction(event, event.getBlocks());
    }

    /*
     * Prevents explosions if enabled by the configs for that world
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {

        WorldManager.getWorld(event.getEntity().getLocation().getWorld()).filter(w -> w.getSettings().disableExplosions).ifPresent(() -> {
            event.setRadius(0);
        });
    }

    /*
     * Prevents creepers and tnt minecarts from exploding if explosions are disabled
     * Doesn't prevent breaking item frames ._. pls spigot, call event before item frames die
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {

        WorldManager.getWorld(event.getLocation().getWorld()).filter(w -> w.getSettings().disableExplosions).ifPresent(world -> {
            event.setCancelled(true);
        });

    }

    /*
     * Prevents liquids from flowing outside of parcels
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {

        WorldManager.getWorld(event.getToBlock().getWorld()).ifPresent(world -> {

            if (!world.getParcelAt(event.getToBlock()).isPresent()) {
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
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player user = event.getPlayer();

        WorldManager.getWorld(user.getWorld()).ifPresent(world -> {

            boolean hasAdminPerm = user.hasPermission(Permissions.ADMIN_BUILDANYWHERE);

            Block clickedB = event.getClickedBlock();
            Optional<Parcel> clickedP = clickedB == null ? Optional.empty() : world.getParcelAt(clickedB.getX(), clickedB.getZ());

            if (clickedP.filter(p -> p.isBanned(user)).isPresent()) {
                Messaging.send(user, "Parcels", Formatting.RED, "You cannot interact with parcels you're banned from");
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
                                Messaging.send(user, "Parcels", Formatting.YELLOW, "You cannot use inputs in this parcel");
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
                                    Messaging.send(user, "Parcels", Formatting.YELLOW, "You cannot use this bed because it would explode");
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
                            Messaging.send(user, "Parcels", Formatting.YELLOW, "That item is disabled in this world");
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
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Player user = event.getPlayer();
        if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
            return;

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
    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {

        Entity e = event.getEntity();
        if (WorldManager.getWorld(e.getWorld()).isPresent()) {
            if (e.getType() == EntityType.ENDERMAN) {
                event.setCancelled(true);
            } else if (e.getType() == EntityType.FALLING_BLOCK) {
                entities.put(e, WorldManager.getParcelAt(e.getLocation()).orElse(null));
            }
        }
    }

    /*
     * Prevents portals from being created if set so in the configs for that world
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityCreatePortal(EntityCreatePortalEvent event) {

        WorldManager.getWorld(event.getEntity().getWorld()).filter(world -> world.getSettings().blockPortalCreation).ifPresent(() -> event.setCancelled(true));
    }

    /*
     * Prevents players from dropping items
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        Player user = event.getPlayer();
        if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
            return;


        if (!WorldManager.isInOtherWorldOrInParcel(event.getItemDrop().getLocation(), p -> p.canBuild(user) || p.getSettings().allowsInteractInventory())) {
            event.setCancelled(true);
        }

    }

    /*
     * Prevents players from picking up items
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {

        Player user = event.getPlayer();
        if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
            return;

        WorldManager.getWorld(user.getWorld()).ifPresent(world -> {

            if (!world.getParcelAt(user.getLocation()).filter(p -> p.canBuild(user)).isPresent()) {
                event.setCancelled(true);
            }
        });
    }
	
	/*
	 * Prevents players from editing inventories
	 */

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {

        Player user = (Player) event.getWhoClicked();
        if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
            return;

        WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
            Inventory inv = event.getInventory();
            if (inv == null) // if hotbar, returns null
                return;

            InventoryHolder holder = inv.getHolder();
            if (holder instanceof Entity && ((Entity) holder).getType() == EntityType.PLAYER)
                return;

            Location loc = inventoryLocation(inv);
            if (loc == null)
                return;

            if (!world.getParcelAt(loc).filter(p -> p.canBuild(user) || p.getSettings().allowsInteractInventory()).isPresent()) {
                event.setCancelled(true);
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

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
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
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        enforceWorldSettingsIfApplicable(event.getWorld());
    }

    private void enforceWorldSettingsIfApplicable(World w) {
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
    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {

        Entity e = event.getEntity();
        WorldManager.getWorld(e.getWorld()).filter(world -> e instanceof LivingEntity && world.getSettings().blockMobSpawning).ifPresent(() -> event.setCancelled(true));
    }

    /*
     * Prevents minecarts/boats from moving outside a plot
     */
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {

        WorldManager.getWorld(event.getTo().getWorld()).ifPresent(world -> {

            if (!world.getParcelAt(event.getTo()).isPresent()) {
                Vehicle vehicle = event.getVehicle();
                Entity passenger = vehicle.getPassenger();
                if (passenger != null) {
                    vehicle.eject();
                    if (passenger.getType() == EntityType.PLAYER) {
                        vehicle.eject();
                        Messaging.send((Player) passenger, "Parcels", Formatting.RED, "Your ride ends here");
                    } else {
                        passenger.remove();
                    }
                }
                vehicle.remove();
            }

        });

    }

    /*
     * Prevents players from removing items from item frames
     * Prevents TNT Minecarts and creepers from destroying entities (This event is called BEFORE EntityExplodeEvent GG)
     * Actually doesn't prevent this because the entities are destroyed anyway, even though the code works?
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

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

    /*
     * Prevents players from deleting paintings and item frames
     * This appears to take care of shooting with a bow, throwing snowballs or throwing ender pearls.
     */
    @EventHandler
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {

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
    @EventHandler(ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {

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
    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {

        WorldManager.getWorld(event.getLocation().getWorld()).ifPresent(world -> {

            world.getParcelAt(event.getLocation()).ifPresentOrElse(parcel -> {

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
            }, () -> {
                event.setCancelled(true);
            });

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
    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {

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
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemSpawn(ItemSpawnEvent event) {

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
    @EventHandler(ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {

        Location from = event.getFrom();
        WorldManager.getWorld(from.getWorld()).ifPresent(world -> {
            world.getParcelAt(from).ifPresent(parcelFrom -> {
                if (!world.getParcelAt(event.getTo()).filter(parcelTo -> parcelTo == parcelFrom).isPresent()) {
                    event.setCancelled(true);
                }
            });
        });
    }

    /*
     * Prevents projectiles from flying out of parcels
     * Prevents players from firing projectiles if they cannot build
     */
    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {

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
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        WorldManager.getWorld(event.getEntity().getWorld()).filter(world -> !world.getSettings().dropEntityItems).ifPresent(() -> {
            event.getDrops().clear();
            event.setDroppedExp(0);
        });
    }

    /*
     * Assigns players their default game mode upon entering the world
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {

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
