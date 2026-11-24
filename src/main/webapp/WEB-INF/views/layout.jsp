<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="base" value="${pageContext.request.contextPath}/app" scope="request" />
<c:set var="assets" value="${pageContext.request.contextPath}/assets" scope="request" />
<%-- Bump when CSS or JS changes so browsers fetch the new files instead of cached ones. --%>
<c:set var="assetVersion" value="20261125n" />
<c:set var="publicScreen" value="${screen eq 'landing' or screen eq 'auth'}" />
<c:set var="workspaceScreen" value="${signedIn and not publicScreen}" />
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${screen eq 'landing' ? 'Expense Tracker' : title.concat(' | Expense Tracker')}"/></title>
    <link rel="icon" href="${assets}/app-logo.png">
    <link rel="stylesheet" href="${assets}/app.css?v=${assetVersion}">
    <c:if test="${publicScreen}">
        <link rel="stylesheet" href="${assets}/public.css?v=${assetVersion}">
    </c:if>
    <c:if test="${screen eq 'landing'}">
        <meta name="description" content="Track your expenses, scan receipts, and manage monthly budgets in a private workspace.">
        <link rel="stylesheet" href="${assets}/hero-preview.css?v=${assetVersion}">
        <link rel="stylesheet" href="${assets}/landing.css?v=${assetVersion}">
    </c:if>
    <c:if test="${screen eq 'auth'}"><link rel="stylesheet" href="${assets}/auth.css?v=${assetVersion}"></c:if>
    <c:if test="${screen eq 'dashboard'}"><link rel="stylesheet" href="${assets}/dashboard.css?v=${assetVersion}"></c:if>
    <%-- Runs before first paint so animated elements start hidden instead of flashing. --%>
    <script src="${assets}/motion-init.js?v=${assetVersion}"></script>
    <script src="${assets}/vendor/gsap.min.js?v=${assetVersion}" defer></script>
    <c:if test="${screen eq 'landing'}"><script src="${assets}/vendor/ScrollTrigger.min.js?v=${assetVersion}" defer></script></c:if>
    <script src="${assets}/app.js?v=${assetVersion}" defer></script>
    <c:if test="${screen eq 'landing'}">
        <script src="${assets}/landing.js?v=${assetVersion}" defer></script>
        <script src="${assets}/hero-preview.js?v=${assetVersion}" defer></script>
    </c:if>
    <script src="${assets}/motion.js?v=${assetVersion}" defer></script>
</head>
<body class="${workspaceScreen ? 'workspace' : 'public'}${screen eq 'dashboard' ? ' dashboard-view' : ''}${screen eq 'auth' ? ' auth-view' : ''}" data-screen="${screen}">
<a class="skip-link" href="#main">Skip to content</a>
<c:choose>
    <c:when test="${workspaceScreen}">
        <aside class="sidebar">
            <a class="wordmark" href="${base}/" title="Expense Tracker home">
                <img src="${assets}/app-logo.png" alt="" width="34" height="34"> Expense Tracker
            </a>
            <p class="sidebar-label">Workspace</p>
            <nav aria-label="Workspace">
                <a href="${base}/dashboard" class="${screen eq 'dashboard' ? 'active' : ''}" aria-current="${screen eq 'dashboard' ? 'page' : 'false'}"><ui:public-icon name="chart"/> Dashboard</a>
                <a href="${base}/expenses" class="${screen eq 'expenses' or screen eq 'expense-form' ? 'active' : ''}" aria-current="${screen eq 'expenses' ? 'page' : 'false'}"><ui:public-icon name="receipt"/> Expenses</a>
                <a href="${base}/reports" class="${screen eq 'reports' ? 'active' : ''}" aria-current="${screen eq 'reports' ? 'page' : 'false'}"><ui:public-icon name="pie"/> Reports</a>
                <a href="${base}/settings" class="${screen eq 'settings' ? 'active' : ''}" aria-current="${screen eq 'settings' ? 'page' : 'false'}"><ui:public-icon name="shield"/> Settings</a>
            </nav>
            <a class="sidebar-cta" href="${base}/expense/new"><span aria-hidden="true">+</span> New expense</a>
            <div class="sidebar-account">
                <span class="account-avatar" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(email, 0, 1))}"/></span>
                <div class="account-details">
                    <span class="account-caption">Signed in as</span>
                    <span class="account-email"><c:out value="${email}"/></span>
                </div>
                <form method="post" action="${base}/logout">
                    <ui:csrf/>
                    <button type="submit" class="sidebar-signout">Sign out</button>
                </form>
            </div>
        </aside>
    </c:when>
    <c:when test="${screen eq 'landing'}">
        <header class="home-header" data-motion="header">
            <div class="home-width home-header-content">
                <a class="home-brand" href="${base}/"><img src="${assets}/app-logo.png" alt="" width="42" height="42"><span class="home-brand-name">Expense Tracker</span></a>
                <nav class="home-navigation" id="home-menu" aria-label="About Expense Tracker">
                    <a href="#features">Features</a><a href="#how-it-works">How it works</a>
                    <a href="#security">Privacy</a><a href="#faq">FAQ</a>
                    <c:if test="${not signedIn}"><a class="home-menu-login" href="${base}/signin">Log in</a></c:if>
                </nav>
                <%-- Phones: the section links fold into this menu. landing.js turns it on, so without
                     scripts the links stay visible on a second row. --%>
                <button class="home-menu-toggle" type="button" aria-controls="home-menu" aria-expanded="false">
                    <span class="sr-only">Open menu</span><span class="home-menu-icon" aria-hidden="true"></span>
                </button>
                <nav class="home-account" aria-label="Account">
                    <c:choose>
                        <c:when test="${signedIn}"><a class="home-button home-dashboard-link" href="${base}/dashboard"><span class="home-label-long">Open dashboard</span><span class="home-label-short">Dashboard</span></a></c:when>
                        <c:otherwise>
                            <a class="home-login" href="${base}/signin">Log in</a>
                            <a class="home-button" href="${base}/register">Sign up</a>
                        </c:otherwise>
                    </c:choose>
                </nav>
            </div>
        </header>
    </c:when>
    <c:when test="${screen eq 'auth'}"></c:when>
    <c:otherwise>
        <header class="public-header">
            <a class="wordmark" href="${base}/">
                <img src="${assets}/app-logo.png" alt="" width="32" height="32"> Expense Tracker
            </a>
            <nav aria-label="Account">
                <a class="button button-ghost" href="${base}/signin">Sign in</a>
                <c:if test="${cloud}"><a class="button" href="${base}/register">Create account</a></c:if>
            </nav>
        </header>
    </c:otherwise>
