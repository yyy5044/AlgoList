<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>메인</title></head>
<body>
    <h1>메인 화면</h1>
    <p>로그인한 사용자라면 누구나 볼 수 있는 페이지입니다.</p>
    <p>접속 계정: <%= request.getRemoteUser() %></p>

    <ul>
        <li><a href="/user">USER 전용 페이지</a></li>
        <li><a href="/admin">ADMIN 전용 페이지</a></li>
    </ul>

    <form action="/logout" method="post">
        <button type="submit">로그아웃</button>
    </form>
</body>
</html>
