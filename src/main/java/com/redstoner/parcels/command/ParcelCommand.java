package com.redstoner.parcels.command;

import com.redstoner.command.Command;
import com.redstoner.command.CommandException;
import com.redstoner.command.CommandScape;
import com.redstoner.command.SenderType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

abstract class ParcelCommand extends Command {

    private final ParcelRequirement requirement;

    public ParcelCommand(String command, ParcelRequirement requirement) {
        super(command);
        checkNotNull(requirement);
        this.requirement = requirement;
        setSenderType(SenderType.PLAYER);
    }

    @Override
    protected String execute(CommandSender sender, CommandScape scape) {
        Player user = (Player) sender;
        return execute(user, new ParcelScape(user, scape, requirement));
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, CommandScape scape) {
        Player user = (Player) sender;
        try {
            return tabComplete(user, new ParcelScape(user, scape, requirement));
        } catch (CommandException e) {
            return new ArrayList<>();
        }

    }

    protected abstract String execute(Player sender, ParcelScape scape);

    protected List<String> tabComplete(Player sender, ParcelScape scape) {
        return scape.proposals();
    }

}
