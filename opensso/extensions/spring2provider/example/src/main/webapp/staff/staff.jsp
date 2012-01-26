<%-- 
    Document   : staff
    Created on : Feb 17, 2009, 11:18:24 AM
    Author     : warrenstrange
--%>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib prefix='security' uri='http://www.springframework.org/security/tags' %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<html>
<f:view>
<body>
    <div align="left"><h2>Welcome to the Staff Page</h2></div>
    <p>This shows an example of how to use the spring security:authorize jsp tags</p>
    <p>If you have the staff role (in a OpenSSO group called "staff") you
    should see a message below</p>
    <security:authorize ifAllGranted="ROLE_STAFF">
        <div align="left"><h2>Congrats!! You have the Staff role</h2></div>
    </security:authorize>
    <security:authorize ifNotGranted="ROLE_STAFF">
        <div align="left"><h2>TOO BAD SO SAD - You do NOT have the Staff role</h2></div>
    </security:authorize>
    
    <ul>
        <li><h:outputLink value="../main.jsp">Main Menu</h:outputLink></li>
       
    </ul>
</body>
</f:view>
</html>

