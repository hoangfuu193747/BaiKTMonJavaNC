package com.qlktx.dao;

import com.qlktx.model.DanhSachSinhVienTruong;
import com.qlktx.util.DatabaseUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class DanhSachSinhVienTruongDAO {
    public Optional<DanhSachSinhVienTruong> findByMaSV(String maSV) throws SQLException {
        try (Connection connection = DatabaseUtil.getConnection()) {
            return findByMaSV(connection, maSV);
        }
    }

    public Optional<DanhSachSinhVienTruong> findByMaSV(Connection connection, String maSV) throws SQLException {
        String sql = "SELECT MaSV,HoTen,NgaySinh,GioiTinh,Lop,Khoa,Email,TrangThai FROM dbo.SinhVienTruong WHERE MaSV=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maSV);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new DanhSachSinhVienTruong(result.getString("MaSV"),
                        result.getString("HoTen"), result.getObject("NgaySinh", LocalDate.class),
                        result.getString("GioiTinh"), result.getString("Lop"), result.getString("Email"),result.getString("Khoa"),result.getBoolean("TrangThai")));
            }
        }
    }
}
