package com.redstoner.utils;

import java.util.function.BiFunction;

public class TriFunction<T, U, V, R> implements BiFunction<T, DuoObject<U, V>, R>{
	
	private BiFunction<T, DuoObject<U, V>, R> func;
	
	public TriFunction(BiFunction<T, DuoObject<U, V>, R> func) {
		this.func = func;
	}

	@Override
	public R apply(T t, DuoObject<U, V> u) {
		return func.apply(t, u);
	}
	
	public R apply(T t, U u, V v) {
		return apply(t, new DuoObject<U, V>(u, v));
	}

}
