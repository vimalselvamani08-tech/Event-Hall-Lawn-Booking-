<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Admin sign in | Event Hall</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css">
</head>
<body class="login-page">
<main class="login-shell">
    <div class="login-brand">
        <span class="brand-mark">EH</span>
        <span>EVENT HALL<br><strong>MANAGEMENT</strong></span>
    </div>
    <section class="login-panel">
        <p class="eyebrow">ADMINISTRATION</p>
        <h1>Admin sign in</h1>
        <p class="muted">Manage venues, customer bookings, and payments.</p>
        <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <c:if test="${not empty error}"><p class="alert error"><c:out value="${error}"/></p></c:if>
        <form method="post" action="${pageContext.request.contextPath}/login" class="form-stack">
            <label>Username<input name="username" autocomplete="username" required autofocus></label>
            <label>Password<input type="password" name="password" autocomplete="current-password" required></label>
            <button class="button primary full" type="submit">Sign in</button>
        </form>
    </section>
    <p class="login-switch">Booking a venue? <a href="${pageContext.request.contextPath}/home">Browse halls</a></p>
    <p class="login-foot">Admin accounts are created by a signed-in administrator.</p>
</main>
</body>
</html>