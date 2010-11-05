<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <meta content="text/html; charset=ISO-8859-1" http-equiv="content-type" />
        <title>Sample Application</title>
<!--
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

   $Id: showHttpHeaders.jsp,v 1.3 2008/08/15 01:05:42 veiming Exp $

   Copyright 2008 Sun Microsystems Inc. All Rights Reserved
-->        
        
        <style type="text/css">
            
            <!-- 
@import url("/agentsample/styles/default.css");

body,td,p,div,span,a,input,big,small{font-family:arial,helvetica,sans-serif}
body,td,p,div,span,a,input{font-size:10pt}
small,.small,small span,.small span,.small a,small a,div.sitelinks,div.sitelinks a,div.footlinks,div.footlinks a{font-size:9pt}
big,.big,big span,.big span,.big a,big a{font-size:11pt}
body,td,p,div,div.sitelinks a#homelink{color:#333}
input.buttonred{background:#acacac;cursor:hand;color:#FFF;height:1.4em;font-weight:bold;padding:0px;margin:0px;border:0px none #000}
input.medium{width:120px;height:18px}
a{text-decoration:none}
a:visited{color:#96C}
a:link,a.named:visited,div.breadcrumb a:visited,div.sitelinks a:visited,div.footlinks a:visited{color:#594FBF}
a:hover{text-decoration:underline}
.footlinks{padding:7px 0px}
.toolbar{padding:7px 0px 3px 0px}
.homenav{padding:7px 0px 0px 0px}
.homeftr{padding:0px}
.htitle div{padding:11px 0px 0px 0px}
.hitemtop div{padding:6px 0px 2px 0px}
.hitem div{padding:3px 0px 2px 0px}
.hitemverybottom div{padding:3px 0px 0px 0px}
.htitle div{font-weight:bold}
.spot div{padding:6px 0px 6px 0px}
.spottop div{padding:0px 0px 6px 0px}
-->
        </style>
    </head>
    <body>
        
        <table style="width: 800px; text-align: left;" border="0" cellpadding="2" cellspacing="2">
            <tbody>
                <tr>
                    <td style="vertical-align: top;"><a href="http://www.sun.com/" id="homelink">sun.com</a> </td>
                    <td style="vertical-align: top; text-align: right;">
                        <a href="http://www.sun.com/software/products/access_mgr/index.html" id="homelink">Sun Java System Access Manager<br>
                    </a></td>
                    <td style="vertical-align: top;">&nbsp;<br>
                    </td>
                </tr>
                <tr>
                    <td style="background-color: rgb(89, 79, 191); vertical-align: top; width: 150px; text-align: left;">
                        <img alt="Sun Microsystems, Inc." src="/agentsample/images/sun_logo.gif" style="width: 107px; height: 54px;"><br>
                        <br>
                    </td>
                    <td style="text-align: left; background-color: rgb(251, 226, 73); vertical-align: middle; font-family: andale sans;">
                        <span style="font-weight: bold;">J2EE Policy Agent Sample Application</span><br>
                    </td>
                    <td style="vertical-align: top; width: 50px;">&nbsp;<br>
                    </td>
                </tr>
                <tr>
                    <td style="vertical-align: top; width: 150px;">
                        <table style="text-align: left; width: 100%;" border="0" cellpadding="0" cellspacing="0">
                            <tbody>
                                <tr valign="top">
                                    <td class="htitle" style="vertical-align: top; text-align: left;">
                                        <hr class="menutop" size="1">
                                        <div>Sample Application</div>
                                    <hr class="light"> </td>
                                </tr>
                                <tr>
                                    <td style="vertical-align: top;"> <a href="/agentsample/public/welcome.html">Welcome </a>
                                        <hr class="faint"><a href="/agentsample/public/declarativesecurity.html">J2EE Declarative Security</a>
                                        <hr class="faint"><a href="/agentsample/public/programmaticsecurity.html">J2EE Security API</a>
                                        <hr class="faint"><a href="/agentsample/public/urlpolicy.html">URL Policy Enforcement</a>
                                        <hr class="faint">Show HTTP Headers
                                        <hr class="faint">
                                    </td>
                                </tr>
                                <tr>
                                    <td class="htitle" style="vertical-align: top; text-align: left;">
                                        <hr class="menutop" size="1">
                                        <div>Other Resources<br>
                                        </div>
                                    <hr class="light"> </td>
                                </tr>
                                <tr>
                                    <td style="vertical-align: top;">
                                        <a href="http://docs.sun.com/app/docs">J2EE Agents Guide</a><br>
                                        <hr class="faint">
                                        <a href="http://java.sun.com/j2ee">J2EE Documentation<br></a>
                                        <hr class="faint"> 
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <br>
                    </td>
                    <td style="vertical-align: top;">
                        <table style="text-align: left; width: 572px; height: 394px;" border="0" cellpadding="2" cellspacing="2">
                            <tbody>
                                <tr>
                                    <td style="vertical-align: top; width: 75px; height: 75px;">&nbsp;<br>
                                    </td>
                                    <td style="vertical-align: top; height: 75px;"> 
                                        <br><h2>Showing Request Headers</h2><br>
                                    </td>
                                    <td style="vertical-align: top; width: 75px; height: 75px;">&nbsp;<br>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="vertical-align: top; width: 75px;">&nbsp;<br>
                                    </td>
                                    <td style="vertical-align: top;">
                                        
                                        <%@ page import="java.util.*" %>
                                        <%@ page import="java.net.URLDecoder" %>
                                        
                                        <%
                                        
                                        /** Shows all the request headers sent on the current request.
                                         */
                                        String fetchMode= request.getParameter("fetch_mode");
                                        Cookie[] cookies = request.getCookies();
                                        String striPlanetDirectoryPro ="";
                                        for (int p=0 ; p<cookies.length ; p++){
                                            if ((cookies[p].getName()).equals("iPlanetDirectoryPro")){
                                                    striPlanetDirectoryPro = cookies[p].getValue();
                                                    break;
                                             }
                                        }
                                        
                                        out.println("<b>FetchMode: </b>" +
                                                fetchMode + "<br>\n" +
                                                "<b>Request Method : </b>" +
                                                request.getMethod() + "<br>\n" +
                                                "<b>Request URI: </b>" +
                                                request.getRequestURI() + "<br>\n" +
                                                "<b>Request Protocol : </b>" +
                                                request.getProtocol() + "<br>\n" +
                                                "<b>Request Scheme : </b>" +
                                                request.getScheme() + "<br>\n" +
                                                "<b>Request Server Name : </b>" +
                                                request.getServerName() + "<br>\n" +
                                                "<b>Request Server Port : </b>" +
                                                request.getServerPort() + "<br>\n" +
                                                 "<b>Request Remote User : </b>" +
                                                "REMOTE_USER:"+request.getRemoteUser() +
                                                "<br>\n" +
                                                "<b>Cookie iPlanetDirectoryPro : </b>" +
                                                "iPlanetDirectoryPro:" + 
                                                striPlanetDirectoryPro + "|<br>\n" +
                                                "<br><br>\n");
                                        if (fetchMode == null) {
                                            out.println("<font color=\"blue\">Header Attributes can be fetched as either<b>" +
                                                    " HTTP_HEADER or REQUEST_ATTRIBUTE or HTTP_COOKIE.</b>"
                                                    + "<br>\n"                                            
                                                    + " To view attributes specific to the fetch mode, append" + "<br>\n" +
                                                    "\t \t <li> ?fetch_mode=HTTP_HEADER or" + "</il>\n" +
                                                    "<li> ?fetch_mode=REQUEST_ATTRIBUTE or" + "</li>\n" +
                                                    "<li> ?fetch_mode=HTTP_COOKIE</font></li>");
                                            fetchMode="HTTP_HEADER";
                                        } 
                                        if (fetchMode.equals("HTTP_HEADER") || 
                                                fetchMode.equals("ALL")) {
                                           out.println("<table border=1 align=\"+ " +
                                                "left\" width=\"100%\">\n" +
                                                "<tr>\n" +
                                                "<th >Header Name<th >Header Value");
                                        Enumeration headerNames = request.getHeaderNames();
                                        ArrayList alAttrNames = new ArrayList();
                                        while(headerNames.hasMoreElements()) {
                                            String headerName = (String)headerNames.nextElement();
                                            out.println("<tr><td >" + headerName);
                                            out.print("    <td >" + headerName+":");
                                            Enumeration headers = request.getHeaders(headerName);
                                            
                                            while(headers.hasMoreElements()) {
                                                String header = (String)headers.nextElement();
                                                alAttrNames.add((String)header);
                                                }
                                            Collections.sort(alAttrNames); 
                                            for(int i=0;i<alAttrNames.size();i++)
                                            {
                                            out.print((String) alAttrNames.get(i)+"|");
                                            }
                                            alAttrNames = new ArrayList();
                                            out.print("$$");
                                            }
                                            out.print("</table>\n");
                                        }else if(fetchMode.equals("REQUEST_ATTRIBUTE") ||
                                                fetchMode.equals("ALL") ){
                                            
                                        %>
                                        
                                    </td>
                                    <td style="vertical-align: top; width: 75px; height: 75px;">&nbsp;<br>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="vertical-align: top; width: 75px;">&nbsp;<br>
                                    </td>
                                    <td style="vertical-align: top;">
                                        <%
                                        
                                        /** Shows all the request attributes sent on the current request.
                                         */
                                        
                                        out.println("<br><br>\n" +
                                                "<table border=1 align=\"left\" width=\"100%\">\n" +
                                                "<tr>\n" +
                                                "<th>Attribute Name<th>Attribute Value");
                                        Enumeration attrNames = request.getAttributeNames();
                                        ArrayList alAttrNames = new ArrayList();
                                        while(attrNames.hasMoreElements()) {
                                            String attrName = (String)attrNames.nextElement();
                                            out.println("<tr><td >" + attrName);
                                            out.print("    <td >" + attrName+":");
                                            Set attributeSet;
                                            if (request.getAttribute(attrName) instanceof Set){
                                                attributeSet=(Set)request.getAttribute(attrName);
                                                //out.print((request.getAttribute(attrName)).getClass().getName());
                                                Iterator it = attributeSet.iterator();
                                                while(it.hasNext()) {
                                                    String attr = (String)it.next();
                                                    alAttrNames.add((String)attr);
                                                }
                                                Collections.sort(alAttrNames); 
                                                for(int i=0;i<alAttrNames.size();i++){
                                                    out.print((String) alAttrNames.get(i)+"|");
                                                }
                                                out.print("$$");
                                            alAttrNames = new ArrayList();
                                            } else{
                                                out.print(request.getAttribute(attrName));
                                                out.print("$$");
                                            }
                                            }
                                            out.println("<tr><td > cookie");
                                            out.print("    <td > cookie:");
                                            out.print(request.getHeader("cookie"));
                                            out.print("$$");                                            
                                        out.print("</table>\n");
                                        } else if(fetchMode.equals("HTTP_COOKIE") ||
                                                fetchMode.equals("ALL")){
                                        %>
                                        
                                    </td>
                                    <td style="vertical-align: top; width: 75px; height: 75px;">&nbsp;<br>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="vertical-align: top; width: 75px;">&nbsp;<br>
                                    </td>
                                    <td style="vertical-align: top;">                        
                                    <%
                                    
                                    String cookieName;
                                    String cookieValue;
                                    String cookieValueDecoded;
                                    out.println("<br><br>\n" +
                                                "<table border=1:" +
                                                " align=\"left\" width=\"100%\">\n" +
                                                "<tr>\n" +
                                                "<th>Cookie Name<th>Cookie Value");
                                    for(int i=0; i<cookies.length; i++) {
                                        Cookie cookie = cookies[i];
                                            cookieName=cookie.getName();
                                            out.println("<tr><td >" + cookieName);
                                            out.print("<td >" + cookieName +":");
                                            cookieValue=cookie.getValue();
                                            int i1;
                                            ArrayList alAttrNames;
                                            if((cookieValue.indexOf("%") != -1)){
                                                cookieValueDecoded=URLDecoder.decode(cookieValue) +"|";
                                                alAttrNames = new ArrayList();
                                                if (cookieValueDecoded.contains("|")) {
                                                    while (cookieValueDecoded.contains("|")){
                                                        i1 = cookieValueDecoded.indexOf("|");
                                                        alAttrNames.add(cookieValueDecoded.substring(0, i1));
                                                        cookieValueDecoded = cookieValueDecoded.substring(i1+1,cookieValueDecoded.length());
                                                        }
                                                Collections.sort(alAttrNames);
                                                for (int j=0;j < alAttrNames.size(); j++) {
                                                    out.print(alAttrNames.get(j) +
                                                            "|" );
                                                }    
                                                }
                                                out.print("$$");
                                             } else {
                                                cookieValue = cookieValue + "|";
                                               // out.print(cookie.getValue() + "|");
                                                //out.println();
                                                alAttrNames = new ArrayList();
                                                if (cookieValue.contains("|")) {
                                                    while (cookieValue.contains("|")){
                                                        i1 = cookieValue.indexOf("|");
                                                        alAttrNames.add(cookieValue.substring(0, i1));
                                                        cookieValue = cookieValue.substring(i1+1,cookieValue.length());
                                                        }
                                                Collections.sort(alAttrNames);
                                                for (int j=0;j < alAttrNames.size(); j++) {
                                                    out.print(alAttrNames.get(j) +
                                                            "|" );
                                                }    
                                                }                                                
                                                out.print("$$");
                                             }
                                        }
                                        out.print("</table>\n");                                    
                                        }
                                    %>
                                    </td>
                                    <td style="vertical-align: top; width: 75px; height: 75px;">&nbsp;<br>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <br>
                    </td>
                    <td style="vertical-align: top; width: 50px;">&nbsp;<br>
                    </td>
                </tr>
                <tr>
                    <td style="vertical-align: top; width: 150px;">
                    <hr class="menutop"></td>
                    <td style="vertical-align: top;">
                        <hr class="menutop">
                        <div style="text-align: right;"></div>
                    </td>
                    <td style="vertical-align: top; width: 50px;">&nbsp;<br>
                    </td>
                </tr>
            </tbody>
        </table>
        <br>
        <br>
    </body>
</html>

