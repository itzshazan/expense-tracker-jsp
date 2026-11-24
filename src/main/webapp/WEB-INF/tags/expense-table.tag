<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="items" required="true" type="java.util.List" %>
<%@ attribute name="actions" required="true" type="java.lang.Boolean" %>
<c:choose>
    <c:when test="${empty items}">
        <p class="empty">No expenses match this view.</p>
    </c:when>
    <c:otherwise>
        <div class="table-scroll">
            <table class="expense-table">
                <thead><tr><th scope="col">Merchant</th><th scope="col">Date</th><th scope="col">Category</th>
                    <th scope="col">Payment</th><th scope="col">Receipt</th><th scope="col" class="number">Amount</th>
                    <c:if test="${actions}"><th scope="col"><span class="sr-only">Actions</span></th></c:if>
                </tr></thead>
                <tbody>
                <c:forEach var="row" items="${items}">
                    <tr>
                        <td class="merchant"><strong><c:out value="${row.merchant}"/></strong>
                            <c:if test="${not empty row.description}"><small><c:out value="${row.description}"/></small></c:if>
                        </td>
                        <td><c:out value="${row.date}"/></td>
                        <td><span class="pill"><c:out value="${row.category}"/></span></td>
                        <td><c:out value="${row.payment}"/></td>
                        <td class="receipt-cell">
                            <c:choose>
                                <c:when test="${row.receipt}"><a href="${base}/receipt?id=${row.id}" target="_blank" rel="noopener">View</a></c:when>
                                <c:when test="${row.failed}"><span class="receipt-failed">Upload failed</span></c:when>
                                <c:otherwise><span class="muted">—</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="number amount"><strong><c:out value="${row.amount}"/></strong></td>
                        <c:if test="${actions}"><td class="number row-actions"><a href="${base}/expense/edit?id=${row.id}">Edit</a></td></c:if>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>
