package com.redstoner.event;

import java.util.function.Consumer;

import com.redstoner.utils.Values;

public class LambdaChangeListener<T> extends ChangeListener<T> {

	private Consumer<Event<T>> function;
	
	public LambdaChangeListener(Consumer<Event<T>> function) {
		Values.validate(function != null, "The passed consumer may not be null");
		this.function = function;
	}
	
	@Override
	public void change(Event<T> event) {
		function.accept(event);
	}

}
