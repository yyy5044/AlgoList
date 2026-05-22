<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>회원가입</title></head>
<body>
    <h1>회원가입</h1>

    <form action="/register" method="post">
        <p>아이디: <input type="text" name="username"></p>
        <p>비밀번호: <input type="password" name="password"></p>
        <button type="submit">가입하기</button>
    </form>

    <p><a href="/login">로그인으로 돌아가기</a></p>
</body>
</html>
