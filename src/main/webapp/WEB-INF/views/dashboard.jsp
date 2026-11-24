<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="overall" value="${dash.overall}" />
<header class="dashboard-heading" data-motion="item">
    <div>
        <p class="dashboard-eyebrow"><c:out value="${dash.monthLabel}"/> · Day ${dash.dayOfMonth} of ${dash.daysInMonth}</p>
        <h1>Dashboard</h1>
        <p class="dashboard-subtitle">How this month is going, compared with the same point last month.</p>
    </div>
    <a class="button" href="${base}/expense/new"><span aria-hidden="true">+</span> Add expense</a>
</header>

<%-- 1. Am I on track? --%>
<section class="dashboard-metrics" aria-label="This month at a glance" data-motion="group">
    <div class="dashboard-stat dashboard-stat-primary">
        <div class="dashboard-stat-label"><span class="dashboard-stat-icon"><ui:public-icon name="chart"/></span> Spent this month</div>
        <strong><c:out value="${dash.spent}"/></strong>
        <span class="stat-change stat-change-${dash.change.direction}"><c:out value="${dash.change.label}"/></span>
    </div>

    <c:choose>
        <c:when test="${not empty overall}">
            <div class="dashboard-stat stat-${overall.level}">
                <div class="dashboard-stat-label"><span class="dashboard-stat-icon"><ui:public-icon name="pie"/></span>
                    ${overall.over ? 'Over budget' : 'Budget left'}</div>
                <strong><c:out value="${overall.left}"/></strong>
                <span>
                    <c:choose>
                        <c:when test="${overall.over}">Above your <c:out value="${overall.limit}"/> limit</c:when>
                        <c:when test="${not empty overall.perDay}">${overall.daysLeft} ${overall.daysLeft eq 1 ? 'day' : 'days'} left · <c:out value="${overall.perDay}"/> a day</c:when>
                        <c:otherwise>Budget fully used</c:otherwise>
                    </c:choose>
                </span>
            </div>
        </c:when>
        <c:otherwise>
            <div class="dashboard-stat dashboard-stat-empty">
                <div class="dashboard-stat-label"><span class="dashboard-stat-icon"><ui:public-icon name="pie"/></span> Budget left</div>
                <strong>—</strong>
                <a href="${base}/settings#budgets">Set a monthly budget <span aria-hidden="true">→</span></a>
            </div>
        </c:otherwise>
    </c:choose>

    <div class="dashboard-stat">
        <div class="dashboard-stat-label"><span class="dashboard-stat-icon"><ui:public-icon name="bolt"/></span> Daily average</div>
        <strong><c:out value="${dash.dailyAverage}"/></strong>
        <span>
            On pace for <c:out value="${dash.projected}"/>
            <c:if test="${not empty overall}">
                · <em class="${overall.projectedOver ? 'is-over' : 'is-under'}">${overall.projectedOver ? 'over' : 'under'} budget by <c:out value="${overall.projectedGap}"/></em>
            </c:if>
        </span>
    </div>

    <div class="dashboard-stat">
        <div class="dashboard-stat-label"><span class="dashboard-stat-icon"><ui:public-icon name="receipt"/></span> Largest this month</div>
        <c:choose>
            <c:when test="${not empty dash.largest}">
                <strong><c:out value="${dash.largest.amount}"/></strong>
                <span><c:out value="${dash.largest.merchant}"/> · <c:out value="${dash.largest.date}"/></span>
            </c:when>
            <c:otherwise><strong>—</strong><span>No expenses yet this month</span></c:otherwise>
        </c:choose>
    </div>
</section>

<%-- 2. Budgets, the most actionable section, straight after the summary --%>
<c:if test="${not empty dash.budgets}">
    <section class="dashboard-panel" aria-labelledby="budgets-title" data-motion="item">
        <header class="dashboard-panel-heading">
            <div><h2 id="budgets-title">Monthly budgets</h2><p>Limits that need attention are listed first</p></div>
            <a href="${base}/settings#budgets">Manage budgets <span aria-hidden="true">→</span></a>
        </header>
        <div class="budget-grid">
            <c:forEach var="budget" items="${dash.budgets}">
                <div class="budget ${budget.level}">
                    <div><strong><c:out value="${budget.label}"/></strong><span><c:out value="${budget.message}"/></span></div>
                    <progress value="${budget.progress}" max="100" aria-label="<c:out value='${budget.label}'/> budget used"></progress>
                    <div class="muted">
                        <span><c:out value="${budget.spent}"/> of <c:out value="${budget.limit}"/> · ${budget.percent}%</span>
                        <span class="budget-remaining"><c:out value="${budget.remaining}"/></span>
                    </div>
                </div>
            </c:forEach>
        </div>
    </section>
