package com.qlktx.util;

import java.sql.*;
import java.time.*;
import java.util.*;

public final class JdbcUtil {
    private JdbcUtil() { }
    @FunctionalInterface public interface Mapper<T> { T map(ResultSet row) throws SQLException; }
    public static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof LocalDate date) value = java.sql.Date.valueOf(date);
            if (value instanceof LocalDateTime time) value = Timestamp.valueOf(time);
            statement.setObject(i + 1, value);
        }
    }
    public static <T> List<T> query(Connection connection, String sql, Mapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(15);
            bind(statement, params);
            try (ResultSet result = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (result.next()) rows.add(mapper.map(result));
                return rows;
            }
        }
    }
    public static <T> List<T> query(String sql, Mapper<T> mapper, Object... params) throws SQLException {
        try (Connection connection = DatabaseUtil.getConnection()) { return query(connection, sql, mapper, params); }
    }
    public static int update(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(15);
            bind(statement, params);
            return statement.executeUpdate();
        }
    }
    public static int insert(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setQueryTimeout(15);
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Generated identifier is missing.");
                return keys.getInt(1);
            }
        }
    }
    public static boolean exists(Connection connection, String sql, Object... params) throws SQLException {
        return !query(connection, sql, result -> true, params).isEmpty();
    }
}

