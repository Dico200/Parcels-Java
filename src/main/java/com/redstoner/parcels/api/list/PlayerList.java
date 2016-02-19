package com.redstoner.parcels.api.list;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.OfflinePlayer;

public class PlayerList {
	
	private List<OfflinePlayer> players;
	private boolean hasStar;
	
	public PlayerList() {
		this.players = new ArrayList<>();
		this.hasStar = false;
	}
	
	public boolean setHasStar(boolean hasStar) {
		if (hasStar == this.hasStar)
			return false;
		this.hasStar = hasStar;
		return true;
	}
	
	public boolean hasStar() {
		return hasStar;
	}
	
	public boolean add(OfflinePlayer toAdd) {
		if (players.contains(toAdd))
			return false;
		return players.add(toAdd);
	}
	
	public boolean remove(OfflinePlayer toRemove) {
		if (!players.contains(toRemove))
			return false;
		return players.remove(toRemove);
	}
	
	public boolean contains(OfflinePlayer toCheck) {
		return hasStar || players.contains(toCheck);
	}
	
	public boolean isPresent(OfflinePlayer toCheck) {
		return players.contains(toCheck);
	}
	
	public Stream<OfflinePlayer> stream() {
		return players.stream();
	}
	
	public List<OfflinePlayer> getAll() {
		return players;
	}
	
	public String toString() {
		String players = String.join(", ", (CharSequence[]) stream().map(p -> p.getName()).toArray(size -> new String[size]));
		if (hasStar)
			if (players.isEmpty())
				players = "*";
			else
				players += ", *";
		return players;
	}
	
	public void clear() {
		players.clear();
	}

}
