package com.redstoner.command;

import java.util.LinkedList;
import java.util.List;

public class Parameters extends LinkedList<Parameter<?>>{
	
	private static final long serialVersionUID = 7607300090766311570L;
	
	private final Command handler;
	private final boolean allowOverflow;
	
	public Parameters(Command handler, Parameter<?>[] params, boolean allowOverflow) {
		this.handler = handler;
		this.allowOverflow = allowOverflow;
		int i = 0;
		boolean lastRequired = true;
		for (Parameter<?> param : params) {
			if (!lastRequired && param.isRequired())
				throw new ArgumentException("You cannot have a required parameter after one that is not required");
			lastRequired = param.isRequired();
			
			param.setIndex(i);
			this.add(param);
			i++;
		}
	}
	
	public String syntax() {
		return String.join(" ", (CharSequence[]) stream().map(param -> param.syntax()).toArray(size -> new String[size]));
	}
	
	public CommandScape toScape(String[] args) {
		return new CommandScape(this, args);
	}
	
	public CommandScape toScape(String[] args, List<String> proposals) {
		return new CommandScape(this, args, proposals);
	}
	
	public List<String> complete(String[] args) {
		int i = args.length - 1;
		if (i < 0) i = 0;
		return get(i).complete(args[i]);
	}
	
	protected boolean allowsOverflow() {
		return allowOverflow;
	}
	
	protected Command getHandler() {
		return handler;
	}

}
