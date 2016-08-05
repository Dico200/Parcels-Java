package com.redstoner.utils;

public class Values {
	
	private Values() {}
	
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
