package com.redstoner.parcels.api.list;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.UUID;

import org.bukkit.Bukkit;

public class PlayerList {
	
	private List<UUID> players;
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
	
	public boolean add(UUID toAdd) {
		if (players.contains(toAdd))
			return false;
		return players.add(toAdd);
	}
	
	public boolean remove(UUID toRemove) {
		if (!players.contains(toRemove))
			return false;
		return players.remove(toRemove);
	}
	
	public boolean contains(UUID toCheck) {
		return hasStar || players.contains(toCheck);
	}
	
	public boolean isPresent(UUID toCheck) {
		return players.contains(toCheck);
	}
	
	public Stream<UUID> stream() {
		return players.stream();
	}
	
	public List<UUID> getAll() {
		return players;
	}
	
	public String toString() {
		String players = String.join(", ", (CharSequence[]) stream().map(p -> Bukkit.getOfflinePlayer(p).getName()).toArray(size -> new String[size]));
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
