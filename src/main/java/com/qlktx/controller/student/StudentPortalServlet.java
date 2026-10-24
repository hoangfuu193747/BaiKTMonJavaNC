package com.qlktx.controller.student;

import com.qlktx.controller.BaseServlet;
import com.qlktx.dao.PortalDAO;
import com.qlktx.model.MenuFeature;
import com.qlktx.model.Role;
import com.qlktx.model.PortalStatus;
import com.qlktx.service.BusinessException;
import com.qlktx.service.FormValidation;
import com.qlktx.service.PortalService;
import com.qlktx.util.DisplayUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns={"/student","/student/","/student/home","/student/profile","/student/rooms","/student/register","/student/history","/student/contracts","/student/invoices","/student/payments","/student/repairs","/student/notifications","/student/password"})
public class StudentPortalServlet extends BaseServlet {
    private final PortalDAO dao = new PortalDAO();
    private final PortalService service = new PortalService();

    private String key(HttpServletRequest req) {
        String path=req.getServletPath();
        return path.equals("/student") || path.equals("/student/") ? "home" : path.substring("/student/".length());
    }

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        String key=key(req),studentId=user(req).getId();
        String title=key.equals("password")?"Đổi mật khẩu":MenuFeature.forRole(Role.STUDENT).stream().filter(f -> f.getKey().equals(key)).map(MenuFeature::getTitle).findFirst().orElse(null);
        if(title==null){res.sendError(404);return;}
        prepare(req,title,key.equals("register") ? "rooms" : key);
        req.setAttribute("pageDescription",key.equals("password") ? "Bảo vệ tài khoản bằng mật khẩu riêng của bạn." : MenuFeature.forRole(Role.STUDENT).stream().filter(f -> f.getKey().equals(key)).map(MenuFeature::getDescription).findFirst().orElse(""));
        req.setAttribute("features",MenuFeature.forRole(Role.STUDENT));req.setAttribute("routePrefix","/student");
        req.setAttribute("statusLabels",PortalStatus.LABELS);req.setAttribute("today",LocalDate.now(PortalService.ZONE));
        try {
            var profile=dao.profile(studentId);req.setAttribute("profile",profile);
            switch(key){
                case "home" -> {
                    var registrations=dao.registrations(studentId,"");var contracts=dao.contracts(studentId);var invoices=dao.invoices(studentId);
                    req.setAttribute("activeContract",contracts.stream().filter(h -> "DANG_O".equals(h.get("trangThai"))).findFirst().orElse(null));
                    req.setAttribute("latestRegistration",registrations.stream().findFirst().orElse(null));
                    req.setAttribute("canRegister",contracts.stream().noneMatch(h -> "DANG_O".equals(h.get("trangThai"))) && registrations.stream().noneMatch(r -> "CHO_DUYET".equals(r.get("trangThai"))));
                    req.setAttribute("registrations",registrations.stream().limit(5).toList());
                    req.setAttribute("invoices",invoices.stream().filter(i -> PortalDAO.money(i,"conNo").signum()>0).toList());
                    req.setAttribute("pendingCount",registrations.stream().filter(r -> "CHO_DUYET".equals(r.get("trangThai"))).count());
                    BigDecimal debt=invoices.stream().map(i -> PortalDAO.money(i,"conNo")).reduce(BigDecimal.ZERO,BigDecimal::add);
                    req.setAttribute("debtText",DisplayUtil.money(debt));
                    req.setAttribute("repairCount",dao.repairs(studentId).stream().filter(r -> !"HOAN_THANH".equals(r.get("trangThai"))).count());
                    req.setAttribute("notifications",dao.notifications("").stream().limit(3).toList());
                }
                case "profile" -> req.setAttribute("editing","edit".equals(text(req,"action")) || req.getAttribute("flashError")!=null);
                case "rooms","register" -> {
                    boolean active=dao.contracts(studentId).stream().anyMatch(h -> "DANG_O".equals(h.get("trangThai")));
                    boolean pending=dao.registrations(studentId,"").stream().anyMatch(r -> "CHO_DUYET".equals(r.get("trangThai")));
                    req.setAttribute("canRegister",!active&&!pending);req.setAttribute("hasActiveContract",active);req.setAttribute("hasPendingRegistration",pending);
                    String gender=(String)profile.get("gioiTinh");
                    if(key.equals("rooms")){
                        for(String name:List.of("q","zone","floor","gender","maxPrice","available"))req.setAttribute(name,text(req,name));
                        String zone=text(req,"zone"),floor=text(req,"floor"),filterGender=text(req,"gender");
                        if((!zone.isEmpty()&&!Set.of("A","B","C","D").contains(zone))||(!floor.isEmpty()&&!Set.of("1","2","3","4","5").contains(floor))||(!filterGender.isEmpty()&&!Set.of("Nam","Nữ").contains(filterGender))){res.sendError(400);return;}
                        BigDecimal maximum=text(req,"maxPrice").isEmpty()?null:PortalService.amount(text(req,"maxPrice"),true);
                        var rooms=dao.rooms(text(req,"q"),zone,floor,filterGender,maximum,"1".equals(text(req,"available")));
                        int totalPages=Math.max(1,(rooms.size()+11)/12);
                        int page=text(req,"page").isEmpty()?1:FormValidation.number(text(req,"page"),"Trang",1,Integer.MAX_VALUE);
                        page=Math.min(page,totalPages);
                        var visibleRooms=rooms.subList((page-1)*12,Math.min(page*12,rooms.size()));
                        for(var room:visibleRooms)room.put("reasons",PortalService.roomReasons(room,gender,active,pending));
                        req.setAttribute("rooms",visibleRooms);req.setAttribute("roomCount",rooms.size());
                        req.setAttribute("currentPage",page);req.setAttribute("totalPages",totalPages);
                    }else if(!text(req,"room").isEmpty()){
                        var room=dao.room(text(req,"room"));if(room.isEmpty()){res.sendError(404);return;}
                        req.setAttribute("room",room.get());req.setAttribute("reasons",PortalService.roomReasons(room.get(),gender,active,pending));
                    }
                }
                case "history" -> {
                    String status=text(req,"status");if(!status.isEmpty()&&!Set.of("CHO_DUYET","DA_DUYET","TU_CHOI","DA_HUY").contains(status)){res.sendError(400);return;}
                    req.setAttribute("status",status);req.setAttribute("registrations",dao.registrations(studentId,status));
                    if(!text(req,"id").isEmpty()){
                        int selectedId=id(req);var selected=dao.registrations(studentId,"").stream().filter(r -> PortalDAO.number(r,"maDK")==selectedId).findFirst();
                        if(selected.isEmpty()){res.sendError(404);return;}req.setAttribute("registration",selected.get());
                    }
                }
                case "contracts" -> {
                    req.setAttribute("contracts",dao.contracts(studentId));
                    if(!text(req,"id").isEmpty()){
                        var contract=dao.contract(id(req),studentId);if(contract.isEmpty()){res.sendError(404);return;}
                        req.setAttribute("contract",contract.get());req.setAttribute("printMode","1".equals(text(req,"print")));
                    }
                }
                case "invoices" -> {
                    req.setAttribute("invoices",dao.invoices(studentId));
                    if(!text(req,"id").isEmpty()){
                        int selectedId=id(req);var invoice=dao.invoice(selectedId,studentId);if(invoice.isEmpty()){res.sendError(404);return;}
                        req.setAttribute("invoice",invoice.get());req.setAttribute("invoicePayments",dao.payments(studentId).stream().filter(t -> PortalDAO.number(t,"maHoaDon")==selectedId).toList());
                    }
                }
                case "payments" -> {
                    req.setAttribute("payments",dao.payments(studentId));req.setAttribute("payableInvoices",dao.invoices(studentId).stream().filter(i -> PortalDAO.money(i,"conNo").signum()>0).toList());
                    if(!text(req,"invoice").isEmpty()){
                        var invoice=dao.invoice(FormValidation.number(text(req,"invoice"),"Mã hóa đơn",1,Integer.MAX_VALUE),studentId);
                        if(invoice.isEmpty()){res.sendError(404);return;}req.setAttribute("invoice",invoice.get());
                    }
                    req.setAttribute("bankName","KTX Bank · Ngân hàng mẫu");
                    req.setAttribute("bankAccount","DEMO-0000000000");
                    req.setAttribute("bankOwner","BAN QUẢN LÝ KÝ TÚC XÁ (MẪU)");
                }
                case "repairs" -> {
                    req.setAttribute("repairs",dao.repairs(studentId));req.setAttribute("activeContract",dao.contracts(studentId).stream().filter(h -> "DANG_O".equals(h.get("trangThai"))).findFirst().orElse(null));
                }
                case "notifications" -> {req.setAttribute("q",text(req,"q"));req.setAttribute("notifications",dao.notifications(text(req,"q")));}
                case "password" -> { }
                default -> {res.sendError(404);return;}
            }
            view(req,res,"student/portal/"+key+".jsp");
        }catch(BusinessException e){
            req.setAttribute("flashError",e.getMessage());res.setStatus(400);
            if(key.equals("rooms")){req.setAttribute("rooms",List.of());view(req,res,"student/portal/rooms.jsp");}
            else res.sendError(400);
        }catch(SQLException e){databaseError(res,e);}
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        String key=key(req),action=text(req,"action"),studentId=user(req).getId();
        try {
            String message,destination="/student/"+key;
            if(key.equals("profile")&&action.equals("update")){service.updatePhone(studentId,text(req,"phone"));message="Đã lưu số điện thoại của bạn.";}
            else if(key.equals("register")&&action.equals("submit")){
                int registration=service.submitRegistration(studentId,text(req,"room"));destination="/student/history?id="+registration;message="Đã gửi đơn. Theo dõi kết quả hoặc hủy khi đơn còn chờ duyệt.";
            }else if(key.equals("history")&&action.equals("cancel")){service.cancelRegistration(studentId,id(req));message="Đã hủy đơn đăng ký.";}
            else if(key.equals("payments")&&action.equals("transfer")){
                service.submitTransfer(studentId,FormValidation.number(text(req,"invoice"),"Mã hóa đơn",1,Integer.MAX_VALUE),text(req,"amount"),text(req,"reference"));message="Đã gửi yêu cầu thanh toán mẫu. Yêu cầu đang chờ Admin duyệt; số tiền đã đóng sẽ cập nhật sau khi được duyệt.";
            }else if(key.equals("repairs")&&action.equals("submit")){service.submitRepair(studentId,text(req,"description"));message="Đã gửi yêu cầu sửa chữa cho phòng đang ở.";}
            else if(key.equals("password")&&action.equals("change")){
                service.changePassword(studentId,raw(req,"currentPassword"),raw(req,"newPassword"),raw(req,"confirmPassword"));req.getSession(false).invalidate();redirect(req,res,"/auth/login?role=student&passwordChanged=1",null,false);return;
            }else{res.sendError(400);return;}
            redirect(req,res,destination,message,false);
        }catch(BusinessException e){req.setAttribute("flashError",e.getMessage());res.setStatus(400);doGet(req,res);}
        catch(SQLException e){
            if(constraintError(e)){req.setAttribute("flashError","Thông tin bị trùng hoặc vừa được xử lý. Hãy kiểm tra lại lịch sử.");res.setStatus(409);doGet(req,res);}else databaseError(res,e);
        }
    }
}
