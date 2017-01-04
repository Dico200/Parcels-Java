package com.redstoner.utils.sql.control;

import java.util.function.Consumer;

public interface UnsafeConsumer<T> extends Consumer<T> {

    @Override
    default void accept(T object) {
        try {
            acceptUnsafe(object);
        } catch (Exception ignored) {
        }
    }

    void acceptUnsafe(T object) throws Exception;

}
