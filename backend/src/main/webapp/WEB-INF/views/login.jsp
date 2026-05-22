<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>로그인</title></head>
<body>
    <h1>로그인</h1>

    <% if (request.getParameter("error") != null) { %>
        <p style="color:red;">아이디 또는 비밀번호가 올바르지 않습니다.</p>
    <% } %>
    <% if (request.getParameter("logout") != null) { %>
        <p style="color:green;">로그아웃 되었습니다.</p>
    <% } %>

    <form action="/login" method="post">
        <p>아이디: <input type="text" name="username"></p>
        <p>비밀번호: <input type="password" name="password"></p>
        <button type="submit">로그인</button>
    </form>

    <p><a href="/register">회원가입</a></p>
</body>
</html>
