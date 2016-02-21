package com.redstoner.utils.mysql.future;

public class Condition {
	
	public static void main(String[] args) {
		System.out.println(Condition.create("world").is("parcels").and("px").is(0).and("pz").is(0));
		
		System.out.println(Condition.create(Condition.create("world").is("parcels").or("world").is("plots")).and("px").is(0).and("pz").is(2));
	}
	
	public static Getter create(String column) {
		return new Getter(new Condition(), column);
	}
	
	public static Getter create(int column) {
		return create(String.format("$%s$", column));
	}
	
	public static Condition create(Condition condition) {
		return new Condition(String.format("(%s)", condition.toString()));
	}
	
	StringBuilder collection;
	
	private Condition() {
		this("");
	}
	
	private Condition(String start) {
		this.collection = new StringBuilder(start);
	}
	
	public Getter and(String column) {
		collection.append(" AND ");
		return new Getter(this, column);
	}
	
	public Getter and(int column) {
		return this.and(String.format("$%s$", column));
	}
	
	public Condition and(Condition condition) {
		collection.append(String.format(" AND (%s)", condition.toString()));
		return this;
	}
	
	public Getter or(String column) {
		collection.append(" OR ");
		return new Getter(this, column);
	}
	
	public Getter or(int column) {
		return this.or(String.format("$%s$", column));
	}
	
	public Condition or(Condition condition) {
		collection.append(String.format(" OR (%s)", condition.toString()));
		return this;
	}
	
	@Override
	public String toString() {
		return collection.toString();
	}
	
}

class Getter {
	
	private Condition start;
	private String column;
	
	Getter(Condition start, String column) {
		this.start = start;
		this.column = column;
	}
	
	public Condition is(Object value) {
		start.collection.append(String.format("`%s`=`%s`", column, value));
		return start;
	}
	
}

