<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="items" required="true" type="java.util.List" %>
<%@ attribute name="selected" required="false" type="java.lang.String" %>
<c:forEach var="option" items="${items}">
    <option value="<c:out value='${option.value}'/>" ${option.value eq selected ? 'selected' : ''}><c:out value="${option.label}"/></option>
</c:forEach>
