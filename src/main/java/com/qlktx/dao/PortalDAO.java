package com.qlktx.dao;

import com.qlktx.util.DisplayUtil;
import com.qlktx.util.JdbcUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Queries for the current eleven-table schema. Personal queries always bind the session identity. */
public class PortalDAO {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static Map<String, Object> row(ResultSet result) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        var metadata = result.getMetaData();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            String key = metadata.getColumnLabel(i);
            Object value = result.getObject(i);
            values.put(key, value);
            if (value instanceof java.sql.Date date) values.put(key + "Text", date.toLocalDate().format(DATE));
            else if (value instanceof Timestamp time) values.put(key + "Text", time.toLocalDateTime().format(TIME));
            else if (value instanceof BigDecimal money) values.put(key + "Text", DisplayUtil.money(money));
        }
        return values;
    }

    public Map<String, Object> profile(String studentId) throws SQLException {
        return JdbcUtil.query("""
            SELECT d.MaSV AS maSV,d.HoTen AS hoTen,d.NgaySinh AS ngaySinh,d.GioiTinh AS gioiTinh,
                   d.Lop AS lop,d.Khoa AS khoa,d.Email AS email,s.SoDienThoai AS soDienThoai,
                   s.NgayDangKy AS ngayDangKy
            FROM dbo.SinhVienTruong d JOIN dbo.SinhVien s ON s.MaSV=d.MaSV WHERE d.MaSV=?
            """, PortalDAO::row, studentId).stream().findFirst().orElse(Map.of());
    }

    private static final String ROOMS = """
        SELECT p.MaPhong AS maPhong,p.MaKhu AS maKhu,k.TenKhu AS tenKhu,p.Tang AS tang,
               k.GioiTinh AS gioiTinh,p.SucChua AS sucChua,p.GiaMoiSV AS giaMoiSV,
               p.TrangThai AS trangThai,k.TrangThai AS khuHoatDong,
               occupancy.dangO AS dangO,p.SucChua-occupancy.dangO AS conCho
        FROM dbo.Phong p JOIN dbo.KhuNha k ON k.MaKhu=p.MaKhu
        CROSS APPLY (SELECT COUNT(*) AS dangO FROM dbo.HopDong h
                     WHERE h.MaPhong=p.MaPhong AND h.TrangThai='DANG_O') occupancy
        """;

    public List<Map<String, Object>> rooms(String search, String zone, String floor,
            String gender, BigDecimal maxPrice, boolean available) throws SQLException {
        return JdbcUtil.query(ROOMS + """
            WHERE (?='' OR p.MaPhong LIKE ? OR k.TenKhu LIKE ?)
              AND (?='' OR p.MaKhu=?) AND (?='' OR CAST(p.Tang AS varchar(2))=?)
              AND (?='' OR k.GioiTinh=?) AND (? IS NULL OR p.GiaMoiSV<=?)
              AND (?=0 OR (p.TrangThai='HOAT_DONG' AND k.TrangThai=1 AND occupancy.dangO<p.SucChua))
            ORDER BY p.MaKhu,p.Tang,p.SoPhong
            """, PortalDAO::row, search, "%" + search + "%", "%" + search + "%",
                zone, zone, floor, floor, gender, gender, maxPrice, maxPrice, available);
    }

    public Optional<Map<String, Object>> room(String roomId) throws SQLException {
        return JdbcUtil.query(ROOMS + " WHERE p.MaPhong=?", PortalDAO::row, roomId).stream().findFirst();
    }

    public List<Map<String, Object>> registrations(String studentId, String status) throws SQLException {
        return JdbcUtil.query("""
            SELECT r.MaDK AS maDK,r.MaSV AS maSV,d.HoTen AS hoTen,r.MaPhong AS maPhong,
                   k.TenKhu AS tenKhu,k.GioiTinh AS gioiTinhPhong,d.GioiTinh AS gioiTinh,
                   r.NgayDangKy AS ngayDangKy,r.TrangThai AS trangThai,r.NgayDuyet AS ngayDuyet,
                   r.LyDoTuChoi AS lyDoTuChoi,p.SucChua AS sucChua,p.TrangThai AS trangThaiPhong,
                   k.TrangThai AS khuHoatDong,t.TrangThai AS taiKhoanHoatDong,d.TrangThai AS hoSoHoatDong,
                   (SELECT COUNT(*) FROM dbo.HopDong h WHERE h.MaPhong=r.MaPhong AND h.TrangThai='DANG_O') AS dangO,
                   (SELECT COUNT(*) FROM dbo.HopDong h WHERE h.MaSV=r.MaSV AND h.TrangThai='DANG_O') AS hopDongDangO
            FROM dbo.DangKyPhong r JOIN dbo.SinhVienTruong d ON d.MaSV=r.MaSV
            JOIN dbo.SinhVien s ON s.MaSV=r.MaSV JOIN dbo.TaiKhoan t ON t.MaTK=s.MaTK
            JOIN dbo.Phong p ON p.MaPhong=r.MaPhong JOIN dbo.KhuNha k ON k.MaKhu=p.MaKhu
            WHERE (?='' OR r.MaSV=?) AND (?='' OR r.TrangThai=?) ORDER BY r.MaDK DESC
            """, PortalDAO::row, studentId, studentId, status, status);
    }

    public List<Map<String, Object>> contracts(String studentId) throws SQLException {
        return JdbcUtil.query("""
            SELECT h.MaHDong AS maHDong,h.MaDK AS maDK,h.MaSV AS maSV,d.HoTen AS hoTen,
                   d.Lop AS lop,d.Khoa AS khoa,d.Email AS email,s.SoDienThoai AS soDienThoai,
                   h.MaPhong AS maPhong,k.TenKhu AS tenKhu,p.Tang AS tang,p.GiaMoiSV AS giaMoiSV,
                   h.NgayBatDau AS ngayBatDau,h.NgayKetThuc AS ngayKetThuc,h.TrangThai AS trangThai
            FROM dbo.HopDong h JOIN dbo.SinhVienTruong d ON d.MaSV=h.MaSV
            JOIN dbo.SinhVien s ON s.MaSV=h.MaSV JOIN dbo.Phong p ON p.MaPhong=h.MaPhong
            JOIN dbo.KhuNha k ON k.MaKhu=p.MaKhu
            WHERE (?='' OR h.MaSV=?) ORDER BY h.MaHDong DESC
            """, PortalDAO::row, studentId, studentId);
    }

    public Optional<Map<String, Object>> contract(int id, String studentId) throws SQLException {
        return contracts(studentId).stream().filter(r -> number(r, "maHDong") == id).findFirst();
    }

    private static final String INVOICES = """
        SELECT i.MaHoaDon AS maHoaDon,i.MaHDong AS maHDong,h.MaSV AS maSV,d.HoTen AS hoTen,
               h.MaPhong AS maPhong,i.Thang AS thang,i.Nam AS nam,i.TienPhong AS tienPhong,
               i.TienDien AS tienDien,i.TienNuoc AS tienNuoc,i.TongTien AS tongTien,
               i.HanThanhToan AS hanThanhToan,paid.daDong AS daDong,paid.choXacNhan AS choXacNhan,
               i.TongTien-paid.daDong AS conNo,i.TongTien-paid.daDong-paid.choXacNhan AS coTheGui,
               CASE WHEN paid.daDong>=i.TongTien THEN 'DA_THANH_TOAN'
                    WHEN paid.daDong>0 THEN 'MOT_PHAN' ELSE 'CHUA_THANH_TOAN' END AS trangThai
        FROM dbo.HoaDon i JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong
        JOIN dbo.SinhVienTruong d ON d.MaSV=h.MaSV
        CROSS APPLY (SELECT COALESCE(SUM(CASE WHEN t.TrangThai='DA_XAC_NHAN' THEN t.SoTien ELSE 0 END),0) AS daDong,
                            COALESCE(SUM(CASE WHEN t.TrangThai='CHO_XAC_NHAN' THEN t.SoTien ELSE 0 END),0) AS choXacNhan
                     FROM dbo.ThanhToan t WHERE t.MaHoaDon=i.MaHoaDon) paid
        """;

    public List<Map<String, Object>> invoices(String studentId) throws SQLException {
        return JdbcUtil.query(INVOICES + " WHERE (?='' OR h.MaSV=?) ORDER BY i.Nam DESC,i.Thang DESC,i.MaHoaDon DESC",
                PortalDAO::row, studentId, studentId);
    }

    public Optional<Map<String, Object>> invoice(int id, String studentId) throws SQLException {
        return JdbcUtil.query(INVOICES + " WHERE i.MaHoaDon=? AND (?='' OR h.MaSV=?)",
                PortalDAO::row, id, studentId, studentId).stream().findFirst();
    }

    public List<Map<String, Object>> payments(String studentId) throws SQLException {
        return JdbcUtil.query("""
            SELECT t.MaTT AS maTT,t.MaHoaDon AS maHoaDon,h.MaSV AS maSV,d.HoTen AS hoTen,
                   h.MaPhong AS maPhong,i.Thang AS thang,i.Nam AS nam,t.SoTien AS soTien,
                   t.PhuongThuc AS phuongThuc,t.MaGiaoDich AS maGiaoDich,t.NgayTao AS ngayTao,
                   t.TrangThai AS trangThai,t.NgayXacNhan AS ngayXacNhan
            FROM dbo.ThanhToan t JOIN dbo.HoaDon i ON i.MaHoaDon=t.MaHoaDon
            JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong JOIN dbo.SinhVienTruong d ON d.MaSV=h.MaSV
            WHERE (?='' OR h.MaSV=?) ORDER BY t.MaTT DESC
            """, PortalDAO::row, studentId, studentId);
    }

    public List<Map<String, Object>> repairs(String studentId) throws SQLException {
        return JdbcUtil.query("""
            SELECT y.MaYC AS maYC,y.MaSV AS maSV,d.HoTen AS hoTen,y.MaPhong AS maPhong,
                   y.NoiDung AS noiDung,y.NgayGui AS ngayGui,y.TrangThai AS trangThai
            FROM dbo.YeuCauSuaChua y JOIN dbo.SinhVienTruong d ON d.MaSV=y.MaSV
            WHERE (?='' OR y.MaSV=?) ORDER BY y.MaYC DESC
            """, PortalDAO::row, studentId, studentId);
    }

    public List<Map<String, Object>> notifications(String search) throws SQLException {
        return JdbcUtil.query("""
            SELECT MaTB AS maTB,TieuDe AS tieuDe,NoiDung AS noiDung,NgayDang AS ngayDang
            FROM dbo.ThongBao WHERE (?='' OR TieuDe LIKE ? OR NoiDung LIKE ?) ORDER BY MaTB DESC
            """, PortalDAO::row, search, "%" + search + "%", "%" + search + "%");
    }

    public static int number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).intValue();
    }

    public static BigDecimal money(Map<String, Object> row, String key) {
        return (BigDecimal) row.get(key);
    }

    public static List<Map<String, Object>> query(Connection connection, String sql, Object... params) throws SQLException {
        return JdbcUtil.query(connection, sql, PortalDAO::row, params);
    }
}
