package com.qlktx.dao;

import com.qlktx.model.AuthAccount;
import com.qlktx.model.Role;
import com.qlktx.model.SessionUser;
import com.qlktx.util.DatabaseUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class QuanTriVienDAO {
    public Optional<AuthAccount> findAccount(String username) throws SQLException {
        String sql = "SELECT MaTK,TenDangNhap,MatKhauHash,TrangThai FROM dbo.TaiKhoan WHERE TenDangNhap=? AND VaiTro='ADMIN' AND MaSV IS NULL";
        try (Connection connection = DatabaseUtil.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                SessionUser user = new SessionUser(result.getString("MaTK"), result.getString("TenDangNhap"),
                        "Quản trị viên",null, Role.ADMIN);
                return Optional.of(new AuthAccount(user, result.getString("MatKhauHash"), result.getBoolean("TrangThai")));
            }
        }
    }

    public boolean createIfMissing(String username, String name, String hash) throws SQLException {
        try (Connection connection = DatabaseUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement check = connection.prepareStatement(
                        "SELECT 1 FROM dbo.TaiKhoan WITH (UPDLOCK, HOLDLOCK) WHERE TenDangNhap=? OR VaiTro='ADMIN'")) {
                    check.setString(1, username);
                    try (ResultSet result = check.executeQuery()) {
                        if (result.next()) { connection.rollback(); return false; }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO dbo.TaiKhoan (TenDangNhap,MatKhauHash,VaiTro,MaSV,TrangThai) VALUES (?,?,'ADMIN',NULL,1)")) {
                    statement.setString(1, username);
                    statement.setString(2, hash);
                    statement.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException e) { connection.rollback(); throw e; }
        }
    }
}
