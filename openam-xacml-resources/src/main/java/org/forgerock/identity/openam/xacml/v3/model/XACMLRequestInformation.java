/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock Incorporated. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.v3.model;

import com.sun.identity.entitlement.xacml3.core.*;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.identity.openam.xacml.v3.commons.CommonType;
import org.forgerock.identity.openam.xacml.v3.commons.ContentType;
import org.forgerock.identity.openam.xacml.v3.commons.POJOToJsonUtility;
import org.forgerock.identity.openam.xacml.v3.commons.POJOToXmlUtility;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * XACMLRequestInformation
 * <p/>
 * Simple POJO to hold all relevant information related to a
 * XACML Operation Request.
 *
 * @author jeff.schenk@forgerock.com
 */
public class XACMLRequestInformation implements Serializable {
    /**
     * Define our Static resource Bundle for our debugger.
     */
    private static Debug debug = Debug.getInstance("amXACML");

    /**
     * Indicates if Request has been Processed or not.
     * Used when a XACMLAuthzDecisionQuery Wrapper of a Request
     * is presented.  The Request may have been already processed
     * within the wrapper processing.
     */
    private boolean requestProcessed;
    /**
     * Request Parsed correctly for either XML or JSON Data.
     */
    private boolean parsedCorrectly;
    /**
     * Meta Alias Information.
     */
    private String metaAlias;
    /**
     * Our PDP Entity ID.
     */
    private String pdpEntityID;
    /**
     * Realm.
     */
    private String realm;
    /**
     *
     */
    private boolean requestNodePresent;
    /**
     * Content Type
     */
    private ContentType contentType;
    /**
     * Original Request Content.
     */
    private String originalContent;
    /**
     * Content, can be either XML DOM or a JSON Object in form of a Map depending upon the specified ContentType.
     */
    private Object content;
    /**
     * Optional HTTP Digest Authorization Request
     */
    private String authenticationHeader;
    /**
     * Content, can be either XML or JSON depending upon the specified ContentType.
     * If this object is Null, we have an Anonymous/Guest Request.
     */
    private Object authenticationContent;
    /**
     *  Information obtained from actual HTTPServletRequest Object.
     */
    private String requestMethod;
    private String requestAuthenticationType;
    private Object requestUserPrincipal;
    private String requestContextPath;
    private String requestPathInfo;
    private String requestQueryString;
    private String requestURI;
    private String requestServletPath;
    private String requestProtocol;
    private String requestScheme;
    private String requestServerName;
    private int requestLocalPort;

    /**
     * XACMLAuthzDecisionQuery Inner Class.
     * <p/>
     * If this is an XACMLAuthzDecisionQuery, then the following fields
     * will be populated during Parsing of the XACMLAuthzDecisionQuery wrapper Document.
     * <p/>
     * These fields are:
     * &lt;attribute name="ID" type="ID" use="required"/>
     * example: ID="ID_1e469be0-ecc4-11da-8ad9-0800200c9a66"
     * &lt;attribute name="Version" type="string" use="required"/>
     * example: Version="2.0"
     * &lt;attribute name="IssueInstant" type="dateTime" use="required"/>
     * example: IssueInstant="2001-12-17T09:30:47.0Z"
     * &lt;attribute name="Destination" type="anyURI" use="optional"/>
     * &lt;attribute name="Consent" type="anyURI" use="optional"/>
     */
    public class XACMLAuthzDecisionQuery {
        private static final String ID_NAME = "id";
        private String id;

        private static final String VERSION_NAME = "version";
        private String version;

        private static final String ISSUE_INSTANT_NAME = "issueinstant";
        private String issueInstant;

        private static final String DESTINATION_NAME = "destination";
        private String destination;

        private static final String CONSTENT_NAME = "consent";
        private String consent;

        XACMLAuthzDecisionQuery() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getIssueInstant() {
            return issueInstant;
        }

        public void setIssueInstant(String issueInstant) {
            this.issueInstant = issueInstant;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getConsent() {
            return consent;
        }

        public void setConsent(String consent) {
            this.consent = consent;
        }

        /**
         * Set Field By Name, using Reflection.
         *
         * @param nodeName
         * @param nodeValue
         * @return boolean - indicator true if field set correctly, otherwise false.
         */
        public boolean setByName(final String nodeName, final String nodeValue) {
            if ((nodeName == null) || (nodeName.isEmpty()) || (nodeValue == null) || (nodeValue.isEmpty())) {
                return false;
            }
            if (nodeName.toLowerCase().contains(ID_NAME)) {
                this.setId(nodeValue);
                return true;
            } else if (nodeName.toLowerCase().contains(VERSION_NAME)) {
                this.setVersion(nodeValue);
                return true;
            } else if (nodeName.toLowerCase().contains(DESTINATION_NAME)) {
                this.setDestination(nodeValue);
                return true;
            } else if (nodeName.toLowerCase().contains(CONSTENT_NAME)) {
                this.setConsent(nodeValue);
                return true;
            } else if (nodeName.toLowerCase().contains(ISSUE_INSTANT_NAME)) {
                this.setIssueInstant(nodeValue);
                return true;
            }
            // Indicate Field Not Set.
            return false;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("XACMLAuthzDecisionQuery");
            sb.append("{id='").append(id).append('\'');
            sb.append(", version='").append(version).append('\'');
            sb.append(", issueInstant='").append(issueInstant).append('\'');
            sb.append(", destination='").append(destination).append('\'');
            sb.append(", consent='").append(consent).append('\'');
            sb.append('}');
            return sb.toString();
        }
    } // End of Inner Class.

