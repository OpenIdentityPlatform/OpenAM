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

   $Id: index.jsp,v 1.7 2009/02/05 00:46:40 mrudulahg Exp $

--%>

<%@page 
import="
java.io.*, 
java.util.*,
com.iplanet.am.util.SystemProperties,
com.sun.identity.liberty.ws.disco.ResourceOffering,
com.sun.identity.liberty.ws.security.SecurityAssertion,
com.sun.identity.plugin.session.SessionManager,
com.sun.identity.plugin.session.SessionProvider,
com.sun.identity.saml.common.*,
com.sun.identity.setup.SetupClientWARSamples,
com.sun.identity.shared.Constants,
com.sun.liberty.jaxrpc.LibertyManagerClient"
%>

<html xmlns="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <head><title>Discovery Service Boot Strapping</title></head>
    <script language="javascript">
        function checkForm() {
            if (document.discomodify.idpUserDN.value == '') {
                alert("IDP User DN is required.");
                return false;
            }
            return true;
        }
    </script>
    <body bgcolor="white">
	<h1>Discovery Service Boot Strapping Resource Offering</h1>
<%!

public void jspInit() {
    try {
        FileInputStream fin = new FileInputStream(
            System.getProperty("user.home") +
            File.separator + SetupClientWARSamples.CLIENT_WAR_CONFIG_TOP_DIR +
            File.separator + SetupClientWARSamples.getNormalizedRealPath(
            getServletConfig().getServletContext()) + "AMConfig.properties");
        Properties props = new Properties();
        props.load(fin);
        props.setProperty(Constants.SERVER_MODE, "false");
        props.setProperty("com.sun.identity.sm.sms_object_class_name",
            "com.sun.identity.sm.jaxrpc.SMSJAXRPCObject");
        props.setProperty(Constants.AM_LOGSTATUS, "INACTIVE");
        SystemProperties.initializeProperties(props);
        fin.close();
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
%>
<%
    try {

        String bootstrapFile = System.getProperty("user.home") +
            File.separator + SetupClientWARSamples.CLIENT_WAR_CONFIG_TOP_DIR +
            File.separator +
            SetupClientWARSamples.getNormalizedRealPath(
            getServletConfig().getServletContext()) +
            "ClientSampleWSC.properties";
        FileInputStream fin = new FileInputStream(bootstrapFile);
        Properties props = new Properties();
        props.load(fin);
        fin.close();

        // WSC-SP Provider ID
        String providerID = props.getProperty("spProviderID");
        SessionProvider sessionProvider = SessionManager.getProvider();
        Object sessionObj = sessionProvider.getSession(request);

        if(sessionObj != null && sessionProvider.isValid(sessionObj)) {
            LibertyManagerClient lmc = new LibertyManagerClient();
            ResourceOffering offering = lmc.getDiscoveryResourceOffering(
                sessionObj, providerID);

	    if(offering == null) {
                %>ERROR: no resource offering in AttributeStatement.<%
            } else {
                String remoteProvider = 
                       offering.getServiceInstance().getProviderID(); 
		String fnSuffix = remoteProvider.replace('/','_')
                    .replace(':','_');
		String fileName = System.getProperty("user.home") +
                    File.separator + 
                    SetupClientWARSamples.CLIENT_WAR_CONFIG_TOP_DIR +
                    File.separator +
                    SetupClientWARSamples.getNormalizedRealPath(
                    getServletConfig().getServletContext()) +
                    "RO_" + fnSuffix;
		PrintWriter pw = new PrintWriter(new FileWriter(fileName));
                pw.print(offering.toString());
                pw.close();
                // get reourceID
                    %>
<form method="GET" action="discovery-query.jsp" name="discoquerycall">
<input type='hidden' name='providerID' value='<%= providerID %>'>
<input type='hidden' name='discoveryResourceOffering' value='<%= fileName %>'>
<input type='hidden' name='actiontaken' value='Send Discovery Lookup'>
<input type="submit" name="Submit" value="Send Discovery Lookup" />
</form>
<p>
<form method="GET" name="discomodify" action="discovery-modify.jsp" onSubmit="return checkForm();">
IDP User DN (IDP user whose has single-sign-on. For example id=idpuser,ou=user,dc=openom,dc=java,dc=net):
<input name="idpUserDN" type="text" size="50" value=""/>
<br><br>
<input type='hidden' name='providerID' value='<%= providerID %>'>
<input type='hidden' name='discoveryResourceOffering' value='<%= fileName %>'>
<input type='hidden' name='actiontaken' value="Add PP Resource Offering" />
<input type="submit" name='Submit' value="Add PP Resource Offering" />
</form>
<pre><%= SAMLUtils.displayXML(offering.toString()) %></pre>
                    <%
                SecurityAssertion sa = lmc.getDiscoveryServiceCredential(
                    sessionObj, providerID);
                if (sa != null) {
                    %>
<pre><%= SAMLUtils.displayXML(sa.toString()) %></pre>
                    <%
                }
	    }
        } else {
	    %>ERROR: user not logged in.<%
        }
    } catch (Exception ex) {
        StringWriter bufex = new StringWriter();
        ex.printStackTrace(new PrintWriter(bufex));
        %>
            ERROR: caught Exception:
            <pre>
        <%
            out.println(bufex.toString());
        %>
            </pre>
        <%
    }
%>
	<hr/>
    </body>
</html>
