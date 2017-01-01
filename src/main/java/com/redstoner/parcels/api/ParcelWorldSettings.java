package com.redstoner.parcels.api;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.DuoObject.BlockType;
import com.redstoner.utils.ErrorPrinter;
import com.redstoner.utils.Maps.CastingMap;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ParcelWorldSettings {

    public final int axisLimit;
    public final GameMode gameMode;
    public final boolean staticTimeDay;
    public final boolean staticWeatherClear;
    public final boolean dropEntityItems;
    public final boolean doTileDrops;

    public final boolean disableExplosions;
    public final boolean blockPortalCreation;
    public final boolean blockMobSpawning;
    public final List<Material> blockedItems;

    public final BlockType wallType;
    public final BlockType floorType;
    public final BlockType fillType;
    public final BlockType pathMainType;
    public final BlockType pathEdgeType;
    public final int parcelSize;
    public final int floorHeight;
    public final int offsetX;
    public final int offsetZ;
    public final int sectionSize;
    public final int pathOffset;

    public final Biome defaultBiome;

    public final BlockType ownerWallBlockType;

    private ParcelWorldSettings(int axisLimit, GameMode gameMode, boolean staticTimeDay, boolean staticWeatherClear,
                                boolean dropEntityItems, boolean doTileDrops,

                                boolean disableExplosions, boolean blockPortalCreation, boolean blockMobSpawning, List<Material> itemsBlocked,

                                BlockType wallType, BlockType floorType, BlockType fillType, BlockType pathMainType, BlockType pathEdgeType,
                                int parcelSize, int pathSize, int floorHeight, int offsetX, int offsetZ, Biome defaultBiome) {

        this.axisLimit = axisLimit;
        this.gameMode = gameMode;
        this.staticTimeDay = staticTimeDay;
        this.staticWeatherClear = staticWeatherClear;
        this.dropEntityItems = dropEntityItems;
        this.doTileDrops = doTileDrops;

        // INTERACTION
        this.disableExplosions = disableExplosions;
        this.blockPortalCreation = blockPortalCreation;
        this.blockMobSpawning = blockMobSpawning;
        this.blockedItems = itemsBlocked;

        // GENERATOR
        this.wallType = wallType;
        this.floorType = floorType;
        this.fillType = fillType;
        this.pathMainType = pathMainType;
        this.pathEdgeType = pathEdgeType;
        this.parcelSize = parcelSize;
        this.floorHeight = floorHeight;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.sectionSize = parcelSize + pathSize;
        this.pathOffset = ((pathSize % 2 == 0) ? pathSize + 2 : pathSize + 1) / 2;
        this.defaultBiome = defaultBiome;

        this.ownerWallBlockType = getOwnerWallBlock(wallType);

    }

    private ParcelWorldSettings(CastingMap<String, Object> settings) {
        this(
                settings.getCasted("parcel-axis-limit"),
                settings.getCasted("game-mode"),
                settings.getCasted("static-time-day"),
                settings.getCasted("static-weather-clear"),
                settings.getCasted("drop-entity-items"),
                settings.getCasted("do-tile-drops"),

                settings.getCasted("interaction.disable-explosions"),
                settings.getCasted("interaction.block-portal-creation"),
                settings.getCasted("interaction.block-mob-spawning"),
                settings.getCasted("interaction.items-blocked"),

                settings.getCasted("generator.wall-type"),
                settings.getCasted("generator.floor-type"),
                settings.getCasted("generator.fill-type"),
                settings.getCasted("generator.path-main-type"),
                settings.getCasted("generator.path-edge-type"),
                settings.getCasted("generator.parcel-size"),
                settings.getCasted("generator.path-size"),
                settings.getCasted("generator.floor-height"),
                settings.getCasted("generator.offset-x"),
                settings.getCasted("generator.offset-z"),
                settings.getCasted("generator.default-biome")
        );

    }

    public static ParcelWorldSettings parseMap(String worldName, Map<String, Object> input, ErrorPrinter errorPrinter) {

        ParcelWorldSettings parsed = null;

        if (worldName.length() > 32) {
            errorPrinter.add("World names longer than 32 characters are not supported by Parcels.");
        }

        CastingMap<String, Object> settings = new CastingMap<>();

        WORLD_SETTINGS_PARSER.forEach((key, function) -> {

            String excPrefix = "Option '" + key + "' ";
            if (!input.containsKey(key)) {
                errorPrinter.add(excPrefix + "is missing from your settings. Aborting world.");
            } else {
                try {
                    settings.put(key, function.apply(input.get(key)));
                } catch (SettingParseException e) {
                    errorPrinter.add(excPrefix + "could not be parsed. Aborting world.");
                    errorPrinter.add("  Cause: " + e.getMessage());
                }
            }

        });

        if (!errorPrinter.willRun()) {
            parsed = new ParcelWorldSettings(settings);
        }

        input.keySet().stream()
                .filter(key -> !WORLD_SETTINGS_PARSER.containsKey(key))
                .filter(key -> !key.equals("interaction") && !key.equals("generator"))
                .forEach(key -> errorPrinter.add(String.format("Just FYI: Key '%s' isn't an option (Ignoring).", key)));

        return parsed;
    }

    public static Optional<ParcelWorldSettings> parseSettings(ConfigurationSection worlds, String worldName) {
        ErrorPrinter errorPrinter = new ErrorPrinter(ParcelsPlugin.getInstance()::error,
                String.format("Exception(s) occurred while loading settings for world '%s':", worldName));

        ParcelWorldSettings parsed = null;

        if (worlds.isConfigurationSection(worldName)) {

            parsed = parseMap(worldName, worlds.getConfigurationSection(worldName).getValues(true), errorPrinter);

        } else {
            errorPrinter.add("A world must be configured as a ConfigurationSection (a map).");
        }

        errorPrinter.runAll();
        return Optional.ofNullable(parsed);
    }

    private static final Map<String, Function<Object, Object>> WORLD_SETTINGS_PARSER = new HashMap<String, Function<Object, Object>>() {
        private static final long serialVersionUID = 1L;

        {
            Function<Object, Object> parseBlockType = obj -> {
                if (obj instanceof String) {
                    try {
                        return BlockType.fromString((String) obj);
                    } catch (NumberFormatException e) {
                        throw new SettingParseException("You have a number formatted incorrectly in your BlockType; " + e.getMessage());
                    } catch (NullPointerException e) {
                        throw new SettingParseException("A BlockType cannot be null");
                    }
                } else {
                    throw new SettingParseException("A BlockType must be formatted 'ID:DATA'");
                }
            };

            Function<Object, Object> checkInteger = obj -> {
                if (obj instanceof Integer) {
                    return obj;
                } else {
                    throw new SettingParseException("This must be an integer");
                }
            };

            Function<Object, Object> parseGameMode = obj -> {
                if (obj instanceof String) {
                    String query = ((String) obj).toUpperCase().replaceAll(" ", "_");
                    if (query.equals("NULL")) {
                        return null;
                    }
                    try {
                        return GameMode.valueOf(query);
                    } catch (IllegalArgumentException e) {
                        throw new SettingParseException("The game mode " + query + " does not exist."
                                + "See https://hub.spigotmc.org/javadocs/spigot/org/bukkit/GameMode.html");
                    }
                }
                throw new SettingParseException("This must be the name of the game mode");
            };

            Function<Object, Object> checkBoolean = obj -> {
                if (obj instanceof Boolean) {
                    return obj;
                } else {
                    throw new SettingParseException("This must be a boolean value");
                }
            };

            @SuppressWarnings("unchecked")
            Function<Object, Object> parseMaterialList = obj -> {
                try {
                    return ((List<String>) obj).stream().map(v -> {
                        Material type = Material.getMaterial(v);
                        checkNotNull(type, new SettingParseException(String.format("Material %s could not be parsed. "
                                + "See https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html", v)));
                        return type;
                    }).collect(Collectors.toList());
                } catch (ClassCastException e) {
                    throw new SettingParseException("This must be a list of material names");
                }
            };

            Function<Object, Object> parseBiome = obj -> {
                if (obj instanceof String) {
                    String query = ((String) obj).toUpperCase().replaceAll(" ", "_");
                    try {
                        return Biome.valueOf(query);
                    } catch (IllegalArgumentException e) {
                        throw new SettingParseException("The biome " + query + " does not exist."
                                + "See https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/Biome.html");
                    }
                }
                throw new SettingParseException("This must be a name for the biome");
            };

            put("parcel-axis-limit", checkInteger);
            put("game-mode", parseGameMode);
            put("static-time-day", checkBoolean);
            put("static-weather-clear", checkBoolean);
            put("drop-entity-items", checkBoolean);
            put("do-tile-drops", checkBoolean);

            put("interaction.disable-explosions", checkBoolean);
            put("interaction.block-portal-creation", checkBoolean);
            put("interaction.block-mob-spawning", checkBoolean);
            put("interaction.items-blocked", parseMaterialList);

            put("generator.wall-type", parseBlockType);
            put("generator.floor-type", parseBlockType);
            put("generator.fill-type", parseBlockType);
            put("generator.path-main-type", parseBlockType);
            put("generator.path-edge-type", parseBlockType);
            put("generator.parcel-size", checkInteger);
            put("generator.path-size", checkInteger);
            put("generator.floor-height", checkInteger);
            put("generator.offset-x", checkInteger);
            put("generator.offset-z", checkInteger);
            put("generator.default-biome", parseBiome);
        }
    };

    @SuppressWarnings("deprecation")
    private static BlockType getOwnerWallBlock(BlockType wallType) {
        switch (Material.getMaterial(wallType.getId())) {
            case CARPET:
                return new BlockType((short) Material.WOOL.getId(), wallType.getData());
            case STEP:
                return new BlockType((short) Material.DOUBLE_STEP.getId(), wallType.getData());
            case WOOD_STEP:
                return new BlockType((short) Material.WOOD_DOUBLE_STEP.getId(), wallType.getData());
            default:
                return wallType;
        }
    }

    private static class SettingParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public SettingParseException(String message) {
            super(message);
        }

    }
}
