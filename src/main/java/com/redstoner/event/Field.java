package com.redstoner.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Field<T, U> {
	
	private volatile Map<U, Optional<T>> values = new HashMap<U, Optional<T>>();
	private Listeners<T> listeners;
	private T defaultValue;
	
	public Field() {
		this(null);
	}
	
	public Field(T defaultValue) {
		this.defaultValue = defaultValue;
		this.listeners = new Listeners<T>();
	}
	
	public Optional<T> get(U holder) {
		return values.getOrDefault(holder, Optional.empty());
	}
	
	public boolean set(U holder, T value) {
		return set(holder, value, null);
	}
	
	public boolean set(U holder, T value, Object cause) {
		return execute(new Event<T>(holder, get(holder), value, cause));
	}
	
	public void initialise(U holder) {
		if (defaultValue != null) {
			values.put(holder, Optional.of(defaultValue));
		}
	}
	
	private boolean execute(Event<T> event) {	
		listeners.execute(event);
		if (!event.isCancelled()) {
			values.replace(event.getHolder(), Optional.ofNullable(event.newValue()));
			return true;
		}
		return false;
	}
	
	public Map<U, Optional<T>> values() {
		return values;
	}
	
	public Listeners<T> listeners() {
		return listeners;
	}
	
	public void addListener(Listener<T> listener) {
		listeners.add(listener);
	}
	
	public static <T> T cast(Class<T> type, Object owner, boolean allowNull) {
		if (!allowNull) assert owner != null;
		assert owner == null || owner.getClass().equals(type);
		return type.cast(owner);
	}
	

}
