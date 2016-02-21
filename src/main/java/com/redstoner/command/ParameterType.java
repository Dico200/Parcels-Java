package com.redstoner.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.redstoner.utils.Values;

public abstract class ParameterType<T> {
	
	public static final ParameterType<String> STRING;
	public static final ParameterType<Integer> INTEGER;
	public static final ParameterType<Float> FLOAT;
	public static final ParameterType<Player> PLAYER;
	public static final ParameterType<OfflinePlayer> OFFLINE_PLAYER;
	public static final ParameterType<Boolean> BOOLEAN;
	
	private static final String EXC_MSG_FORMAT = "Argument '$ARG$' must be $DESC$, %s.";
	
	private String message;
	private String typeName;
	
	public ParameterType(String typeName, String requirement) {
		this.typeName = typeName;
		this.message = String.format(EXC_MSG_FORMAT, requirement);
	}
	
	public ParameterType(String typeName) {
		this.typeName = typeName;
		this.message = null;
	}
	
	protected String exceptionMessage() {
		Values.validate(this.message != null, new ArgumentException("No exception message can be thrown if there is no requirement"));
		return message;
	}
	
	protected String typeName() {
		return typeName;
	}
	
	abstract T handle(String input);
	
	List<String> complete(String input) {
		return new ArrayList<>();
	}
	
	static {
		
		STRING = new ParameterType<String>("") {

			@Override
			String handle(String input) {
				return input;
			}
		};
		
		INTEGER = new ParameterType<Integer>("Number", "a round number") {

			@Override
			Integer handle(String input) {
				try {
					return Integer.parseInt(input);
				} catch (NumberFormatException e) {
					throw new CommandException(exceptionMessage());
				}
			}

			@Override
			List<String> complete(String input) {
				List<Character> list = new ArrayList<>();
				char[] chars = input.toCharArray();
				for (int i = 0; i < chars.length; i++) {
					 if (Character.isDigit(chars[i])) {
						 list.add(chars[i]);
					 }
				}
				char[] result = new char[list.size()];
				for (int i = 0; i < result.length; i++) {
					result[i] = list.get(i);
				}
				return Arrays.asList(new String[]{String.valueOf(result)});
			}
		};
		
		FLOAT = new ParameterType<Float>("Amount", "a numeric value") {

			@Override
			Float handle(String input) {
				try {
					return Float.parseFloat(input);
				} catch (NumberFormatException e) {
					throw new CommandException(exceptionMessage());
				}
			}

			@Override
			List<String> complete(String input) {
				List<Character> list = new ArrayList<>();
				char[] chars = input.toCharArray();
				for (int i = 0; i < chars.length; i++) {
					char c = chars[i];
					if (Character.isDigit(c) || c == '.' || c == ',' || c == 'e' || c == 'E') {
						list.add(chars[i]);
					}
				}
				char[] result = new char[list.size()];
				for (int i = 0; i < result.length; i++) {
					result[i] = list.get(i);
				}
				return Arrays.asList(new String[]{String.valueOf(result)});
			}
		};
		
		PLAYER = new ParameterType<Player>("Player", "the name of an online player") {

			@Override
			Player handle(String input) {
				Player user = Bukkit.getPlayer(input);
				Validate.notNull(user, exceptionMessage());
				return user;
			}

			@Override
			List<String> complete(String input) {
				return Arrays.asList(Bukkit.matchPlayer(input).stream().map(player -> player.getName()).toArray(size -> new String[size]));
			}
		};
		
		OFFLINE_PLAYER = new ParameterType<OfflinePlayer>("Player", "the name of a known player") {

			@SuppressWarnings("deprecation")
			@Override
			OfflinePlayer handle(String input) {
				OfflinePlayer user = Bukkit.getOfflinePlayer(input);
				Validate.isTrue(user.hasPlayedBefore() || user.isOnline(), exceptionMessage());
				return user;
			}

			@Override
			List<String> complete(String input) {
				return Arrays.asList(Bukkit.matchPlayer(input).stream().map(player -> player.getName()).toArray(size -> new String[size]));
			}
		};
		
		BOOLEAN = new ParameterType<Boolean>("Bool", "true/yes/false/no") {
			
			private final String[] inputs = {"true", "false", "yes", "no"};
			private final boolean[] outputs = {true, false, true, false};

			@Override
			Boolean handle(String input) {
				if (input == null)
					return null;
				input = input.toLowerCase();
				for (int i = 0; i < inputs.length; i++)
					if (input.equals(inputs[i]))
						return outputs[i];
				throw new CommandException(exceptionMessage());
			}
			
			@Override
			List<String> complete(String input) {
				String starts = input.toLowerCase();
				return Arrays.stream(inputs).filter(string -> string.startsWith(starts)).collect(Collectors.toList());
			}
		};
	}
}
