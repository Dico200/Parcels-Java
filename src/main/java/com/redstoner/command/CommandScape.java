package com.redstoner.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.function.BiPredicate;

public class CommandScape {
	
	private static final BiPredicate<Parameter<?>, Integer> EQUAL_INDEX = (param, index) -> param.getIndex() == index;
	private static final BiPredicate<Parameter<?>, String> EQUAL_NAME = (param, name) -> param.getName().equals(name);
	
	private String[] original;
	private List<String> proposals;
	private String[] overflow;
	private HashMap<Parameter<?>, Object> parsed;
	
	public CommandScape(Parameters params, String[] original, List<String> proposals) {
		assert original != null;
		this.original = original;
		this.parsed = null;
		this.overflow = null;
		this.proposals = proposals;
	}
	
	public CommandScape(Parameters params, String[] original) {
		assert original != null;
		this.original = original;
		this.parsed = new HashMap<>();
		this.proposals = null;
		
		String[] toParse = Arrays.copyOfRange(original, 0, params.size());
		
		for (int i = 0; i < params.size(); i++) {
			Parameter<?> param = params.get(i);
			String input = toParse[i];
			parsed.put(param, param.accept(input));
		}
		
		if (params.size() >= original.length) {
			this.overflow = new String[]{};
		} else if (!params.allowsOverflow()) {
			throw new CommandException(String.format("Too many arguments, expected no more than %s.", params.size()));
		} else {
			this.overflow = Arrays.copyOfRange(original, params.size(), original.length);
		}
	}
	
	public CommandScape(CommandScape toCast) {
		this.original = toCast.original;
		this.parsed = toCast.parsed;
		this.proposals = toCast.proposals;
		this.overflow = toCast.overflow;
	}
	
	public String[] original() {
		return original;
	}
	
	public String[] overflow() {
		assert overflow != null : new UnsupportedOperationException();
		return overflow;
	}
	
	public List<String> proposals() {
		assert proposals != null : new UnsupportedOperationException();
		return proposals;
	}
	public <T> T get(int index) {
		return get(EQUAL_INDEX, index);
	}
	
	public <T> T get(String name) {
		return get(EQUAL_NAME, name);
	}
	
	@SuppressWarnings("unchecked")
	private <T, U> T get(BiPredicate<Parameter<?>, U> filter, U identifier) {
		assert parsed != null : new UnsupportedOperationException();
		for (Entry<Parameter<?>, Object> entry : parsed.entrySet()) {
			if (filter.test(entry.getKey(), identifier)) {
				try {
					return (T) entry.getValue();
				} catch (ClassCastException e) {
					throw new ArgumentException("Wrong class type requested for parameter " + identifier + ": " + e.getMessage());
				}
			}
		}
		throw new ArgumentException("Requested parameter does not exist: " + identifier);
	}

}
