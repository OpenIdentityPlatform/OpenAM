<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: idpSSOInit.jsp,v 1.9 2009/06/24 23:05:30 mrudulahg Exp $

   Portions Copyrighted 2013-2016 ForgeRock AS.
--%>

<%@ page import="com.sun.identity.saml.common.SAMLUtils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaUtils" %>
<%@ page import="com.sun.identity.saml2.profile.IDPSSOUtil" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="org.forgerock.guice.core.InjectorHolder" %>
<%@ page import="org.forgerock.openam.audit.AuditEventPublisher" %>
<%@ page import="org.forgerock.openam.saml2.audit.SAML2Auditor" %>
<%@ page import="org.forgerock.openam.audit.AuditEventFactory" %>

<%--
    idpssoinit.jsp initiates Unsolicited SSO at the Identity Provider.

    Following are the list of supported query parameters :

    Required parameters to this jsp are :

    Query Parameter Name    Description

    1. metaAlias	    MetaAlias for Identity Provider. The format of
			    this parameter is /realm_name/IDP name.

    2. spEntityID	    Identifier for Service Provider.

    Optional Query Parameters :

    Query Parameter Name    Description

    3. RelayState	    Target URL on successful complete of SSO/Federation

    4. RelayStateAlias	    Specify the parameter(s) to use as the RelayState.
                            e.g. if the request URL has :
                             ?TARGET=http://server:port/uri&RelayStateAlias=TARGET
                            then the TARGET query parameter will be interpreted as
                            RelayState and on successful completion of
                            SSO/Federation user will be redirected to the TARGET URL.


    5. NameIDFormat	    NameID format Identifier Value.
			    For example,
                                urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
                                urn:oasis:names:tc:SAML:2.0:nameid-format:transient

    6. binding              URI value that identifies a SAML protocol binding to
                            used when returning the Response message.
                            The supported values are :
                                HTTP-Artifact
                                HTTP-POST
			  
			    NOTE: There are other SAML defined values for these
				  which are not supported by FM/AM.
    7. affiliationID	    affiliation entity ID
--%>
<%
    AuditEventPublisher aep = InjectorHolder.getInstance(AuditEventPublisher.class);
    AuditEventFactory aef = InjectorHolder.getInstance(AuditEventFactory.class);
    SAML2Auditor saml2Auditor = new SAML2Auditor(aep, aef, request);
    saml2Auditor.setMethod("idpSSOInit");
    saml2Auditor.setSessionTrackingId(session.getId());
    saml2Auditor.auditAccessAttempt();
    // Retrieve the Request Query Parameters
    // metaAlias and spEntiyID are the required query parameters
    // metaAlias - Identity Provider Entity Id
    // spEntityID - Service Provider Identifier

    try {
        String cachedResID = request.getParameter(SAML2Constants.RES_INFO_ID);
        // if this id is set, then this is a redirect from the COT
        // cookie writer. There is already an assertion response
        // cached in this provider. Send it back directly.
        if ((cachedResID != null) && (cachedResID.length() != 0)) {
            IDPSSOUtil.sendResponse(request, response, new PrintWriter(out, true), cachedResID);
            return;
        }

	    String metaAlias = request.getParameter("metaAlias");
        saml2Auditor.setRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
        if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
            SAMLUtils.sendError(
                    request, response, response.SC_BAD_REQUEST, "nullIDPEntityID",
		            SAML2Utils.bundle.getString("nullIDPEntityID"));
            saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                    SAML2Utils.bundle.getString("nullSPEntityID"));
	        return;
        }
        String spEntityID = request.getParameter("spEntityID");

        if ((spEntityID == null) || (spEntityID.length() == 0)) {
            SAMLUtils.sendError(
                    request, response, response.SC_BAD_REQUEST, "nullSPEntityID",
		            SAML2Utils.bundle.getString("nullSPEntityID"));
            saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                    SAML2Utils.bundle.getString("nullSPEntityID"));
	        return;
        }

	    // get the nameIDPolicy
	    String nameIDFormat = request.getParameter(SAML2Constants.NAMEID_POLICY_FORMAT);
	    String relayState = SAML2Utils.getRelayState(request);
	    IDPSSOUtil.doSSOFederate(request, response, new PrintWriter(out, true), null, spEntityID, metaAlias,
                nameIDFormat, relayState, saml2Auditor);
        saml2Auditor.auditAccessSuccess();
    } catch (SAML2Exception sse) {
	    SAML2Utils.debug.error("Error processing request " , sse);
	    SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
            "requestProcessingError", 
	        SAML2Utils.bundle.getString("requestProcessingError") + " " +
            sse.getMessage());
        saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                SAML2Utils.bundle.getString("requestProcessingError"));
        return;
    } catch (Exception e) {
        SAML2Utils.debug.error("Error processing request ",e);
	    SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
            "requestProcessingError",
	        SAML2Utils.bundle.getString("requestProcessingError") + " " +
            e.getMessage());
        saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                SAML2Utils.bundle.getString("requestProcessingError"));
        return;
    }
%>
