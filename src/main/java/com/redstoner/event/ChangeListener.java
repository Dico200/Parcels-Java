package com.redstoner.event;


public abstract class ChangeListener<T> implements Listener<T> {
	
	/**
	 * constructs a new ChangeListener which does not ignore cancelled events
	 * The order is set to the default NORMAL
	 */
	public ChangeListener() {
		this(Order.NORMAL);
	}
	
	/**
	 * constructs a new ChangeListener which does not ignore cancelled events
	 * @param order The order, see Listener
	 */
	public ChangeListener(Order order) {
		this(order, false);
	}
	
	/**
	 * constructs a new ChangeListener
	 * @param order The order, see Listener
	 * @param ignoreCancelled Whether the listener should ignore cancelled events
	 */
	public ChangeListener(Order order, boolean ignoreCancelled) {
		this.order = order;
		this.ignoreCancelled = ignoreCancelled;
	}
	
	/**
	 * @return Whether the listener ignores cancelled events
	 */
	public boolean ignoresCancelled() {
		return ignoreCancelled;
	}
	
	/**
	 * @return The order, see Listener
	 */
	public Order getOrder() {
		return order;
	}

	private boolean ignoreCancelled;
	private Order order;
	
}
