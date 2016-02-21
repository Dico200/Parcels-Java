package com.redstoner.parcels.api.list;

import java.util.UUID;

public abstract class SqlPlayerMap<T> extends PlayerMap<T> {
	
	public SqlPlayerMap(T standard) {
		super(standard);
	}
	
	@Override
	public boolean add(UUID toAdd, T value) {
		if (super.add(toAdd, value)) {
			addToSQL(toAdd, value);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean remove(UUID toRemove, T value) {
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
	
	public void addIgnoreSQL(UUID toAdd, T value) {
		super.add(toAdd, value);
	}
	
	protected abstract void addToSQL(UUID toAdd, T value);
	protected abstract void removeFromSQL(UUID toRemove);
	protected abstract void clearSQL();

}