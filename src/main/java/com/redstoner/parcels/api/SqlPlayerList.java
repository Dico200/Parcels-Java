package com.redstoner.parcels.api;

import org.bukkit.OfflinePlayer;

public abstract class SqlPlayerList extends PlayerList {
	
	@Override
	public boolean remove(OfflinePlayer toRemove) {
		if (super.remove(toRemove)) {
			removeFromSQL(toRemove);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean add(OfflinePlayer toAdd) {
		if (super.add(toAdd)) {
			addToSQL(toAdd);
			return true;
		}
		return false;
	}
	
	boolean addIgnoreSQL(OfflinePlayer toAdd) {
		return super.add(toAdd);
	}
	
	protected abstract void removeFromSQL(OfflinePlayer toRemove);
	protected abstract void addToSQL(OfflinePlayer toAdd);

}
