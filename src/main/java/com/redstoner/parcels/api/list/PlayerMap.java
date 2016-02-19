package com.redstoner.parcels.api.list;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.OfflinePlayer;

public class PlayerMap<T> {
	
	private Map<OfflinePlayer, T> players;
	
	// Will be the same instance across entries
	private T standard;
	
	public PlayerMap(T standard) {
		this.players = new ConcurrentHashMap<>();
		this.standard = standard;
	}
	
	public boolean add(OfflinePlayer toAdd) {
		return add(toAdd, standard);
	}
	
	public boolean add(OfflinePlayer toAdd, T value) {
		if (players.containsKey(toAdd))
			return false;
		players.put(toAdd, value);
		return true;
	}
	
	public boolean remove(OfflinePlayer toRemove, T value) {
		if (get(toRemove) != value)
			return false;
		players.remove(toRemove);
		return true;
	}
	
	public T get(OfflinePlayer toCheck) {
		return players.get(toCheck);
	}
	
	public boolean is(OfflinePlayer toCheck, T value) {
		T current = get(toCheck);
		return (current == null && value == null) || current == value;
	}
	
	public Map<OfflinePlayer, T> getMap() {
		return players;
	}
	
	public String toString(T value) {
		String players = String.join(", ", (CharSequence[]) this.players.entrySet().stream()
				.filter(entry -> entry.getValue().equals(value))
				.map(entry -> entry.getKey().getName())
				.toArray(size -> new String[size]));
		return players;
	}
	
	public void clear() {
		players.clear();
	}

}