</c:if>

<%-- 3. Where the money is going --%>
<div class="dashboard-charts" data-motion="group">
    <section class="dashboard-panel" aria-labelledby="trend-title">
        <header class="dashboard-panel-heading">
            <div><h2 id="trend-title">Spending by week</h2><p>This month next to the same days last month</p></div>
        </header>
        <c:choose>
            <c:when test="${not dash.hasMonthSpending and not dash.hasLastMonth}"><p class="dashboard-empty">Your weekly spending will appear here when you log an expense.</p></c:when>
            <c:otherwise>
                <ul class="trend-chart">
                    <c:forEach var="week" items="${dash.trend}">
                        <li class="${week.upcoming ? 'is-upcoming' : ''}">
                            <strong aria-hidden="true">${week.upcoming ? 'Upcoming' : week.amount}</strong>
                            <div class="trend-track" aria-hidden="true">
                                <progress class="trend-previous" value="${week.previousValue}" max="${week.max}"></progress>
                                <c:if test="${not week.upcoming}"><progress class="trend-current" value="${week.value}" max="${week.max}"></progress></c:if>
                            </div>
                            <span aria-hidden="true">${week.days}</span>
                            <span class="sr-only">Days ${week.days}: ${week.upcoming ? 'not started yet' : week.amount} this month, ${week.previousAmount} last month.</span>
                        </li>
                    </c:forEach>
                </ul>
                <p class="trend-legend" aria-hidden="true"><span class="legend-current"></span> This month <span class="legend-previous"></span> Last month</p>
            </c:otherwise>
        </c:choose>
    </section>
    <section class="dashboard-panel" aria-labelledby="categories-title">
        <header class="dashboard-panel-heading">
            <div><h2 id="categories-title">Top categories</h2><p>Share of this month, and the change from the same days last month</p></div>
        </header>
        <c:choose>
            <c:when test="${empty dash.categories}"><p class="dashboard-empty">No categories with spending this month.</p></c:when>
            <c:otherwise>
                <ul class="dashboard-categories">
                    <c:forEach var="category" items="${dash.categories}">
                        <li>
                            <div class="category-details">
                                <span><c:out value="${category.label}"/></span>
                                <strong><c:out value="${category.total}"/></strong>
                            </div>
                            <progress value="${category.share}" max="100" aria-label="<c:out value='${category.label}'/> share of monthly spending">${category.share}%</progress>
                            <div class="category-meta">
                                <span>${category.share}% of spending</span>
                                <span class="category-change category-change-${category.direction}"><c:out value="${category.change}"/></span>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<%-- 4. What was logged --%>
<section class="dashboard-panel dashboard-recent" aria-labelledby="recent-title" data-motion="item">
    <header class="dashboard-panel-heading">
        <h2 id="recent-title">Recent expenses</h2>
        <a href="${base}/expenses">View all expenses <span aria-hidden="true">→</span></a>
    </header>
    <c:if test="${dash.missingReceipts gt 0}">
        <a class="receipt-nudge" href="${base}/expenses?receipt=NO_RECEIPT&amp;from=${dash.monthStart}&amp;to=${dash.monthEnd}">
            <span class="dashboard-stat-icon"><ui:public-icon name="receipt"/></span>
            <span><strong>${dash.missingReceipts} ${dash.missingReceipts eq 1 ? 'expense' : 'expenses'} this month ${dash.missingReceipts eq 1 ? 'has' : 'have'} no receipt.</strong>
                Attach them while you still have the paper copy.</span>
            <span class="receipt-nudge-action">Review <span aria-hidden="true">→</span></span>
        </a>
    </c:if>
    <ui:expense-table items="${rows}" actions="false"/>
</section>
