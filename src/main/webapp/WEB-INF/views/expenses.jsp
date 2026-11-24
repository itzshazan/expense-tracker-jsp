<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<header class="page-heading">
    <div><h1>Expenses</h1><p class="muted">Search, filter, and manage your transactions.</p></div>
    <a class="button" href="${base}/expense/new">Add expense</a>
</header>
<form class="card section-card filter-grid" method="get" action="${base}/expenses" aria-label="Filter expenses">
    <label class="wide">Search
        <input name="q" type="search" maxlength="150" value="<c:out value='${param.q}'/>" placeholder="Merchant or notes">
    </label>
    <label>Category
        <select name="category"><option value="">All categories</option><ui:options items="${categories}" selected="${param.category}"/></select>
    </label>
    <label>Payment
        <select name="payment"><option value="">All methods</option><ui:options items="${payments}" selected="${param.payment}"/></select>
    </label>
    <label>Receipt
        <select name="receipt"><option value="">Any status</option><ui:options items="${receiptOptions}" selected="${param.receipt}"/></select>
    </label>
    <label>From <input type="date" name="from" value="<c:out value='${param.from}'/>"></label>
    <label>To <input type="date" name="to" value="<c:out value='${param.to}'/>"></label>
    <label>Sort by
        <select name="sort">
            <option value="newest">Newest</option>
            <option value="oldest" ${param.sort eq 'oldest' ? 'selected' : ''}>Oldest</option>
            <option value="largest" ${param.sort eq 'largest' ? 'selected' : ''}>Largest amount</option>
            <option value="smallest" ${param.sort eq 'smallest' ? 'selected' : ''}>Smallest amount</option>
        </select>
    </label>
    <div class="actions filter-actions"><button type="submit" class="button">Apply filters</button><a class="button button-secondary" href="${base}/expenses">Clear</a></div>
</form>
<section class="card section-card">
    <header class="section-heading"><h2>Expense ledger</h2><span class="muted">${rows.size()} transactions</span></header>
    <ui:expense-table items="${rows}" actions="true"/>
</section>
