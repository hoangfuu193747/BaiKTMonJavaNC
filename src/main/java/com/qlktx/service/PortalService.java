package com.qlktx.service;

import com.qlktx.dao.PortalDAO;
import com.qlktx.util.BCryptPasswordUtil;
import com.qlktx.util.JdbcUtil;
import com.qlktx.util.TransactionUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Current-schema workflows. Lock order: student, room when needed, then the operation's record. */
public class PortalService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static BigDecimal amount(String value, boolean allowZero) throws BusinessException {
        if (value == null || !value.matches("[0-9]{1,12}"))
            throw new BusinessException("Số tiền phải là số nguyên đồng, tối đa 12 chữ số.");
        BigDecimal amount = new BigDecimal(value);
        if (!allowZero && amount.signum() == 0) throw new BusinessException("Số tiền phải lớn hơn 0.");
        return amount;
    }

    public static String content(String value, String label, int max) throws BusinessException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > max || text.indexOf('\0') >= 0)
            throw new BusinessException(label + " cần có từ 1 đến " + max + " ký tự.");
        return text;
    }

    public static List<String> roomReasons(Map<String, Object> room, String gender, boolean active, boolean pending) {
        List<String> reasons = new ArrayList<>();
        if (active) reasons.add("Bạn đã có hợp đồng đang ở.");
        if (pending) reasons.add("Bạn đã có đơn chờ duyệt.");
        if (!"HOAT_DONG".equals(room.get("trangThai")) || !Boolean.TRUE.equals(room.get("khuHoatDong")))
            reasons.add("Phòng hoặc khu đang bảo trì / tạm dừng.");
        if (gender == null || !gender.equals(room.get("gioiTinh"))) reasons.add("Phòng không phù hợp với giới tính trong hồ sơ trường.");
        if (PortalDAO.number(room, "conCho") <= 0) reasons.add("Phòng đã hết chỗ trống.");
        return reasons;
    }

    private Map<String, Object> lockStudent(Connection c, String id, boolean requireActive) throws SQLException, BusinessException {
        var rows = PortalDAO.query(c, """
            SELECT s.MaSV AS maSV,d.GioiTinh AS gioiTinh,t.TrangThai AS taiKhoanHoatDong,d.TrangThai AS hoSoHoatDong
            FROM dbo.SinhVien s WITH (UPDLOCK,HOLDLOCK)
            JOIN dbo.TaiKhoan t ON t.MaTK=s.MaTK AND t.MaSV=s.MaSV AND t.VaiTro='SINH_VIEN'
            JOIN dbo.SinhVienTruong d ON d.MaSV=s.MaSV WHERE s.MaSV=?
            """, id);
        if (rows.isEmpty()) throw new BusinessException("Không tìm thấy hồ sơ sinh viên hợp lệ.");
        var row = rows.get(0);
        if (requireActive && (!Boolean.TRUE.equals(row.get("taiKhoanHoatDong")) || !Boolean.TRUE.equals(row.get("hoSoHoatDong"))))
            throw new BusinessException("Tài khoản hoặc hồ sơ trường không còn hoạt động.");
        return row;
    }

    private void admin(Connection c, int accountId) throws SQLException, BusinessException {
        if (!JdbcUtil.exists(c,"SELECT 1 FROM dbo.TaiKhoan WHERE MaTK=? AND VaiTro='ADMIN' AND TrangThai=1",accountId))
            throw new BusinessException("Tài khoản quản trị không còn hoạt động.");
    }

    private Map<String, Object> lockRoom(Connection c, String id) throws SQLException, BusinessException {
        var rows = PortalDAO.query(c, """
            SELECT p.MaPhong AS maPhong,p.SucChua AS sucChua,p.TrangThai AS trangThai,
                   k.TrangThai AS khuHoatDong,k.GioiTinh AS gioiTinh,p.GiaMoiSV AS giaMoiSV
            FROM dbo.Phong p WITH (UPDLOCK,HOLDLOCK) JOIN dbo.KhuNha k ON k.MaKhu=p.MaKhu WHERE p.MaPhong=?
            """, id);
        if (rows.isEmpty()) throw new BusinessException("Không tìm thấy phòng.");
        var room = rows.get(0);
        int occupied = JdbcUtil.query(c,"SELECT COUNT(*) FROM dbo.HopDong WHERE MaPhong=? AND TrangThai='DANG_O'",r -> r.getInt(1),id).get(0);
        room.put("conCho",PortalDAO.number(room,"sucChua")-occupied);
        return room;
    }

    public void updatePhone(String studentId, String phone) throws SQLException, BusinessException {
        String value = FormValidation.phone(phone);
        TransactionUtil.run(c -> {
            lockStudent(c,studentId,true);
            JdbcUtil.update(c,"UPDATE dbo.SinhVien SET SoDienThoai=? WHERE MaSV=?",value.isEmpty()?null:value,studentId);
            return null;
        });
    }

    public void changePassword(String studentId, String current, String next, String confirmation) throws SQLException, BusinessException {
        FormValidation.passwordChange(current,next,confirmation);
        String hash = BCryptPasswordUtil.hash(next);
        TransactionUtil.run(c -> {
            lockStudent(c,studentId,true);
            String stored = JdbcUtil.query(c,"SELECT MatKhauHash FROM dbo.TaiKhoan WITH (UPDLOCK,HOLDLOCK) WHERE MaSV=? AND VaiTro='SINH_VIEN'",r -> r.getString(1),studentId).get(0);
            if (!BCryptPasswordUtil.verify(current,stored)) throw new BusinessException("Mật khẩu hiện tại không đúng.");
            JdbcUtil.update(c,"UPDATE dbo.TaiKhoan SET MatKhauHash=? WHERE MaSV=? AND VaiTro='SINH_VIEN'",hash,studentId);
            return null;
        });
    }

    public int submitRegistration(String studentId, String roomId) throws SQLException, BusinessException {
        return TransactionUtil.run(c -> {
            var student = lockStudent(c,studentId,true);
            var room = lockRoom(c,roomId);
            boolean active = JdbcUtil.exists(c,"SELECT 1 FROM dbo.HopDong WHERE MaSV=? AND TrangThai='DANG_O'",studentId);
            boolean pending = JdbcUtil.exists(c,"SELECT 1 FROM dbo.DangKyPhong WHERE MaSV=? AND TrangThai='CHO_DUYET'",studentId);
            var reasons = roomReasons(room,(String)student.get("gioiTinh"),active,pending);
            if (!reasons.isEmpty()) throw new BusinessException(String.join(" ",reasons));
            return JdbcUtil.insert(c,"INSERT INTO dbo.DangKyPhong(MaSV,MaPhong) VALUES (?,?)",studentId,roomId);
        });
    }

    public void cancelRegistration(String studentId, int id) throws SQLException, BusinessException {
        TransactionUtil.run(c -> {
            lockStudent(c,studentId,true);
            int changed = JdbcUtil.update(c,"UPDATE dbo.DangKyPhong SET TrangThai='DA_HUY' WHERE MaDK=? AND MaSV=? AND TrangThai='CHO_DUYET'",id,studentId);
            if (changed != 1) throw new BusinessException("Chỉ có thể hủy đơn chờ duyệt của bạn. Đơn có thể đã được Admin xử lý.");
            return null;
        });
    }

    public int approveRegistration(int accountId, int id, String startText, String endText) throws SQLException, BusinessException {
        LocalDate start = FormValidation.date(startText,"ngày bắt đầu",true);
        LocalDate end = FormValidation.date(endText,"ngày kết thúc",false);
        if (end != null && (end.isBefore(start) || end.isBefore(LocalDate.now(ZONE))))
            throw new BusinessException("Ngày kết thúc phải từ ngày bắt đầu và không được ở trong quá khứ.");
        return TransactionUtil.run(c -> {
            admin(c,accountId);
            var initial = PortalDAO.query(c,"SELECT MaSV AS maSV,MaPhong AS maPhong FROM dbo.DangKyPhong WHERE MaDK=?",id);
            if (initial.isEmpty()) throw new BusinessException("Không tìm thấy đơn đăng ký.");
            String studentId = (String)initial.get(0).get("maSV");
            var student = lockStudent(c,studentId,true);
            String roomId = (String)initial.get(0).get("maPhong");
            var room = lockRoom(c,roomId);
            var rows = PortalDAO.query(c,"SELECT TrangThai AS trangThai FROM dbo.DangKyPhong WITH (UPDLOCK,HOLDLOCK) WHERE MaDK=?",id);
            if (!"CHO_DUYET".equals(rows.get(0).get("trangThai"))) throw new BusinessException("Đơn đã được xử lý.");
            boolean active = JdbcUtil.exists(c,"SELECT 1 FROM dbo.HopDong WHERE MaSV=? AND TrangThai='DANG_O'",studentId);
            var reasons = roomReasons(room,(String)student.get("gioiTinh"),active,false);
            if (!reasons.isEmpty()) throw new BusinessException(String.join(" ",reasons));
            int contract = JdbcUtil.insert(c,"INSERT INTO dbo.HopDong(MaDK,MaSV,MaPhong,NgayBatDau,NgayKetThuc) VALUES (?,?,?,?,?)",id,studentId,roomId,start,end);
            JdbcUtil.update(c,"UPDATE dbo.DangKyPhong SET TrangThai='DA_DUYET',NguoiDuyet=?,NgayDuyet=SYSDATETIME(),LyDoTuChoi=NULL WHERE MaDK=?",accountId,id);
            return contract;
        });
    }

    public void rejectRegistration(int accountId, int id, String reason) throws SQLException, BusinessException {
        String text = content(reason,"Lý do từ chối",255);
        TransactionUtil.run(c -> {
            admin(c,accountId);
            var initial = PortalDAO.query(c,"SELECT MaSV AS maSV FROM dbo.DangKyPhong WHERE MaDK=?",id);
            if (initial.isEmpty()) throw new BusinessException("Không tìm thấy đơn.");
            lockStudent(c,(String)initial.get(0).get("maSV"),false);
            if (JdbcUtil.update(c,"UPDATE dbo.DangKyPhong SET TrangThai='TU_CHOI',NguoiDuyet=?,NgayDuyet=SYSDATETIME(),LyDoTuChoi=? WHERE MaDK=? AND TrangThai='CHO_DUYET'",accountId,text,id)!=1)
                throw new BusinessException("Đơn không còn chờ duyệt.");
            return null;
        });
    }

    public void endContract(int accountId, int id) throws SQLException, BusinessException {
        TransactionUtil.run(c -> {
            admin(c,accountId);
            var rows = PortalDAO.query(c,"SELECT MaSV AS maSV,MaPhong AS maPhong FROM dbo.HopDong WHERE MaHDong=?",id);
            if (rows.isEmpty()) throw new BusinessException("Không tìm thấy hợp đồng.");
            lockStudent(c,(String)rows.get(0).get("maSV"),false);
            lockRoom(c,(String)rows.get(0).get("maPhong"));
            if (JdbcUtil.update(c,"UPDATE dbo.HopDong SET TrangThai='DA_KET_THUC',NgayKetThuc=CASE WHEN NgayBatDau>? THEN NgayBatDau ELSE ? END WHERE MaHDong=? AND TrangThai='DANG_O'",LocalDate.now(ZONE),LocalDate.now(ZONE),id)!=1)
                throw new BusinessException("Hợp đồng đã kết thúc.");
            return null;
        });
    }

    public int createInvoice(int accountId, int contractId, int month, int year, String electricity, String water, String dueText) throws SQLException, BusinessException {
        if (month<1 || month>12 || year<2020 || year>2100) throw new BusinessException("Tháng hoặc năm hóa đơn không hợp lệ.");
        BigDecimal electric = amount(electricity,true), waterCost = amount(water,true);
        LocalDate due = FormValidation.date(dueText,"hạn thanh toán",true);
        return TransactionUtil.run(c -> {
            admin(c,accountId);
            var initial = PortalDAO.query(c,"SELECT MaSV AS maSV FROM dbo.HopDong WHERE MaHDong=?",contractId);
            if (initial.isEmpty()) throw new BusinessException("Không tìm thấy hợp đồng.");
            lockStudent(c,(String)initial.get(0).get("maSV"),false);
            var contract = PortalDAO.query(c,"SELECT h.NgayBatDau AS startDate,h.NgayKetThuc AS endDate,p.GiaMoiSV AS price FROM dbo.HopDong h WITH (UPDLOCK,HOLDLOCK) JOIN dbo.Phong p ON p.MaPhong=h.MaPhong WHERE h.MaHDong=?",contractId).get(0);
            YearMonth period = YearMonth.of(year,month);
            YearMonth start = YearMonth.from(((java.sql.Date)contract.get("startDate")).toLocalDate());
            java.sql.Date end = (java.sql.Date)contract.get("endDate");
            if (period.isBefore(start) || (end!=null && period.isAfter(YearMonth.from(end.toLocalDate()))))
                throw new BusinessException("Kỳ hóa đơn phải nằm trong thời gian của hợp đồng.");
            if (JdbcUtil.exists(c,"SELECT 1 FROM dbo.HoaDon WHERE MaHDong=? AND Thang=? AND Nam=?",contractId,month,year))
                throw new BusinessException("Hợp đồng đã có hóa đơn cho kỳ này.");
            return JdbcUtil.insert(c,"INSERT INTO dbo.HoaDon(MaHDong,Thang,Nam,TienPhong,TienDien,TienNuoc,HanThanhToan) VALUES (?,?,?,?,?,?,?)",contractId,month,year,contract.get("price"),electric,waterCost,due);
        });
    }

    private Map<String, Object> lockInvoice(Connection c, int invoiceId) throws SQLException, BusinessException {
        var rows = PortalDAO.query(c,"SELECT i.MaHoaDon AS maHoaDon,i.TongTien AS tongTien,h.MaSV AS maSV FROM dbo.HoaDon i WITH (UPDLOCK,HOLDLOCK) JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong WHERE i.MaHoaDon=?",invoiceId);
        if (rows.isEmpty()) throw new BusinessException("Không tìm thấy hóa đơn.");
        var invoice = rows.get(0);
        var sums = PortalDAO.query(c,"SELECT COALESCE(SUM(CASE WHEN TrangThai='DA_XAC_NHAN' THEN SoTien ELSE 0 END),0) AS daDong,COALESCE(SUM(CASE WHEN TrangThai='CHO_XAC_NHAN' THEN SoTien ELSE 0 END),0) AS choXacNhan FROM dbo.ThanhToan WHERE MaHoaDon=?",invoiceId).get(0);
        invoice.putAll(sums);
        return invoice;
    }

    private void checkPayment(Map<String, Object> invoice, BigDecimal amount, boolean reservePending) throws BusinessException {
        BigDecimal remaining = PortalDAO.money(invoice,"tongTien").subtract(PortalDAO.money(invoice,"daDong"));
        if (reservePending) remaining = remaining.subtract(PortalDAO.money(invoice,"choXacNhan"));
        if (amount.compareTo(remaining)>0)
            throw new BusinessException("Số tiền vượt phần còn có thể thanh toán. Các khoản chờ xác nhận cũng được giữ để tránh gửi trùng.");
    }

    public int submitTransfer(String studentId, int invoiceId, String value, String reference) throws SQLException, BusinessException {
        BigDecimal money = amount(value,false);
        String code = reference == null || reference.isBlank()
                ? "DEMO-" + java.util.UUID.randomUUID() : content(reference,"Mã thanh toán",100);
        if (!code.matches("[A-Za-z0-9._/-]{1,100}")) throw new BusinessException("Mã giao dịch chỉ gồm chữ, số, dấu chấm, gạch ngang, gạch dưới hoặc /.");
        return TransactionUtil.run(c -> {
            lockStudent(c,studentId,true);
            if (!JdbcUtil.exists(c,"SELECT 1 FROM dbo.HoaDon i JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong WHERE i.MaHoaDon=? AND h.MaSV=?",invoiceId,studentId))
                throw new BusinessException("Không tìm thấy hóa đơn của bạn.");
            var invoice = lockInvoice(c,invoiceId);
            if (!studentId.equals(invoice.get("maSV"))) throw new BusinessException("Không tìm thấy hóa đơn của bạn.");
            checkPayment(invoice,money,true);
            if (JdbcUtil.exists(c,"SELECT 1 FROM dbo.ThanhToan WHERE MaGiaoDich=?",code)) throw new BusinessException("Mã giao dịch này đã được gửi. Hãy xem lịch sử thanh toán.");
            return JdbcUtil.insert(c,"INSERT INTO dbo.ThanhToan(MaHoaDon,SoTien,PhuongThuc,MaGiaoDich) VALUES (?,?,'CHUYEN_KHOAN',?)",invoiceId,money,code);
        });
    }

    private void refreshInvoice(Connection c, int invoiceId) throws SQLException {
        JdbcUtil.update(c,"""
            UPDATE dbo.HoaDon SET TrangThai=CASE
              WHEN (SELECT COALESCE(SUM(SoTien),0) FROM dbo.ThanhToan WHERE MaHoaDon=? AND TrangThai='DA_XAC_NHAN')>=TongTien THEN 'DA_THANH_TOAN'
              WHEN EXISTS(SELECT 1 FROM dbo.ThanhToan WHERE MaHoaDon=? AND TrangThai='DA_XAC_NHAN') THEN 'MOT_PHAN'
              ELSE 'CHUA_THANH_TOAN' END WHERE MaHoaDon=?
            """,invoiceId,invoiceId,invoiceId);
    }

    public void reviewTransfer(int accountId, int paymentId, boolean accept) throws SQLException, BusinessException {
        TransactionUtil.run(c -> {
            admin(c,accountId);
            var initial = PortalDAO.query(c,"SELECT t.MaHoaDon AS maHoaDon,h.MaSV AS maSV FROM dbo.ThanhToan t JOIN dbo.HoaDon i ON i.MaHoaDon=t.MaHoaDon JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong WHERE t.MaTT=?",paymentId);
            if (initial.isEmpty()) throw new BusinessException("Không tìm thấy thanh toán.");
            lockStudent(c,(String)initial.get(0).get("maSV"),false);
            int invoiceId = PortalDAO.number(initial.get(0),"maHoaDon");
            var invoice = lockInvoice(c,invoiceId);
            var payment = PortalDAO.query(c,"SELECT SoTien AS soTien,TrangThai AS trangThai,PhuongThuc AS phuongThuc FROM dbo.ThanhToan WITH (UPDLOCK,HOLDLOCK) WHERE MaTT=?",paymentId).get(0);
            if (!"CHO_XAC_NHAN".equals(payment.get("trangThai")) || !"CHUYEN_KHOAN".equals(payment.get("phuongThuc")))
                throw new BusinessException("Thanh toán đã được xử lý.");
            if (accept) checkPayment(invoice,PortalDAO.money(payment,"soTien"),false);
            JdbcUtil.update(c,"UPDATE dbo.ThanhToan SET TrangThai=?,NguoiXacNhan=?,NgayXacNhan=SYSDATETIME() WHERE MaTT=?",accept?"DA_XAC_NHAN":"TU_CHOI",accountId,paymentId);
            refreshInvoice(c,invoiceId);
            return null;
        });
    }

    public int recordCash(int accountId, int invoiceId, String value) throws SQLException, BusinessException {
        BigDecimal money = amount(value,false);
        return TransactionUtil.run(c -> {
            admin(c,accountId);
            var initial = PortalDAO.query(c,"SELECT h.MaSV AS maSV FROM dbo.HoaDon i JOIN dbo.HopDong h ON h.MaHDong=i.MaHDong WHERE i.MaHoaDon=?",invoiceId);
            if (initial.isEmpty()) throw new BusinessException("Không tìm thấy hóa đơn.");
            lockStudent(c,(String)initial.get(0).get("maSV"),false);
            var invoice = lockInvoice(c,invoiceId);
            checkPayment(invoice,money,true);
            int id = JdbcUtil.insert(c,"INSERT INTO dbo.ThanhToan(MaHoaDon,SoTien,PhuongThuc,TrangThai,NguoiXacNhan,NgayXacNhan) VALUES (?,?,'TIEN_MAT','DA_XAC_NHAN',?,SYSDATETIME())",invoiceId,money,accountId);
            refreshInvoice(c,invoiceId);
            return id;
        });
    }

    public int submitRepair(String studentId, String description) throws SQLException, BusinessException {
        String text = content(description,"Nội dung yêu cầu",1000);
        return TransactionUtil.run(c -> {
            lockStudent(c,studentId,true);
            var contracts = PortalDAO.query(c,"SELECT MaPhong AS maPhong FROM dbo.HopDong WITH (UPDLOCK,HOLDLOCK) WHERE MaSV=? AND TrangThai='DANG_O'",studentId);
            if (contracts.isEmpty()) throw new BusinessException("Bạn cần có hợp đồng đang ở để gửi yêu cầu sửa chữa.");
            return JdbcUtil.insert(c,"INSERT INTO dbo.YeuCauSuaChua(MaSV,MaPhong,NoiDung) VALUES (?,?,?)",studentId,contracts.get(0).get("maPhong"),text);
        });
    }

    public void updateRepair(int accountId, int id, String status) throws SQLException, BusinessException {
        if (!Set.of("MOI","DANG_XU_LY","HOAN_THANH").contains(status)) throw new BusinessException("Trạng thái xử lý không hợp lệ.");
        TransactionUtil.run(c -> {
            admin(c,accountId);
            if (JdbcUtil.update(c,"UPDATE dbo.YeuCauSuaChua SET TrangThai=? WHERE MaYC=? AND (TrangThai<>'HOAN_THANH' OR ?='HOAN_THANH')",status,id,status)!=1)
                throw new BusinessException("Không tìm thấy yêu cầu hoặc yêu cầu đã hoàn thành.");
            return null;
        });
    }

    public int publishNotification(int accountId, String title, String body) throws SQLException, BusinessException {
        String heading = content(title,"Tiêu đề",200), text = content(body,"Nội dung thông báo",20000);
        return TransactionUtil.run(c -> {
            admin(c,accountId);
            return JdbcUtil.insert(c,"INSERT INTO dbo.ThongBao(TieuDe,NoiDung,MaAdmin) VALUES (?,?,?)",heading,text,accountId);
        });
    }
}
