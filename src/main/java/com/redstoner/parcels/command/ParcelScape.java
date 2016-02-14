package com.redstoner.parcels.command;

import com.redstoner.command.CommandScape;
import com.redstoner.parcels.api.Parcel;

public class ParcelScape extends CommandScape {

	public ParcelScape(CommandScape scape, Parcel at) {
		super(scape);
		this.at = at;
	}
	
	private Parcel at;
	
	public Parcel getParcelAt() {
		return at;
	}
	
	
	
	
	

}
