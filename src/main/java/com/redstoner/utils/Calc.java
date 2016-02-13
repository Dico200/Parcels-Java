package com.redstoner.utils;

public class Calc {
	
	private Calc() {}
	
	public static int posModulo(int a, int b) {
		return ((a %= b) < 0)? a + b : a;
	}
	
	public static double posModulo(double a, double b) {
		return ((a %= b) < 0)? a + b : a;
	}

}
