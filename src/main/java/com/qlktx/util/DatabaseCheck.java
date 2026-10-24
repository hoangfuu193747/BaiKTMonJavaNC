package com.qlktx.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Startup check: fail before opening the website if database access is invalid. */
public final class DatabaseCheck {
    private DatabaseCheck() { }

    public static void main(String[] args) {
        try (Connection connection = DatabaseUtil.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT DB_NAME() AS DatabaseName");
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                System.err.println("Cannot read the connected database name.");
                System.exit(1);
            }
            System.out.println("SQL Server connection OK: " + result.getString("DatabaseName"));
        } catch (SQLException e) {
            boolean loginFailure = "28000".equals(e.getSQLState());
            for (SQLException current = e; current != null; current = current.getNextException()) {
                loginFailure |= current.getErrorCode() == 18456;
            }
            if (loginFailure) {
                System.err.println("SQL Server rejected the database login. Enter the SQL Server/SSMS password, not the website Admin password.");
            } else {
                System.err.println("Cannot connect to SQL Server. Check database configuration, SQL Server service and TCP port.");
                System.err.println("SQL state: " + e.getSQLState() + "; error code: " + e.getErrorCode());
            }
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("Cannot load database configuration. Check KTX_DB_CONFIG.");
            System.exit(1);
        }
    }
}
