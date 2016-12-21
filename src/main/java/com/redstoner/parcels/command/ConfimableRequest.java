package com.redstoner.parcels.command;

import com.redstoner.command.Messaging;
import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.utils.Formatting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

class ConfirmableRequest {

    public static void file(Player sender, Parcel parcel, Parcel optionalParcel, RequestType type) {
        requests.add(new ConfirmableRequest(sender, parcel, optionalParcel, type));
    }

    public static void registerConfirmationListener() {
        ParcelsPlugin.getInstance().getServer().getPluginManager().registerEvents(new Listener() {

            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {

                if (event.getMessage().toLowerCase().equals("/pconfirm")) {
                    Messaging.send(event.getPlayer(), ParcelCommands.PREFIX, Formatting.GREEN, confirm(event.getPlayer()));
                    event.setCancelled(true);
                }

            }

        }, ParcelsPlugin.getInstance());
    }

    private static String confirm(Player player) {
        long time = System.currentTimeMillis();
        Iterator<ConfirmableRequest> it = requests.iterator();
        ConfirmableRequest request;
        ConfirmableRequest toConfirm = null;
        while (it.hasNext()) {
            request = it.next();
            if (request.expireTime <= time) {
                it.remove();
            } else if (request.equals(player)) {
                toConfirm = request;
            }
        }
        return toConfirm == null ? null : toConfirm.type.execute(toConfirm);
    }

    private static final Set<ConfirmableRequest> requests = new HashSet<>();

    private static final long clearRequestExpireTime = 30000;

    private final Player player;
    private final Parcel parcel;
    private final Parcel optionalParcel;
    private final long expireTime;
    private final RequestType type;

    // One request per player, so if player is equal, request is "equal" (for Hashing)
    @Override
    public boolean equals(Object other) {
        return other != null && other instanceof Player && (Player) other == player;
    }

    @Override
    public int hashCode() {
        return player.hashCode();
    }

    private ConfirmableRequest(Player player, Parcel parcel, Parcel optionalParcel, RequestType type) {
        checkNotNull(player);
        checkNotNull(parcel);
        checkNotNull(type);
        checkArgument(type != RequestType.SWAP || optionalParcel != null);

        this.player = player;
        this.parcel = parcel;
        this.optionalParcel = optionalParcel;
        this.type = type;
        this.expireTime = System.currentTimeMillis() + clearRequestExpireTime;
    }

    static enum RequestType {
        CLEAR {
            @Override
            protected void executeRequest(ConfirmableRequest request) {
                request.parcel.getWorld().clear(request.parcel);
            }

        },
        RESET {
            @Override
            protected void executeRequest(ConfirmableRequest request) {
                request.parcel.getWorld().reset(request.parcel);
            }

        },
        SWAP {
            @Override
            protected void executeRequest(ConfirmableRequest request) {
                request.parcel.getWorld().swap(request.parcel, request.optionalParcel);
            }

        };

        protected abstract void executeRequest(ConfirmableRequest request);

        private String execute(ConfirmableRequest request) {
            Messaging.send(request.player, "Parcels", Formatting.BLUE, participlePresent() + ", hang tight...");
            long startTime = System.currentTimeMillis();
            executeRequest(request);
            return String.format("%s successfully, %.2fs elapsed", participlePast(), (System.currentTimeMillis() - startTime) / 1000D);
        }

        private String participlePresent() {
            switch (this) {
                case CLEAR:
                    return "Clearing this parcel";
                case RESET:
                    return "Resetting this parcel";
                case SWAP:
                    return "Swapping these parcels";
                default:
                    return null;
            }
        }

        private String participlePast() {
            switch (this) {
                case CLEAR:
                    return "Cleared this parcel";
                case RESET:
                    return "Reset this parcel";
                case SWAP:
                    return "Swapped these parcels";
                default:
                    return null;
            }
        }
    }

}
