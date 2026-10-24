package com.qlktx.model;

import java.time.LocalDate;

public final class DanhSachSinhVienTruong {
    private final String maSV;
    private final String hoTen;
    private final LocalDate ngaySinh;
    private final String gioiTinh;
    private final String lop;
    private final String emailTruong;
    private final String khoa;
    private final boolean active;

    public DanhSachSinhVienTruong(String maSV, String hoTen, LocalDate ngaySinh,
            String gioiTinh, String lop, String emailTruong) {
        this(maSV,hoTen,ngaySinh,gioiTinh,lop,emailTruong,"",true);
    }
    public DanhSachSinhVienTruong(String maSV,String hoTen,LocalDate ngaySinh,String gioiTinh,String lop,String emailTruong,String khoa,boolean active){
        this.maSV = maSV;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.lop = lop;
        this.emailTruong = emailTruong;
        this.khoa=khoa;
        this.active=active;
    }

    public String getMaSV() { return maSV; }
    public String getHoTen() { return hoTen; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public String getGioiTinh() { return gioiTinh; }
    public String getLop() { return lop; }
    public String getEmailTruong() { return emailTruong; }
    public String getKhoa(){return khoa;}
    public boolean isActive(){return active;}
}
