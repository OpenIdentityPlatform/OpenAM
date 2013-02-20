<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.iplanet.sso.*" %>
<%@ page import="com.sun.identity.authentication.*" %>
<%@ page import="com.sun.identity.authentication.UI.*" %>
<%@ page import="com.sun.identity.policy.*" %>
<%@ page import="com.sun.identity.security.AdminTokenAction" %>
<%@ page import="java.security.AccessController" %>
<%@ page import="javax.security.auth.callback.*" %>
<%@ page import="com.sun.identity.setup.*" %>

<center>
<%
    String userid = request.getParameter("userid");
    String pwd = request.getParameter("pwd");
    try {
        AuthContext ac = new AuthContext("/");
        ac.login();

        if (ac.hasMoreRequirements()) {
            Callback[] callbacks = ac.getRequirements();
            if (callbacks != null) {
                for (int i = 0; i < callbacks.length; i++) {
                    if (callbacks[i] instanceof NameCallback) {
                        NameCallback nc = (NameCallback) callbacks[i];
                        nc.setName(userid);
                    } else if (callbacks[i] instanceof PasswordCallback) {
                        PasswordCallback pc = (PasswordCallback) callbacks[i];
                        pc.setPassword(pwd.toCharArray());
                    }
                }
                ac.submitRequirements(callbacks);
            }
        }
        if (ac.getStatus() != AuthContext.Status.SUCCESS) {
            throw (new Exception("Authentication Failed"));
        }
        request.getRequestDispatcher("/hello.jsp").forward(request, response);
    } catch (Exception e) {
        out.println("Authentication Fails<br/>");
        out.println("Please try again, <a href=\"login.jsp\">Login page</a>");
    }

%>
</center>
