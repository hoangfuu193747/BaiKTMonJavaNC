package com.qlktx.model;

import java.util.List;

/** Immutable menu metadata shared by controllers and JSP layouts. */
public final class MenuFeature {
    private final String key,title,description;

    private MenuFeature(String key,String title,String description){
        this.key=key;this.title=title;this.description=description;
    }
    public String getKey(){return key;}
    public String getTitle(){return title;}
    public String getDescription(){return description;}

    private static final List<MenuFeature> ADMIN=List.of(
        new MenuFeature("home","Tổng quan","Tài khoản, phòng và tình hình nội trú."),
        new MenuFeature("students","Quản lý sinh viên","Danh sách trường, hồ sơ đã đăng ký và tài khoản."),
        new MenuFeature("zones","Quản lý khu nhà","Bốn khu A/B dành cho nam, C/D dành cho nữ."),
        new MenuFeature("rooms","Quản lý phòng","Thông tin phòng và số chỗ tính từ hợp đồng đang ở."),
        new MenuFeature("registrations","Duyệt đăng ký","Đơn chờ duyệt và kết quả đăng ký phòng."),
        new MenuFeature("contracts","Quản lý hợp đồng","Hợp đồng lưu trú và tình trạng đang ở."),
        new MenuFeature("invoices","Quản lý hóa đơn","Tiền phòng, điện, nước theo từng tháng."),
        new MenuFeature("payments","Xác nhận thanh toán","Thanh toán tiền mặt hoặc chuyển khoản."),
        new MenuFeature("repairs","Yêu cầu sửa chữa","Tiếp nhận các vấn đề từ sinh viên."),
        new MenuFeature("notifications","Thông báo","Thông báo chung của ban quản lý."),
        new MenuFeature("reports","Tra cứu / báo cáo","Tình hình nội trú theo khu."));

    private static final List<MenuFeature> STUDENT=List.of(
        new MenuFeature("home","Tổng quan","Hồ sơ và tình hình nội trú của bạn."),
        new MenuFeature("profile","Hồ sơ của tôi","Thông tin từ danh sách trường và tài khoản đã đăng ký."),
        new MenuFeature("rooms","Danh sách phòng","Xem khu, tầng, giới tính, giá và số chỗ còn lại."),
        new MenuFeature("register","Đăng ký phòng","Xem thông tin phòng trước khi đăng ký."),
        new MenuFeature("history","Lịch sử đăng ký","Theo dõi kết quả các đơn của bạn."),
        new MenuFeature("contracts","Hợp đồng của tôi","Thông tin hợp đồng lưu trú cá nhân."),
        new MenuFeature("invoices","Hóa đơn của tôi","Tiền phòng, điện và nước cần thanh toán."),
        new MenuFeature("payments","Thanh toán","Các khoản thanh toán và kết quả xác nhận."),
        new MenuFeature("repairs","Yêu cầu sửa chữa","Báo hỏng hóc trong phòng đang ở."),
        new MenuFeature("notifications","Thông báo","Thông báo từ ban quản lý."));

    public static List<MenuFeature> forRole(Role role){return role==Role.ADMIN?ADMIN:STUDENT;}
}
