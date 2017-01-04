package com.redstoner.utils.sql.control;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.Consumer;

public interface SQL {
    // this class actually turned out to be useless which is nice

    boolean isQuery();

    String getSql();

    Connection getConnection() throws NullPointerException;

    default Connection getConnectionSafely() {
        try {
            return getConnection();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    void setConnection(Connection conn);

    ExceptionHandler getExceptionHandler();

    void executeStatementUnsafe(Connection conn) throws Exception;

    default void executeStatementUnsafe() throws Exception {
        executeStatementUnsafe(getConnection());
    }

    default void executeStatement(Connection conn) {
        try {
            executeStatementUnsafe(conn);
        } catch (Exception ex) {
            getExceptionHandler().handle(ex);
        }
    }

    default void executeStatement() {
        executeStatement(getConnection());
    }

    SQL onError(ExceptionHandler exceptionHandler);

    default SQL suppress() {
        return onError(ExceptionHandler.SUPPRESSED);
    }

    default SQL log(String action) {
        return onError(ExceptionHandler.log(Unprepared.defaultExceptionLogger, action));
    }

    default SQL log(PrintStream out, String action) {
        return onError(ExceptionHandler.log(out, action));
    }

    default SQL log(Consumer<String> out, String action) {
        return onError(ExceptionHandler.log(out, action));
    }

    default Prepared.Dynamic asDynamicallyPrepared() {
        return (Prepared.Dynamic) this;
    }

    interface Query extends SQL {
        @Override
        default boolean isQuery() {
            return true;
        }

        @Override
        Query onError(ExceptionHandler exceptionHandler);

        @Override
        default Query suppress() {
            return onError(ExceptionHandler.SUPPRESSED);
        }

        @Override
        default Query log(String action) {
            return onError(ExceptionHandler.log(Unprepared.defaultExceptionLogger, action));
        }

        @Override
        default Query log(PrintStream out, String action) {
            return onError(ExceptionHandler.log(out, action));
        }

        @Override
        default Query log(Consumer<String> out, String action) {
            return onError(ExceptionHandler.log(out, action));
        }

        @Override
        default void executeStatementUnsafe(Connection conn) throws Exception {
            executeUnsafe(conn);
        }

        ResultSet executeUnsafe(Connection conn) throws Exception;

        default ResultSet executeUnsafe() throws Exception {
            return executeUnsafe(getConnection());
        }

        default ResultSet execute(Connection conn) {
            try {
                return executeUnsafe(conn);
            } catch (Exception ex) {
                getExceptionHandler().handle(ex);
                return null;
            }
        }

        default ResultSet execute() {
            return execute(getConnection());
        }

        default void forEachRow(Connection conn, ResultSetProcedure procedure) {
            forEachRow(conn, getExceptionHandler().isUnsafe() ? ExceptionHandler.SUPPRESSED : getExceptionHandler(), procedure);
        }

        default void forEachRow(ResultSetProcedure procedure) {
            forEachRow(getConnection(), procedure);
        }

        default void forEachRow(Connection conn, ExceptionHandler rowHandler, ResultSetProcedure procedure) {
            Objects.requireNonNull(rowHandler);
            if (rowHandler.isUnsafe()) {
                throw new IllegalArgumentException("row's exception handler may not throw exceptions");
            }
            try (ResultSet set = execute(conn)) {
                if (set == null) {
                    // exception handled
                    return;
                }
                while (set.next()) {
                    try {
                        procedure.doProcedure(set);
                    } catch (Exception ex) {
                        rowHandler.handle(ex);
                    }
                }
            } catch (Exception ex) {
                getExceptionHandler().handle(ex);
            }
        }

        default void forEachRow(ExceptionHandler rowHandler, ResultSetProcedure procedure) {
            forEachRow(getConnection(), rowHandler, procedure);
        }

        @Override
        default Prepared.Dynamic.Query asDynamicallyPrepared() {
            return (Prepared.Dynamic.Query) this;
        }
    }

    interface Update extends SQL {
        @Override
        default boolean isQuery() {
            return false;
        }

        @Override
        Update onError(ExceptionHandler exceptionHandler);

        @Override
        default Update suppress() {
            return onError(ExceptionHandler.SUPPRESSED);
        }

        @Override
        default Update log(String action) {
            return onError(ExceptionHandler.log(Unprepared.defaultExceptionLogger, action));
        }

        @Override
        default Update log(PrintStream out, String action) {
            return onError(ExceptionHandler.log(out, action));
        }

        @Override
        default Update log(Consumer<String> out, String action) {
            return onError(ExceptionHandler.log(out, action));
        }

        @Override
        default void executeStatementUnsafe(Connection conn) throws Exception {
            executeUnsafe(conn);
        }

        int executeUnsafe(Connection conn) throws Exception;

        default int executeUnsafe() throws Exception {
            return executeUnsafe(getConnection());
        }

        default int execute(Connection conn) {
            try {
                return executeUnsafe(conn);
            } catch (Exception ex) {
                getExceptionHandler().handle(ex);
                return -1;
            }
        }

        default int execute() {
            return execute(getConnection());
        }

        @Override
        default Prepared.Dynamic.Update asDynamicallyPrepared() {
            return (Prepared.Dynamic.Update) this;
        }
    }

    static Query newQuery(String sql) {
        return new Unprepared.Query(sql);
    }

    static Query newQuery(Connection conn, String sql) {
        return new Unprepared.Query(conn, sql);
    }

    static Update newUpdate(String sql) {
        return new Unprepared.Update(sql);
    }

    static Update newUpdate(Connection conn, String sql) {
        return new Unprepared.Update(conn, sql);
    }

    abstract class Prepared extends Unprepared {

        Prepared(String sql) {
            super(sql);
        }

        Prepared(Connection conn, String sql) {
            super(conn, sql);
        }

        protected PreparedStatement createStatement(Connection conn) throws Exception {
            try (PreparedStatement psm = conn.prepareStatement(sql)) {
                prepare(psm);
                return psm;
            }
        }

        protected abstract void prepare(PreparedStatement psm) throws Exception;

        public static SQL.Query newQuery(String sql, Preparer preparer) {
            return newQuery(null, sql, preparer);
        }

        public static SQL.Query newQuery(Connection conn, String sql, Preparer preparer) {
            return new Query(conn, sql) {
                @Override
                protected void prepare(PreparedStatement psm) throws Exception {
                    preparer.prepare(psm);
                }
            };
        }

        public static abstract class Query extends Prepared implements SQL.Query {

            public Query(String sql) {
                super(sql);
            }

            public Query(Connection conn, String sql) {
                super(conn, sql);
            }

            @Override
            public SQL.Query onError(ExceptionHandler exceptionHandler) {
                return (SQL.Query) super.onError(exceptionHandler);
            }

            @Override
            public ResultSet executeUnsafe(Connection conn) throws Exception {
                try (PreparedStatement psm = createStatement(conn)) {
                    return psm.executeQuery();
                }
            }

        }

        public static SQL.Update newUpdate(String sql, Preparer consumer) {
            return newUpdate(null, sql, consumer);
        }

        public static SQL.Update newUpdate(Connection conn, String sql, Preparer preparer) {
            return new Update(conn, sql) {
                @Override
                protected void prepare(PreparedStatement psm) throws Exception {
                    preparer.prepare(psm);
                }
            };
        }

        public static abstract class Update extends Prepared implements SQL.Update {

            public Update(String sql) {
                super(sql);
            }

            public Update(Connection conn, String sql) {
                super(conn, sql);
            }

            @Override
            public SQL.Update onError(ExceptionHandler exceptionHandler) {
                return (SQL.Update) super.onError(exceptionHandler);
            }

            @Override
            public int executeUnsafe(Connection conn) throws Exception {
                try (PreparedStatement psm = createStatement(conn)) {
                    return psm.executeUpdate();
                }
            }

        }

        public interface Dynamic extends SQL {
            Preparer getDefaultPreparer() throws NullPointerException;

            default Preparer getDefaultPreparerSafely() {
                try {
                    return getDefaultPreparer();
                } catch (NullPointerException ex) {
                    return null;
                }
            }

            void setDefaultPreparer(Preparer defaultPreparer);

            @Override
            Dynamic onError(ExceptionHandler exceptionHandler);

            @Override
            default Dynamic suppress() {
                return onError(ExceptionHandler.SUPPRESSED);
            }

            @Override
            default Dynamic log(String action) {
                return onError(ExceptionHandler.log(Unprepared.defaultExceptionLogger, action));
            }

            @Override
            default Dynamic log(PrintStream out, String action) {
                return onError(ExceptionHandler.log(out, action));
            }

            @Override
            default Dynamic log(Consumer<String> out, String action) {
                return onError(ExceptionHandler.log(out, action));
            }

            void executeStatementUnsafe(Connection conn, Preparer preparer) throws Exception;

            default void executeStatementUnsafe(Preparer preparer) throws Exception {
                executeStatementUnsafe(getConnection(), preparer);
            }

            default void executeStatement(Connection conn, Preparer preparer) {
                try {
                    executeStatementUnsafe(conn, preparer);
                } catch (Exception ex) {
                    getExceptionHandler().handle(ex);
                }
            }

            default void executeStatement(Preparer preparer) {
                executeStatement(getConnection(), preparer);
            }

            interface Query extends SQL.Query, Dynamic {
                @Override
                Dynamic.Query onError(ExceptionHandler exceptionHandler);

                @Override
                default Dynamic.Query suppress() {
                    return onError(ExceptionHandler.SUPPRESSED);
                }

                @Override
                default Dynamic.Query log(String action) {
                    return onError(ExceptionHandler.log(Unprepared.defaultExceptionLogger, action));
                }

                @Override
                default Dynamic.Query log(PrintStream out, String action) {
                    return onError(ExceptionHandler.log(out, action));
                }

                @Override
                default Dynamic.Query log(Consumer<String> out, String action) {
                    return onError(ExceptionHandler.log(out, action));
                }

                @Override
                default void executeStatementUnsafe(Connection conn, Preparer preparer) throws Exception {
                    executeUnsafe(conn, preparer);
                }

                @Override
                default void executeStatementUnsafe(Connection conn) throws Exception {
                    executeStatementUnsafe(conn, getDefaultPreparer());
                }

                @Override
                default ResultSet executeUnsafe(Connection conn) throws Exception {
                    return executeUnsafe(conn, getDefaultPreparer());
                }

                ResultSet executeUnsafe(Connection conn, Preparer preparer) throws Exception;

                default ResultSet executeUnsafe(Preparer preparer) throws Exception {
                    return executeUnsafe(getConnection(), preparer);
                }

                default ResultSet execute(Connection conn, Preparer preparer) {
                    try {
                        return executeUnsafe(conn, preparer);
                    } catch (Exception ex) {
                        getExceptionHandler().handle(ex);
                        return null;
                    }
                }

                default ResultSet execute(Preparer preparer) {
                    return execute(getConnection(), preparer);
                }

                default void forEachRow(Connection conn, Preparer preparer, ResultSetProcedure procedure) {
                    forEachRow(conn, preparer, getExceptionHandler().isUnsafe() ? ExceptionHandler.SUPPRESSED : getExceptionHandler(), procedure);
                }

                default void forEachRow(Preparer preparer, ResultSetProcedure procedure) {
                    forEachRow(getConnection(), preparer, procedure);
                }

                default void forEachRow(Connection conn, Preparer preparer, ExceptionHandler rowHandler, ResultSetProcedure procedure) {
                    Objects.requireNonNull(rowHandler);
                    if (rowHandler.isUnsafe()) {
                        throw new IllegalArgumentException("row's exception handler may not throw exceptions");
                    }
                    try (ResultSet set = execute(conn, preparer)) {
                        if (set == null) {
                            // exception handled
                            return;
                        }
                        while (set.next()) {
                            try {
                                procedure.doProcedure(set);
                            } catch (Exception ex) {
                                rowHandler.handle(ex);
                            }
                        }
                    } catch (Exception ex) {
                        getExceptionHandler().handle(ex);
                    }
                }

                default void forEachRow(Preparer preparer, ExceptionHandler rowHandler, ResultSetProcedure procedure) {
                    forEachRow(getConnection(), preparer, rowHandler, procedure);
                }

            }

            interface Update extends SQL.Update, Dynamic {
                @Override
                Dynamic.Update onError(ExceptionHandler exceptionHandler);

                @Override
                default Dynamic.Update suppress() {
                    return onError(ExceptionHandler.SUPPRESSED);
                }

                @Override
                default Dynamic.Update log(String action) {
                    return onError(ExceptionHandler.log(Unprepared.defaultExceptionLogger, action));
                }

                @Override
                default Dynamic.Update log(PrintStream out, String action) {
                    return onError(ExceptionHandler.log(out, action));
                }

                @Override
                default Dynamic.Update log(Consumer<String> out, String action) {
                    return onError(ExceptionHandler.log(out, action));
                }

                @Override
                default void executeStatementUnsafe(Connection conn, Preparer preparer) throws Exception {
                    executeUnsafe(conn, preparer);
                }

                @Override
                default int executeUnsafe(Connection conn) throws Exception {
                    return executeUnsafe(conn, getDefaultPreparer());
                }

                int executeUnsafe(Connection conn, Preparer preparer) throws Exception;

                default int executeUnsafe(Preparer preparer) throws Exception {
                    return executeUnsafe(getConnection(), preparer);
                }

                default int execute(Connection conn, Preparer preparer) {
                    try {
                        return executeUnsafe(conn, preparer);
                    } catch (Exception ex) {
                        getExceptionHandler().handle(ex);
                        return -1;
                    }
                }

                default int execute(Preparer preparer) {
                    return execute(getConnection(), preparer);
                }

            }

            class DynamicQuery extends DynamicBase implements Dynamic.Query {

                public DynamicQuery(String sql) {
                    super(sql);
                }

                public DynamicQuery(Connection conn, String sql) {
                    super(conn, sql);
                }

                public DynamicQuery(String sql, Preparer defaultPreparer) {
                    super(sql, defaultPreparer);
                }

                public DynamicQuery(Connection conn, String sql, Preparer defaultPreparer) {
                    super(conn, sql, defaultPreparer);
                }

                @Override
                public Dynamic.Query onError(ExceptionHandler exceptionHandler) {
                    return (Dynamic.Query) super.onError(exceptionHandler);
                }

                @Override
                public ResultSet executeUnsafe(Connection conn, Preparer preparer) throws Exception {
                    return createStatement(conn, preparer).executeQuery();
                }

            }

            class DynamicUpdate extends DynamicBase implements Dynamic.Update {

                public DynamicUpdate(String sql) {
                    super(sql);
                }

                public DynamicUpdate(Connection conn, String sql) {
                    super(conn, sql);
                }

                public DynamicUpdate(String sql, Preparer defaultPreparer) {
                    super(sql, defaultPreparer);
                }

                public DynamicUpdate(Connection conn, String sql, Preparer defaultPreparer) {
                    super(conn, sql, defaultPreparer);
                }

                @Override
                public Dynamic.Update onError(ExceptionHandler exceptionHandler) {
                    return (Dynamic.Update) super.onError(exceptionHandler);
                }

                @Override
                public int executeUnsafe(Connection conn, Preparer preparer) throws Exception {
                    try (PreparedStatement sm = createStatement(conn, preparer)) {
                        return sm.executeUpdate();
                    }
                }

            }

            static Query newQuery(String sql) {
                return new DynamicQuery(sql);
            }

            static Query newQuery(Connection conn, String sql) {
                return new DynamicQuery(conn, sql);
            }

            static Query newQuery(String sql, Preparer preparer) {
                return new DynamicQuery(sql, preparer);
            }

            static Query newQuery(Connection conn, String sql, Preparer preparer) {
                return new DynamicQuery(conn, sql, preparer);
            }

            static Update newUpdate(String sql) {
                return new DynamicUpdate(sql);
            }

            static Update newUpdate(Connection conn, String sql) {
                return new DynamicUpdate(conn, sql);
            }

            static Update newUpdate(String sql, Preparer preparer) {
                return new DynamicUpdate(sql, preparer);
            }

            static Update newUpdate(Connection conn, String sql, Preparer preparer) {
                return new DynamicUpdate(conn, sql, preparer);
            }

        }

        static abstract class DynamicBase extends Prepared implements Dynamic {
            protected volatile Preparer defaultPreparer;

            DynamicBase(String sql) {
                super(sql);
            }

            DynamicBase(Connection conn, String sql) {
                super(conn, sql);
            }

            DynamicBase(String sql, Preparer defaultPreparer) {
                super(sql);
                this.defaultPreparer = defaultPreparer;
            }

            DynamicBase(Connection conn, String sql, Preparer defaultPreparer) {
                super(conn, sql);
                this.defaultPreparer = defaultPreparer;
            }

            @Override
            public Dynamic onError(ExceptionHandler exceptionHandler) {
                return (Dynamic) super.onError(exceptionHandler);
            }

            @Override
            public void setDefaultPreparer(Preparer defaultPreparer) {
                this.defaultPreparer = Objects.requireNonNull(defaultPreparer);
            }

            @Override
            public Preparer getDefaultPreparer() throws NullPointerException {
                if (defaultPreparer == null) {
                    throw new NullPointerException("default preparer not set");
                }
                return defaultPreparer;
            }

            @Override
            protected void prepare(PreparedStatement psm) throws Exception {
                prepare(psm, getDefaultPreparer());
            }

            protected void prepare(PreparedStatement psm, Preparer preparer) throws Exception {
                preparer.prepare(psm);
            }

            protected PreparedStatement createStatement(Connection conn, Preparer preparer) throws Exception {
                try (PreparedStatement psm = conn.prepareStatement(sql)) {
                    prepare(psm, getDefaultPreparer());
                    return psm;
                }
            }

        }

    }

    abstract class Unprepared implements SQL {
        private static volatile Consumer<String> defaultExceptionLogger = System.out::println;
        protected volatile Connection conn;
        protected volatile String sql;
        protected volatile ExceptionHandler exceptionHandler = ExceptionHandler.RUNTIME;

        Unprepared(String sql) {
            this.sql = Objects.requireNonNull(sql);
        }

        Unprepared(Connection conn, String sql) {
            this(sql);
            this.conn = conn;
        }

        //@Override
        public String getSql() {
            return null;
        }

        //@Override
        public Connection getConnection() throws NullPointerException {
            if (conn == null) {
                throw new NullPointerException("Connection not set");
            }
            return conn;
        }

        //@Override
        public void setConnection(Connection conn) {
            this.conn = conn;
        }

        //@Override
        public ExceptionHandler getExceptionHandler() {
            return exceptionHandler;
        }

        //@Override
        public SQL onError(ExceptionHandler exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        public static Consumer<String> getDefaultExceptionLogger() {
            return defaultExceptionLogger;
        }

        public static void setDefaultExceptionHandler(Consumer<String> defaultExceptionLogger) {
            Unprepared.defaultExceptionLogger = Objects.requireNonNull(defaultExceptionLogger);
        }

        public static class Query extends Unprepared implements SQL.Query {

            public Query(String sql) {
                super(sql);
            }

            public Query(Connection conn, String sql) {
                super(conn, sql);
            }

            @Override
            public SQL.Query onError(ExceptionHandler exceptionHandler) {
                return (SQL.Query) super.onError(exceptionHandler);
            }

            @Override
            public ResultSet executeUnsafe(Connection conn) throws Exception {
                try (Statement sm = conn.createStatement()) {
                    return sm.executeQuery(sql);
                }
            }

        }

        public static class Update extends Unprepared implements SQL.Update {

            public Update(String sql) {
                super(sql);
            }

            public Update(Connection conn, String sql) {
                super(conn, sql);
            }

            @Override
            public SQL.Update onError(ExceptionHandler exceptionHandler) {
                return (SQL.Update) super.onError(exceptionHandler);
            }

            @Override
            public int executeUnsafe(Connection conn) throws Exception {
                try (Statement sm = conn.createStatement()) {
                    return sm.executeUpdate(sql);
                }
            }

        }

    }

}