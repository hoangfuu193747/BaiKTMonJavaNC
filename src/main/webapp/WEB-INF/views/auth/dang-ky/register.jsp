<%@ page pageEncoding="UTF-8" %>
<%@ include file="../../common/layouts/auth-start.jspf" %>

<h2 >Đăng ký tài khoản</h2>

<div class="registration-step"><strong><span>1</span> Đối chiếu hồ sơ</strong><i></i><strong style="${empty schoolStudent ? 'color:#8b9b80' : ''}"><span>2</span> Tạo tài khoản</strong></div>
<%@ include file="../../common/components/form-errors.jspf" %>
<c:if test="${not empty errors.studentId}"><div class="auth-message error" role="alert"><c:out value="${errors.studentId}"/></div></c:if>
<c:choose><c:when test="${empty schoolStudent}">
<form method="post" action="<c:url value='/auth/register'/>">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="lookup">
<div class="field"><label for="studentId">Mã sinh viên *</label><input class="form-control" id="studentId" name="studentId" value="<c:out value='${studentId}'/>" maxlength="10" autocomplete="username" placeholder="Mã sinh viên do trường cấp" pattern="[A-Za-z0-9_.\-]{1,10}" required></div>
<div class="field"><label for="email">Email đã đăng ký với trường *</label><input id="email" name="email" type="email" class="form-control" value="<c:out value='${email}'/>" maxlength="120" autocomplete="email" placeholder="Nhập Email đã cung cấp cho trường" required><c:if test="${not empty errors.email}"><span class="field-error"><c:out value="${errors.email}"/></span></c:if></div>
<button class="btn btn-primary" type="submit">Kiểm tra thông tin →</button>
</form>
</c:when><c:otherwise>
<div class="verified-student"><small>✓ THÔNG TIN KHỚP DANH SÁCH TRƯỜNG</small><strong><c:out value="${schoolStudent.hoTen}"/></strong><p>Mã SV: <c:out value="${schoolStudent.maSV}"/> · Lớp: <c:out value="${schoolStudent.lop}"/> · <c:out value="${schoolStudent.gioiTinh}"/></p><p>Khoa: <c:out value="${schoolStudent.khoa}"/></p></div>
<form method="post" action="<c:url value='/auth/register'/>">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="register"><input type="hidden" name="studentId" value="<c:out value='${schoolStudent.maSV}'/>">
<div class="field"><label for="email">Email</label><input id="email" name="email" type="email" class="form-control" value="<c:out value='${email}'/>" maxlength="120" readonly required></div>
<div class="field"><label for="phone">Số điện thoại</label><input id="phone" name="phone" type="tel" class="form-control" value="<c:out value='${phone}'/>" maxlength="15" autocomplete="tel" placeholder="Không bắt buộc"><c:if test="${not empty errors.phone}"><span class="field-error"><c:out value="${errors.phone}"/></span></c:if></div>
<div class="form-grid"><div class="field"><label for="newPassword">Mật khẩu *</label><div class="password-input"><input id="newPassword" name="password" type="password" class="form-control" minlength="8" maxlength="72" autocomplete="new-password" placeholder="Tối thiểu 8 ký tự" required><button type="button" class="password-toggle" data-password="newPassword" aria-label="Hiện mật khẩu" aria-pressed="false">Hiện</button></div><c:if test="${not empty errors.password}"><span class="field-error"><c:out value="${errors.password}"/></span></c:if></div><div class="field"><label for="confirmPassword">Xác nhận mật khẩu *</label><input id="confirmPassword" name="confirmPassword" type="password" class="form-control" minlength="8" maxlength="72" autocomplete="new-password" required><c:if test="${not empty errors.confirmPassword}"><span class="field-error"><c:out value="${errors.confirmPassword}"/></span></c:if></div></div>
<button class="btn btn-primary" type="submit">Tạo tài khoản →</button>
</form><p class="auth-link"><a href="<c:url value='/auth/register'/>">← Đối chiếu thông tin khác</a></p>
</c:otherwise></c:choose>
<p class="auth-link">Đã có tài khoản? <a href="<c:url value='/auth/login?role=student'/>">Về đăng nhập</a></p>
<%@ include file="../../common/layouts/auth-end.jspf" %>

