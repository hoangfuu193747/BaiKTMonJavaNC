<%@ page pageEncoding="UTF-8" %>
<%@ include file="../../common/layouts/app-start.jspf" %>
<%@ include file="../../common/components/student-heading.jspf" %>
<c:choose><c:when test="${not empty invoice}">
<div class="button-group non-print section-gap"><a class="btn btn-outline" href="<c:url value='/student/invoices'/>">← Tất cả hóa đơn</a></div>
<%@ include file="../../common/components/portal-invoice-detail.jspf" %>
<section class="panel"><div class="panel-head"><h2>Các khoản thanh toán · HD${invoice.maHoaDon}</h2></div><c:set var="payments" value="${invoicePayments}"/><%@ include file="../../common/components/portal-payments.jspf" %></section>
</c:when><c:otherwise><section class="panel"><div class="panel-head"><h2>Hóa đơn theo kỳ</h2><span class="muted">Mới nhất trước</span></div><%@ include file="../../common/components/portal-invoices.jspf" %></section></c:otherwise></c:choose>
<%@ include file="../../common/layouts/app-end.jspf" %>
