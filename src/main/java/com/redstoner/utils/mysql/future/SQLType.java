package com.redstoner.utils.mysql.future;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;

public class SQLType<T> {
	
	public static final SQLType<Integer> 	INT = new SQLType<>("INT", "getInt");
	public static final SQLType<Long> 		BIGINT = new SQLType<>("BIGINT", "getLong");
	public static final SQLType<String> 	VARCHAR = new SQLType<>("VARCHAR(%s)", "getString");
	
	public static <T> SQLType<T> addLength(int length, SQLType<T> wrapped) {
		if (!wrapped.needsLength)
			return null;
		SQLType<T> result = new SQLType<T>(String.format(wrapped.type, length), wrapped.intMethod, wrapped.stringMethod);
		result.needsLength = false;
		return result;
	}

	private final String type;
	private final Method stringMethod, intMethod;
	private boolean needsLength;
	
	private SQLType(String type, String method) {
		this.needsLength = type.contains("(%s)");
		this.type = type;
		try {
			this.intMethod = ResultSet.class.getDeclaredMethod(method, int.class);
			this.stringMethod = ResultSet.class.getDeclaredMethod(method, String.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	private SQLType(String type, Method intMethod, Method stringMethod) {
		this.type = type;
		this.intMethod = intMethod;
		this.stringMethod = stringMethod;
	}
	
	public String type() {
		return type;
	}
	
	@SuppressWarnings("unchecked")
	public T get(ResultSet set, String col) {
		if (needsLength)
			throw new UnsupportedOperationException();
		try {
			return (T) stringMethod.invoke(set, col);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public T get(ResultSet set, int col) {
		if (needsLength)
			throw new UnsupportedOperationException();
		try {
			return (T) intMethod.invoke(set, col);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
