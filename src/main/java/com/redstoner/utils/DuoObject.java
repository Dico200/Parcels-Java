package com.redstoner.utils;

import static com.google.common.base.Preconditions.checkNotNull;

public class DuoObject<T, U> {

    public DuoObject(T v1, U v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    protected final T v1;
    protected final U v2;

    public T v1() {
        return v1;
    }

    public U v2() {
        return v2;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", v1, v2);
    }

    @Override
    public int hashCode() {
        return 31 * (31 + ((v1 == null) ? 0 : v1.hashCode())) + ((v2 == null) ? 0 : v2.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DuoObject) {
            DuoObject<?, ?> other = (DuoObject<?, ?>) obj;
            return (v1 == other.v1 || (v1 != null && v1.equals(other.v1))) && (v2 == other.v2 || (v2 != null && v2.equals(other.v2)));
        }
        return false;
    }

    public static class Coord extends DuoObject<Integer, Integer> {

        public static Coord of(int x, int z) {
            return new Coord(x, z);
        }

        private Coord(int v1, int v2) {
            super(v1, v2);
        }

        public int getX() {
            return v1;
        }

        public int getZ() {
            return v2;
        }

    }

    public static class DCoord extends DuoObject<Double, Double> {

        public DCoord(double v1, double v2) {
            super(v1, v2);
        }

        public double getX() {
            return v1;
        }

        public double getZ() {
            return v2;
        }

    }

    public static class Entry<K, V> extends DuoObject<K, V> {

        public Entry(K v1, V v2) {
            super(v1, v2);
        }

        public K getKey() {
            return v1;
        }

        public V getValue() {
            return v2;
        }

    }

    public static class BlockType extends DuoObject<Short, Byte> {

        public static BlockType fromString(String s) throws NumberFormatException {
            checkNotNull(s);
            String[] both = s.split(":");
            String id;
            String data = "0";
            switch (both.length) {
                case 2:
                    data = both[1];
                case 1:
                    id = both[0];
                    break;
                default:
                    throw new NumberFormatException();
            }
            return new BlockType(Short.parseShort(id), Byte.parseByte(data));
        }

        public BlockType(Short v1, Byte v2) {
            super(v1, v2);
        }

        public short getId() {
            return v1;
        }

        public byte getData() {
            return v2;
        }

    }

}