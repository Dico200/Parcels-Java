package com.redstoner.command;

import com.redstoner.utils.Formatting;
import org.bukkit.command.CommandSender;

public class Messaging {

    public static final Formatting SUCCESS = Formatting.GREEN;
    public static final Formatting EXCEPT = Formatting.YELLOW;

    private static final String PREFIX_FORMAT = Formatting.translateChars('&', "&4[&c%s&4] ");
    private static final String MESSAGE_FORMAT = "%s%s%s";

    private static String formatPrefix(String prefix) {
        return (prefix == null || prefix.isEmpty()) ? "" : String.format(PREFIX_FORMAT, prefix);
    }

    public static void send(CommandSender recipient, String prefix, Formatting format, String message) {
        recipient.sendMessage(String.format(MESSAGE_FORMAT, formatPrefix(prefix), format, Formatting.translateChars('&', message)));
    }

    public static void send(CommandSender recipient, Formatting format, String message) {
        send(recipient, null, format, message);
    }

}

