package com.redstoner.utils;

import java.util.Deque;
import java.util.LinkedList;

public class MultiRunner {
	
	private Runnable toRunFirst, toRunLast;
	private Deque<Runnable> toRun = new LinkedList<Runnable>();
	
	public MultiRunner() {
		this(null);
	}
	
	public MultiRunner(Runnable atStart) {
		this(atStart, null);
	}
	
	public MultiRunner(Runnable atStart, Runnable atEnd) {
		this.toRunFirst = atStart;
		this.toRunLast = atEnd;
	}
	
	public void add(Runnable toRun) {
		assert toRun != null: "Runnable cannot be null";
		this.toRun.addLast(toRun);
	}
	
	public void addFirst(Runnable toRun) {
		this.toRun.addFirst(toRun);
	}
	
	public boolean willRun() {
		return this.toRun.size() > 0;
	}
	
	public void runAll() {
		if (willRun()) {
			if (toRunFirst != null)
				toRunFirst.run();
			this.toRun.stream().forEach(Runnable::run);
			if (toRunLast != null)
				toRunLast.run();
		}	
	}
	
	public void reset() {
		this.toRun = new LinkedList<>();
	}
}
