<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<section class="card auth-card">
    <h1><c:out value="${title}"/></h1>
    <p><c:out value="${problemMessage}"/></p>
    <a class="button" href="${base}/">Back to Expense Tracker</a>
</section>
