package com.qlktx.controller.auth;

import com.qlktx.model.Role;
import com.qlktx.model.SessionUser;
import com.qlktx.service.AuthService;
import com.qlktx.service.RegistrationForm;
import com.qlktx.service.ValidationException;
import com.qlktx.util.CsrfUtil;
import com.qlktx.util.LoginAttemptLimiter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/auth/login", "/auth/register", "/auth/logout"})
public class AuthServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(AuthServlet.class.getName());
    private final AuthService service = new AuthService();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/auth/logout".equals(path)) { response.sendError(405); return; }
        SessionUser current = currentUser(request);
        if (current != null) { response.sendRedirect(request.getContextPath() + current.getHomePath()); return; }
        if ("/auth/register".equals(path)) {
            renderRegistration(request, response);
        } else {
            request.setAttribute("selectedRole", "admin".equals(request.getParameter("role")) ? "admin" : "student");
            renderLogin(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (request.getServletPath()) {
            case "/auth/login" -> login(request, response);
            case "/auth/register" -> register(request, response);
            case "/auth/logout" -> {
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                redirect(request, response, "/auth/login?loggedOut=1");
            }
            default -> response.sendError(404);
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String roleValue = text(request, "role");
        if (!roleValue.equals("student") && !roleValue.equals("admin")) { response.sendError(400); return; }
        request.setAttribute("selectedRole", roleValue);
        String username = text(request, "username");
        String password = raw(request, "password");
        request.setAttribute("username", username);
        if (username.isBlank() || username.length() > 50 || password.isBlank() || password.length() > 128) {
            request.setAttribute("errors", Map.of("global", "Vui lòng nhập đầy đủ tài khoản và mật khẩu hợp lệ."));
            renderLogin(request, response);
            return;
        }
        String key = limiter.key(request.getRemoteAddr(), roleValue, username);
        if (limiter.blocked(key)) {
            response.setStatus(429);
            request.setAttribute("errors", Map.of("global", "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau 15 phút."));
            renderLogin(request, response);
            return;
        }
        try {
            Role role = roleValue.equals("admin") ? Role.ADMIN : Role.STUDENT;
            Optional<SessionUser> result = service.login(role, username, password);
            if (result.isEmpty()) {
                limiter.failed(key);
                response.setStatus(401);
                request.setAttribute("errors", Map.of("global", "Tài khoản hoặc mật khẩu không đúng, hoặc tài khoản không được phép đăng nhập."));
                renderLogin(request, response);
                return;
            }
            limiter.succeeded(key);
            HttpSession old = request.getSession(false);
            if (old != null) old.invalidate();
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("authUser", result.get());
            CsrfUtil.token(session);
            redirect(request, response, result.get().getHomePath());
        } catch (SQLException e) {
            databaseFailure(request, response, e);
            renderLogin(request, response);
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (currentUser(request) != null) { response.sendError(400); return; }
        String studentId = text(request, "studentId");
        request.setAttribute("studentId", studentId);
        request.setAttribute("email", text(request, "email"));
        request.setAttribute("phone", text(request, "phone"));
        request.setAttribute("birthDate", text(request, "birthDate"));
        try {
            String action = text(request, "action");
            if ("lookup".equals(action)) {
                request.setAttribute("schoolStudent", service.lookupStudent(studentId,text(request,"email")));
            } else if ("register".equals(action)) {
                RegistrationForm form = new RegistrationForm(studentId, text(request, "email"), text(request, "phone"),
                        text(request, "birthDate"), raw(request, "password"), raw(request, "confirmPassword"));
                service.register(form);
                redirect(request, response, "/auth/login?role=student&registered=1");
                return;
            } else { response.sendError(400); return; }
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
            if (!e.getErrors().containsKey("studentId")) {
                try { request.setAttribute("schoolStudent", service.lookupStudent(studentId,text(request,"email"))); }
                catch (ValidationException ignored) { /* Keep the original validation error. */ }
                catch (SQLException databaseError) { databaseFailure(request, response, databaseError); }
            }
        } catch (SQLException e) { databaseFailure(request, response, e); }
        renderRegistration(request, response);
    }

    private void renderLogin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("pageTitle", "Đăng nhập");
        request.setAttribute("csrfToken", CsrfUtil.token(request.getSession()));
        request.getRequestDispatcher("/WEB-INF/views/auth/dang-nhap/login.jsp").forward(request, response);
    }

    private void renderRegistration(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("pageTitle", "Đăng ký tài khoản");
        request.setAttribute("csrfToken", CsrfUtil.token(request.getSession()));
        request.getRequestDispatcher("/WEB-INF/views/auth/dang-ky/register.jsp").forward(request, response);
    }

    private void databaseFailure(HttpServletRequest request, HttpServletResponse response, SQLException exception) {
        LOG.log(Level.SEVERE, "Authentication database operation failed.", exception);
        response.setStatus(503);
        request.setAttribute("errors", Map.of("global", "Hệ thống tạm thời chưa kết nối được. Vui lòng thử lại sau."));
    }

    private static SessionUser currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute("authUser");
    }

    private static String text(HttpServletRequest request, String name) { return raw(request, name).trim(); }
    private static String raw(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value;
    }

    private static void redirect(HttpServletRequest request, HttpServletResponse response, String path) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", request.getContextPath() + path);
    }
}
