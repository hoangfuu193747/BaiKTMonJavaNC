package com.qlktx.filter;

import com.qlktx.model.Role;
import com.qlktx.model.SessionUser;
import com.qlktx.util.CsrfUtil;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AuthFilterTest {
    private HttpSession session(Map<String, Object> attributes) {
        return (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpSession.class}, (proxy, method, args) -> {
            if (method.getName().equals("getAttribute")) return attributes.get(args[0]);
            if (method.getName().equals("setAttribute")) { attributes.put((String) args[0], args[1]); return null; }
            return null;
        });
    }

    private Map<String, Object> check(String path, HttpSession session) throws Exception {
        Map<String, Object> outcome = new HashMap<>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getServletPath" -> path;
            case "getContextPath" -> "/QuanLyKyTucXa";
            case "getSession" -> session;
            default -> null;
        });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletResponse.class}, (proxy, method, args) -> {
            if (method.getName().equals("sendRedirect")) outcome.put("redirect", args[0]);
            if (method.getName().equals("sendError")) outcome.put("status", args[0]);
            return null;
        });
        AtomicBoolean passed = new AtomicBoolean();
        new AuthFilter(){
            @Override protected java.util.Optional<SessionUser> refreshUser(SessionUser user){
                return "disabled".equals(user.getId())?java.util.Optional.empty():java.util.Optional.of(user);
            }
        }.doFilter(request, response, (req, res) -> passed.set(true));
        outcome.put("passed", passed.get());
        return outcome;
    }

    @Test void anonymousUserIsRedirectedToLogin() throws Exception {
        var result = check("/admin/home", null);
        assertEquals("/QuanLyKyTucXa/auth/login?role=admin", result.get("redirect"));
        assertEquals(false, result.get("passed"));
    }

    @Test void studentCannotAccessAdminPagesIncludingStaticDemoPages() throws Exception {
        HttpSession session = session(new HashMap<>(Map.of("authUser", new SessionUser("23110001", "23110001", "Sinh viên", "sv@example.com", Role.STUDENT))));
        assertEquals(403, check("/admin/home", session).get("status"));
        assertEquals(403, check("/admin/sinh-vien.html", session).get("status"));
        assertEquals(true, check("/student/home", session).get("passed"));
    }

    @Test void adminCannotUseStudentIdentityRoutes() throws Exception {
        HttpSession session = session(new HashMap<>(Map.of("authUser", new SessionUser("1", "admin", "Admin", null, Role.ADMIN))));
        assertEquals(403, check("/student/home", session).get("status"));
        assertEquals(true, check("/admin/home", session).get("passed"));
    }

    @Test void csrfRequiresTheTokenFromTheSameSession() {
        HttpSession first = session(new HashMap<>());
        HttpSession second = session(new HashMap<>());
        String token = CsrfUtil.token(first);
        assertEquals(token, CsrfUtil.token(first));
        assertTrue(CsrfUtil.valid(first, token));
        assertFalse(CsrfUtil.valid(second, token));
        assertFalse(CsrfUtil.valid(null, token));
        assertFalse(CsrfUtil.valid(first, "invalid"));
    }
    @Test void disabledAccountLosesAccessDuringAnExistingSession() throws Exception {
        HttpSession session=session(new HashMap<>(Map.of("authUser",new SessionUser("disabled","disabled","Sinh viên",null,Role.STUDENT))));
        var result=check("/student/rooms",session);
        assertEquals(false,result.get("passed"));
        assertEquals("/QuanLyKyTucXa/auth/login?role=student",result.get("redirect"));
    }
}
