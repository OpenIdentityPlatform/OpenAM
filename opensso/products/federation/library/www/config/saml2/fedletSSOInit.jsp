<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: fedletSSOInit.jsp,v 1.8 2009/06/24 23:05:30 mrudulahg Exp $

--%>




<%@ page import="com.sun.identity.shared.debug.Debug" %>
<%@ page import="com.sun.identity.saml.common.SAMLUtils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaManager" %>
<%@ page import="com.sun.identity.saml2.profile.SPCache" %>
<%@ page import="com.sun.identity.saml2.profile.SPSSOFederate" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.StringTokenizer" %>

<%--
    fedletSSOInit.jsp initiates the Single Sign-On at the Service Provider.

    Following are the list of supported query parameters :

    Query Parameter Name    Description

    1. metaAlias         MetaAlias for Service Provider. The format of
                         this parameter is /realm_name/SP name. If unspecified,
                         first available hosted SP is used.

    2. idpEntityID       Identifier for Identity Provider. If unspecified,
                         first available remote IDP is used.

    3. RelayState        Target URL on successful complete of SSO/Federation

    4. RelayStateAlias   Specify the parameter(s) to use as the RelayState.
                         e.g. if the request URL has :
                         ?TARGET=http://server:port/uri&RelayStateAlias=TARGET
                         then the TARGET query parameter will be interpreted as
                         RelayState and on successful completion of 
                         SSO/Federation user will be redirected to the TARGET 
                         URL.

    5. NameIDFormat      NameIDPolicy format Identifier Value.
                         For example,
                         urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
                         urn:oasis:names:tc:SAML:2.0:nameid-format:transient
                         Note : transient will always be used for Fedlet
            
    6. binding           URI value that identifies a SAML protocol binding to
                         used when returning the Response message.
                         The supported values are :
                             HTTP-Artifact
                             HTTP-POST (default for Fedlet)

    7. AssertionConsumerServiceIndex 
                         An integer number indicating the location
                         to which the Response message should be returned to
                         the requester.

    8. AttributeConsumingServiceIndex
                         Indirectly specifies information associated
                         with the requester describing the SAML attributes
                         the requester desires or requires to be supplied
                         by the IDP in the generated Response message.
                         Note: This parameter may not be supported for
                         this release.

    9. isPassive         true or false value indicating whether the IDP 
                         should authenticate passively.

    10. ForceAuthN       true or false value indicating if IDP must
                         force authentication OR false if IDP can rely on
                         reusing existing security contexts.
                         true - force authentication

    11.AllowCreate       Value indicates if IDP is allowed to created a new
                         identifier for the principal if it does not exist.
                         Value of this parameter can be true OR false.
                         true - IDP can dynamically create user.

    12.Destination       A URI Reference indicating the address to which the
                         request has been sent.

    13.AuthnContextDeclRef  
                         Specifies the AuthnContext Declaration Reference.
                         The value is a pipe separated value with multiple
                         references.

    14.AuthnContextClassRef 
                         Specifies the AuthnContext Class References.
                         The value is a pipe separated value with multiple
                         references.

    15 AuthLevel         The Authentication Level of the Authentication
                         Context to use for Authentication. 

    16.AuthComparison    The comparison method used to evaluate the
                         requested context classes or statements.
                         Allowed values are :
                             exact
                             minimum
                             maximum
                             better

    17.Consent           Specifies a URI a SAML defined identifier 
                         known as Consent Identifiers.These are defined in
                         the SAML 2 Assertions and Protocols Document.
                         Note: This parameter may not be supported for
                         this release.

    18.reqBinding        URI value that identifies a SAML protocol binding to
                         used when sending the AuthnRequest.
                         The supported values are :
                             urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect
                             urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST

    19.affiliationID     Affiliation entity ID

