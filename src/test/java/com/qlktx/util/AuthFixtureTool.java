package com.qlktx.util;
import java.sql.Connection;
/** Exact, random fixture IDs for check-auth.mjs; never alters existing seed rows. */
public final class AuthFixtureTool {
    public static void main(String[] args)throws Exception{
        if(args.length!=2||!args[1].matches("QA[A-F0-9]{6}"))throw new IllegalArgumentException("Invalid fixture arguments.");
        String action=args[0],prefix=args[1];
        try(Connection c=DatabaseUtil.getConnection()){
            c.setAutoCommit(false);
            try{
                for(int i=1;i<=3;i++){
                    String id=prefix+"S"+i;
                    switch(action){
                        case "create" -> JdbcUtil.update(c,"INSERT INTO dbo.SinhVienTruong (MaSV,HoTen,NgaySinh,GioiTinh,Lop,Khoa,Email,TrangThai) VALUES (?,N'Sinh viên thử '+?,'2005-01-01',N'Nam',N'QA',N'Kiểm thử',?,?)",id,id,id+"@example.test",i!=3);
                        case "cleanup" -> {JdbcUtil.update(c,"DELETE FROM dbo.SinhVien WHERE MaSV=?",id);JdbcUtil.update(c,"DELETE FROM dbo.TaiKhoan WHERE MaSV=?",id);JdbcUtil.update(c,"DELETE FROM dbo.SinhVienTruong WHERE MaSV=?",id);}
                        case "lock" -> {if(i==1)JdbcUtil.update(c,"UPDATE dbo.TaiKhoan SET TrangThai=0 WHERE MaSV=?",id);}
                        case "inspect" -> {
                            if(i<3){
                                var rows=JdbcUtil.query(c,"SELECT t.VaiTro,LEN(t.MatKhauHash) AS HashLength,CASE WHEN t.MatKhauHash LIKE '$2a$%' THEN 1 ELSE 0 END AS Bcrypt,s.MaSV FROM dbo.TaiKhoan t JOIN dbo.SinhVien s ON s.MaTK=t.MaTK AND s.MaSV=t.MaSV WHERE t.MaSV=?",r->r.getString(1)+"|"+r.getInt(2)+"|"+r.getInt(3)+"|"+r.getString(4),id);
                                if(rows.size()!=1)throw new IllegalStateException("Account/profile mismatch.");
                                System.out.println(rows.get(0));
                            }
                        }
                        default -> throw new IllegalArgumentException("Unknown action.");
                    }
                }c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }
}
