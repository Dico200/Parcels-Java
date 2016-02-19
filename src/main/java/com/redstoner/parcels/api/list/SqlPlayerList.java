package com.redstoner.parcels.api.list;

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
	
	@Override
	public void clear() {
		super.clear();
		clearSQL();
	}
	
	public void addIgnoreSQL(OfflinePlayer toAdd) {
		super.add(toAdd);
	}
	
	protected abstract void removeFromSQL(OfflinePlayer toRemove);
	protected abstract void addToSQL(OfflinePlayer toAdd);
	protected abstract void clearSQL();

}
