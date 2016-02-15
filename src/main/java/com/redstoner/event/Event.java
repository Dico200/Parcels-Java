package com.redstoner.event;

import com.redstoner.command.ArgumentException;
import com.redstoner.utils.Optional;

public class Event<T> {
	
	private Optional<T> oldValue;
	private T newValue;
	private boolean cancelled;
	private Object holder, cause;
	
	protected Event(Object holder, Optional<T> optional, T newValue, Object cause) {
		this.cancelled = false;
		this.holder = holder;
		this.oldValue = optional;
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
	public Optional<T> oldValue() {
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
		System.out.println("Cancelled state set to: " + cancelled);
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
