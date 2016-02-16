package com.redstoner.utils;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class DuoObject<T, U> {
	
	public DuoObject(T v1, U v2) {
		this.v1 = v1;
		this.v2 = v2;
	}
	
	protected final T v1;
	protected final U v2;
	
	public T v1() {
		return v1;
	}
	
	public U v2() {
		return v2;
	}
	
	public String toString() {
		return String.format("(%s, %s)", v1, v2);
	}
	
	public static class Coord extends DuoObject<Integer, Integer> {
		
		public static Coord of(int x, int z) {
			return new Coord(x, z);
		}

		private Coord(int v1, int v2) {
			super(v1, v2);
		}
		
		public int getX() {
			return v1;
		}
		
		public int getZ() {
			return v2;
		}
		
	}
	
	public static class DCoord extends DuoObject<Double, Double> {

		public DCoord(double v1, double v2) {
			super(v1, v2);
		}
		
		public double getX() {
			return v1;
		}
		
		public double getZ() {
			return v2;
		}
		
	}
	
	public static class Entry<K, V> extends DuoObject<K, V> {

		public Entry(K v1, V v2) {
			super(v1, v2);
		}
		
		public K getKey() {
			return v1;
		}
		
		public V getValue() {
			return v2;
		}
		
	}
	
	public static class BlockType extends DuoObject<Short, Byte> {
		
		public static BlockType fromString(String s) throws NumberFormatException {
			Values.validate(s != null, "BlockType was passed null");
			String[] both = s.split(":");
			String id;
			String data = "0";
			switch (both.length) {
			case 2:
				data = both[1];
			case 1:
				id = both[0];
				break;
			default:
				throw new NumberFormatException();	
			}
			return new BlockType(Short.parseShort(id), Byte.parseByte(data));
		}

		public BlockType(Short v1, Byte v2) {
			super(v1, v2);
		}
		
		public short getId() {
			return v1;
		}
		
		public byte getData() {
			return v2;
		}
		
	}
	
	public static class TriConsumer<T, U, V> implements BiConsumer<T, DuoObject<U, V>> {
		
		BiConsumer<T, DuoObject<U, V>> cons;
		
		public TriConsumer(BiConsumer<T, DuoObject<U, V>> cons) {
			this.cons = cons;
		}
		
		@Override
		public void accept(T t, DuoObject<U, V> u) {
			cons.accept(t, u);
		}
		
		public void accept(T t, U u, V v) {
			accept(t, new DuoObject<U, V>(u, v));
		}
		
	}
	
	public static class TriFunction<T, U, V, R> implements BiFunction<T, DuoObject<U, V>, R>{
		
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
}