package com.redstoner.parcels.api.schematic.block;

import org.bukkit.block.CommandBlock;

public class CommandBlockBlock extends StateBlock<CommandBlock> {
    private String command, name;

    public CommandBlockBlock(CommandBlock state) {
        super(state);
        command = state.getCommand();
        name = state.getName();
    }

    public String getCommand() {
        return command;
    }

    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return Type.COMMAND_BLOCK;
    }

    @Override
    protected void paste(CommandBlock state) {
        state.setCommand(command);
        state.setName(name);
    }

}
