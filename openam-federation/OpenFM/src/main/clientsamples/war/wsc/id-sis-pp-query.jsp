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

   $Id: id-sis-pp-query.jsp,v 1.3 2009/02/05 00:46:40 mrudulahg Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<%@page import="
java.io.*,
java.util.*,
javax.xml.bind.*,
org.w3c.dom.Document,
org.w3c.dom.Element,
com.sun.identity.liberty.ws.dst.*,
com.sun.identity.liberty.ws.disco.jaxb.*,
com.sun.identity.liberty.ws.disco.*,
com.sun.identity.liberty.ws.interaction.*,
com.sun.identity.liberty.ws.soapbinding.*,
com.sun.identity.liberty.ws.security.*,
com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken,
com.sun.identity.plugin.session.SessionManager,
com.sun.identity.plugin.session.SessionProvider,
com.sun.identity.saml.common.*,
com.sun.identity.shared.xml.XMLUtils"
%>

<html xmlns="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<head><title>IDPP Query</title></head>
<body bgcolor="white">
    <h1>IDPP Query</h1>
<%
    if (request.getMethod().equals("GET")) {
        // First check if there is a need for interaction.
        String resend = request.getParameter(
                        InteractionManager.RESEND_MESSAGE);
        if (resend != null) {
            // resend requet after interaction happened
            try {
                // resend message
                Message ret = InteractionManager.getInstance().resendRequest(
                    request.getRequestURL().toString(), request, response);
                %>
                    <pre><%= SAMLUtils.displayXML(ret.toString()) %></pre>
                <%
            } catch (SOAPFaultException sfex) {
                %>
                    ERROR: <%= sfex.getSOAPFaultMessage().getSOAPFault().getFaultString() %>
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
            %>
                <p><a href="index.jsp">Return to index.jsp</a></p>

            <%
        } else {
            String soapEndPoint = "";
            List mechs = null; 
            String providerID = request.getParameter("providerID");
	    String fnSuffix = providerID.replace('/', '_');
	    String securityAssertion = 
                   request.getParameter("securityAssertion");
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
            <form name="ppquery" method="POST">
		<input type='hidden' name='resOffFN' value='<%=resOffFN %>' />
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
                        value="/PP/CommonName/AnalyzedName/FN"/></td>
                </tr>
            </table>
            <input type="hidden" name="providerID" value="<%= providerID %>" />
	    <input type='hidden' name='securityAssertion' 
                   value='<%= securityAssertion %>'/>
            <input type="submit" value="send PPQueryRequest" />
            </form>
            <%
        }
    } else {
        String queryString = request.getParameter("queryStr");
        String providerID = request.getParameter("providerID");
	String resOffFN = request.getParameter("resOffFN");
        String secAss = request.getParameter("secAssertion");
        String authMech = request.getParameter("authMech");
        if(resOffFN == null || resOffFN.equals("")) {
           %>ERROR: resource offering missing<%
        } else {  
          try {
            ResourceOffering ro = null;
	    BufferedInputStream bir = 
                 new BufferedInputStream(new FileInputStream(resOffFN));
	    Document doc = XMLUtils.toDOMDocument(bir, null);
	    Element elem = doc.getDocumentElement();
	    ro = new ResourceOffering(elem);

            SecurityAssertion secAssertion = null;
            if(secAss != null && !secAss.equals("") && !secAss.equals("null")) {
	       BufferedInputStream secIn =
                   new BufferedInputStream(new FileInputStream(secAss));
	       Element elem2 = XMLUtils.toDOMDocument(
                   secIn, null).getDocumentElement();
	       secAssertion = new SecurityAssertion(elem2);
            }

            SessionProvider sessionProvider = SessionManager.getProvider();
            Object sessionObj = sessionProvider.getSession(request);
           
	    DSTClient client =
	        new DSTClient(ro, providerID, sessionObj, request, response);
            if(secAssertion != null) {
               client.setSecurityAssertion(secAssertion);
            }
        
            if(authMech != null && !authMech.equals("null")) {
               client.setSecurityMech(authMech);
            }

            if ((queryString == null) 
                 || (queryString.equals("")) || (queryString.equals("null"))) {
                %>ERROR: Query String can not be null<%
                 return;
            }

            List items = new ArrayList();
            DSTQueryItem item = new DSTQueryItem(queryString,
                "urn:liberty:sis-2003-08:pp");
            item.setId("id1");
            item.setItemID("name1");
            item.setNameSpaceURI("urn:liberty:id-sis-pp:2003-08");
            item.setNameSpacePrefix("pp");
            items.add(item);
            List data = null;
                    
            ServiceInstanceUpdateHeader siuHeader =  null;
            try {
                data = client.getData(items);
            } catch (DSTException de) {
                siuHeader = client.getServiceInstanceUpdateHeader(); 
            }
            siuHeader  =  client.getServiceInstanceUpdateHeader();
            if(siuHeader != null) {
               String newEndpoint = siuHeader.getEndpoint();
               client.setSOAPEndPoint(newEndpoint);
               List secs = siuHeader.getSecurityMechIDs();
               String newSec = null;
               if(secs != null && secs.size() != 0) {
                  newSec = (String)secs.get(0);
               }

               if(newSec != null && 
                  newSec.equals("urn:liberty:security:2003-08:TLS:X509")) {
                  SecurityTokenManagerClient securityTokenManagerClient = 
                        new SecurityTokenManagerClient(sessionObj);
                  BinarySecurityToken authToken = 
                        securityTokenManagerClient.getX509CertificateToken(); 

                  DSTClient newClient1 = new DSTClient(authToken,
                   newEndpoint, providerID, request, response);
                  newClient1.setResourceID(ro.getResourceID().getResourceID());
                  data = newClient1.getData(items);
               } else {
                  DSTClient newClient2 = 
                   new DSTClient(newEndpoint, providerID, request, response);
                  newClient2.setResourceID(ro.getResourceID().getResourceID());
                  data = client.getData(items);
               }
            }

            if(data == null || data.size() == 0) {
                       %> No data found. <%
                  
            } else {
                   Iterator iter = data.iterator();
                   while (iter.hasNext()) {
                        DSTData dstData = (DSTData)iter.next();
                        %>
                            <h2>Got result:</h2>
                            <pre><%= SAMLUtils.displayXML(dstData.toString()) %>
                            </pre>
                        <%
                    }
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
