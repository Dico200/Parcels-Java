package com.redstoner.parcels.api;

import org.bukkit.OfflinePlayer;

public abstract class SqlPlayerList extends PlayerList {
	private static final long serialVersionUID = 5258340707312744320L;
	
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
	
	void addIgnoreSQL(OfflinePlayer toAdd) {
		super.add(toAdd);
	}
	
	protected abstract void removeFromSQL(OfflinePlayer toRemove);
	protected abstract void addToSQL(OfflinePlayer toAdd);

}
