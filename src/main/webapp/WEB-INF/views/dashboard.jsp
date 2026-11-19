<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="en"><head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Overview | Event Hall</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css">
</head><body><%@ include file="_header.jspf" %>
<section class="page-heading"><div><p class="eyebrow">OPERATIONS</p><h1>Overview</h1><p class="muted">A clear view of the events and balances that need attention.</p></div>
    <a class="button primary" href="${pageContext.request.contextPath}/admin/bookings">+ New booking</a></section>
<c:if test="${not empty sessionScope.notice}"><p class="alert success"><c:out value="${sessionScope.notice}"/></p><c:remove var="notice" scope="session"/></c:if>
<section class="metric-row">
    <div class="metric"><span>Upcoming events</span><strong><c:out value="${upcoming.size()}"/></strong><small>Scheduled dates ahead</small></div>
    <div class="metric accent-metric"><span>Pending balances</span><strong><c:out value="${pending.size()}"/></strong><small>Bookings with a balance due</small></div>
</section>
<section class="content-section"><div class="section-heading"><div><p class="eyebrow">SCHEDULE</p><h2>Upcoming events</h2></div><a class="text-link" href="${pageContext.request.contextPath}/admin/reports">All reports</a></div>
    <div class="table-wrap"><table><thead><tr><th>Date</th><th>Event</th><th>Customer</th><th>Venue</th><th>Slot</th><th>Balance</th></tr></thead><tbody>
    <c:forEach items="${upcoming}" var="day"><c:forEach items="${day.value}" var="booking">
        <tr><td><strong><c:out value="${booking.eventDate}"/></strong></td><td><c:out value="${booking.eventType}"/></td>
            <td><c:out value="${booking.customer.name}"/></td><td><c:out value="${booking.venue.name}"/></td>
            <td><c:out value="${booking.slot}"/></td><td><c:out value="${booking.balance}"/></td></tr>
    </c:forEach></c:forEach>
    <c:if test="${empty upcoming}"><tr><td colspan="6" class="empty-state">No events are scheduled yet.</td></tr></c:if>
    </tbody></table></div>
</section>
<%@ include file="_footer.jspf" %></body></html>