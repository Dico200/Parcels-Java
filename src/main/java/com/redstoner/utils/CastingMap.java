package com.redstoner.utils;

import java.util.HashMap;

public class CastingMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = -2142136718375689729L;
	
	@SuppressWarnings("unchecked")
	public <T extends V> T getCasted(Object key) {
		return (T) super.get(key);
	}
	
}
