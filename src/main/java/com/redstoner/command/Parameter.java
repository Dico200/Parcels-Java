package com.redstoner.command;

import java.util.List;

public class Parameter<T> {
	
	private String name;
	private ParameterType<T> type;
	private String description;
	private boolean required;
	private int index;
	
	public Parameter(String name, ParameterType<T> type, String description, boolean required) {
		this.name = name;
		this.type = type;
		this.description = description;
		this.required = required;
		this.index = 0;
	}
	
	public Parameter(String name, ParameterType<T> type, String description) {
		this(name, type, description, true);
	}
	
	String getName() {
		return name;
	}
	
	ParameterType<T> getHandler() {
		return type;
	}
	
	boolean isRequired() {
		return required;
	}
	
	int getIndex() {
		return index;
	}
	
	void setIndex(int index) {
		this.index = index;
	}
	
	public T accept(String input) {
		if (input == null || input.isEmpty()) {
			if (required)
				throw new CommandException("EXEC:CommandAction.DISPLAY_SYNTAX");
			return null;
		}
		try {
			return type.handle(input);
		} catch (CommandException e) {
			throw new CommandException(e.getMessage().replace("$ARG$", name).replace("$DESC$", description));
		}
	}
	
	public List<String> complete(String input) {
		return type.complete(input);
	}
	
	public String syntax() {
		String syntax = name.trim();
		return String.format(required? "<%s>" : "[%s]", syntax);
	}

}
