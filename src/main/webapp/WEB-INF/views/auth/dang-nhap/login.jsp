<%@ page pageEncoding="UTF-8" %>
<%@ include file="../../common/layouts/auth-start.jspf" %>

<h2 >Đăng nhập</h2>

<nav class="role-tabs" aria-label="Chọn vai trò đăng nhập"><a class="role-tab ${selectedRole == 'student' ? 'active' : ''}" href="<c:url value='/auth/login?role=student'/>">Sinh viên</a><a class="role-tab ${selectedRole == 'admin' ? 'active' : ''}" href="<c:url value='/auth/login?role=admin'/>">Quản trị viên</a></nav>
<c:if test="${param.registered == '1'}"><div class="auth-message success" role="status">Tạo tài khoản thành công. Bạn có thể đăng nhập bằng mã sinh viên.</div></c:if>
<c:if test="${param.loggedOut == '1'}"><div class="auth-message success" role="status">Bạn đã đăng xuất.</div></c:if>
<c:if test="${param.passwordChanged == '1'}"><div class="auth-message success" role="status">Đã đổi mật khẩu. Hãy đăng nhập bằng mật khẩu mới.</div></c:if>
<%@ include file="../../common/components/form-errors.jspf" %>
<form method="post" action="<c:url value='/auth/login'/>">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
<input type="hidden" name="role" value="<c:out value='${selectedRole}'/>">
<div class="field"><label for="username">${selectedRole == 'admin' ? 'Tên đăng nhập' : 'Mã sinh viên'}</label><input id="username" name="username" class="form-control" value="<c:out value='${username}'/>" maxlength="50" autocomplete="username" placeholder="${selectedRole == 'admin' ? 'Nhập tên đăng nhập' : 'Nhập mã sinh viên'}" required></div>
<div class="field"><label for="password">Mật khẩu</label><div class="password-input"><input id="password" name="password" type="password" class="form-control" maxlength="128" autocomplete="current-password" placeholder="Nhập mật khẩu" required><button type="button" class="password-toggle" data-password="password" aria-label="Hiện mật khẩu" aria-pressed="false">Hiện</button></div></div>
<button class="btn btn-primary" type="submit">Đăng nhập <span aria-hidden="true">→</span></button>
</form>
<c:choose><c:when test="${selectedRole == 'student'}"><p class="auth-link">Bạn chưa có tài khoản? <a href="<c:url value='/auth/register'/>">Đăng ký sinh viên</a></p></c:when><c:otherwise></c:otherwise></c:choose>
<%@ include file="../../common/layouts/auth-end.jspf" %>

