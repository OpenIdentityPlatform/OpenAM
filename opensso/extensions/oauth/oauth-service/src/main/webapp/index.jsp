<%--
    Document   : index
    Created on : Apr 27, 2009, 9:15:49 AM
    Author     : Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Token Service</title>
    </head>
    <body>
        <h1>A minimalistic tool to load Service Consumers metadata</h1>
        <h2>
            Register a service consumer<br>
        </h2>
        <form name="consumer_reg" action="registerconsumer.jsp" method="POST">
            Service Consumer Name: <input type="text" name="name" value="" size="30" /><br><br>
            Service Consumer URI: <input type="text" name="svcuri" value="http://" size="100" /><br><br>
            Service Consumer Icon URI: <input type="text" name="iconuri" value="http://" size="100" /><br><br>
            Service Consumer RSA public key (optional): <textarea name="rsakey" rows="10" cols="80"></textarea><br><br><br>
            Register this Service Consumer <input type="submit" value="Register" name="cons_reg" /><br>
        </form>
        <hr><br>
        <h2>
            Delete a service consumer<br>
        </h2>
        <form name="consumer_del" action="deleteconsumer.jsp" method="POST">
            Service Consumer Key: <input type="text" name="conskey" value="" size="100" /><br><br>

            Register this Service Consumer <input type="submit" value="Delete" name="cons_del" /><br>
        </form>
        <hr><br>
    </body>
</html>
