<!DOCTYPE html>
<html>
<head>
    <title>OAuth2 Provider Login</title>
</head>
<body>
    <form action="authenticate" method="post">
        <label for="username">Username</label>
        <input id="username" name="username"/>
        <label for="password">Password</label>
        <input id="password" name="password" type="password"/>
        <input id="goto" name="goto" type="hidden" value="<%= request.getParameter("goto") %>">
        <button type="submit">Login</button>
    </form>
</body>
</html>