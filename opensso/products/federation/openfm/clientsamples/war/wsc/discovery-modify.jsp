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

   $Id: discovery-modify.jsp,v 1.6 2009/02/05 00:46:38 mrudulahg Exp $

--%>


<%@page import="
java.io.*,
java.util.*,
com.sun.identity.liberty.ws.disco.*,
com.sun.identity.liberty.ws.disco.common.*,
com.sun.identity.liberty.ws.idpp.plugin.IDPPResourceIDMapper,
com.sun.identity.plugin.session.SessionManager,
com.sun.identity.plugin.session.SessionProvider,
com.sun.identity.saml.common.*,
com.sun.identity.setup.SetupClientWARSamples,
com.sun.identity.shared.xml.XMLUtils,
com.sun.liberty.jaxrpc.LibertyManagerClient"
%>

<html xmlns="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<head><title>Discovery Service Modification</title></head>
<body bgcolor="white">
<h1>Discovery Service Modification</h1>
<%
    if (request.getMethod().equals("GET")) {
        String resourceOfferingFile =
            request.getParameter("discoveryResourceOffering");
        if (resourceOfferingFile == null) {
            resourceOfferingFile= "";
        }
        String entryID = request.getParameter("entryID");
        if (entryID == null) {
            entryID= "";
        }

        String idpUserDN = request.getParameter("idpUserDN");
        
        // The following three values need to be changed to register a personal 
        // profile resource offering for a user.

        String providerID = request.getParameter("providerID");
        String newPPRO = "";
        String submit = request.getParameter("Submit");
        String actiontaken = request.getParameter("actiontaken");
        submit = actiontaken;
        if ((submit != null) && submit.equals("Add PP Resource Offering")) {

            String bootstrapFile = System.getProperty("user.home") +
                File.separator + 
                SetupClientWARSamples.CLIENT_WAR_CONFIG_TOP_DIR +
                File.separator +
                SetupClientWARSamples.getNormalizedRealPath(
                getServletConfig().getServletContext())
                + "ClientSampleWSC.properties";
            FileInputStream fin = new FileInputStream(bootstrapFile);
            Properties props = new Properties();
            props.load(fin);
            fin.close();

            String idpProt = props.getProperty("idpProt");
            String idpHost = props.getProperty("idpHost");
            String idpPort = props.getProperty("idpPort");
            String idpDeploymenturi = props.getProperty("idpDeploymenturi");
            if (!idpDeploymenturi.startsWith("/")) {
                idpDeploymenturi = "/" + idpDeploymenturi;
            }

            String ppProviderID = idpProt + "://" + idpHost + ":" + idpPort +
                idpDeploymenturi + "/Liberty/idpp";
            String ppEndPoint = ppProviderID;

	    String ppResourceID = (new IDPPResourceIDMapper()).getResourceID(
                   ppProviderID, idpUserDN);

            newPPRO = 
                "<ResourceOffering xmlns=\"urn:liberty:disco:2003-08\">" 
                + "  <ResourceID>" + ppResourceID + "</ResourceID>\n"
                + "  <ServiceInstance>\n"
                + "    <ServiceType>urn:liberty:id-sis-pp:2003-08</ServiceType>\n"
                + "    <ProviderID>" + ppProviderID + "</ProviderID>\n"
                + "    <Description>"
                + "      <SecurityMechID>urn:liberty:security:2003-08:null:null"
                + "</SecurityMechID>\n" 
                + "      <Endpoint>" + ppEndPoint + "</Endpoint>\n"
                + "    </Description>\n"
                + "  </ServiceInstance>\n"
                + "  <Abstract>This is xyz </Abstract>\n"
                + "</ResourceOffering>";
        }
%>
<form method="POST" name="discomodify">
<table>
<tr>
<td>ResourceOffering (for discovery service itself)</td>
<td>
<textarea rows="2" cols="30" name="discoResourceOffering"><%= resourceOfferingFile %></textarea>
</td>
</tr>
<tr>
<td>PP ResourceOffering to add</td>
<td>
<textarea rows="20" cols="60" name="insertStr"><%= newPPRO %></textarea>
</td>
</tr>
<tr>
<td>AND/OR PP ResourceOffering to remove</td>
<td>
<textarea rows="2" cols="30" name="entryID"><%= entryID %></textarea>
</td>
</tr>
</table>
<input type="hidden" name="providerID" value="<%= providerID %>" />
<input type="submit" value="Send Discovery Update Request" />
</form>
<%
    } else {
        try {
            String resourceXMLFile = request.getParameter("discoResourceOffering");
	    String resourceXML = null;
            try {
                BufferedReader bir = new BufferedReader(
                    new FileReader(resourceXMLFile));
           	StringBuffer buffer = new StringBuffer(2000);
            	int b1;
            	while ((b1=bir.read ())!= -1) {
                	buffer.append((char) b1);
                }
           	resourceXML = buffer.toString();
            } catch (Exception e) {
                %>Warning: cannot read disco resource offering.<%
            }
            String insertString = request.getParameter("insertStr");
            String entryID = request.getParameter("entryID");
            String providerID = request.getParameter("providerID");
            if (resourceXML == null || resourceXML.equals("")) {
                %>ERROR: resource offering missing<%
            } else {
                ResourceOffering offering;
		try {
                    offering = new ResourceOffering(DiscoUtils.parseXML(
						resourceXML));
                    SessionProvider sessionProvider =
                        SessionManager.getProvider();
                    Object sessionObj = sessionProvider.getSession(request);

                    LibertyManagerClient lmc = new LibertyManagerClient();
                    List discoCreds = new ArrayList();
                    discoCreds.add(lmc.getDiscoveryServiceCredential(
                        sessionObj, providerID));

                    DiscoveryClient client = new DiscoveryClient(
                        offering, sessionObj, providerID, discoCreds);
                    Modify mod = new Modify();
                    mod.setResourceID(offering.getResourceID());
		    mod.setEncryptedResourceID(offering.getEncryptedResourceID());
                    if ((insertString != null) &&
                            !(insertString.equals("")))
                    {
			InsertEntry insert = new InsertEntry(
			    new ResourceOffering(
					DiscoUtils.parseXML(insertString)),
			    null);
// Uncommnent the following when it's required.
//                        List directives = new ArrayList();
//                        Directive dir1 = new Directive(
//                          Directive.AUTHENTICATE_REQUESTER);
//                        Directive dir2 = new Directive(
//                          Directive.AUTHORIZE_REQUESTER);
//                        directives.add(dir2);
//                        insert.setAny(directives);
			List inserts = new ArrayList();
			inserts.add(insert);
			mod.setInsertEntry(inserts);
                    }
		    if ((entryID != null) && !(entryID.equals(""))) {
                        RemoveEntry remove = new RemoveEntry(
                            XMLUtils.escapeSpecialCharacters(entryID));
                        List removes = new ArrayList();
                        removes.add(remove);
                        mod.setRemoveEntry(removes);
                    }
                    if ((mod.getInsertEntry() == null) &&
                                (mod.getRemoveEntry() == null))
                    {
                            %>ERROR: empty Modify<%
                    } else {
                        %>
                            <h2>Formed Modify :</h2>
                            <pre><%= SAMLUtils.displayXML(mod.toString()) %></pre>
                        <%
                            ModifyResponse resp2 = client.modify(mod);
                        %>
                            <h2>Got result:</h2>
                            <pre><%= SAMLUtils.displayXML(resp2.toString()) %></pre>
                        <%
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    StringWriter buf = new StringWriter();
                    t.printStackTrace(new PrintWriter(buf));
                    %>
                        ERROR: caught exception:
                        <pre>
                    <% 
                              out.println(buf.toString());
                    %>
                        </pre>
                    <%
                }
            }
%>
            <p><a href="index.jsp">Return to index.jsp</a></p>
<%
        } catch (Throwable e) {
            e.printStackTrace();
            StringWriter buf = new StringWriter();
            e.printStackTrace(new PrintWriter(buf));
            %>
                ERROR: oocaught exception:
                <pre>
            <%
                out.println(buf.toString());
            %>
                </pre>
            <%
        }
    } 
%>
        <hr/>
    </body>
</html>
