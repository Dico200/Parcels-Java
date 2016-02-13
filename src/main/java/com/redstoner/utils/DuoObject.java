package com.redstoner.utils;

public class DuoObject<T, U> {
	
	public DuoObject(T v1, U v2) {
		this.v1 = v1;
		this.v2 = v2;
	}
	
	protected T v1;
	protected U v2;
	
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

		public Coord(int v1, int v2) {
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
}