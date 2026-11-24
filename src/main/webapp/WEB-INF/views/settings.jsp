<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<header class="page-heading">
    <div><h1>Settings</h1><p class="muted">Manage display preferences, monthly limits, and your account.</p></div>
</header>
<section class="card section-card">
    <h2>Preferences</h2>
    <form method="post" action="${base}/settings/save" class="stack">
        <ui:csrf/>
        <div class="form-grid">
            <label>Display currency <select name="currency"><ui:options items="${currencies}" selected="${preferences.currency}"/></select></label>
            <label>Date format <select name="dateStyle"><ui:options items="${dateStyles}" selected="${preferences.date}"/></select></label>
            <label>Default payment method <select name="payment"><option value="">No default</option><ui:options items="${payments}" selected="${preferences.payment}"/></select></label>
            <label>Default category <select name="category"><option value="">No default</option><ui:options items="${categoryNames}" selected="${preferences.category}"/></select></label>
            <label>OCR language <select name="ocr"><ui:options items="${languages}" selected="${preferences.ocr}"/></select></label>
            <label>Budget alert threshold (%) <input type="number" name="threshold" min="50" max="100" value="${preferences.threshold}" required></label>
        </div>
        <label class="check"><input type="checkbox" name="autoScan" ${preferences.autoScan ? 'checked' : ''}> Scan receipts when attached</label>
        <div><button type="submit" class="button">Save preferences</button></div>
    </form>
    <form method="post" action="${base}/settings/reset" class="reset-button">
        <ui:csrf/><button type="submit" class="text-button">Restore defaults</button>
    </form>
</section>
<section id="budgets" class="card section-card">
    <h2>Monthly budgets</h2>
    <p class="muted">Save an overall limit or a separate limit for a category. Saving the same category updates its limit.</p>
    <form method="post" action="${base}/budget/save" class="budget-form">
        <ui:csrf/>
        <label>Budget category
            <select name="category"><option value="">Overall</option><ui:options items="${categories}"/></select>
        </label>
        <label>Monthly limit <input type="number" name="limit" min="0.01" max="9999999999.99" step="0.01" required></label>
        <button type="submit" class="button">Save budget</button>
    </form>
    <c:if test="${not empty budgets}">
        <ul class="budget-list">
            <c:forEach var="budget" items="${budgets}">
                <li><span><strong><c:out value="${budget.label}"/></strong> · <c:out value="${budget.amount}"/> / month</span>
                    <form method="post" action="${base}/budget/delete" data-confirm="Remove this monthly limit?">
                        <ui:csrf/><input type="hidden" name="id" value="${budget.id}"><button class="text-button danger" type="submit">Remove</button>
                    </form></li>
            </c:forEach>
        </ul>
    </c:if>
</section>
<section class="card section-card">
    <h2>Export all expenses</h2>
    <div class="actions"><a class="button button-secondary" href="${base}/reports/export?format=csv&amp;range=all">Download CSV</a>
        <a class="button button-secondary" href="${base}/reports/export?format=pdf&amp;range=all">Download PDF</a></div>
</section>
<section class="card section-card">
    <h2>Account</h2>
    <p class="muted"><c:out value="${email}"/></p>
    <form method="post" action="${base}/account/name" class="stack account-form">
        <ui:csrf/>
        <label>Display name <input name="displayName" maxlength="100" value="<c:out value='${displayName}'/>" required></label>
        <div><button type="submit" class="button button-secondary">Update display name</button></div>
    </form>
    <form method="post" action="${base}/account/password" class="stack account-form">
        <ui:csrf/>
        <div class="form-grid">
            <label>New password <input name="password" type="password" autocomplete="new-password" minlength="6" maxlength="1024" required></label>
            <label>Confirm password <input name="confirmPassword" type="password" autocomplete="new-password" minlength="6" maxlength="1024" required></label>
        </div>
        <div><button type="submit" class="button button-secondary">Change password</button></div>
    </form>
    <details class="account-form">
        <summary class="danger">Delete account</summary>
        <p>This permanently removes your login, expenses, budgets, and attached receipts.</p>
        <form method="post" action="${base}/account/delete" class="stack" data-confirm="Permanently delete your account and expense data?">
            <ui:csrf/>
            <label>Type DELETE to confirm <input name="confirmation" required pattern="DELETE" maxlength="6" autocomplete="off"></label>
            <div><button type="submit" class="button button-danger">Delete account permanently</button></div>
        </form>
    </details>
</section>
