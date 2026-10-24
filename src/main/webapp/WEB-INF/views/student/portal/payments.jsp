<%@ page pageEncoding="UTF-8" %>
<%@ include file="../../common/layouts/app-start.jspf" %>
<%@ include file="../../common/components/student-heading.jspf" %>
<div class="portal-payment-layout">
  <section class="panel"><div class="panel-head"><h2>Gửi thông tin thanh toán</h2><span class="tag amber">Mô phỏng</span></div><div class="panel-body">
    <c:choose><c:when test="${empty payableInvoices}"><div class="student-empty"><h3>Không có khoản cần thanh toán</h3><p>Bạn có thể xem lại các khoản đã đóng trong lịch sử bên dưới.</p><a class="btn btn-outline" href="<c:url value='/student/invoices'/>">Xem hóa đơn</a></div></c:when><c:otherwise>
      <form method="get" action="<c:url value='/student/payments'/>"><div class="field"><label for="chooseInvoice">Chọn hóa đơn</label><select id="chooseInvoice" class="form-select" name="invoice" required data-submit-on-change><option value="">Chọn kỳ cần thanh toán</option><c:forEach items="${payableInvoices}" var="bill"><option value="${bill.maHoaDon}" ${invoice.maHoaDon == bill.maHoaDon ? 'selected' : ''}>HD${bill.maHoaDon} · ${bill.thang}/${bill.nam} · <c:out value="${bill.conNoText}"/></option></c:forEach></select></div><noscript><button class="btn btn-outline">Tiếp tục</button></noscript></form>
    </c:otherwise></c:choose>
    <c:if test="${not empty invoice}">
      <p class="portal-help">HD${invoice.maHoaDon} · Phòng <c:out value="${invoice.maPhong}"/> · Kỳ ${invoice.thang}/${invoice.nam} · Hạn đóng <c:out value="${invoice.hanThanhToanText}"/></p>
      <div class="portal-payment-summary"><div><small>Đã xác nhận</small><strong><c:out value="${invoice.daDongText}"/></strong></div><div><small>Chờ xác nhận</small><strong><c:out value="${invoice.choXacNhanText}"/></strong></div><div><small>Có thể gửi thêm</small><strong><c:out value="${invoice.coTheGuiText}"/></strong></div></div>
      <c:choose><c:when test="${invoice.coTheGui > 0}">
        <form class="portal-form section-gap" method="post" action="<c:url value='/student/payments'/>" data-confirm="Gửi yêu cầu thanh toán mô phỏng để ban quản lý xác nhận?">
          <%@ include file="../../common/components/portal-token.jspf" %><input type="hidden" name="action" value="transfer"><input type="hidden" name="invoice" value="${invoice.maHoaDon}">
          <div class="field"><label for="amount">Số tiền (VNĐ)</label><input id="amount" name="amount" class="form-control" type="number" min="1" max="${invoice.coTheGui}" step="1" aria-describedby="amount-help" value="<c:out value='${empty param.amount ? invoice.coTheGui : param.amount}'/>" required><small id="amount-help">Nhập số nguyên đồng, tối đa <c:out value="${invoice.coTheGuiText}"/>. Có thể gửi một phần số tiền.</small></div>
          <details class="portal-optional" ${not empty flashError and not empty param.reference ? 'open' : ''}><summary>Thêm mã giao dịch (tùy chọn)</summary><div class="field"><label for="reference">Mã giao dịch mẫu</label><input id="reference" name="reference" class="form-control" maxlength="100" pattern="[A-Za-z0-9._\/\-]{1,100}" aria-describedby="reference-help" value="<c:out value='${param.reference}'/>" placeholder="Để trống để tạo mã mẫu tự động"><small id="reference-help">Chỉ dùng chữ không dấu, số, dấu chấm, /, gạch ngang hoặc gạch dưới.</small></div></details>
          <button class="btn btn-primary">Gửi yêu cầu xác nhận</button>
        </form>
      </c:when><c:otherwise><p class="info-box section-gap">Số tiền đã đóng đủ hoặc đã có yêu cầu chờ xác nhận. Bạn không cần gửi thêm.</p></c:otherwise></c:choose>
      <a class="text-link section-gap" href="<c:url value='/student/invoices'/>?id=${invoice.maHoaDon}">Xem chi tiết hóa đơn →</a>
    </c:if>
    <p class="portal-inline-note">Khoản đã đóng chỉ cập nhật khi ban quản lý xác nhận yêu cầu.</p>
  </div></section>
  <aside class="panel portal-bank-panel"><div class="panel-head"><h2>Thông tin ngân hàng mẫu</h2></div><div class="panel-body"><div class="portal-bank-card"><span class="portal-bank-mark" aria-hidden="true">K</span><div><small>NGÂN HÀNG MINH HỌA</small><h3><c:out value="${bankName}"/></h3></div><div class="portal-bank-account"><c:out value="${bankAccount}"/></div><p><c:out value="${bankOwner}"/></p></div><div class="student-demo-note"><strong>Chỉ dùng để mô phỏng</strong>Không chuyển tiền vào tài khoản mẫu. Nút gửi chỉ tạo yêu cầu trong hệ thống, không kết nối ngân hàng hay trừ tiền.</div><c:if test="${not empty invoice}"><dl class="detail-list section-gap"><div><dt>Nội dung mẫu</dt><dd>KTX <c:out value="${profile.maSV}"/> HD${invoice.maHoaDon}</dd></div></dl></c:if></div></aside>
</div>
<section class="panel"><div class="panel-head"><h2>Lịch sử thanh toán</h2></div><%@ include file="../../common/components/portal-payments.jspf" %></section>
<%@ include file="../../common/layouts/app-end.jspf" %>
