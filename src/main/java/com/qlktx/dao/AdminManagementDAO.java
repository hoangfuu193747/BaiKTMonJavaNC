package com.qlktx.dao;

import com.qlktx.util.JdbcUtil;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminManagementDAO {
    public List<Map<String,Object>> students(String search) throws SQLException {
        return JdbcUtil.query("""
            SELECT d.MaSV AS maSV,d.HoTen AS hoTen,d.NgaySinh AS ngaySinh,d.GioiTinh AS gioiTinh,
                   d.Lop AS lop,d.Khoa AS khoa,d.Email AS email,d.TrangThai AS hoSoHoatDong,
                   s.SoDienThoai AS soDienThoai,t.MaTK AS maTK,t.TrangThai AS taiKhoanHoatDong,
                   (SELECT TOP(1) h.MaPhong FROM dbo.HopDong h WHERE h.MaSV=d.MaSV AND h.TrangThai='DANG_O') AS maPhong
            FROM dbo.SinhVienTruong d LEFT JOIN dbo.SinhVien s ON s.MaSV=d.MaSV
            LEFT JOIN dbo.TaiKhoan t ON t.MaSV=d.MaSV AND t.VaiTro='SINH_VIEN'
            WHERE (?='' OR d.MaSV LIKE ? OR d.HoTen LIKE ? OR d.Email LIKE ? OR d.Lop LIKE ?)
            ORDER BY d.MaSV
            """,PortalDAO::row,search,"%"+search+"%","%"+search+"%","%"+search+"%","%"+search+"%");
    }

    public Optional<Map<String,Object>> student(String id) throws SQLException {
        return students(id).stream().filter(row -> id.equals(row.get("maSV"))).findFirst();
    }

    public List<Map<String,Object>> zones() throws SQLException {
        return JdbcUtil.query("""
            SELECT k.MaKhu AS maKhu,k.TenKhu AS tenKhu,k.GioiTinh AS gioiTinh,k.SoTang AS soTang,
                   k.TrangThai AS hoatDong,COUNT(p.MaPhong) AS soPhong,COALESCE(SUM(CAST(p.SucChua AS int)),0) AS tongCho
            FROM dbo.KhuNha k LEFT JOIN dbo.Phong p ON p.MaKhu=k.MaKhu
            GROUP BY k.MaKhu,k.TenKhu,k.GioiTinh,k.SoTang,k.TrangThai ORDER BY k.MaKhu
            """,PortalDAO::row);
    }

    public Map<String,Object> overview() throws SQLException {
        return JdbcUtil.query("""
            SELECT (SELECT COUNT(*) FROM dbo.SinhVienTruong) AS sinhVien,
                   (SELECT COUNT(*) FROM dbo.TaiKhoan WHERE VaiTro='SINH_VIEN' AND TrangThai=1) AS taiKhoan,
                   (SELECT COUNT(*) FROM dbo.Phong) AS soPhong,
                   (SELECT COUNT(*) FROM dbo.HopDong WHERE TrangThai='DANG_O') AS dangO,
                   (SELECT COUNT(*) FROM dbo.DangKyPhong WHERE TrangThai='CHO_DUYET') AS choDuyet,
                   (SELECT COUNT(*) FROM dbo.YeuCauSuaChua WHERE TrangThai<>'HOAN_THANH') AS suaChua
            """,PortalDAO::row).get(0);
    }

    public List<Map<String,Object>> reports(String zone,Integer month,Integer year) throws SQLException {
        return JdbcUtil.query("""
            SELECT k.MaKhu AS maKhu,k.TenKhu AS tenKhu,k.GioiTinh AS gioiTinh,
                (SELECT COUNT(*) FROM dbo.Phong p WHERE p.MaKhu=k.MaKhu) AS soPhong,
                (SELECT COALESCE(SUM(CAST(p.SucChua AS int)),0) FROM dbo.Phong p WHERE p.MaKhu=k.MaKhu) AS tongCho,
                (SELECT COUNT(*) FROM dbo.HopDong h JOIN dbo.Phong p ON p.MaPhong=h.MaPhong WHERE p.MaKhu=k.MaKhu AND h.TrangThai='DANG_O') AS dangO,
                (SELECT COUNT(*) FROM dbo.DangKyPhong r JOIN dbo.Phong p ON p.MaPhong=r.MaPhong WHERE p.MaKhu=k.MaKhu AND r.TrangThai='CHO_DUYET') AS choDuyet,
                COALESCE(finance.tongTien,0) AS tongTien,COALESCE(finance.daDong,0) AS daDong,
                COALESCE(finance.choXacNhan,0) AS choXacNhan,COALESCE(finance.tongTien-finance.daDong,0) AS conNo
            FROM dbo.KhuNha k OUTER APPLY (
                SELECT SUM(i.TongTien) AS tongTien,SUM(payments.daDong) AS daDong,SUM(payments.choXacNhan) AS choXacNhan
                FROM dbo.HoaDon i JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong JOIN dbo.Phong p ON p.MaPhong=h.MaPhong
                OUTER APPLY (SELECT COALESCE(SUM(CASE WHEN t.TrangThai='DA_XAC_NHAN' THEN t.SoTien ELSE 0 END),0) AS daDong,
                                    COALESCE(SUM(CASE WHEN t.TrangThai='CHO_XAC_NHAN' THEN t.SoTien ELSE 0 END),0) AS choXacNhan
                             FROM dbo.ThanhToan t WHERE t.MaHoaDon=i.MaHoaDon) payments
                WHERE p.MaKhu=k.MaKhu AND (? IS NULL OR i.Thang=?) AND (? IS NULL OR i.Nam=?)
            ) finance WHERE (?='' OR k.MaKhu=?) ORDER BY k.MaKhu
            """,PortalDAO::row,month,month,year,year,zone,zone);
    }

    public Optional<Map<String,Object>> admin(int accountId) throws SQLException {
        return JdbcUtil.query("SELECT MaTK AS maTK,TenDangNhap AS username,NgayTao AS ngayTao FROM dbo.TaiKhoan WHERE MaTK=? AND VaiTro='ADMIN' AND TrangThai=1",PortalDAO::row,accountId).stream().findFirst();
    }
}
