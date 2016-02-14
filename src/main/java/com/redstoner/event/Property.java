package com.redstoner.event;

import java.util.Optional;


public class Property<T> {
	
	private Optional<T> value;
	private Listeners<T> listeners;
	private Object holder;
	 
	public Property(Object holder) {
		this(holder, null);
	}
	
	public Property(Object holder, T value) {
		this.holder = holder;
		this.value = Optional.ofNullable(value);
		this.listeners = new Listeners<T>();
	}
	
	public Optional<T> get() {
		return value;
	}
	
	public Listeners<T> listeners() {
		return listeners;
	}
	
	public void addListener(Listener<T> listener) {
		listeners.add(listener);
	}
	
	private boolean execute(Event<T> event) {
		listeners.execute(event);
		if (!event.isCancelled()) {
			this.value = Optional.ofNullable(event.newValue());
			return true;
		}
		return false;
	}
	
	public boolean set(T newValue, Object cause) {
		return execute(new Event<T>(holder, value, newValue, cause));
	}
	
	public boolean set(T newValue) {
		return set(newValue, null);
	}
	
	

}
