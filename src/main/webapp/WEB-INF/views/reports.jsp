<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<header class="page-heading">
    <div><h1>Reports</h1><p class="muted">See how spending breaks down across categories and payment methods.</p></div>
    <div class="actions">
        <a class="button button-secondary" href="${base}/reports/export?format=csv&amp;from=${from}&amp;to=${to}">Export CSV</a>
        <a class="button button-secondary" href="${base}/reports/export?format=pdf&amp;from=${from}&amp;to=${to}">Export PDF</a>
    </div>
</header>
<section class="card section-card date-controls">
    <form method="get" action="${base}/reports" class="actions">
        <label>From <input type="date" name="from" value="${from}"></label>
        <label>To <input type="date" name="to" value="${to}"></label>
        <button type="submit" class="button">Update range</button>
    </form>
    <nav aria-label="Report date presets" class="actions presets">
        <a href="${base}/reports?range=month">This month</a><a href="${base}/reports?range=30">Last 30 days</a>
        <a href="${base}/reports?range=year">This year</a><a href="${base}/reports?range=all">All time</a>
    </nav>
</section>
<div class="metrics" aria-label="Report summary">
    <article class="card stat hero-stat"><span>Total spending</span><strong><c:out value="${total}"/></strong></article>
    <article class="card stat"><span>Transactions</span><strong>${count}</strong></article>
    <article class="card stat"><span>Average expense</span><strong><c:out value="${average}"/></strong></article>
    <article class="card stat"><span>Largest expense</span><strong><c:out value="${highest}"/></strong></article>
</div>
<div class="two-columns">
    <ui:breakdown heading="Categories" items="${categoryBreakdown}"/>
    <ui:breakdown heading="Payment methods" items="${paymentBreakdown}"/>
</div>
<section class="card section-card">
    <header class="section-heading"><h2>Transactions in this report</h2><button class="text-button" type="button" data-print>Print report</button></header>
    <ui:expense-table items="${rows}" actions="false"/>
</section>
