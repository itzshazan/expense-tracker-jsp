<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="heading" required="true" %>
<%@ attribute name="items" required="true" type="java.util.List" %>
<section class="card section-card">
    <h2><c:out value="${heading}"/></h2>
    <c:if test="${empty items}"><p class="empty">No spending in this date range.</p></c:if>
    <c:forEach var="item" items="${items}">
        <div class="breakdown">
            <div><strong><c:out value="${item.label}"/></strong><strong><c:out value="${item.total}"/></strong></div>
            <progress value="${item.share}" max="100" aria-label="<c:out value='${item.label}'/> share of spending"></progress>
            <div class="muted"><span>${item.count} transactions</span><span>${item.share}%</span></div>
        </div>
    </c:forEach>
</section>
