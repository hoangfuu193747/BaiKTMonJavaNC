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

public class SinhVienDAO {
    public Optional<AuthAccount> findAccount(String maSV) throws SQLException {
        String sql = "SELECT t.MaSV,t.TenDangNhap,t.MatKhauHash,t.TrangThai,d.HoTen,d.Email,d.TrangThai AS SchoolActive "
                + "FROM dbo.TaiKhoan t JOIN dbo.SinhVien s ON s.MaTK=t.MaTK AND s.MaSV=t.MaSV "
                + "JOIN dbo.SinhVienTruong d ON d.MaSV=t.MaSV WHERE t.MaSV=? AND t.TenDangNhap=? AND t.VaiTro='SINH_VIEN'";
        try (Connection connection = DatabaseUtil.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maSV);
            statement.setString(2,maSV);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                SessionUser user = new SessionUser(result.getString("MaSV"), result.getString("MaSV"),
                        result.getString("HoTen"), result.getString("Email"), Role.STUDENT);
                return Optional.of(new AuthAccount(user, result.getString("MatKhauHash"), result.getBoolean("TrangThai")&&result.getBoolean("SchoolActive")));
            }
        }
    }

    public boolean exists(Connection connection, String maSV) throws SQLException {
        return hasValue(connection, "SELECT 1 FROM dbo.TaiKhoan WHERE MaSV=? OR TenDangNhap=?", maSV);
    }

    private boolean hasValue(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setString(2,value);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    public void insert(Connection connection, String maSV, String email, String phone, String hash) throws SQLException {
        int accountId=com.qlktx.util.JdbcUtil.insert(connection,
                "INSERT INTO dbo.TaiKhoan (TenDangNhap,MatKhauHash,VaiTro,MaSV,TrangThai) VALUES (?,?,'SINH_VIEN',?,1)",maSV,hash,maSV);
        com.qlktx.util.JdbcUtil.update(connection,"INSERT INTO dbo.SinhVien (MaSV,MaTK,SoDienThoai) VALUES (?,?,?)",maSV,accountId,phone.isBlank()?null:phone);
    }
}
