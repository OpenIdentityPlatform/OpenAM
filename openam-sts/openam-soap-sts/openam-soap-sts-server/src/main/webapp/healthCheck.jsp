<%--

   The contents of this file are subject to the terms of the Common Development and
   Distribution License (the License). You may not use this file except in compliance with the
   License.
   
   You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
   specific language governing permission and limitations under the License.
   
   When distributing Covered Software, include this CDDL Header Notice in each file and include
   the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
   Header, with the fields enclosed by brackets [] replaced by your own identifying
   information: "Portions Copyrighted [year] [name of copyright owner]".
   
   Copyright 2016 Agile Digital Engineering

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" session="false" %>
<%@ page import="
    org.forgerock.openam.sts.soap.config.SoapSTSInjectorHolder,
    org.forgerock.openam.sts.soap.healthcheck.HealthCheck,
    com.google.inject.Key"  
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>STS Health Check</title>
</head>
<body>
<%

HealthCheck healthCheck = SoapSTSInjectorHolder.getInstance(Key.get(HealthCheck.class));
int numSts = 0;
if (healthCheck != null) {
    numSts = healthCheck.getNumPublishedInstances();
}
String numInstancesRequiredStr = request.getParameter("num-instances-required");
Integer numInstancesRequired = 1;
if (numInstancesRequiredStr != null) {
    if (numInstancesRequiredStr.matches("\\d+")) {
        numInstancesRequired = Integer.parseInt(numInstancesRequiredStr);
    }
}
String result = "DEPLOYING";
if (numSts >= numInstancesRequired) {
    result = "READY";
}
%>
<h1>The STS is <%= result %></h1>
<p>
The number of published instances is: <%= numSts %>
<p>
</body>
</html>