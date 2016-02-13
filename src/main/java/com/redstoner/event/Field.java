package com.redstoner.event;

import java.util.HashMap;
import java.util.Map;

public class Field<T, U> {
	
	private volatile Map<U, T> values = new HashMap<U, T>();
	private Listeners<T> listeners;
	private T defaultValue;
	
	public Field() {
		this(null);
	}
	
	public Field(T defaultValue) {
		this.defaultValue = defaultValue;
		this.listeners = new Listeners<T>();
	}
	
	public T get(U holder) {
		return values.get(holder);
	}
	
	public boolean set(U holder, T value) {
		return set(holder, value, null);
	}
	
	public boolean set(U holder, T value, Object cause) {
		return execute(new Event<T>(holder, get(holder), value, cause));
	}
	
	public void initialise(U holder) {
		values.put(holder, defaultValue);
	}
	
	private boolean execute(Event<T> event) {	
		listeners.execute(event);
		if (!event.isCancelled()) {
			values.put(event.getHolder(), event.newValue());
			return true;
		}
		return false;
	}
	
	public Map<U, T> values() {
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
