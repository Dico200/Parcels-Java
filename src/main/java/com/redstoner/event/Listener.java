package com.redstoner.event;



public interface Listener<T> {
	
	
	/**
	 * @param event The event
	 */
	public void change(Event<T> event);
	
	/**
	 * @return Whether the listener ignores cancelled events
	 */
	public boolean ignoresCancelled();
	
	/**
	 * @return The stage during which the listener is consulted
	 */
	public Order getOrder();

	
}
