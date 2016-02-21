package com.redstoner.parcels.api.list;

import java.util.UUID;

public abstract class SqlPlayerList extends PlayerList {
	@Override
	public boolean remove(UUID toRemove) {
		if (super.remove(toRemove)) {
			removeFromSQL(toRemove);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean add(UUID toAdd) {
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
	
	public void addIgnoreSQL(UUID toAdd) {
		super.add(toAdd);
	}
	
	protected abstract void removeFromSQL(UUID toRemove);
	protected abstract void addToSQL(UUID toAdd);
	protected abstract void clearSQL();

}
