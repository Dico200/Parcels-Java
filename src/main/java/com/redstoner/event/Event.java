package com.redstoner.event;

import com.redstoner.command.ArgumentException;

public class Event<T> {
	
	private T oldValue, newValue;
	private boolean cancelled;
	private Object holder, cause;
	
	protected Event(Object holder, T oldValue, T newValue, Object cause) {
		this.cancelled = false;
		this.holder = holder;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.cause = cause;
	}
	
	/**
	 * @return The proposed value
	 */
	public T newValue() {
		return newValue;
	}
	
	/**
	 * @return The current value
	 */
	public T oldValue() {
		return oldValue;
	}
	
	/**
	 * @return Whether the event was cancelled by a previous listener
	 */
	public boolean isCancelled() {
		return cancelled;
	}
	
	/**
	 * @param cancelled The new canceled state
	 */
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	/**
	 * @return An object describing why the change would happen
	 */
	@SuppressWarnings("unchecked")
	public <U> U getCause() {
		try {
			return (U) cause;
		} catch (ClassCastException e) {
			throw new ArgumentException("Wrong class type requested for cause: " + e.getMessage());
		}
	}
	
	/**
	 * @return The object that holds the value or field
	 */
	@SuppressWarnings("unchecked")
	public <U> U getHolder() {
		try {
			return (U) holder;
		} catch (ClassCastException e) {
			throw new ArgumentException("Wrong class type requested for holder: " + e.getMessage());
		}
	}
}