    /**
     * Wrapper Object for the XACMLAuthzDecisionQuery Element Attributes
     */
    XACMLAuthzDecisionQuery xacmlAuthzDecisionQuery;
    /**
     * Indicates if this Request has been authenticated or not.
     */
    private boolean authenticated;
    /**
     * Response Field for Request.
     * Contains XACML Response Object.
     */
    private Response xacmlResponse = new Response();

    /**
     * Default Constructor.
     *
     * @param contentType
     * @param metaAlias
     * @param pdpEntityID
     * @param realm
     * @param request
     */
    public XACMLRequestInformation(ContentType contentType, String metaAlias, String pdpEntityID,
                                   String realm, HttpServletRequest request) {
        this.contentType = contentType;
        this.metaAlias = metaAlias;
        this.pdpEntityID = pdpEntityID;
        this.realm = realm;
        this.xacmlAuthzDecisionQuery = new XACMLAuthzDecisionQuery();
        // Save our Request Information.
        this.requestMethod = request.getMethod();
        this.requestAuthenticationType = request.getAuthType();
        this.requestUserPrincipal = request.getUserPrincipal();
        this.requestContextPath = request.getContextPath();
        this.requestPathInfo = request.getPathInfo();
        this.requestQueryString = request.getQueryString();
        this.requestURI = request.getRequestURI();
        this.requestServletPath = request.getServletPath();
        this.requestProtocol = request.getProtocol();
        this.requestScheme = request.getScheme();
        this.requestServerName = request.getServerName();
        this.requestLocalPort = request.getLocalPort();
        // Save our Original Content...
        // Get Raw Content Body.
        if (request.getContentLength() > 0) {
            this.originalContent = this.getRequestBody(request);
        }
        // Pull our Authentication Header if One Exists as a response from an initial Digest.
        this.setAuthenticationHeader(request.getHeader(XACML3Constants.AUTHORIZATION));
    }


    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getMetaAlias() {
        return metaAlias;
    }

    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
    }

    public String getPdpEntityID() {
        return pdpEntityID;
    }