--%>
<%
    // Retreive the Request Query Parameters
    // metaAlias and idpEntiyID are the required query parameters
    // metaAlias - Service Provider Entity Id
    // idpEntityID - Identity Provider Identifier
    // Query parameters supported will be documented.
    String idpEntityID = null;
    String metaAlias= null;
    Map paramsMap = null;
    try {
    String reqID = request.getParameter("requestID");
    if (reqID != null) {
       //get the preferred idp
       idpEntityID = SAML2Utils.getPreferredIDP(request);
       paramsMap = (Map)SPCache.reqParamHash.get(reqID);
       metaAlias = (String) paramsMap.get("metaAlias");
       SPCache.reqParamHash.remove(reqID);
    } else {
        // this is an original request check
        // get the metaAlias ,idpEntityID
        // if idpEntityID is null redirect to IDP Discovery
        // Service to retrieve.
        metaAlias = request.getParameter("metaAlias");
        if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
            SAML2MetaManager manager = new SAML2MetaManager();
            List spMetaAliases = 
                manager.getAllHostedServiceProviderMetaAliases("/");
            if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                // get first one
                metaAlias = (String) spMetaAliases.get(0);
            }
            if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
                SAMLUtils.sendError(request, response,
                   response.SC_BAD_REQUEST, "nullSPEntityID",
                   SAML2Utils.bundle.getString("nullSPEntityID"));
                return;
            }
        }

        idpEntityID = request.getParameter("idpEntityID");
        paramsMap = SAML2Utils.getParamsMap(request);
        // always use transient
        List list = new ArrayList();
        list.add(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        paramsMap.put(SAML2Constants.NAMEID_POLICY_FORMAT, list);
        if (paramsMap.get(SAML2Constants.BINDING) == null) {
            // use POST binding
            list = new ArrayList();
            list.add(SAML2Constants.HTTP_POST);
            paramsMap.put(SAML2Constants.BINDING, list);
        } 

        if ((idpEntityID == null) || (idpEntityID.length() == 0)) {
            // get reader url
            String readerURL = SAML2Utils.getReaderURL(metaAlias);
            if (readerURL != null) {
                String rID = SAML2Utils.generateID();
                String redirectURL =
                    SAML2Utils.getRedirectURL(readerURL,rID,request);
                if (redirectURL != null) {
                    paramsMap.put("metaAlias",metaAlias);
                    SPCache.reqParamHash.put(rID,paramsMap);
                    response.sendRedirect(redirectURL);
                    return;
                }
            }
        }
    }

    if ((idpEntityID == null) || (idpEntityID.length() == 0)) {
        SAML2MetaManager manager = new SAML2MetaManager();
        List idpEntities = manager.getAllRemoteIdentityProviderEntities("/"); 
        if ((idpEntities == null) || idpEntities.isEmpty()) {
            SAMLUtils.sendError(request, response,
               response.SC_BAD_REQUEST, "idpNotFound",
               SAML2Utils.bundle.getString("idpNotFound"));
            return;
        } else if (idpEntities.size() == 1) {
            // only one IDP, just use it
            idpEntityID = (String) idpEntities.get(0);
        } else { 
            // multiple IDP configured in fedlet
            SAMLUtils.sendError(request, response,
               response.SC_BAD_REQUEST, "nullIDPEntityID",
               SAML2Utils.bundle.getString("nullIDPEntityID"));
            return;
        }
    }
    // get the parameters and put it in a map.
    SPSSOFederate.initiateAuthnRequest(request,response,metaAlias,
        idpEntityID, paramsMap);
    } catch (SAML2Exception sse) {
        SAML2Utils.debug.error("Error sending AuthnRequest " , sse);
        SAMLUtils.sendError(request, response,
            response.SC_BAD_REQUEST, "requestProcessingError", 
            SAML2Utils.bundle.getString("requestProcessingError") + " " +
            sse.getMessage());
    } catch (Exception e) {
        SAML2Utils.debug.error("Error processing Request ",e);
        SAMLUtils.sendError(request, response,
            response.SC_BAD_REQUEST, "requestProcessingError",
            SAML2Utils.bundle.getString("requestProcessingError") + " " +
            e.getMessage());
    }
%>
