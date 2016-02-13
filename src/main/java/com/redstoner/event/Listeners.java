package com.redstoner.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Listeners<T> {
	
	private static <T> Map<Order, List<Listener<T>>> defaultListeners() {
		return new LinkedHashMap<Order, List<Listener<T>>>() {
			private static final long serialVersionUID = 1L;
			{
				Arrays.stream(Order.values()).forEach(order ->
						put(order, new ArrayList<Listener<T>>()));
			}
		};
	}
	
	private Map<Order, List<Listener<T>>> listeners = defaultListeners();
	
	public void add(Listener<T> listener) {
		listeners.get(listener.getOrder()).add(listener);
	}
	
	public Map<Order, List<Listener<T>>> getListeners() {
		return listeners;
	}
	
	public void removeAll() {
		listeners = defaultListeners();
	}
	
	public void removeAll(Order ofOrder) {
		assert ofOrder != null;
		listeners.put(ofOrder, new ArrayList<Listener<T>>());
	}
	
	public boolean remove(Listener<T> listener) {
		return listeners.get(listener.getOrder()).remove(listener);
	}
	
	public void execute(Event<T> event) {
		listeners.values().stream().forEach(list -> {
			boolean canceled = event.isCancelled();
			list.stream().filter(listener -> !canceled || listener.ignoresCancelled())
						 .forEach(listener -> listener.change(event));
		});
	}

}
