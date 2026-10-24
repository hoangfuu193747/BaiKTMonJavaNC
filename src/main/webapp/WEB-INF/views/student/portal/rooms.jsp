<%@ page pageEncoding="UTF-8" %>
<%@ include file="../../common/layouts/app-start.jspf" %>
<%@ include file="../../common/components/student-heading.jspf" %>
<c:if test="${hasActiveContract}"><p class="info-box">Bạn đang có hợp đồng. <a href="<c:url value='/student/contracts'/>">Xem hợp đồng</a></p></c:if>
<c:if test="${hasPendingRegistration}"><p class="info-box">Bạn có đơn chờ duyệt. <a href="<c:url value='/student/history'/>">Xem đơn</a></p></c:if>
<section class="panel">
  <form class="filter-bar portal-room-filters" method="get" action="<c:url value='/student/rooms'/>">
    <div class="field"><label for="room-search">Tìm phòng</label><input id="room-search" class="control" name="q" placeholder="Mã phòng hoặc tên khu" maxlength="100" value="<c:out value='${q}'/>"></div>
    <div class="field"><label for="room-zone">Khu nhà</label><select id="room-zone" class="form-select" name="zone"><option value="">Tất cả khu</option><c:forTokens items="A,B,C,D" delims="," var="zoneOption"><option value="${zoneOption}" ${zone == zoneOption ? 'selected' : ''}>Khu ${zoneOption}</option></c:forTokens></select></div>
    <div class="field"><label for="room-floor">Tầng</label><select id="room-floor" class="form-select" name="floor"><option value="">Tất cả tầng</option><c:forEach begin="1" end="5" var="floorOption"><option value="${floorOption}" ${floor == floorOption ? 'selected' : ''}>Tầng ${floorOption}</option></c:forEach></select></div>
    <div class="field"><label for="room-gender">Dành cho</label><select id="room-gender" class="form-select" name="gender"><option value="">Tất cả</option><option value="Nam" ${gender == 'Nam' ? 'selected' : ''}>Nam</option><option value="Nữ" ${gender == 'Nữ' ? 'selected' : ''}>Nữ</option></select></div>
    <div class="field"><label for="room-price">Giá tối đa / tháng (VNĐ)</label><input id="room-price" class="control" type="number" name="maxPrice" min="0" max="999999999999" step="1" placeholder="Không giới hạn" value="<c:out value='${maxPrice}'/>"></div>
    <label class="portal-checkbox"><input type="checkbox" name="available" value="1" ${available == '1' ? 'checked' : ''}> Còn chỗ</label>
    <div class="button-group"><button class="btn btn-primary">Lọc</button><a class="btn btn-outline" href="<c:url value='/student/rooms'/>">Đặt lại</a></div>
  </form>
</section>
<div class="portal-list-meta"><span><c:out value="${empty roomCount ? 0 : roomCount}"/> phòng</span><c:if test="${totalPages > 1}"><span>Trang ${currentPage} / ${totalPages}</span></c:if></div>
<div class="portal-room-grid">
  <c:forEach items="${rooms}" var="r"><article class="room-card"><div class="room-card-body">
    <div class="portal-card-head"><h2><c:out value="${r.maPhong}"/></h2><span class="tag ${r.trangThai != 'HOAT_DONG' or not r.khuHoatDong ? 'gray' : (r.conCho > 0 ? '' : 'red')}">${r.trangThai != 'HOAT_DONG' or not r.khuHoatDong ? 'Tạm dừng' : (r.conCho > 0 ? r.conCho : 0)}${r.trangThai == 'HOAT_DONG' and r.khuHoatDong ? ' chỗ trống' : ''}</span></div>
    <p><c:out value="${r.tenKhu}"/> · Tầng ${r.tang} · <c:out value="${r.gioiTinh}"/></p>
    <strong class="room-price"><c:out value="${r.giaMoiSVText}"/><small> / tháng</small></strong>
    <div class="room-card-bottom"><span class="muted">${r.trangThai != 'HOAT_DONG' or not r.khuHoatDong ? 'Tạm dừng' : (empty r.reasons ? 'Có thể đăng ký' : 'Chưa đủ điều kiện')}</span><a class="btn btn-outline btn-sm" href="<c:url value='/student/register'/>?room=<c:out value='${r.maPhong}'/>">Xem phòng</a></div>
  </div></article></c:forEach>
</div>
<c:if test="${empty rooms}"><section class="panel"><div class="student-empty"><h2>Không tìm thấy phòng phù hợp</h2><p>Thử đổi khu, tầng hoặc tăng mức giá tối đa để có thêm lựa chọn.</p><a class="btn btn-outline" href="<c:url value='/student/rooms'/>">Xóa bộ lọc</a></div></section></c:if>
<c:if test="${totalPages > 1}"><nav class="portal-pagination" aria-label="Phân trang phòng">
  <c:forEach begin="1" end="${totalPages}" var="p">
    <c:url value="/student/rooms" var="pageUrl"><c:param name="page" value="${p}"/><c:param name="q" value="${q}"/><c:param name="zone" value="${zone}"/><c:param name="floor" value="${floor}"/><c:param name="gender" value="${gender}"/><c:param name="maxPrice" value="${maxPrice}"/><c:param name="available" value="${available}"/></c:url>
    <a class="${p == currentPage ? 'active' : ''}" href="<c:out value='${pageUrl}'/>" ${p == currentPage ? 'aria-current="page"' : ''}>${p}</a>
  </c:forEach>
</nav></c:if>
<%@ include file="../../common/layouts/app-end.jspf" %>
