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

   $Id: discovery-query.jsp,v 1.6 2009/02/05 00:46:39 mrudulahg Exp $

--%>


<%@page import="
java.io.*,
java.util.*,
com.sun.identity.liberty.ws.disco.*,
com.sun.identity.liberty.ws.disco.common.*,
com.sun.identity.liberty.ws.security.*,
com.sun.identity.plugin.session.SessionManager,
com.sun.identity.plugin.session.SessionProvider,
com.sun.identity.saml.common.*,
com.sun.identity.setup.SetupClientWARSamples,
com.sun.identity.shared.xml.XMLUtils,
com.sun.liberty.jaxrpc.LibertyManagerClient"
%>


<html xmlns="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <head><title>Discovery Service Query Sample</title></head>
    <body bgcolor="white">
        <h1>Discovery Service Query Sample</h1>
<%
        if (request.getMethod().equals("GET")) {
            String discoOfferingFile =
                request.getParameter("discoveryResourceOffering");
            if (discoOfferingFile == null) {
                discoOfferingFile= "";
            }
        
            String providerID = request.getParameter("providerID");
            if ((providerID == null) || (providerID.length() == 0)) {
                String bootstrapFile = System.getProperty("user.home") +
                    File.separator +
                    SetupClientWARSamples.CLIENT_WAR_CONFIG_TOP_DIR +
                    File.separator +
                    SetupClientWARSamples.getNormalizedRealPath(
                    getServletConfig().getServletContext()) +
                    "ClientSampleWSC.properties";
                FileInputStream fin = new FileInputStream(bootstrapFile);
                Properties props = new Properties();
                props.load(fin);
                fin.close();

                providerID = props.getProperty("spProviderID");
            }
%>
            <form method="POST" name="discoquery">
                <table>
                    <tr>
                        <td>ResourceOffering (for discovery service itself)</td>                        <td>
                             <textarea rows="2" cols="50" name="discoResourceOffering"><%= discoOfferingFile %></textarea>
                         </td>
                    </tr>
                    <tr>
                        <td>ServiceType to look for</td>
                        <td><input type="text" name="serviceType" value="urn:liberty:id-sis-pp:2003-08"/></td>
                    </tr>
                </table>
                <input type="hidden" name="providerID" 
                    value='<%= providerID %>'>
                <input type="submit" value="Send Discovery Lookup Request" />
            </form>
<%
        } else {
            String resourceXMLfile =
                   request.getParameter("discoResourceOffering");
            String resourceXML = null;
            try {
                BufferedReader bir = new BufferedReader(
                      new FileReader(resourceXMLfile));
                StringBuffer buffer = new StringBuffer(2000);
                int b1;
                while ((b1=bir.read ())!= -1) {
                    buffer.append((char) b1);
                }
                resourceXML = buffer.toString();
            } catch (Exception e) {
                %>Warning: cannot read disco resource offering.<%
                  e.printStackTrace();
            }




            String serviceType = request.getParameter("serviceType");
            String providerID = request.getParameter("providerID");
            String fnSuffix = null; 

            ResourceOffering offering = null;
            try {
                SessionProvider sessionProvider = SessionManager.getProvider();
                Object sessionObj = sessionProvider.getSession(request);
                LibertyManagerClient lmc = new LibertyManagerClient();
                if (resourceXML != null && resourceXML.length() > 0) {
                    offering = new ResourceOffering(XMLUtils.toDOMDocument(
                        resourceXML, null).getDocumentElement());
                } else {
                    offering = lmc.getDiscoveryResourceOffering(sessionObj,
                        providerID);
                } 
                if(offering == null) {
                    %>ERROR: no resource offering.<%
                } else {
                    List discoCreds = new ArrayList();
                    discoCreds.add(lmc.getDiscoveryServiceCredential(
                        sessionObj, providerID));
                    DiscoveryClient client = new DiscoveryClient(
                        offering, sessionObj, providerID, discoCreds);        
                    List types = new ArrayList();
                    if ((serviceType != null) && !(serviceType.equals(""))) {
                        types.add(serviceType);
                    }
                    QueryResponse result = client.getResourceOffering(types);
                    List results = result.getResourceOffering();
                    if ((results == null) || (results.size() == 0)) {
                        %>
                        <h2>Query result:</h2>
                         No ResourceOffering found.
                        <p><a href="index.jsp">Return to index.jsp</a></p>
                        <%
                    } else {
                        %>
                        <h2>Query result :</h2>
                        <pre><%= SAMLUtils.displayXML(result.toString()) %></pre>
                        <%
                        List creds = result.getCredentials();
                            int itemSize = results.size();
                            for (int i = 0; i < itemSize; i ++) {
                                ResourceOffering offer =
                                (ResourceOffering) results.get(i);
                            String remoteProvider = 
                                  offer.getServiceInstance().getProviderID();
                            fnSuffix = remoteProvider.replace('/','_')
                                .replace(':','_');
                            String entryID = offer.getEntryID();
                            String resOffFN = System.getProperty(
                                "user.home") + File.separator + 
                                SetupClientWARSamples.CLIENT_WAR_CONFIG_TOP_DIR
                                + File.separator + 
                                SetupClientWARSamples.getNormalizedRealPath(
                                getServletConfig().getServletContext()) + 
                                "RO_"+ fnSuffix + "_" + i;
                            String secAssertion = null;
                            try {
                                FileWriter resOffWriter = 
                                      new FileWriter(resOffFN);
                                resOffWriter.write(offer.toString());
                                resOffWriter.close();
                            } catch(Exception ex) {
                                %>Cannot write security assertion to file:<%
                                 ex.printStackTrace();
                            }

                        if(creds != null && creds.size() != 0) {
                           SecurityAssertion secAss =
                               (SecurityAssertion)creds.get(0);
                           secAssertion = "/tmp/secAFN_" + fnSuffix; 
                           try {
                               FileWriter secWriter =
                                     new FileWriter(secAssertion);
                               secWriter.write(secAss.toString(true, true));
                               secWriter.close();
                           } catch(Exception ex) {
                               %>Cannot write security assertion to file:<%
                               ex.printStackTrace();
                           }
                        }

                        %>
<pre><%= SAMLUtils.displayXML(offer.toString()) %></pre>
<form name="ppquerycall" method="GET" action="id-sis-pp-query.jsp">
<input type='hidden' name='providerID' value='<%= providerID %>'>
<input type='hidden' name='resOffFN' value='<%=resOffFN %>'>
<input type='hidden' name='secAssertion' value='<%=secAssertion %>'>
<input type="submit" name="Submit" value="Send PP Query" />
</form>

<p>

<form name="ppmodifycall" method="GET" action="id-sis-pp-modify.jsp">
<input type='hidden' name='providerID' value='<%= providerID %>'>
<input type='hidden' name='resOffFN' value='<%=resOffFN %>'>
<input type='hidden' name='secAssertion' value='<%=secAssertion %>'>
<input type="submit" name="Submit" value="Send PP Modify" />
</form>

<p>

<form name="discomodifycall" method="GET" action="discovery-modify.jsp">
<input type='hidden' name='providerID' value='<%= providerID %>'>
<input type='hidden' name='discoveryResourceOffering'
    value='<%= resourceXMLfile %>'>
<input type='hidden' name='entryID' value='<%= entryID %>'>
<input type="submit" name="Submit" value="Remove this PP Resource Offering from Discovery Service" />
</form>

<hr>
                            <%
                        }
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
        <hr />
    </body>
</html>
