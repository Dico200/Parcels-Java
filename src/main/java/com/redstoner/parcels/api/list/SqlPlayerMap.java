package com.redstoner.parcels.api.list;

import org.bukkit.OfflinePlayer;

public abstract class SqlPlayerMap<T> extends PlayerMap<T> {
	
	public SqlPlayerMap(T standard) {
		super(standard);
	}
	
	@Override
	public boolean add(OfflinePlayer toAdd, T value) {
		if (super.add(toAdd, value)) {
			addToSQL(toAdd, value);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean remove(OfflinePlayer toRemove, T value) {
		if (super.remove(toRemove, value)) {
			removeFromSQL(toRemove);
			return true;
		}
		return false;
	}
	
	@Override
	public void clear() {
		super.clear();
	}
	
	public void addIgnoreSQL(OfflinePlayer toAdd, T value) {
		super.add(toAdd, value);
	}
	
	protected abstract void addToSQL(OfflinePlayer toAdd, T value);
	protected abstract void removeFromSQL(OfflinePlayer toRemove);
	protected abstract void clearSQL();

}
