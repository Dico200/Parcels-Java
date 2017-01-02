package com.redstoner.parcels.api.schematic.block;

import org.bukkit.Note;
import org.bukkit.block.NoteBlock;

public class NoteBlockBlock extends StateBlock<NoteBlock> {
    private Note note;

    public NoteBlockBlock(NoteBlock state) {
        super(state);
        note = state.getNote();
    }

    public Note getNote() {
        return note;
    }

    @Override
    public Type getType() {
        return Type.NOTE_BLOCK;
    }

    @Override
    protected void paste(NoteBlock state) {
        state.setNote(note);
    }

}
