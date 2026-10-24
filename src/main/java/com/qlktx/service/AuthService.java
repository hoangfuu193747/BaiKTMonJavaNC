package com.qlktx.service;
import com.qlktx.dao.*;
import com.qlktx.model.*;
import com.qlktx.util.*;
import java.sql.*;
import java.util.*;

public class AuthService {
    private final DanhSachSinhVienTruongDAO schoolDao=new DanhSachSinhVienTruongDAO();
    private final SinhVienDAO studentDao=new SinhVienDAO();
    private final QuanTriVienDAO adminDao=new QuanTriVienDAO();
    private static final String DUMMY_HASH=BCryptPasswordUtil.hash(BCryptPasswordUtil.randomPassword());

    public DanhSachSinhVienTruong lookupStudent(String studentId,String email)throws SQLException,ValidationException{
        if(!validStudentId(studentId))throw new ValidationException(Map.of("studentId","Mã sinh viên hợp lệ có tối đa 10 ký tự."));
        if(!validEmail(email))throw new ValidationException(Map.of("email","Nhập Email trong danh sách trường, tối đa 120 ký tự."));
        try(Connection connection=DatabaseUtil.getConnection()){
            var student=verifiedSchoolStudent(connection,studentId,email,false);
            if(studentDao.exists(connection,student.getMaSV()))throw duplicate();
            return student;
        }
    }
    private DanhSachSinhVienTruong verifiedSchoolStudent(Connection connection,String id,String email,boolean lock)throws SQLException,ValidationException{
        if(lock)JdbcUtil.query(connection,"SELECT MaSV FROM dbo.SinhVienTruong WITH (UPDLOCK,HOLDLOCK) WHERE MaSV=?",r->r.getString(1),id);
        var student=schoolDao.findByMaSV(connection,id).orElseThrow(()->mismatch());
        if(!student.isActive()||!student.getEmailTruong().equalsIgnoreCase(email))throw mismatch();
        return student;
    }
    private ValidationException mismatch(){return new ValidationException(Map.of("studentId","Mã SV và Email không khớp cùng một hồ sơ hợp lệ trong danh sách trường."));}
    private ValidationException duplicate(){return new ValidationException(Map.of("studentId","Mã sinh viên này đã có tài khoản. Vui lòng đăng nhập."));}

    public void register(RegistrationForm form)throws SQLException,ValidationException{
        var errors=validateRegistration(form);if(!errors.isEmpty())throw new ValidationException(errors);
        String hash=BCryptPasswordUtil.hash(form.password());
        try(Connection connection=DatabaseUtil.getConnection()){
            connection.setAutoCommit(false);
            try{
                var student=verifiedSchoolStudent(connection,form.studentId(),form.email(),true);
                if(studentDao.exists(connection,student.getMaSV()))throw duplicate();
                studentDao.insert(connection,student.getMaSV(),student.getEmailTruong(),form.phone(),hash);
                connection.commit();
            }catch(SQLException e){
                connection.rollback();
                if(e.getErrorCode()==2601||e.getErrorCode()==2627)throw duplicate();
                throw e;
            }catch(ValidationException|RuntimeException e){connection.rollback();throw e;}
        }
    }
    public Optional<SessionUser> login(Role role,String username,String password)throws SQLException{
        var account=role==Role.ADMIN?adminDao.findAccount(username):studentDao.findAccount(username);
        boolean matches=BCryptPasswordUtil.verify(password,account.map(AuthAccount::passwordHash).orElse(DUMMY_HASH));
        if(account.isEmpty()||!matches||!account.get().active())return Optional.empty();
        return Optional.of(account.get().user());
    }
    public static Map<String,String> validateRegistration(RegistrationForm form){
        Map<String,String> errors=new LinkedHashMap<>();
        if(!validStudentId(form.studentId()))errors.put("studentId","Mã sinh viên hợp lệ có tối đa 10 ký tự.");
        if(!validEmail(form.email()))errors.put("email","Nhập Email hợp lệ, tối đa 120 ký tự.");
        if(!form.phone().isBlank()&&!form.phone().matches("^\\+?[0-9]{9,14}$"))errors.put("phone","Số điện thoại cần có 9–14 chữ số; có thể bắt đầu bằng +.");
        if(form.password().length()<8||form.password().isBlank()||!BCryptPasswordUtil.withinLimit(form.password()))errors.put("password","Mật khẩu ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        if(!form.password().equals(form.confirmPassword()))errors.put("confirmPassword","Mật khẩu xác nhận chưa khớp.");
        return errors;
    }
    public static boolean validStudentId(String value){return value!=null&&value.matches("^[A-Za-z0-9_.-]{1,10}$");}
    private static boolean validEmail(String value){return value!=null&&value.length()<=120&&value.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$");}
}

