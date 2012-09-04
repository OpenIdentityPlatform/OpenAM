<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: id-sis-pp-modify.jsp,v 1.3 2009/02/05 00:46:39 mrudulahg Exp $

--%>

<%@page import="
java.io.*,
java.util.*,
javax.xml.bind.*,
org.w3c.dom.Document,
org.w3c.dom.Element,
com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken,
com.sun.identity.liberty.ws.dst.*,
com.sun.identity.liberty.ws.disco.*,
com.sun.identity.liberty.ws.disco.jaxb.*,
com.sun.identity.liberty.ws.idpp.jaxb.FNElement,
com.sun.identity.liberty.ws.interaction.*,
com.sun.identity.liberty.ws.security.*,
com.sun.identity.liberty.ws.soapbinding.*,
com.sun.identity.saml.common.*,
com.sun.identity.plugin.session.SessionManager,
com.sun.identity.plugin.session.SessionProvider,
com.sun.identity.shared.xml.XMLUtils"
%>
<html xmlns="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<head><title>IDPP Modify</title></head>
<body bgcolor="white">
    <h1>IDPP Modify</h1>
<%
    if (request.getMethod().equals("GET")) {
        String resend = request.getParameter(
                        InteractionManager.RESEND_MESSAGE);
        if (resend != null) {
            // resend message after interaction
            try {
                // resend message
                Message ret = InteractionManager.getInstance().resendRequest(
                    request.getRequestURL().toString(), request, response);
                %>
                    <pre><%= SAMLUtils.displayXML(ret.toString()) %></pre>
                    <p><a href="index.jsp">Return to index.jsp</a></p>
                <%
            } catch (Throwable t) {
                t.printStackTrace();
                StringWriter buf = new StringWriter();
                t.printStackTrace(new PrintWriter(buf));
                %>
                    ERROR: caught exception:
                    <pre><%= SAMLUtils.displayXML(buf.toString()) %></pre>
                <%
            }
        } else {
            String soapEndPoint = "";
            List mechs = null;
            String providerID = request.getParameter("providerID");
            String fnSuffix = providerID.replace('/', '_');
            String securityAssertion = 
                      request.getParameter("secAssertion");
            String resOffFN = request.getParameter("resOffFN");
            try {
                ResourceOffering ro = null;
                BufferedInputStream bir = 
                    new BufferedInputStream(new FileInputStream(resOffFN));
                Document doc = XMLUtils.toDOMDocument(bir, null);
                Element elem = doc.getDocumentElement();
		ro = new ResourceOffering(elem);
                ServiceInstance si = ro.getServiceInstance();
                Description desc = (Description)si.getDescription().get(0);
                mechs = desc.getSecurityMechID();
  
            } catch(Exception ex) {
               %>ERROR: Unable to parse resource offering<%
                 ex.printStackTrace();
            }
            %>
            <form name="ppmodify" method="POST">
                <table>
                    <tr>
                        <td>Authentication Mechanism</td>
                        <td>
                <%
                int len = mechs.size(); 
                for (int i = 0; i < len; i++) {
                    %>
                    <input type='radio' name="authMech"
                        value='<%= mechs.get(i) %>'/> <%= mechs.get(i) %><br>
                    <% 
                } 
                %>
                        </td>
                    </tr>
                    <tr>
                        <td>XPath Expression</td>
                        <td><input type="text" name="queryStr" 
                            value="/PP/CommonName/AnalyzedName/FN" /></td>
                    </tr>
                    <tr>
                        <td>Value</td>
                        <td><input type="text" name="valueStr" /></td>
                    </tr>
                </table>
                <input type="hidden" name="providerID" 
                    value="<%= providerID %>">
                <input type="hidden" name="resOffFN" 
                    value="<%= resOffFN %>">
                <input type="hidden" name="secAssertion" 
                    value="<%= securityAssertion %>">
                <input type="submit" value="Send PP Modify Request" />
            </form>
            <%
            }
    } else {
        String queryString = request.getParameter("queryStr");
        String attrToModify = queryString.substring(queryString.lastIndexOf("/") + 1, queryString.length());
        String valueString = request.getParameter("valueStr");
        String providerID = request.getParameter("providerID");
        String resOffFN = request.getParameter("resOffFN");
        String secAss = request.getParameter("secAssertion");
        String authMech = request.getParameter("authMech");

        if (resOffFN == null || resOffFN.equals("")) {
            %>ERROR: resource offering missing<%
        } else {
            try {
                BufferedInputStream bir = 
                    new BufferedInputStream(new FileInputStream(resOffFN));
                Document doc = XMLUtils.toDOMDocument(bir, null);
                Element elem = doc.getDocumentElement();
                ResourceOffering ro = new ResourceOffering(elem);

                SecurityAssertion secAssertion = null;
                if ((secAss != null) && !secAss.equals("") &&
                    !secAss.equals("null")) {
                    BufferedInputStream secIn = 
                        new BufferedInputStream(new FileInputStream(secAss));
	            Element elem2 = XMLUtils.toDOMDocument(secIn, null)
                        .getDocumentElement();

	            secAssertion = new SecurityAssertion(elem2);
                }

                SessionProvider sessionProvider = SessionManager.getProvider();
                Object sessionObj = sessionProvider.getSession(request);

                DSTClient client = new DSTClient(ro, providerID, sessionObj,
                    request, response); 
                if (secAss != null) {
                    client.setSecurityAssertion(secAssertion);
                }

                if (authMech != null) {
                    client.setSecurityMech(authMech);
                }

                if ((queryString != null) && !(queryString.equals(""))) {
                    List items = new ArrayList();
                    DSTModification item = new DSTModification();
                    item.setNameSpaceURI(DSTConstants.IDPP_SERVICE_TYPE);
                    item.setSelect(queryString);
                    item.setOverrideAllowed(true);
                    item.setId("modify-item-#1");
                    if ((valueString != null) && !(valueString.equals(""))) {
                         String xml = "<" + attrToModify + " xmlns =\"" + 
                                DSTConstants.IDPP_SERVICE_TYPE + "\">" +
                                valueString + "</" + attrToModify + ">";
                        List values = new ArrayList();
                        values.add(DSTUtils.parseXML(xml));
                        item.setNewDataValue(values);
                    }
                    items.add(item);
                    DSTModifyResponse response1 = client.modify(items);
                    %>
                        <h2>Got result:</h2>
                        <pre><%= SAMLUtils.displayXML(response1.toString()) %>
                        </pre>
                    <%
                } else {
                    %>ERROR: Select String is null<% 
                }
            } catch (Throwable t) {
                t.printStackTrace();
                StringWriter buf = new StringWriter();
                t.printStackTrace(new PrintWriter(buf));
                %>
                    ERROR: caught exception:
                    <pre><%= SAMLUtils.displayXML(buf.toString()) %></pre>
                <%
            }
        }
%>
        <p><a href="index.jsp">Return to index.jsp</a></p>
<%
    } 
%>
    <hr/>
    </body>
</html>
