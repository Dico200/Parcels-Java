package com.redstoner.utils;

import java.util.function.Consumer;
import java.util.function.Function;

public class ErrorPrinter extends MultiRunner {
	
	private final Function<String, Runnable> printer;
	
	public ErrorPrinter(Consumer<String> printer, String initialLine, String finalLine) {
		super(() -> printer.accept(initialLine), () -> printer.accept(initialLine));
		this.printer = s -> () -> printer.accept("  " + s);
	}
	
	public ErrorPrinter(Consumer<String> printer, String initialLine) {
		super(() -> printer.accept(initialLine));
		this.printer = s -> () -> printer.accept("  " + s);
	}
	
	public ErrorPrinter(Consumer<String> printer) {
		super();
		this.printer = s -> () -> printer.accept("  " + s);
	}
	
	public ErrorPrinter(String initialLine, String finalLine) {
		this(s -> System.out.println(s), initialLine, finalLine);
	}
	
	public ErrorPrinter(String initialLine) {
		this(s -> System.out.println(s), initialLine);
	}
	
	public ErrorPrinter() {
		this(s -> System.out.println(s));
	}
	
	public void add(String error) {
		add(printer.apply(error));
	}
	
	public void addFirst(String error) {
		addFirst(printer.apply(error));
	}

}
