<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<header class="page-heading"><div><a class="back-link" href="${base}/expenses">All expenses</a><h1><c:out value="${title}"/></h1></div></header>
<form class="card section-card expense-form" method="post" action="${base}/expense/save" enctype="multipart/form-data"
      data-receipt-form data-scan-url="${base}/receipt/scan" data-autoscan="${autoScan}">
    <ui:csrf/>
    <input type="hidden" name="id" value="${expenseId}">
    <div class="form-grid">
        <label>Merchant <input type="text" name="merchant" maxlength="200" value="<c:out value='${fields.merchant}'/>" required></label>
        <label>Amount <input type="number" name="amount" min="0.01" max="9999999999.99" step="0.01" value="<c:out value='${fields.amount}'/>" required></label>
        <label>Expense date <input type="date" name="date" value="<c:out value='${fields.date}'/>" required></label>
        <label>Category <select name="category" required><option value="">Choose a category</option><ui:options items="${categories}" selected="${fields.category}"/></select></label>
        <label>Payment method <select name="payment" required><option value="">Choose a method</option><ui:options items="${payments}" selected="${fields.payment}"/></select></label>
        <label class="span-two">Notes <textarea name="description" rows="3" maxlength="1500"><c:out value="${fields.description}"/></textarea></label>
    </div>
    <section class="receipt-box" aria-label="Receipt">
        <h2>Receipt</h2>
        <c:if test="${hasReceipt}">
            <p><a href="${base}/receipt?id=${expenseId}" target="_blank" rel="noopener">View attached receipt</a></p>
            <label class="check"><input type="checkbox" name="removeReceipt"> Remove the attached receipt</label>
        </c:if>
        <label>Upload a JPG or PNG<input name="receipt" type="file" accept="image/jpeg,image/png,.jpg,.jpeg,.png"></label>
        <div class="actions">
            <button type="button" class="button button-secondary" data-scan>Scan receipt</button>
            <button type="button" class="button button-secondary" data-apply hidden>Apply suggestions</button>
        </div>
        <p class="muted" data-scan-note role="status" aria-live="polite"></p>
    </section>
    <div class="actions"><button type="submit" class="button">Save expense</button><a class="button button-secondary" href="${base}/expenses">Cancel</a></div>
</form>
<c:if test="${not empty expenseId}">
    <form class="delete-form" method="post" action="${base}/expense/delete" data-confirm="Delete this expense and its receipt?">
        <ui:csrf/><input type="hidden" name="id" value="${expenseId}"><button type="submit" class="button button-danger">Delete expense</button>
    </form>
</c:if>
