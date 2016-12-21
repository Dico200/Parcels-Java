package com.redstoner.utils;

import com.redstoner.utils.DuoObject.Entry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Maps {

    @SafeVarargs
    public static <K, V> Map<K, V> putAll(Map<K, V> map, Entry<K, V>... entries) {
        Arrays.stream(entries).forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        return map;
    }

    public static <K, V> Map<K, V> putAll(Map<K, V> map, K[] keys, V[] values) {
        if (keys.length != values.length)
            throw new IllegalArgumentException();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> DuoObject<K[], V[]> strip(Map<K, V> map) {
        K[] karray = (K[]) map.keySet().toArray();
        V[] varray = (V[]) map.values().toArray();
        return new DuoObject<>(karray, varray);
    }

    public static class CastingMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = -2142136718375689729L;

        @SuppressWarnings("unchecked")
        public <T extends V> T getCasted(Object key) {
            return (T) super.get(key);
        }

    }

}
