package com.qlktx.service;
import com.qlktx.dao.SinhVienDAO;
import com.qlktx.model.Role;
import com.qlktx.util.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="KTX_TEST_AUTH_DB",matches="true")
class AuthDatabaseTest {
    private final String id="QA"+UUID.randomUUID().toString().replace("-","").substring(0,8);
    private final String email=id+"@example.test";
    private final AuthService service=new AuthService();
    private RegistrationForm form(){return new RegistrationForm(id,email,"0912345678","","Test-pass-2026","Test-pass-2026");}
    @BeforeEach void setup()throws Exception{
        try(Connection c=DatabaseUtil.getConnection()){JdbcUtil.update(c,"INSERT INTO dbo.SinhVienTruong (MaSV,HoTen,NgaySinh,GioiTinh,Lop,Khoa,Email,TrangThai) VALUES (?,N'Sinh viên kiểm thử','2005-01-01',N'Nam',N'TEST',N'TEST',?,1)",id,email);}
    }
    @AfterEach void cleanup()throws Exception{
        try(Connection c=DatabaseUtil.getConnection()){
            c.setAutoCommit(false);
            try{JdbcUtil.update(c,"DELETE FROM dbo.SinhVien WHERE MaSV=?",id);JdbcUtil.update(c,"DELETE FROM dbo.TaiKhoan WHERE MaSV=?",id);JdbcUtil.update(c,"DELETE FROM dbo.SinhVienTruong WHERE MaSV=?",id);c.commit();}
            catch(Exception e){c.rollback();throw e;}
        }
    }
    @Test void matchingSchoolProfileCreatesBothRecordsWithServerAssignedRole()throws Exception{
        assertThrows(ValidationException.class,()->service.lookupStudent(id,"wrong@example.test"));
        assertDoesNotThrow(()->service.lookupStudent(id,email));
        var bad=new RegistrationForm(id,"wrong@example.test","","","Test-pass-2026","Test-pass-2026");
        assertThrows(ValidationException.class,()->service.register(bad));
        service.register(form());
        var account=new SinhVienDAO().findAccount(id).orElseThrow();
        assertEquals(60,account.passwordHash().length());
        assertEquals(Role.STUDENT,account.user().getRole());
        assertTrue(service.login(Role.STUDENT,id,form().password()).isPresent());
        assertTrue(service.login(Role.ADMIN,id,form().password()).isEmpty());
        assertThrows(ValidationException.class,()->service.register(form()));
        try(Connection c=DatabaseUtil.getConnection()){assertEquals(1,JdbcUtil.query(c,"SELECT COUNT(*) FROM dbo.SinhVien s JOIN dbo.TaiKhoan t ON t.MaTK=s.MaTK WHERE s.MaSV=? AND t.MaSV=s.MaSV AND t.VaiTro='SINH_VIEN'",r->r.getInt(1),id).get(0));}
    }
    @Test void disabledRosterOrAccountCannotAuthenticate()throws Exception{
        try(Connection c=DatabaseUtil.getConnection()){JdbcUtil.update(c,"UPDATE dbo.SinhVienTruong SET TrangThai=0 WHERE MaSV=?",id);}
        assertThrows(ValidationException.class,()->service.lookupStudent(id,email));
        assertThrows(ValidationException.class,()->service.register(form()));
        try(Connection c=DatabaseUtil.getConnection()){JdbcUtil.update(c,"UPDATE dbo.SinhVienTruong SET TrangThai=1 WHERE MaSV=?",id);}
        service.register(form());
        try(Connection c=DatabaseUtil.getConnection()){JdbcUtil.update(c,"UPDATE dbo.TaiKhoan SET TrangThai=0 WHERE MaSV=?",id);}
        assertTrue(service.login(Role.STUDENT,id,form().password()).isEmpty());
        try(Connection c=DatabaseUtil.getConnection()){JdbcUtil.update(c,"UPDATE dbo.TaiKhoan SET TrangThai=1 WHERE MaSV=?",id);JdbcUtil.update(c,"UPDATE dbo.SinhVienTruong SET TrangThai=0 WHERE MaSV=?",id);}
        assertFalse(new SinhVienDAO().findAccount(id).orElseThrow().active());
    }
    @Test void simultaneousRegistrationCannotCreateDuplicateOrPartialAccounts()throws Exception{
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch go=new CountDownLatch(1);
        try{
            Callable<Boolean> signup=()->{go.await();try{service.register(form());return true;}catch(ValidationException expected){return false;}};
            var a=pool.submit(signup);var b=pool.submit(signup);go.countDown();
            assertNotEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));
            try(Connection c=DatabaseUtil.getConnection()){
                assertEquals(1,JdbcUtil.query(c,"SELECT COUNT(*) FROM dbo.TaiKhoan WHERE MaSV=?",r->r.getInt(1),id).get(0));
                assertEquals(1,JdbcUtil.query(c,"SELECT COUNT(*) FROM dbo.SinhVien WHERE MaSV=?",r->r.getInt(1),id).get(0));
            }
        }finally{go.countDown();pool.shutdownNow();}
    }
}
