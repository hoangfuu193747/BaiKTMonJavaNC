package com.qlktx.util;
import com.qlktx.service.BusinessException;
import java.sql.*;
public final class TransactionUtil {
    private TransactionUtil() { }
    @FunctionalInterface public interface Work<T> { T execute(Connection connection) throws SQLException, BusinessException; }
    public static <T> T run(Work<T> work) throws SQLException, BusinessException {
        try (Connection connection = DatabaseUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | BusinessException | RuntimeException failure) {
                try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }
    }
}

