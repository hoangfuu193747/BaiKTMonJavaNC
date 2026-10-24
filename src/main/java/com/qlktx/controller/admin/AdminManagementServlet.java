package com.qlktx.controller.admin;

import com.qlktx.controller.BaseServlet;
import com.qlktx.model.PortalStatus;
import com.qlktx.dao.AdminManagementDAO;
import com.qlktx.dao.PortalDAO;
import com.qlktx.model.MenuFeature;
import com.qlktx.model.Role;
import com.qlktx.service.AdminManagementService;
import com.qlktx.service.BusinessException;
import com.qlktx.service.FormValidation;
import com.qlktx.util.CsvUtil;
import com.qlktx.util.DisplayUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns={"/admin","/admin/","/admin/home","/admin/students","/admin/zones","/admin/rooms","/admin/reports","/admin/profile","/admin/password"})
public class AdminManagementServlet extends BaseServlet {
    private final AdminManagementDAO dao=new AdminManagementDAO();
    private final PortalDAO portal=new PortalDAO();
    private final AdminManagementService service=new AdminManagementService();

    private String key(HttpServletRequest req) {
        String path=req.getServletPath();
        return path.equals("/admin")||path.equals("/admin/")?"home":path.substring("/admin/".length());
    }

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        String key=key(req);
        String title=key.equals("profile")?"Hồ sơ quản trị":key.equals("password")?"Đổi mật khẩu":MenuFeature.forRole(Role.ADMIN).stream().filter(feature -> feature.getKey().equals(key)).map(MenuFeature::getTitle).findFirst().orElse("");
        prepare(req,title,key);
        req.setAttribute("features",MenuFeature.forRole(Role.ADMIN));req.setAttribute("routePrefix","/admin");
        req.setAttribute("statusLabels",PortalStatus.LABELS);req.setAttribute("adminView",true);
        for(String name:List.of("q","zone","floor","status","month","year"))req.setAttribute(name,text(req,name));
        try {
            switch(key) {
                case "home" -> {
                    req.setAttribute("stats",dao.overview());req.setAttribute("reports",dao.reports("",null,null));
                    req.setAttribute("registrations",portal.registrations("","CHO_DUYET").stream().limit(6).toList());
                    var invoices=portal.invoices("");
                    BigDecimal debt=invoices.stream().map(invoice -> PortalDAO.money(invoice,"conNo")).reduce(BigDecimal.ZERO,BigDecimal::add);
                    req.setAttribute("debtText",DisplayUtil.money(debt));
                }
                case "students" -> {
                    var students=dao.students(FormValidation.text(text(req,"q"),"Từ khóa",100,false));
                    paginate(req,students,"students");
                    String action=text(req,"action");
                    boolean editing=action.equals("edit")||action.equals("update");
                    req.setAttribute("editing",editing);req.setAttribute("showForm",editing||action.equals("new")||action.equals("create"));
                    if(editing){var student=dao.student(text(req,"id"));if(student.isEmpty()){res.sendError(404);return;}req.setAttribute("student",student.get());}
                }
                case "zones" -> {
                    var zones=dao.zones();req.setAttribute("zones",zones);
                    if(!text(req,"id").isEmpty()){
                        var zone=zones.stream().filter(row -> text(req,"id").equals(row.get("maKhu"))).findFirst();
                        if(zone.isEmpty()){res.sendError(404);return;}req.setAttribute("selectedZone",zone.get());
                    }
                }
                case "rooms" -> {
                    validateFilter(text(req,"zone"),Set.of("A","B","C","D"));
                    validateFilter(text(req,"floor"),Set.of("1","2","3","4","5"));
                    validateFilter(text(req,"status"),Set.of("HOAT_DONG","BAO_TRI"));
                    var rooms=portal.rooms(FormValidation.text(text(req,"q"),"Từ khóa",100,false),text(req,"zone"),text(req,"floor"),"",null,false).stream().filter(room -> text(req,"status").isEmpty()||text(req,"status").equals(room.get("trangThai"))).toList();
                    paginate(req,rooms,"rooms");
                    if(!text(req,"id").isEmpty()){
                        var room=portal.room(text(req,"id"));if(room.isEmpty()){res.sendError(404);return;}req.setAttribute("selectedRoom",room.get());
                    }
                }
                case "reports" -> {
                    validateFilter(text(req,"zone"),Set.of("A","B","C","D"));
                    boolean period=!text(req,"month").isEmpty()||!text(req,"year").isEmpty();
                    Integer month=period?FormValidation.number(text(req,"month"),"Tháng",1,12):null;
                    Integer year=period?FormValidation.number(text(req,"year"),"Năm",2020,2100):null;
                    var rows=dao.reports(text(req,"zone"),month,year);req.setAttribute("reports",rows);
                    if("csv".equals(text(req,"export"))){export(res,rows);return;}
                }
                case "profile" -> {
                    var profile=dao.admin(Integer.parseInt(user(req).getId()));if(profile.isEmpty()){res.sendError(403);return;}req.setAttribute("profile",profile.get());
                }
                case "password" -> { }
                default -> {res.sendError(404);return;}
            }
            view(req,res,"admin/management/"+key+".jsp");
        }catch(BusinessException error){
            req.setAttribute("flashError",error.getMessage());res.setStatus(400);
            req.setAttribute("students",List.of());req.setAttribute("rooms",List.of());req.setAttribute("reports",List.of());
            view(req,res,"admin/management/"+key+".jsp");
        }catch(SQLException error){databaseError(res,error);}
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        String key=key(req),action=text(req,"action");int accountId=Integer.parseInt(user(req).getId());
        try {
            if(key.equals("students")) {
                switch(action){
                    case "create","update" -> {
                        Map<String,String> values=new LinkedHashMap<>();
                        for(String name:List.of("id","name","birthday","gender","className","faculty","email","phone"))values.put(name,text(req,name));
                        service.saveStudent(accountId,action.equals("create"),values);
                    }
                    case "accountState","rosterState" -> {
                        if(!Set.of("0","1").contains(text(req,"active")))throw new BusinessException("Trạng thái không hợp lệ.");
                        service.studentState(accountId,text(req,"id"),text(req,"active").equals("1"),action.equals("rosterState"));
                    }
                    case "delete" -> service.deleteStudent(accountId,text(req,"id"));
                    default -> {res.sendError(400);return;}
                }
            }else if(key.equals("zones")&&action.equals("update")){
                if(!Set.of("0","1").contains(text(req,"active")))throw new BusinessException("Trạng thái không hợp lệ.");
                service.saveZone(accountId,text(req,"id"),text(req,"name"),text(req,"active").equals("1"));
            }else if(key.equals("rooms")&&action.equals("update"))service.saveRoom(accountId,text(req,"id"),text(req,"roomStatus"));
            else if(key.equals("password")&&action.equals("change")){
                service.changePassword(accountId,raw(req,"currentPassword"),raw(req,"newPassword"),raw(req,"confirmPassword"));
                req.getSession(false).invalidate();redirect(req,res,"/auth/login?role=admin&passwordChanged=1",null,false);return;
            }else{res.sendError(400);return;}
            redirect(req,res,"/admin/"+key,"Đã lưu thay đổi.",false);
        }catch(BusinessException error){req.setAttribute("flashError",error.getMessage());res.setStatus(400);doGet(req,res);}
        catch(SQLException error){
            if(constraintError(error)){req.setAttribute("flashError","Dữ liệu bị trùng hoặc đã có lịch sử liên quan. Kiểm tra lại trước khi lưu.");res.setStatus(409);doGet(req,res);}
            else databaseError(res,error);
        }
    }

    private void validateFilter(String value,Set<String> allowed) throws BusinessException {
        if(!value.isEmpty()&&!allowed.contains(value))throw new BusinessException("Bộ lọc không hợp lệ.");
    }

    private void paginate(HttpServletRequest req,List<Map<String,Object>> rows,String attribute) throws BusinessException {
        int pages=Math.max(1,(rows.size()+19)/20);
        int page=text(req,"page").isEmpty()?1:FormValidation.number(text(req,"page"),"Trang",1,Integer.MAX_VALUE);
        page=Math.min(page,pages);
        req.setAttribute(attribute,rows.subList((page-1)*20,Math.min(page*20,rows.size())));
        req.setAttribute("currentPage",page);req.setAttribute("totalPages",pages);req.setAttribute("totalRecords",rows.size());
    }

    private void export(HttpServletResponse res,List<Map<String,Object>> rows) throws IOException {
        res.setContentType("text/csv; charset=UTF-8");res.setHeader("Content-Disposition","attachment; filename=bao-cao-ky-tuc-xa.csv");
        var writer=res.getWriter();writer.print('\ufeff');
        writer.print("Khu,Tên khu,Giới tính,Số phòng,Tổng chỗ,Đang ở,Đơn chờ duyệt,Tổng hóa đơn,Đã xác nhận,Chờ xác nhận,Còn phải đóng\r\n");
        for(var row:rows){
            boolean first=true;
            for(String key:List.of("maKhu","tenKhu","gioiTinh","soPhong","tongCho","dangO","choDuyet","tongTien","daDong","choXacNhan","conNo")){
                if(!first)writer.print(',');writer.print(CsvUtil.cell(row.get(key)));first=false;
            }
            writer.print("\r\n");
        }
    }

}
