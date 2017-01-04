package com.redstoner.utils.sql.control;

import java.sql.PreparedStatement;

public interface Preparer extends UnsafeConsumer<PreparedStatement> {

    @Override
    default void acceptUnsafe(PreparedStatement object) throws Exception {
        prepare(object);
    }

    void prepare(PreparedStatement psm) throws Exception;

}
