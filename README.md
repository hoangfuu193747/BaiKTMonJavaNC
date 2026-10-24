# Quản lý đăng ký ký túc xá

Đồ án Java Web theo mô hình MVC: Java 21, Servlet/JSP, JDBC, SQL Server,
HTML/CSS, Bootstrap, JSTL + EL, Apache Tomcat 9. Có thể mở dự án Maven bằng NetBeans hoặc Eclipse.
Thư viện bổ trợ: jBCrypt để băm mật khẩu và JUnit 5 để kiểm thử.
JavaScript phục vụ tương tác giao diện; PowerShell và Node.js phục vụ chạy/kiểm tra tại máy.

## Chạy web

Cần Java 21, Maven, Tomcat 9 và SQL Server có sẵn database `QuanLyKyTucXa` theo schema hiện hành.
Dự án chưa kèm script tạo database hoặc dữ liệu ban đầu.

```powershell
mvn clean package
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-local.ps1
```

Nhập mật khẩu SQL Server khi được hỏi. Mở `http://localhost:18080/QuanLyKyTucXa/`.
Giữ cửa sổ server mở; nhấn Ctrl+C để dừng trước khi build/chạy lại.

Script mặc định dùng Tomcat tại `D:\Dowload\apache-tomcat-9.0.122`;
đổi bằng `-TomcatHome`, đổi cổng bằng `-Port`.
Cấu hình kết nối bằng `KTX_DB_URL`, `KTX_DB_USER`, `KTX_DB_PASSWORD`,
hoặc đặt `KTX_DB_CONFIG` trỏ tới bản sao ngoài source của
`src/main/resources/config/db.properties.example`. Không lưu mật khẩu trong mã nguồn.
Nếu chưa có Admin, chạy script với `-CreateAdmin -GenerateAdminPassword` để tạo tài khoản.

## Chức năng

- Sinh viên: đăng ký tài khoản bằng mã sinh viên/email đối chiếu hồ sơ trường;
  đăng nhập, cập nhật điện thoại, đổi mật khẩu, tìm/đăng ký/hủy đơn phòng,
  xem/in hợp đồng, hóa đơn, thanh toán mẫu, yêu cầu sửa chữa và thông báo.
- Admin: quản lý hồ sơ trường và trạng thái tài khoản, khu/phòng, duyệt/từ chối đơn,
  kết thúc hợp đồng, lập hóa đơn, xác nhận thanh toán, sửa chữa, thông báo và báo cáo/CSV.
- Phân quyền, CSRF, BCrypt, kiểm tra trạng thái tài khoản và transaction khi ghi dữ liệu.

Thanh toán/ngân hàng chỉ mô phỏng. Cấu hình nghiệp vụ: 4 khu A–D,
5 tầng/khu, 10 phòng/tầng, 4 người/phòng, 700.000 đồng/người/tháng.
Khu A/B dành cho nam, C/D dành cho nữ. Khóa tài khoản hoặc bảo trì phòng giữ nguyên lịch sử lưu trú.

## Cấu trúc

```text
src/main/java/com/qlktx/
  controller/   Nhận yêu cầu HTTP cho xác thực, Admin và sinh viên
  service/      Kiểm tra dữ liệu và xử lý nghiệp vụ
  dao/          Truy vấn JDBC
  model/        Dữ liệu tài khoản, hồ sơ, menu và trạng thái
  filter/       UTF-8, phân quyền, CSRF
  util/         Kết nối, transaction, mật khẩu, CSV
src/main/webapp/WEB-INF/views/   JSP và thành phần dùng chung
src/main/webapp/assets/         CSS, JavaScript, icon
src/test/                       Kiểm thử và dữ liệu thử
scripts/                        Chạy web, kiểm tra HTTP/giao diện
target/                        Kết quả build, không chỉnh sửa trực tiếp
```

Database hiện hành có 11 bảng: `SinhVienTruong`, `TaiKhoan`, `SinhVien`, `KhuNha`,
`Phong`, `DangKyPhong`, `HopDong`, `HoaDon`, `ThanhToan`, `YeuCauSuaChua`, `ThongBao`.
Controller đang dùng: `AuthServlet`, `StudentPortalServlet`, `AdminManagementServlet`, `AdminWorkflowServlet`.

## Kiểm thử

```powershell
mvn test
node scripts/check-auth.mjs
node scripts/check-student-portal.mjs
node scripts/check-admin-management.mjs
```

Các script HTTP cần web đang chạy và database phát triển, có tạo/xóa dữ liệu thử.
`check-auth.mjs` cần `KTX_DB_PASSWORD` và `KTX_ADMIN_PASSWORD`.
Hai script nghiệp vụ dùng Windows Authentication của SQL Server để tạo dữ liệu thử;
đặt `KTX_ADMIN_PASSWORD` theo tài khoản Admin đang dùng.
`KTX_WEB_URL` cho phép đổi URL web ở cả ba script.

Kiểm thử quản trị tạm thay mật khẩu Admin và trạng thái khu/phòng rồi khôi phục trong phần dọn dẹp;
không chạy đồng thời với người đang quản trị. Chỉ chạy trên database phát triển.
Đặt `PORTAL_VISUAL=true` cho script sinh viên hoặc `KTX_VISUAL_CHECK=true` cho script quản trị
để kiểm tra giao diện bằng Chrome. Ảnh được lưu trong `target/ui-preview`.
Kiểm thử xác thực trực tiếp database cần bật `KTX_TEST_AUTH_DB=true`;
nếu không, các test đó được bỏ qua.
