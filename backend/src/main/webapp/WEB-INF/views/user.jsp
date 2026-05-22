<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>USER 페이지</title></head>
<body>
    <h1>USER 전용 페이지</h1>
    <p>USER 권한이 있어야 접근할 수 있는 페이지입니다.</p>
    <p>접속 계정: <%= request.getRemoteUser() %></p>

    <p><a href="/main">메인으로 돌아가기</a></p>
</body>
</html>
