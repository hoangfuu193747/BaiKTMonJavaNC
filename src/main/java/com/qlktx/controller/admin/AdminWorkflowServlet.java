package com.qlktx.controller.admin;

import com.qlktx.controller.BaseServlet;
import com.qlktx.model.PortalStatus;
import com.qlktx.dao.PortalDAO;
import com.qlktx.model.MenuFeature;
import com.qlktx.model.Role;
import com.qlktx.service.BusinessException;
import com.qlktx.service.FormValidation;
import com.qlktx.service.PortalService;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns={"/admin/registrations","/admin/contracts","/admin/invoices","/admin/payments","/admin/repairs","/admin/notifications"})
public class AdminWorkflowServlet extends BaseServlet {
    private final PortalDAO dao=new PortalDAO();private final PortalService service=new PortalService();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        String key=req.getServletPath().substring("/admin/".length());
        String title=MenuFeature.forRole(Role.ADMIN).stream().filter(f -> f.getKey().equals(key)).map(MenuFeature::getTitle).findFirst().orElse(null);
        if(title==null){res.sendError(404);return;}prepare(req,title,key);
        req.setAttribute("features",MenuFeature.forRole(Role.ADMIN));req.setAttribute("routePrefix","/admin");req.setAttribute("statusLabels",PortalStatus.LABELS);req.setAttribute("adminView",true);
        LocalDate today=LocalDate.now(PortalService.ZONE);req.setAttribute("today",today);req.setAttribute("defaultEnd",today.plusMonths(9));
        try {
            switch(key){
                case "registrations" -> {
                    String status=text(req,"status");if(!status.isEmpty()&&!Set.of("CHO_DUYET","DA_DUYET","TU_CHOI","DA_HUY").contains(status)){res.sendError(400);return;}
                    req.setAttribute("status",status);var registrations=dao.registrations("",status);
                    for(var registration:registrations){
                        var room=Map.<String,Object>of("trangThai",registration.get("trangThaiPhong"),"khuHoatDong",registration.get("khuHoatDong"),"gioiTinh",registration.get("gioiTinhPhong"),"conCho",PortalDAO.number(registration,"sucChua")-PortalDAO.number(registration,"dangO"));
                        var reasons=PortalService.roomReasons(room,(String)registration.get("gioiTinh"),PortalDAO.number(registration,"hopDongDangO")>0,false);
                        if(!Boolean.TRUE.equals(registration.get("taiKhoanHoatDong"))||!Boolean.TRUE.equals(registration.get("hoSoHoatDong")))reasons.add("Tài khoản hoặc hồ sơ trường đã bị khóa.");
                        registration.put("reasons",reasons);
                    }req.setAttribute("registrations",registrations);
                }
                case "contracts" -> {
                    req.setAttribute("contracts",dao.contracts(""));if(!text(req,"id").isEmpty()){
                        var contract=dao.contract(id(req),"");if(contract.isEmpty()){res.sendError(404);return;}req.setAttribute("contract",contract.get());req.setAttribute("printMode","1".equals(text(req,"print")));
                    }
                }
                case "invoices" -> {
                    req.setAttribute("contracts",dao.contracts(""));req.setAttribute("invoices",dao.invoices(""));if(!text(req,"id").isEmpty()&&!"POST".equals(req.getMethod())){
                        var invoice=dao.invoice(id(req),"");if(invoice.isEmpty()){res.sendError(404);return;}req.setAttribute("invoice",invoice.get());
                    }
                }
                case "payments" -> {req.setAttribute("payments",dao.payments(""));req.setAttribute("payableInvoices",dao.invoices("").stream().filter(i -> PortalDAO.money(i,"coTheGui").signum()>0).toList());}
                case "repairs" -> req.setAttribute("repairs",dao.repairs(""));
                case "notifications" -> {req.setAttribute("notifications",dao.notifications(text(req,"q")));req.setAttribute("q",text(req,"q"));}
                default -> {res.sendError(404);return;}
            }
            view(req,res,key.equals("contracts")?"student/portal/contracts.jsp":"admin/portal/"+key+".jsp");
        }catch(BusinessException e){res.sendError(400);}catch(SQLException e){databaseError(res,e);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        String key=req.getServletPath().substring("/admin/".length()),action=text(req,"action");int accountId=Integer.parseInt(user(req).getId());
        try {
            String message,destination="/admin/"+key;
            if(key.equals("registrations")&&action.equals("approve")){
                int contract=service.approveRegistration(accountId,id(req),text(req,"start"),text(req,"end"));destination="/admin/contracts?id="+contract;message="Đã duyệt đơn và tạo hợp đồng cho sinh viên.";
            }else if(key.equals("registrations")&&action.equals("reject")){service.rejectRegistration(accountId,id(req),text(req,"reason"));message="Đã từ chối đơn đăng ký.";}
            else if(key.equals("contracts")&&action.equals("finish")){service.endContract(accountId,id(req));message="Đã kết thúc hợp đồng và giải phóng chỗ ở.";}
            else if(key.equals("invoices")&&action.equals("create")){
                service.createInvoice(accountId,id(req),FormValidation.number(text(req,"month"),"Tháng",1,12),FormValidation.number(text(req,"year"),"Năm",2020,2100),text(req,"electricity"),text(req,"water"),text(req,"due"));message="Đã tạo hóa đơn để sinh viên xem và thanh toán.";
            }else if(key.equals("payments")&&(action.equals("confirm")||action.equals("reject"))){service.reviewTransfer(accountId,id(req),action.equals("confirm"));message="Đã cập nhật kết quả đối chiếu thanh toán.";}
            else if(key.equals("payments")&&action.equals("cash")){service.recordCash(accountId,id(req),text(req,"amount"));message="Đã ghi nhận tiền mặt và cập nhật hóa đơn.";}
            else if(key.equals("repairs")&&action.equals("update")){service.updateRepair(accountId,id(req),text(req,"status"));message="Đã cập nhật tiến độ sửa chữa.";}
            else if(key.equals("notifications")&&action.equals("publish")){service.publishNotification(accountId,text(req,"title"),text(req,"body"));message="Đã đăng thông báo cho sinh viên.";}
            else{res.sendError(400);return;}
            redirect(req,res,destination,message,false);
        }catch(BusinessException e){req.setAttribute("flashError",e.getMessage());res.setStatus(400);doGet(req,res);}
        catch(SQLException e){if(constraintError(e)){req.setAttribute("flashError","Bản ghi bị trùng hoặc vừa được xử lý. Hãy kiểm tra lại danh sách.");res.setStatus(409);doGet(req,res);}else databaseError(res,e);}
    }
}
