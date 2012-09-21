<%--
 The contents of this file are subject to the terms
 of the Common Development and Distribution License
 (the License). You may not use this file except in
 compliance with the License.
 
 You can obtain a copy of the License at
 https://opensso.dev.java.net/public/CDDLv1.0.html or
 opensso/legal/CDDLv1.0.txt
 See the License for the specific language governing
 permission and limitations under the License.

 When distributing Covered Code, include this CDDL
 Header Notice in each file and include the License file
 at opensso/legal/CDDLv1.0.txt.
 If applicable, add the following below the CDDL Header,
 with the fields enclosed by brackets [] replaced by
 your own identifying information:
 "Portions Copyrighted [year] [name of copyright owner]"
 
 $Id: aIDRequest.jsp,v 1.1 2008/08/26 22:33:23 sridharev Exp $
 
 Copyright 2007 Sun Microsystems Inc. All Rights Reserved
--%>

<%@ page import="java.io.*"%>
<%
try {
    String fpath = request.getParameter("path");
    File file = new File(fpath);
    String inputLine;
    response.addHeader("content-type", "text/html");
    PrintWriter rsp = response.getWriter();
    BufferedReader br = new BufferedReader(new InputStreamReader(
            new FileInputStream(file)));
    String subS = "ID=";
    String subL = "IssueInstant";
    String temp = null;
    while ((inputLine = br.readLine()) != null) {
        if (inputLine.contains("</samlp:Status><saml:Assertion xmlns:saml")) {
            int i = inputLine.indexOf(subS) + 4;
            int j = inputLine.indexOf(subL)- 2;
            String AssertionId = inputLine.substring(i,j);
            temp = AssertionId;
            rsp.flush();
        }
    }
    if(temp !=null){
        rsp.println("ID=" + temp + "$ID");
    } else {
        rsp.println("-1");
    }
    rsp.flush();
    br.close();
} catch (FileNotFoundException ex) {
    out.println(ex);
} catch (IOException ex) {
    out.println(ex);
} catch(Exception e) {
    out.println(e);
}
%>
