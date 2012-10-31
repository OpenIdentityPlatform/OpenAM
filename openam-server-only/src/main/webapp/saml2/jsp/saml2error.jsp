<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: saml2error.jsp,v 1.3 2009/06/17 03:10:28 exu Exp $

--%><%--
   This is the default error display page.
   There are three parameters passed down to this URL:
     1. errorcode : Error code, this is the I18n key of the error message.
     2. httpstatuscode : Http status code
     3. errormessage : More detailed I18n'd error message  
   Here is a list of error codes (from locale files):
     * nullSPEntityID
     * nullIDPEntityID
     * idpNotFound
     * requestProcessingError
     * failedToProcessSSOResponse
     * nullInput 
     * requestProcessingMNIError
     * nullRequestType 
     * nullSSOToken
     * LogoutRequestProcessingError
     * LogoutResponseProcessingError
     * largeContentLength
     * errorMetaManager
     * metaDataError
     * nullSessionProvider
     * SSOFailed
     * LogoutRequestCreationError
     * nullAssertionID
     * failedToGetAssertionIDRequestMapper
     * failedToAuthenticateRequesterURI
     * invalidAssertionID
     * invalidAssertion 
     * unsupportedEncoding 
     * MissingSAMLRequest
     * nullDecodedStrFromSamlResponse
     * nullIDPMetaAlias
     * metaDataError
     * invalidSOAPMessage
     * unableToCreateArtifactResponse
     * LogoutRequestCreationError
     * UnableToRedirectToAuth
     * errorCreateArtifact
     * failedToSendECPResponse 
     * notSupportedHTTPMethod
     * missingArtifact
     * errorObtainArtifact
     * failedToGetIDPSSODescriptor 
     * errorCreateArtifactResolve
     * errorInSOAPCommunication
     * invalidIDP
     * cannotFindArtifactResolutionUrl
     * soapError
     * failedToCreateArtifactResponse
     * missingArtifactResponse
     * invalidSignature
     * invalidInResponseTo 
     * invalidIssuer
     * invalidStatusCode
     * failedToCreateSOAPMessage
     * failedToCreateResponse
     * assertionNotSigned
     * missingSAMLResponse
     * errorObtainResponse
     * errorDecodeResponse
     * invalidHttpRequestFromECP
     * failedToProcessQueryRequest
     * failedToCreateAssertionIDRequest
     * nullPathInfo 
     * invalidMetaAlias 
     * failedToCreateAttributeQuery 
     * failedToCreateAuthnQuery 
     * nameIDMappingFailed
     * failedToInitECPRequest
     * singleLogoutFailed 
     * nullRequestUri
     * invalidRequestUri
     * noRedirectionURL
     * readerServiceFailed
     * nullSessionIndex
     * nullNameID
     Here is the list of error codes for SAML v1.x:
     * untrustedSite
     * nullInputParameter
     * invalidConfig
     * missingTargetHost
     * nullTrustedSite
     * errorCreateArtifact
     * targetForbidden
     * failedCreateSSOToken
     * missingTargetSite
     * couldNotCreateResponse
     * errorSigningResponse
     * errorEncodeResponse
     * missingSAMLResponse
     * errorDecodeResponse
     * errorObtainResponse
     * invalidResponse
--%><%@ page language="java" 
        import="com.sun.identity.saml.common.SAMLConstants,                
                com.sun.identity.saml2.common.SAML2Utils"
%><%
    String errorCode = request.getParameter(SAMLConstants.ERROR_CODE);
    String httpStatusCode = 
        request.getParameter(SAMLConstants.HTTP_STATUS_CODE);
    String errorMessage = request.getParameter(SAMLConstants.ERROR_MESSAGE);
    if (((errorMessage == null) || (errorMessage.length() == 0)) &&
        (errorCode != null)) {
        errorMessage = SAML2Utils.bundle.getString(errorCode);
    }
    int sc = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    if (httpStatusCode != null) {
        try {
            sc = Integer.parseInt(httpStatusCode);
        } catch (NumberFormatException nfe) { 
            // ignore
        }
    }
    response.sendError(sc, errorMessage); 
%>
