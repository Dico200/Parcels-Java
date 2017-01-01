package com.redstoner.parcels.command;

import com.redstoner.parcels.api.*;
import com.redstoner.parcels.api.storage.StorageManager;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.UUIDUtil;
import io.dico.dicore.command.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ParcelCommands {
    private final String prefix;

    public ParcelCommands(String prefix) {
        this.prefix = prefix;
    }

    public void register() {
        CommandManager.register(prefix, PARCEL);
        CommandManager.register(PARCEL_AUTO);
        CommandManager.register(PARCEL_INFO);
        CommandManager.register(PARCEL_HOME);
        CommandManager.register(PARCEL_CLAIM);
        CommandManager.register(PARCEL_OPTION);
        CommandManager.register(PARCEL_OPTION_INPUTS);
        CommandManager.register(PARCEL_OPTION_INVENTORY);
        CommandManager.register(PARCEL_ALLOW);
        CommandManager.register(PARCEL_DISALLOW);
        CommandManager.register(PARCEL_BAN);
        CommandManager.register(PARCEL_UNBAN);
        CommandManager.register(PARCEL_GLOBAL);
        CommandManager.register(PARCEL_GLOBAL_ALLOW);
        CommandManager.register(PARCEL_GLOBAL_DISALLOW);
        CommandManager.register(PARCEL_GLOBAL_BAN);
        CommandManager.register(PARCEL_GLOBAL_UNBAN);
        CommandManager.register(PARCEL_SETOWNER);
        CommandManager.register(PARCEL_DISPOSE);
        CommandManager.register(PARCEL_TP);
        CommandManager.register(PARCEL_RESET);
        CommandManager.register(PARCEL_CLEAR);
        CommandManager.register(PARCEL_SWAP);
        CommandManager.register(PARCEL_RANDOM);
        CommandManager.register(PARCEL_SETBIOME);
        CommandManager.register(PARCEL_TPWORLD);
    }

    private final ParameterType<Coord> PARCEL_PARAMETER_TYPE = new ParameterType<Coord>("Parcel", "the ID of a parcel") {

        @Override
        protected Coord handle(String input) {
            String[] both = input.split(":");
            Validate.isTrue(both.length == 2, exceptionMessage());
            int x, z;
            try {
                x = Integer.parseInt(both[0]);
                z = Integer.parseInt(both[1]);
            } catch (NumberFormatException e) {
                throw new CommandException(exceptionMessage());
            }
            return Coord.of(x, z);
        }

    };

    private final Command PARCEL = new Command("parcel") {
        {
            setPermission(Permissions.PARCEL_COMMAND);
            setDescription("manages your parcels");
            setAliases("plot", "p");
            setOnSyntaxRequest(CommandAction.CONTINUE);
            setHelpInformation("The command for anything parcel-related");
        }

        @Override
        protected String execute(CommandSender sender, CommandScape scape) {
            return "EXEC:CommandAction.DISPLAY_HELP";
        }

    };

    private final Command PARCEL_AUTO = new ParcelCommand("parcel auto", ParcelRequirement.IN_WORLD) {
        {
            setDescription("sets you up with a fresh, unclaimed parcel");
            setHelpInformation("Finds the unclaimed parcel nearest to origin,", "and gives it to you");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Validate.isTrue(StorageManager.connected, "You cannot claim parcels right now.");
            ParcelWorld w = scape.getWorld();
            Validate.isTrue(w.getOwned(sender).length < Permissions.getParcelLimit(sender), "You have enough plots for now");
            Optional<Parcel> p = scape.getWorld().getNextUnclaimed();
            Validate.isTrue(p.isPresent(), "This world is full, please ask an admin to upsize it");
            p.get().setOwner(sender.getUniqueId());
            w.teleport(sender, p.get());
            return "Enjoy your new parcel!";
        }

    };

    private final Command PARCEL_INFO = new ParcelCommand("parcel info", ParcelRequirement.IN_PARCEL) {
        {
            setDescription("displays information about this parcel");
            setHelpInformation("Displays general information", "about the parcel you're on");
            setAliases("i");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Parcel parcel = scape.getParcel();
            Validate.isTrue(parcel.getOwner().isPresent(), String.format("This parcel with ID (%s) is unowned", parcel.getId()));
            return parcel.getInfo();
        }

    };

    private final Command PARCEL_HOME = new ParcelCommand("parcel home", ParcelRequirement.IN_WORLD) {
        {
            setDescription("teleports you to parcels");
            setHelpInformation("Teleports you to your parcels,", "unless another player was specified.", "You can specify an index number if you have", "more than one parcel");
            setAliases("h");
            setParameters(new Parameter<Integer>("id", ParameterType.INTEGER, "the home id of your parcel", false),
                    new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player whose parcels to teleport to", false));
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            int number = scape.<Integer>getOptional("id").orElse(0);
            OfflinePlayer owner = scape.<OfflinePlayer>getOptional("player").orElse(sender);
            Validate.isTrue(owner == sender || sender.hasPermission(Permissions.PARCEL_HOME_OTHERS), "You do not have permission to teleport to other people's parcels");
            Parcel[] owned = scape.getWorld().getOwned(owner);
            Validate.isTrue(number < owned.length, "That parcel id does not exist, they have " + owned.length + " parcels");
            scape.getWorld().teleport(sender, owned[number]);
            return String.format("Teleported you to %s's parcel %s", owner.getName(), number);
        }

        @Override
        protected List<String> tabComplete(Player sender, ParcelScape scape) {
            if (scape.original().length > 0) {
                return scape.proposals();
            }
            return IntStream.iterate(0, i -> i + 1).limit(scape.getWorld().getOwned(sender).length).mapToObj(Integer::toString).collect(Collectors.toList());
        }

    };

    private final Command PARCEL_CLAIM = new ParcelCommand("parcel claim", ParcelRequirement.IN_PARCEL) {
        {
            setDescription("claims this parcel");
            setHelpInformation("If this parcel is unowned, makes you the owner");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Validate.isTrue(StorageManager.connected, "You cannot claim parcels right now.");
            Parcel p = scape.getParcel();
            Validate.isTrue(!p.isClaimed(), "This parcel is not available");
            ParcelWorld w = scape.getWorld();
            Validate.isTrue(w.getOwned(sender).length < Permissions.getParcelLimit(sender), "You have enough plots for now");
            p.setOwner(sender.getUniqueId());
            return "Enjoy your new parcel!";
        }

    };

    private final Command PARCEL_OPTION = new ParcelCommand("parcel option", ParcelRequirement.IN_WORLD) {
        {
            setDescription("changes interaction options for this parcel");
            setHelpInformation("Sets whether players who are not allowed to", "build here can interact with certain things.");
            setOnSyntaxRequest(CommandAction.CONTINUE);
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            return "EXEC:CommandAction.DISPLAY_HELP";
        }

    };

    private final Command PARCEL_OPTION_INPUTS = new ParcelCommand("parcel option inputs", ParcelRequirement.IN_PARCEL) {
        {
            setDescription("allows using inputs");
            setHelpInformation("Sets whether players who are not allowed to", "build here can use levers, buttons," + "pressure plates, tripwire or redstone ore");
            setParameters(new Parameter<Boolean>("enabled", ParameterType.BOOLEAN, "whether the option is enabled", false));
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Boolean enabled = scape.get("enabled");
            Parcel p = scape.getParcel();
            if (enabled == null) {
                String word = p.getSettings().allowsInteractInputs() ? "" : "not ";
                return "This parcel does " + word + "allow using levers, buttons, etc.";
            }
            Validate.isTrue(sender.hasPermission(Permissions.ADMIN_MANAGE) || p.isOwner(sender), "You must own this parcel to change its options");
            String word = enabled ? "enabled" : "disabled";
            Validate.isTrue(scape.getParcel().getSettings().setAllowsInteractInputs(enabled), "That option was already " + word);
            return "That option is now " + word;
        }

    };

    private final Command PARCEL_OPTION_INVENTORY = new ParcelCommand("parcel option inventory", ParcelRequirement.IN_PARCEL) {
        {
            setDescription("allows editing inventories");
            setHelpInformation("Sets whether players who are not allowed to", "build here can interact with inventories");
            setParameters(new Parameter<Boolean>("enabled", ParameterType.BOOLEAN, "whether the option is enabled", false));
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Boolean enabled = scape.get("enabled");
            Parcel p = scape.getParcel();
            if (enabled == null) {
                String word = p.getSettings().allowsInteractInventory() ? "" : "not ";
                return "This parcel does " + word + "allow interaction with inventories";
            }
            Validate.isTrue(sender.hasPermission(Permissions.ADMIN_MANAGE) || p.isOwner(sender), "You must own this parcel to change its options");
            String word = enabled ? "enabled" : "disabled";
            Validate.isTrue(scape.getParcel().getSettings().setAllowsInteractInventory(enabled), "That option was already " + word);
            return "That option is now " + word;
        }

    };

    private final Command PARCEL_ALLOW = new ParcelCommand("parcel allow", ParcelRequirement.IN_OWNED) {
        {
            setDescription("allows a player to build on this parcel");
            setHelpInformation("Allows a player to build on this parcel");
            setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to allow"));
            setAliases("add", "permit");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Parcel parcel = scape.getParcel();
            Validate.isTrue(parcel.getOwner().isPresent(), String.format("This parcel with ID (%s) is unowned", parcel.getId()));
            OfflinePlayer allowed = scape.get("player");
            Validate.isTrue(!parcel.getOwner().filter(owner -> owner.equals(allowed.getUniqueId())).isPresent()
                            && parcel.getAdded().add(allowed.getUniqueId(), true),
                    allowed.getName() + " is already allowed to build on this parcel");
            return allowed.getName() + " is now allowed to build on this parcel";
        }

    };

    private final Command PARCEL_DISALLOW = new ParcelCommand("parcel disallow", ParcelRequirement.IN_OWNED) {
        {
            setDescription("disallows a player to build on this parcel");
            setHelpInformation("Disallows a player to build on this parcel,", "they won't be allowed to anymore");
            setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to disallow"));
            setAliases("remove", "forbid");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            OfflinePlayer forbidden = scape.get("player");
            Validate.isTrue(scape.getParcel().getAdded().remove(forbidden.getUniqueId(), true),
                    forbidden.getName() + " wasn't allowed to build on this parcel");
            return forbidden.getName() + " is no longer allowed to build on this parcel";
        }

    };

    private final Command PARCEL_BAN = new ParcelCommand("parcel ban", ParcelRequirement.IN_OWNED) {
        {
            setDescription("bans a player from this parcel");
            setHelpInformation("Bans a player from this parcel,", "making them unable to enter");
            setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to ban"));
            setAliases("deny");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Parcel parcel = scape.getParcel();
            Validate.isTrue(parcel.getOwner().isPresent(), String.format("This parcel with ID (%s) is unowned", parcel.getId()));
            OfflinePlayer banned = scape.get("player");
            Validate.isTrue(!parcel.getOwner().filter(owner -> owner.equals(banned.getUniqueId())).isPresent(),
                    "The owner of this parcel cannot be banned from it");
            Validate.isTrue(parcel.getAdded().add(banned.getUniqueId(), false), banned.getName() + " is already banned from this parcel");
            return banned.getName() + " is now banned from this parcel";
        }

    };

    private final Command PARCEL_UNBAN = new ParcelCommand("parcel unban", ParcelRequirement.IN_OWNED) {
        {
            setDescription("unbans a player from this parcel");
            setHelpInformation("Unbans a player from this parcel,", "they will be able to enter it again");
            setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to unban"));
            setAliases("undeny");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            OfflinePlayer unbanned = scape.get("player");
            Validate.isTrue(scape.getParcel().getAdded().remove(unbanned.getUniqueId(), false), unbanned.getName() + " wasn't banned from this parcel");
            return unbanned.getName() + " is no longer banned from this parcel";
        }

    };

    private final Command PARCEL_GLOBAL = new ParcelCommand("parcel global", ParcelRequirement.NONE) {
        {
            setDescription("manages your globally added players");
            setHelpInformation("Manages the players who you trust or want", "banned from all the parcels you own.");
            setOnSyntaxRequest(CommandAction.CONTINUE);
            setAliases("g");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            return "EXEC:CommandAction.DISPLAY_HELP";
        }

    };

    private final Command PARCEL_GLOBAL_ALLOW = new ParcelCommand("parcel global allow", ParcelRequirement.NONE) {
        {
            setDescription("Globally allows a player to build on your parcels");
            setHelpInformation("Globally allows a player to build on all", "the parcels that you own.");
            setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to allow globally"));
            setAliases("add", "permit");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            OfflinePlayer allowed = scape.get("player");
            Validate.isTrue(GlobalTrusted.addPlayer(sender.getUniqueId(), allowed.getUniqueId(), true),
                    allowed.getName() + " is already globally allowed to build on your parcels");
            return allowed.getName() + " is now globally allowed to build on your parcels";
        }

    };

    private final Command PARCEL_GLOBAL_DISALLOW = new ParcelCommand("parcel global disallow", ParcelRequirement.NONE) {
        {
            setDescription("Globally disallows a player to build on your parcels");
            setHelpInformation("Globally disallows a player to build on", "the parcels that you own.",
                    "If the player is allowed to build on specific", "parcels, they can still build there.");
            setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to disallow globally"));
            setAliases("remove", "forbid");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            OfflinePlayer forbidden = scape.get("player");
            Validate.isTrue(GlobalTrusted.removePlayer(sender.getUniqueId(), forbidden.getUniqueId(), true),
                    forbidden.getName() + " was not globally allowed to build on your parcels");
            return forbidden.getName() + " is no longer globally allowed to build on your parcels";
        }

    };

    private final Command PARCEL_GLOBAL_BAN = new ParcelCommand("parcel global ban", ParcelRequirement.NONE) {
        {
            setDescription("Globally bans a player from your parcels");
            setHelpInformation("Globally bans a player from all the parcels", "that you own, making them unable to enter.");
            setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to ban globally"));
            setAliases("deny");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            OfflinePlayer banned = scape.get("player");
            Validate.isTrue(GlobalTrusted.addPlayer(sender.getUniqueId(), banned.getUniqueId(), false),
                    banned.getName() + " is already globally banned from your parcels");
            return banned.getName() + " is now globally banned from your parcels";
        }

    };

    private final Command PARCEL_GLOBAL_UNBAN = new ParcelCommand("parcel global unban", ParcelRequirement.NONE) {
        {
            setDescription("Globally unbans a player from your parcels");
            setHelpInformation("Globally unbans a player from all the parcels", "that you own, they can enter again.",
                    "If the player is banned from specific parcels,", "they will still be banned there.");
            setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the player to unban globally"));
            setAliases("undeny");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            OfflinePlayer unbanned = scape.get("player");
            Validate.isTrue(GlobalTrusted.removePlayer(sender.getUniqueId(), unbanned.getUniqueId(), false),
                    unbanned.getName() + " was not globally banned from your parcels");
            return unbanned.getName() + " is no longer globally banned from your parcels";
        }

    };

    private final Command PARCEL_SETOWNER = new ParcelCommand("parcel setowner", ParcelRequirement.IN_PARCEL) {
        {
            setDescription("sets the owner of this parcel");
            setHelpInformation("Sets a new owner for this parcel,", "the owner has rights to manage it.");
            setParameters(new Parameter<OfflinePlayer>("owner", ParameterType.OFFLINE_PLAYER, "the new owner"));
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            UUID uuid = scape.<OfflinePlayer>get("owner").getUniqueId();
            Parcel parcel = scape.getParcel();

            Validate.isTrue(scape.getParcel().setOwner(uuid), "That player already owns this parcel");

            if (parcel.getAdded().get(uuid) != null) {
                parcel.getAdded().remove(uuid);
            }

            return scape.<OfflinePlayer>get("owner").getName() + " now owns this parcel";
        }

    };

    private final Command PARCEL_DISPOSE = new ParcelCommand("parcel dispose", ParcelRequirement.IN_OWNED) {
        {
            setDescription("removes any data about this parcel");
            setHelpInformation("removes any data about this parcel, it will be", "unowned, and noone will be allowed or banned.", "This command will not clear this parcel");
            setAliases("unclaim");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            scape.getParcel().dispose();
            return "This parcel no longer has any data";
        }

    };

    private final Command PARCEL_TP = new ParcelCommand("parcel tp", ParcelRequirement.IN_WORLD) {
        {
            setDescription("teleports to a parcel");
            setHelpInformation("Teleports you or a target player", "to the parcel you specify by ID");
            setAliases("teleport");
            setParameters(new Parameter<Coord>("parcel", PARCEL_PARAMETER_TYPE, "the parcel to teleport to"),
                    new Parameter<Player>("target", ParameterType.PLAYER, "the player to teleport", false));
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            ParcelWorld w = scape.getWorld();
            Coord xz = scape.get("parcel");
            Parcel p = Validate.returnIfPresent(w.getParcelAtID(xz.getX(), xz.getZ()), "That ID is not within this world's boundaries");
            Player target = scape.<Player>getOptional("target").orElse(sender);
            w.teleport(target, p);
            String format = "%s teleported %s to the " + p.toString();
            if (target == sender)
                return String.format(format, "You", "yourself");
            Messaging.send(target, prefix, Messaging.SUCCESS, String.format(format, sender.getName(), "you"));
            return String.format(format, "you", target.getName());
        }

    };

    private final Command PARCEL_RESET = new ParcelCommand("parcel reset", ParcelRequirement.IN_OWNED) {
        {
            setDescription("clears and disposes this parcel");
            setHelpInformation("Clears and disposes this parcel,", "see /parcel clear and /parcel dispose");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Validate.isTrue(!scape.getParcel().hasBlockVisitors(), "This parcel is currently under construction");
            ConfirmableRequest.file(sender, scape.getParcel(), null, ConfirmableRequest.RequestType.RESET);
            return Formatting.BLUE + "If you really want to reset this parcel, use /pconfirm within 30 seconds.";
        }

    };

    private final Command PARCEL_CLEAR = new ParcelCommand("parcel clear", ParcelRequirement.IN_OWNED) {
        {
            setDescription("clears this parcel");
            setHelpInformation("Clears this parcel, resetting all of its blocks", "and removing all entities inside");
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Validate.isTrue(!scape.getParcel().hasBlockVisitors(), "This parcel is currently under construction");
            ConfirmableRequest.file(sender, scape.getParcel(), null, ConfirmableRequest.RequestType.CLEAR);
            return Formatting.BLUE + "If you really want to clear this parcel, use /pconfirm within 30 seconds.";
        }

    };

    private final Command PARCEL_SWAP = new ParcelCommand("parcel swap", ParcelRequirement.IN_PARCEL) {
        {
            setDescription("swaps this parcel and its blocks with another");
            setHelpInformation("Swaps this parcel's data and any other contents,", "such as blocks and entities, with the target parcel");
            setParameters(new Parameter<Coord>("parcel", PARCEL_PARAMETER_TYPE, "the parcel to swap with"));
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            Validate.isTrue(!scape.getParcel().hasBlockVisitors(), "This parcel is currently under construction");
            Coord coord = scape.get("parcel");
            Parcel parcel2 = Validate.returnIfPresent(scape.getWorld().getParcelAtID(coord.getX(), coord.getZ()), "The target parcel does not exist");
            Validate.isTrue(!parcel2.hasBlockVisitors(), "That parcel is currently under construction");
            ConfirmableRequest.file(sender, scape.getParcel(), parcel2, ConfirmableRequest.RequestType.SWAP);
            return Formatting.BLUE + "If you really want to swap these parcels, use /pconfirm within 30 seconds.";
        }

    };

    private final Command PARCEL_RANDOM = new ParcelCommand("parcel random", ParcelRequirement.IN_WORLD) {
        {
            setDescription("teleports you to a random parcel");
            setHelpInformation("Teleports you to a random parcel in your current world", "to check it out");
            setParameters(new Parameter<OfflinePlayer>("player", ParameterType.OFFLINE_PLAYER, "the specific player that owns the random parcel", false));
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            OfflinePlayer owner = scape.get("player");
            Validate.isTrue(owner == null || sender.hasPermission(Permissions.PARCEL_RANDOM_SPECIFIC),
                    "You do not have permission to specify an owner");
            List<Parcel> ownedParcels = scape.getWorld().getParcels().stream()
                    .filter(owner == null ? p -> p.getOwner().isPresent() : p -> p.isOwner(owner)).collect(Collectors.toList());
            Validate.isTrue(ownedParcels.size() != 0, "There is no owned parcel to teleport to in this world");
            Parcel target = ownedParcels.get((int) (Math.random() * ownedParcels.size()));
            scape.getWorld().teleport(sender, target);
            return String.format("Teleported to the %s, it is owned by %s", target.toString(), UUIDUtil.getName(target.getOwner().get()));
        }

    };

    private final Command PARCEL_SETBIOME = new ParcelCommand("parcel setbiome", ParcelRequirement.IN_OWNED) {
        {
            setDescription("changes the biome of this parcel");
            setHelpInformation("Changes the biome of this parcel to the requested one,");
            setParameters(true, new Parameter<String>("biome", ParameterType.STRING, "the biome to set"));
        }

        private final String allBiomes = String.join(", ", Arrays.stream(Biome.values())
                .map(biome -> biome.toString().toLowerCase().replaceAll("_", " ")).toArray(CharSequence[]::new));

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            List<String> args = scape.get("biome");
            String biomeName = String.join("_", args.toArray(new CharSequence[args.size()])).toUpperCase();
            Biome biome;
            try {
                biome = Biome.valueOf(biomeName);
            } catch (IllegalArgumentException e) {
                throw new CommandException("That biome could not be found. All biomes: " + allBiomes);
            }
            scape.getWorld().setBiome(scape.getParcel(), biome);
            return "Set the biome to " + biomeName.toLowerCase().replaceAll("_", " ");
        }

    };

    private final Command PARCEL_TPWORLD = new ParcelCommand("parcel tpworld", ParcelRequirement.NONE) {
        {
            setDescription("teleport to the given parcel world");
            setHelpInformation("Teleports you to the requested parcel world.");
            setParameters(new Parameter<String>("name", ParameterType.STRING, "the name of the world"));
        }

        @Override
        protected String execute(Player sender, ParcelScape scape) {
            String name = scape.get("name");
            Validate.isAuthorized(sender, "parcels.command.tpworld." + name);
            sender.teleport(Validate.returnIfPresent(WorldManager.getWorld(name), "That parcel world does not exist").getWorld().getSpawnLocation());
            return "Teleported you to " + name;
        }

        @Override
        protected List<String> tabComplete(Player sender, ParcelScape scape) {
            return WorldManager.getWorlds().keySet().stream().collect(Collectors.toList());
        }

    };
}
