package com.redstoner.utils;

public class OneTimeRunner {
	
	private boolean ran;
	private Runnable toRun;
	
	public OneTimeRunner(Runnable toRun) {
		this.ran = false;
		this.toRun = toRun;
	}
	
	public void run() {
		if (ran? false : (ran = true)) toRun.run();
	}

}
