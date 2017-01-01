package com.redstoner.parcels.command;

import com.redstoner.parcels.api.BlockOperations;
import com.redstoner.parcels.api.Parcel;
import io.dico.dicore.command.Formatting;
import io.dico.dicore.command.Messaging;
import io.dico.dicore.util.Registrator;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ConfirmableRequest {
    private static String prefix;
    private static final Map<Player, ConfirmableRequest> requests = new HashMap<>();
    private static final long clearRequestExpireTime = 30000;

    public static void registerListener(String prefix, Registrator registrator) {
        ConfirmableRequest.prefix = prefix;
        registrator.registerListener(PlayerCommandPreprocessEvent.class, true, ConfirmableRequest::onPlayerCommandPreprocess);
    }

    public static void file(Player sender, Parcel parcel, Parcel optionalParcel, RequestType type) {
        requests.put(sender, new ConfirmableRequest(sender, parcel, optionalParcel, type));
    }

    private static void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().toLowerCase().equals("/pconfirm")) {
            long time = System.currentTimeMillis();
            requests.values().removeIf(request -> request.expireTime <= time);
            ConfirmableRequest confirmed = requests.get(event.getPlayer());
            if (confirmed == null) {
                return;
            }

            confirmed.execute();
            event.setCancelled(true);
        }
    }

    private final Player player;
    private final Parcel parcel;
    private final Parcel optionalParcel;
    private final long expireTime;
    private long executionStartTime;
    private final RequestType type;

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

    private void sendFinishedMessage() {
        Messaging.send(player, prefix, Formatting.GREEN, String.format("%s successfully, %.2fs elapsed",
                type.participlePast(), (System.currentTimeMillis() - executionStartTime) / 1000D));
    }

    private void execute() {
        type.execute(this);
    }

    enum RequestType {
        CLEAR {
            @Override
            protected void executeRequest(ConfirmableRequest request) {
                BlockOperations.clear(request.parcel, request::sendFinishedMessage);
            }
        },
        RESET {
            @Override
            protected void executeRequest(ConfirmableRequest request) {
                request.parcel.dispose();
                CLEAR.execute(request);
            }
        },
        SWAP {
            @Override
            protected void executeRequest(ConfirmableRequest request) {
                BlockOperations.swap(request.parcel, request.optionalParcel, request::sendFinishedMessage);
            }
        };

        protected abstract void executeRequest(ConfirmableRequest request);

        private void execute(ConfirmableRequest request) {
            Messaging.send(request.player, "Parcels", Formatting.BLUE, participlePresent() + ", hang tight...");
            request.executionStartTime = System.currentTimeMillis();
            executeRequest(request);
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