    public void setPdpEntityID(String pdpEntityID) {
        this.pdpEntityID = pdpEntityID;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getAuthenticationHeader() {
        return authenticationHeader;
    }

    public void setAuthenticationHeader(String authenticationHeader) {
        this.authenticationHeader = authenticationHeader;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public XACMLAuthzDecisionQuery getXacmlAuthzDecisionQuery() {
        return xacmlAuthzDecisionQuery;
    }

    public void setXacmlAuthzDecisionQuery(XACMLAuthzDecisionQuery xacmlAuthzDecisionQuery) {
        this.xacmlAuthzDecisionQuery = xacmlAuthzDecisionQuery;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Object getAuthenticationContent() {
        return authenticationContent;
    }

    public void setAuthenticationContent(Object authenticationContent) {
        this.authenticationContent = authenticationContent;
    }

    public boolean isRequestNodePresent() {
        return requestNodePresent;
    }

    public void setRequestNodePresent(boolean requestNodePresent) {
        this.requestNodePresent = requestNodePresent;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestAuthenticationType() {
        return requestAuthenticationType;
    }

    public void setRequestAuthenticationType(String requestAuthenticationType) {
        this.requestAuthenticationType = requestAuthenticationType;
    }

    public Object getRequestUserPrincipal() {
        return requestUserPrincipal;
    }

    public void setRequestUserPrincipal(Object requestUserPrincipal) {
        this.requestUserPrincipal = requestUserPrincipal;
    }

    public String getRequestContextPath() {
        return requestContextPath;
    }

    public void setRequestContextPath(String requestContextPath) {
        this.requestContextPath = requestContextPath;
    }

    public String getRequestPathInfo() {
        return requestPathInfo;
    }

    public void setRequestPathInfo(String requestPathInfo) {
        this.requestPathInfo = requestPathInfo;
    }

    public String getRequestQueryString() {
        return requestQueryString;
    }

    public void setRequestQueryString(String requestQueryString) {
        this.requestQueryString = requestQueryString;
    }

    public String getRequestServletPath() {
        return requestServletPath;
    }

    public void setRequestServletPath(String requestServletPath) {
        this.requestServletPath = requestServletPath;
    }

    public String getRequestProtocol() {
        return requestProtocol;
    }

    public void setRequestProtocol(String requestProtocol) {
        this.requestProtocol = requestProtocol;
    }

    public String getRequestScheme() {
        return requestScheme;
    }

    public void setRequestScheme(String requestScheme) {
        this.requestScheme = requestScheme;
    }

    public String getRequestServerName() {
        return requestServerName;
    }

    public void setRequestServerName(String requestServerName) {
        this.requestServerName = requestServerName;
    }

    public int getRequestLocalPort() {
        return requestLocalPort;
    }

    public void setRequestLocalPort(int requestLocalPort) {
        this.requestLocalPort = requestLocalPort;
    }

    // **********************************************
    // Response Fields for Request
    // **********************************************

    public boolean isParsedCorrectly() {
        return parsedCorrectly;
    }

    public void setParsedCorrectly(boolean parsedCorrectly) {
        this.parsedCorrectly = parsedCorrectly;
    }

    /**
     * Produces String Content based upon Request Content Type.
     * @param requestContentType
     * @return
     */
    public String getXacmlStringResponseBasedOnContent(ContentType requestContentType) {
        if (requestContentType.commonType().equals(CommonType.XML)) {
            try {
                return POJOToXmlUtility.toXML(this.getXacmlResponse());
            } catch(Exception exception) {
                debug.error(this.getClass().getSimpleName()+" Exception performing POJOToXmlUtility.toXML: " +
                        ""+exception.getMessage(),exception);
                return null;
            }
        } else {
            try {
                return POJOToJsonUtility.toJSON(this.getXacmlResponse());
            } catch(Exception exception) {
                debug.error(this.getClass().getSimpleName()+" Exception performing POJOToJsonUtility.toXML: " +
                        ""+exception.getMessage(),exception);
                return null;
            }
        }
    }

    public Response getXacmlResponse() {
        if ( (this.xacmlResponse == null) || (this.xacmlResponse.getResult() == null) ||
             (this.xacmlResponse.getResult().isEmpty()) ) {
            return new XACMLDefaultResponse();
        } else {
            return this.xacmlResponse;
        }
    }

    public void setXacmlResponse(Response xacmlResponse) {
        if (this.xacmlResponse == null) {
            this.xacmlResponse =  new XACMLDefaultResponse();
        } else {
            this.xacmlResponse = xacmlResponse;
        }
    }

    public boolean isRequestProcessed() {
        return requestProcessed;
    }

    public void setRequestProcessed(boolean requestProcessed) {
        this.requestProcessed = requestProcessed;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("XACMLRequestInformation");
        sb.append("{requestProcessed=").append(requestProcessed);
        sb.append(", parsedCorrectly=").append(parsedCorrectly);
        sb.append(", metaAlias='").append(metaAlias).append('\'');
        sb.append(", pdpEntityID='").append(pdpEntityID).append('\'');
        sb.append(", realm='").append(realm).append('\'');
        sb.append(", requestNodePresent=").append(requestNodePresent);
        sb.append(", contentType=").append(contentType);
        sb.append(", originalContent='").append(originalContent).append('\'');
        sb.append(", content=").append(content);
        sb.append(", authenticationHeader='").append(authenticationHeader).append('\'');
        sb.append(", authenticated=").append(authenticated);
        sb.append(", authenticationContent=").append(authenticationContent);
        sb.append(", requestMethod='").append(requestMethod).append('\'');
        sb.append(", requestAuthenticationType='").append(requestAuthenticationType).append('\'');
        sb.append(", requestUserPrincipal=").append(requestUserPrincipal);
        sb.append(", requestContextPath='").append(requestContextPath).append('\'');
        sb.append(", requestPathInfo='").append(requestPathInfo).append('\'');
        sb.append(", requestQueryString='").append(requestQueryString).append('\'');
        sb.append(", requestURI='").append(requestURI).append('\'');
        sb.append(", requestServletPath='").append(requestServletPath).append('\'');
        sb.append(", requestProtocol='").append(requestProtocol).append('\'');
        sb.append(", requestScheme='").append(requestScheme).append('\'');
        sb.append(", requestServerName='").append(requestServerName).append('\'');
        sb.append(", requestLocalPort=").append(requestLocalPort);
        sb.append(", xacmlAuthzDecisionQuery=").append(xacmlAuthzDecisionQuery);
        sb.append(", xacmlResponse=").append(xacmlResponse);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Return the Request Body Content.
     *
     * @param request
     * @return String - Request Content Body.
     */
    private String getRequestBody(final HttpServletRequest request) {
        // Get the body content of the HTTP request,
        // remember we have no normal WS* SOAP Body, just String
        // data either XML or JSON.
        try {
            InputStream inputStream = request.getInputStream();
            return new Scanner(inputStream).useDelimiter("\\A").next();
        } catch (IOException ioe) {
            // Do Nothing...
        } catch (NoSuchElementException nse) {   // runtime exception.
            //Do Nothing...
        }
        return null;
    }

    /**
     * Get our Server URL construct from our incoming Request.
     *
     * @return - Base XACML URL yields "schema://serverName:LocalPort/contextPath/servletPath"
     */
    public final String getXacmlHome() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getRequestScheme());
        sb.append("://");
        sb.append(this.getRequestServerName());
        sb.append(":");
        sb.append(this.getRequestLocalPort());
        sb.append(this.getRequestContextPath());
        sb.append(this.getRequestServletPath());
        return sb.toString();
    }

}