</c:choose>
<main id="main" class="${screen eq 'landing' ? 'landing-main' : screen eq 'auth' ? 'auth-main' : signedIn ? 'workspace-main' : 'public-main'}">
    <c:if test="${screen ne 'auth' and not empty notice}">
        <div class="notice ${notice.type}" role="${notice.type eq 'error' ? 'alert' : 'status'}"><c:out value="${notice.text}"/></div>
    </c:if>
    <jsp:include page="/WEB-INF/views/${screen}.jsp" />
</main>
<c:if test="${screen eq 'landing'}">
    <footer class="home-footer">
        <div class="home-width home-footer-main" data-reveal>
            <div class="home-footer-about">
                <a class="home-brand" href="${base}/"><img src="${assets}/app-logo.png" alt="" width="42" height="42">Expense Tracker</a>
                <p>Scan receipts, set monthly budgets, and see where your money goes in one private workspace.</p>
                <a class="home-button" href="${base}/${signedIn ? 'dashboard' : 'register'}">${signedIn ? 'Open dashboard' : 'Create an account'} <ui:public-icon name="arrow-right"/></a>
            </div>
            <nav class="home-footer-column" aria-label="Product">
                <h2>Product</h2>
                <a href="#how-it-works">How it works</a>
                <a href="#features">Features</a>
                <a href="#security">Privacy</a>
            </nav>
            <nav class="home-footer-column" aria-label="Account">
                <h2>Account</h2>
                <c:choose>
                    <c:when test="${signedIn}">
                        <a href="${base}/dashboard">Dashboard</a>
                        <a href="${base}/expenses">Expenses</a>
                        <a href="${base}/settings">Settings</a>
                    </c:when>
                    <c:otherwise>
                        <a href="${base}/signin">Log in</a>
                        <a href="${base}/register">Sign up</a>
                        <a href="${base}/forgot">Reset your password</a>
                    </c:otherwise>
                </c:choose>
            </nav>
            <nav class="home-footer-column" aria-label="Help">
                <h2>Help</h2>
                <a href="#faq">Frequently asked questions</a>
                <a href="#faq">Exporting your expenses</a>
                <a href="#faq">Scanning receipts</a>
            </nav>
        </div>
        <div class="home-width home-footer-bottom">
            <span>© 2026 Expense Tracker</span>
            <span><ui:public-icon name="shield"/> Your expenses and receipts stay in your own account.</span>
        </div>
        <p class="home-footer-wordmark" aria-hidden="true">Expense Tracker</p>
    </footer>
</c:if>
</body>
</html>
