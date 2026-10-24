package com.qlktx.controller;
import com.qlktx.model.SessionUser;
import com.qlktx.service.BusinessException;
import com.qlktx.util.CsrfUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
public abstract class BaseServlet extends HttpServlet {
    protected String text(HttpServletRequest req,String name){String value=req.getParameter(name);return value==null?"":value.trim();}
    protected String raw(HttpServletRequest req,String name){String value=req.getParameter(name);return value==null?"":value;}
    protected SessionUser user(HttpServletRequest req){return (SessionUser)req.getSession(false).getAttribute("authUser");}
    protected int id(HttpServletRequest req)throws BusinessException{return com.qlktx.service.FormValidation.number(text(req,"id"),"Mã bản ghi",1,Integer.MAX_VALUE);}
    protected String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    protected void prepare(HttpServletRequest req,String title,String menu){
        req.setAttribute("pageTitle",title);req.setAttribute("selectedMenu",menu);
        req.setAttribute("csrfToken",CsrfUtil.token(req.getSession()));
        for(String key:new String[]{"flashSuccess","flashError"}){
            Object value=req.getSession().getAttribute(key);if(value!=null){req.setAttribute(key,value);req.getSession().removeAttribute(key);}
        }
    }
    protected void view(HttpServletRequest req,HttpServletResponse res,String path)throws IOException,ServletException{req.getRequestDispatcher("/WEB-INF/views/"+path).forward(req,res);}
    protected void redirect(HttpServletRequest req,HttpServletResponse res,String path,String message,boolean error){
        if(message!=null)req.getSession().setAttribute(error?"flashError":"flashSuccess",message);
        res.setStatus(303);res.setHeader("Location",req.getContextPath()+path);
    }
    protected void databaseError(HttpServletResponse res,SQLException error)throws IOException{
        getServletContext().log("Database operation failed in " + getClass().getSimpleName(),error);res.sendError(503);
    }
    protected boolean constraintError(SQLException error){return error.getErrorCode()==2601||error.getErrorCode()==2627||error.getErrorCode()==547;}
}

