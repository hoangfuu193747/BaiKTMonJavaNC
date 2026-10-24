<%@ page pageEncoding="UTF-8" %>
<%@ include file="../../common/layouts/app-start.jspf" %>
<%@ include file="../../common/components/admin-heading.jspf" %>
<section class="panel admin-profile-panel"><div class="profile-cover"><span class="avatar" aria-hidden="true">QT</span><div><h2>Quản trị viên</h2><p><c:out value="${profile.username}"/></p></div></div><div class="panel-body"><dl class="detail-list"><div><dt>Tên đăng nhập</dt><dd><c:out value="${profile.username}"/></dd></div><div><dt>Vai trò</dt><dd>Quản trị viên</dd></div><div><dt>Ngày tạo</dt><dd><c:out value="${profile.ngayTaoText}"/></dd></div><div><dt>Trạng thái</dt><dd><span class="tag">Hoạt động</span></dd></div></dl><a class="btn btn-primary section-gap" href="<c:url value='/admin/password'/>">Đổi mật khẩu</a></div></section>
<%@ include file="../../common/layouts/app-end.jspf" %>
