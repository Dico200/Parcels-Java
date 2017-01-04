package com.redstoner.utils.sql.control;

import java.sql.ResultSet;

public interface ResultSetProcedure extends UnsafeConsumer<ResultSet> {

    @Override
    default void acceptUnsafe(ResultSet object) throws Exception {
        doProcedure(object);
    }

    void doProcedure(ResultSet set) throws Exception;

}
