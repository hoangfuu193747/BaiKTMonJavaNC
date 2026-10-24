package com.qlktx.filter;

import com.qlktx.model.Role;
import com.qlktx.model.SessionUser;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthFilter implements Filter {
    protected java.util.Optional<SessionUser> refreshUser(SessionUser user) throws java.sql.SQLException {
        var account=user.getRole()==Role.ADMIN
                ? new com.qlktx.dao.QuanTriVienDAO().findAccount(user.getUsername())
                : new com.qlktx.dao.SinhVienDAO().findAccount(user.getId());
        return account.filter(a->a.active()).map(a->a.user());
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "same-origin");
        String path = req.getServletPath();
        if (!path.startsWith("/assets/")) res.setHeader("Cache-Control", "no-store");
        boolean admin = path.equals("/admin") || path.startsWith("/admin/");
        boolean student = path.equals("/student") || path.startsWith("/student/");
        if (admin || student) {
            HttpSession session = req.getSession(false);
            SessionUser user = session == null ? null : (SessionUser) session.getAttribute("authUser");
            if (user == null) {
                res.sendRedirect(req.getContextPath() + "/auth/login?role=" + (admin ? "admin" : "student"));
                return;
            }
            if ((admin && user.getRole() != Role.ADMIN) || (student && user.getRole() != Role.STUDENT)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            try {
                var current=refreshUser(user);
                if(current.isEmpty()){
                    session.invalidate();
                    res.sendRedirect(req.getContextPath()+"/auth/login?role="+(admin?"admin":"student"));
                    return;
                }
                session.setAttribute("authUser",current.get());
            }catch(java.sql.SQLException e){
                java.util.logging.Logger.getLogger(AuthFilter.class.getName()).log(java.util.logging.Level.SEVERE,"Cannot verify active session.",e);
                res.sendError(503);return;
            }
        }
        chain.doFilter(request, response);
    }
}
