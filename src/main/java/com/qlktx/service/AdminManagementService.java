package com.qlktx.service;

import com.qlktx.dao.PortalDAO;
import com.qlktx.util.BCryptPasswordUtil;
import com.qlktx.util.JdbcUtil;
import com.qlktx.util.TransactionUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public class AdminManagementService {
    private void admin(Connection connection,int accountId) throws SQLException,BusinessException {
        if(!JdbcUtil.exists(connection,"SELECT 1 FROM dbo.TaiKhoan WHERE MaTK=? AND VaiTro='ADMIN' AND TrangThai=1",accountId))
            throw new BusinessException("Tài khoản quản trị không còn hoạt động.");
    }

    public static String studentId(String value) throws BusinessException {
        String result=FormValidation.id(value,"Mã sinh viên");
        if(result.length()>10)throw new BusinessException("Mã sinh viên tối đa 10 ký tự.");
        return result;
    }

    public void saveStudent(int accountId,boolean creating,Map<String,String> values) throws SQLException,BusinessException {
        String code=studentId(values.get("id"));
        String name=FormValidation.text(values.get("name"),"Họ tên",100,true);
        LocalDate birth=FormValidation.date(values.get("birthday"),"ngày sinh",true);
        if(!birth.isBefore(LocalDate.now(PortalService.ZONE)))throw new BusinessException("Ngày sinh phải ở trong quá khứ.");
        String gender=values.get("gender");
        if(!Set.of("Nam","Nữ").contains(gender))throw new BusinessException("Giới tính không hợp lệ.");
        String className=FormValidation.text(values.get("className"),"Lớp",20,true);
        String faculty=FormValidation.text(values.get("faculty"),"Khoa",100,true);
        String email=FormValidation.email(values.get("email"),true);
        String phone=FormValidation.phone(values.get("phone"));
        TransactionUtil.run(connection -> {
            admin(connection,accountId);
            JdbcUtil.query(connection,"SELECT MaSV FROM dbo.SinhVien WITH (UPDLOCK,HOLDLOCK) WHERE MaSV=?",row -> row.getString(1),code);
            boolean exists=JdbcUtil.exists(connection,"SELECT 1 FROM dbo.SinhVienTruong WITH (UPDLOCK,HOLDLOCK) WHERE MaSV=?",code);
            if(creating&&exists)throw new BusinessException("Mã sinh viên đã tồn tại.");
            if(!creating&&!exists)throw new BusinessException("Không tìm thấy hồ sơ sinh viên.");
            if(creating){
                JdbcUtil.update(connection,"INSERT INTO dbo.SinhVienTruong(MaSV,HoTen,NgaySinh,GioiTinh,Lop,Khoa,Email,TrangThai) VALUES(?,?,?,?,?,?,?,1)",code,name,birth,gender,className,faculty,email);
            }else{
                if(JdbcUtil.exists(connection,"SELECT 1 FROM dbo.HopDong h JOIN dbo.Phong p ON p.MaPhong=h.MaPhong JOIN dbo.KhuNha k ON k.MaKhu=p.MaKhu WHERE h.MaSV=? AND h.TrangThai='DANG_O' AND k.GioiTinh<>?",code,gender)
                    ||JdbcUtil.exists(connection,"SELECT 1 FROM dbo.DangKyPhong r JOIN dbo.Phong p ON p.MaPhong=r.MaPhong JOIN dbo.KhuNha k ON k.MaKhu=p.MaKhu WHERE r.MaSV=? AND r.TrangThai='CHO_DUYET' AND k.GioiTinh<>?",code,gender))
                    throw new BusinessException("Giới tính mới không phù hợp với phòng đang ở hoặc đơn chờ duyệt. Hãy xử lý hồ sơ nội trú trước.");
                JdbcUtil.update(connection,"UPDATE dbo.SinhVienTruong SET HoTen=?,NgaySinh=?,GioiTinh=?,Lop=?,Khoa=?,Email=? WHERE MaSV=?",name,birth,gender,className,faculty,email,code);
                JdbcUtil.update(connection,"UPDATE dbo.SinhVien SET SoDienThoai=? WHERE MaSV=?",phone.isEmpty()?null:phone,code);
            }
            return null;
        });
    }

    public void studentState(int accountId,String id,boolean active,boolean roster) throws SQLException,BusinessException {
        String code=studentId(id);
        TransactionUtil.run(connection -> {
            admin(connection,accountId);
            JdbcUtil.query(connection,"SELECT MaSV FROM dbo.SinhVien WITH (UPDLOCK,HOLDLOCK) WHERE MaSV=?",row -> row.getString(1),code);
            String sql=roster?"UPDATE dbo.SinhVienTruong SET TrangThai=? WHERE MaSV=?":"UPDATE dbo.TaiKhoan SET TrangThai=? WHERE MaSV=? AND VaiTro='SINH_VIEN'";
            if(JdbcUtil.update(connection,sql,active,code)!=1)throw new BusinessException(roster?"Không tìm thấy hồ sơ.":"Sinh viên chưa có tài khoản.");
            return null;
        });
    }

    public void deleteStudent(int accountId,String id) throws SQLException,BusinessException {
        String code=studentId(id);
        TransactionUtil.run(connection -> {
            admin(connection,accountId);
            JdbcUtil.query(connection,"SELECT MaSV FROM dbo.SinhVien WITH (UPDLOCK,HOLDLOCK) WHERE MaSV=?",row -> row.getString(1),code);
            if(!JdbcUtil.exists(connection,"SELECT 1 FROM dbo.SinhVienTruong WITH (UPDLOCK,HOLDLOCK) WHERE MaSV=?",code))throw new BusinessException("Không tìm thấy hồ sơ.");
            if(JdbcUtil.exists(connection,"SELECT 1 FROM dbo.TaiKhoan WHERE MaSV=?",code)
                ||JdbcUtil.exists(connection,"SELECT 1 FROM dbo.DangKyPhong WHERE MaSV=?",code)
                ||JdbcUtil.exists(connection,"SELECT 1 FROM dbo.HopDong WHERE MaSV=?",code))throw new BusinessException("Hồ sơ đã được sử dụng. Hãy vô hiệu hóa hồ sơ hoặc khóa tài khoản để giữ lịch sử.");
            JdbcUtil.update(connection,"DELETE FROM dbo.SinhVienTruong WHERE MaSV=?",code);
            return null;
        });
    }

    public void saveZone(int accountId,String code,String name,boolean active) throws SQLException,BusinessException {
        if(!Set.of("A","B","C","D").contains(code))throw new BusinessException("Khu nhà không hợp lệ.");
        String title=FormValidation.text(name,"Tên khu",30,true);
        TransactionUtil.run(connection -> {
            admin(connection,accountId);
            if(JdbcUtil.update(connection,"UPDATE dbo.KhuNha SET TenKhu=?,TrangThai=? WHERE MaKhu=?",title,active,code)!=1)throw new BusinessException("Không tìm thấy khu nhà.");
            return null;
        });
    }

    public void saveRoom(int accountId,String code,String status) throws SQLException,BusinessException {
        if(code==null||!code.matches("[ABCD][1-5](0[1-9]|10)"))throw new BusinessException("Mã phòng không hợp lệ.");
        if(!Set.of("HOAT_DONG","BAO_TRI").contains(status))throw new BusinessException("Trạng thái phòng không hợp lệ.");
        TransactionUtil.run(connection -> {
            admin(connection,accountId);
            var room=PortalDAO.query(connection,"SELECT MaPhong FROM dbo.Phong WITH (UPDLOCK,HOLDLOCK) WHERE MaPhong=?",code);
            if(room.isEmpty())throw new BusinessException("Không tìm thấy phòng.");
            JdbcUtil.update(connection,"UPDATE dbo.Phong SET TrangThai=? WHERE MaPhong=?",status,code);
            return null;
        });
    }

    public void changePassword(int accountId,String current,String next,String confirmation) throws SQLException,BusinessException {
        FormValidation.passwordChange(current,next,confirmation);
        String hash=BCryptPasswordUtil.hash(next);
        TransactionUtil.run(connection -> {
            var accounts=JdbcUtil.query(connection,"SELECT MatKhauHash FROM dbo.TaiKhoan WITH (UPDLOCK,HOLDLOCK) WHERE MaTK=? AND VaiTro='ADMIN' AND TrangThai=1",row -> row.getString(1),accountId);
            if(accounts.isEmpty()||!BCryptPasswordUtil.verify(current,accounts.get(0)))throw new BusinessException("Mật khẩu hiện tại không đúng.");
            JdbcUtil.update(connection,"UPDATE dbo.TaiKhoan SET MatKhauHash=? WHERE MaTK=?",hash,accountId);
            return null;
        });
    }
}
