package com.redstoner.utils;

public class Values {
	
	private Values() {}
	
	public static <T> T checkNotNull(T obj) {
		if (obj == null) 
			throw new NullPointerException();
		return obj;
	}
	
	public static <T, U extends Throwable> T checkNotNull(T obj, U exc) throws U {
		if (obj == null)
			throw exc;
		return obj;
	}
	
	public static void validate(boolean b, String s) {
		validate(b, new RuntimeException(s));
	}
	
	public static void validate(boolean b, RuntimeException e) {
		if (!b) throw e;
	}
	
	public static boolean inRangeE(double x, double min, double max) {
		return min < x && x < max;
	}
	
	public static boolean inRangeI(int x, int min, int max) {
		return min <= x && x <= max;
	}
	
	public static boolean inRange(int x, int min, int max) {
		return min <= x && x < max;
	}
	
	public static int posModulo(int a, int b) {
		return ((a %= b) < 0)? a + b : a;
	}
	
	public static double posModulo(double a, double b) {
		return ((a %= b) < 0)? a + b : a;
	}

}
