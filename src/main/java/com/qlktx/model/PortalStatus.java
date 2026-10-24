package com.qlktx.model;

import java.util.Map;

public final class PortalStatus {
    private PortalStatus(){}
    public static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("CHO_DUYET","Chờ duyệt"),Map.entry("DA_DUYET","Đã duyệt"),Map.entry("TU_CHOI","Từ chối"),
        Map.entry("DA_HUY","Đã hủy"),Map.entry("DANG_O","Đang ở"),Map.entry("DA_KET_THUC","Đã kết thúc"),
        Map.entry("CHUA_THANH_TOAN","Chưa thanh toán"),Map.entry("MOT_PHAN","Đã đóng một phần"),Map.entry("DA_THANH_TOAN","Đã thanh toán"),
        Map.entry("CHO_XAC_NHAN","Chờ duyệt"),Map.entry("DA_XAC_NHAN","Đã duyệt"),
        Map.entry("TIEN_MAT","Tiền mặt"),Map.entry("CHUYEN_KHOAN","Chuyển khoản"),
        Map.entry("MOI","Mới gửi"),Map.entry("DANG_XU_LY","Đang xử lý"),Map.entry("HOAN_THANH","Hoàn thành"));
}
