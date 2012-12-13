/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AuthXMLRequest.java,v 1.10 2009/08/17 21:17:50 mrudul_uchil Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.authentication.server;

import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.util.ISAuthConstants;
import java.util.List;

/**
 * The <code>AuthXMLRequest</code> represents the <code>AuthRequest</code> XML
 * document. The table shows the Request and the possible 
 * Responses for each Request:
 * <pre>
 * Request                 Response
 * ----------------------------------------------------------------------
 * NewAuthContext          LoginStatus or Exception
 * QueryInformation        QueryResult or Exception
 * Login                   GetRequirements or LoginStatus or Exception
 * SubmitRequirements      GetRequirements or LoginStatus or Exception
 * Logout                  LoginStatus or Exception
 * Abort                   LoginStatus or Exception
 * </pre>
 */
public class AuthXMLRequest {
    /**
     * Constant of Authentication request for new AuthContext 
     */
    public static final int NewAuthContext = 0;
    /**
     * Constant of Authentication request for login 
     */
    public static final int Login = 1;
    /**
     * Constant of Authentication request for submit auth requirements 
     */
    public static final int SubmitRequirements = 2;
    /**
     * Constant of Authentication request for query information 
     */
    public static final int QueryInformation = 3;
    /**
     * Constant of Authentication request for logout
     */
    public static final int Logout = 4;
    /**
     * Constant of Authentication request for abort
     */
    public static final int Abort = 5;
    /**
     * Constant of Authentication request for login index
     */
    public static final int LoginIndex = 6;
    /**
     * Constant of Authentication request for login principal
     */
    public static final int LoginPrincipal = 7;
    /**
     * Constant of Authentication request for login subject
     */
    public static final int LoginSubject = 8;

    private int requestType; 
    private String version=null;
    private String authIdentifier=null;
    private String appSSOTokenID=null;
    private String orgName=null;
    private String hostName=null;
    private String forceAuth=null;
    private boolean validSessionNoUpgrade=false;
    private String requestedInformation=null;
    private boolean isPCookie=false;
    private AuthContext.IndexType indexType=null;
    private String indexName=null;
    private String locale=null;
    private Principal principal;
    private char[] password;
    private Callback[] submittedCallbacks;
    private Subject subject;
    private String  params = null;
    private List env = null;
    AuthContextLocal authContext = null;
    HttpServletRequest servletRequest = null;
    HttpServletRequest clientRequest = null;
    HttpServletResponse clientResponse = null;
    //String origAuthIdentifier=null;

    static com.sun.identity.shared.debug.Debug debug =
        com.sun.identity.shared.debug.Debug.getInstance("amXMLHandler");

    /**
     * This method is used primarily at the server side to reconstruct
     * a <code>AuthXMLRequest</code> object based on the XML document
     * received from client. The DTD of this XML document is described above.
     *
     * @param xml The <code>AuthXMLRequest</code> XML.
     * @param req HTTP Servlet Request.
     * @return <code>AuthXMLRequest</code> if xml parsed without problem.
     * @throws AuthException if xml parsed with problem.
     */
    public static AuthXMLRequest parseXML(String xml, HttpServletRequest req)
        throws AuthException {
        debug.message("Calling AuthXMLRequestParser");
        /* if (debug.messageEnabled()) {
            debug.message("AuthXMLREquest: xmlString is" + xml);
        } */
        AuthXMLRequestParser authParser = new AuthXMLRequestParser(xml, req);
        debug.message("After AuthXMLRequestParser");
        return authParser.parseXML();
    }
 

    /**
     * Sets the request type.
     *
     * @param i Request type.
     */
    public void setRequestType(int i) {
        requestType = i;
    }

    /**
     * Sets the request version.
     *
     * @param version Version.
     */
    public void setRequestVersion(String version) {
        this.version = version;
    }

    /**
     * Sets the <code>authIdentifier</code> - session ID
     *
     * @param authIdentifier
     */
    public void setAuthIdentifier(String authIdentifier) {
        this.authIdentifier = authIdentifier;
    }

    /**
     * Sets the Application SSO Token id as received in PLL request
     *
     * @param appSSOTokenID Application SSOToken Id.
     *
     */
    public void setAppSSOTokenID(String appSSOTokenID) {
	this.appSSOTokenID = appSSOTokenID;
    }

    /**
     * Sets the organization name.
     *
     * @param orgName Organization Name.
     */
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    /**
     * Sets the host name.
     *
     * @param hostName Host Name.
     */
    public void setHostName(String hostName) {
	this.hostName = hostName;
    }

    /**
     * Sets the Force Auth attribute.
     *
     * @param aForceAuth Force Auth flag.
     */
    public void setForceAuth(String aForceAuth) {
	this.forceAuth = aForceAuth;
    }

    /**
     * Sets the attribute for valid session 
     * and no session upgrade in request.
     *
     * @param aValidSessionNoUpgrade Session is valid No upgrade needed.
     */
    public void setValidSessionNoUpgrade(boolean 
        aValidSessionNoUpgrade) {
	this.validSessionNoUpgrade = aValidSessionNoUpgrade;
    }

    /**
     * Sets the <code>requestinfo</code> - <code>moduleInstances</code>.
     *
     * @param requestInfo Request Information.
     */
    public void setRequestInformation(String requestInfo) {
         requestedInformation = requestInfo;
    }

    /**
     * Sets the index Type.
     *
     * @param strIndexType
     */
    public void setIndexType(String strIndexType) {
        if (strIndexType.equalsIgnoreCase("service")) {
            this.indexType = AuthContext.IndexType.SERVICE;
        } else if (strIndexType.equalsIgnoreCase("authLevel")) {
            this.indexType = AuthContext.IndexType.LEVEL;
        } else if (strIndexType.equalsIgnoreCase("role")) {
            this.indexType = AuthContext.IndexType.ROLE;
        } else if (strIndexType.equalsIgnoreCase("moduleInstance")) {
            this.indexType = AuthContext.IndexType.MODULE_INSTANCE;
        } else if (strIndexType.equalsIgnoreCase("user")) {
            this.indexType = AuthContext.IndexType.USER;
        } else if (strIndexType.equalsIgnoreCase("compositeAdvice")) {
            this.indexType = AuthContext.IndexType.COMPOSITE_ADVICE;
        } else if (strIndexType.equalsIgnoreCase(
            ISAuthConstants.IP_RESOURCE_ENV_PARAM)) {
            this.indexType = AuthContext.IndexType.RESOURCE;
        }
   }

    /**
     * Sets the locale
     *
     * @param locale locale setting.
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Sets the index name.
     *
     * @param indexName Index Name.
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * sets the principal for the class
     * @param className will have the principal.
     * @param principalValue will be set for the class.
     */
    public void setPrincipal(String className,String principalValue) {
        try {
            Class clName = Class.forName(className);
            principal = (Principal) clName.newInstance();
        } catch (ClassNotFoundException ce) {
            //debug.error("Error creating class instance " , e); 
        } catch (IllegalAccessException ia) {
        } catch (InstantiationException ie) {
        } catch (Exception e) {
        }
    }
  
    /**
     * Sets the password.
     *
     * @param password Password.
     */
    public void setPassword(String password) {
        this.password = password.toCharArray();
    }

    /**
     * Sets the submitted callbacks.
     *
     * @param submittedCallbacks Submitted callbacks.
     */
    public void setSubmittedCallbacks(Callback submittedCallbacks[]) {
        this.submittedCallbacks = submittedCallbacks;
    }

    /**
     * Sets the client request
     *
     * @param request The client Http Servlet Request
     */
    public void setClientRequest(HttpServletRequest request) {
       clientRequest = request;
    }

    /**
     * Sets the client response
     *
     * @param response The client Http Servlet Response
     */
    public void setClientResponse(HttpServletResponse response) {
       clientResponse = response;
    }

    /**
     * Gets the client request
     *
     * @return The client Http Servlet Request
     */
    public HttpServletRequest getClientRequest() {
       return clientRequest;
    }

    /**
     * Gets the client response
     *
     * @return The client Http Servlet Response
     */
    public HttpServletResponse getClientResponse() {
       return clientResponse;
    }

    /**
     * Returns the request type.
     *
     * @return the request type.
     */
    public int getRequestType() {
        return requestType;
    }

    /**
     * Returns the organization name.
     *
     * @return the organization name.
     */
    public String getOrgName() {
        return orgName;
    }

    /**
     * Returns the host name.
     *
     * @return the host name.
     */
    public String getHostName() {
 	return hostName;
    }

    /**
     * Returns the force auth flag.
     *
     * @return the force auth flag.
     */
    public String getForceAuth() {
 	return forceAuth;
    }

    /**
     * Returns the attribute for valid session 
     * and no session upgrade in request.
     *
     * @return aValidSessionNoUpgrade.
     */
    public boolean getValidSessionNoUpgrade() {
	return validSessionNoUpgrade;
    }

    /**
     * Returns the authentication Identifier - session ID
     *
     * @return authentication identifier.
     */
    public String getAuthIdentifier() {
        return authIdentifier;
    }

    /**
     * Returns the Application SSO Token id as set by 
     * <code>setAppSSOTokenID</code>
     *
     * @return Application SSO Token id
     *
     */
    public String getAppSSOTokenID() {
        return appSSOTokenID;
    }
 
   /**
     * Returns the callbacks set by client.
     *
     * @return the callbacks set by client.
     */
   public Callback[] getSubmittedCallbacks() {
        return submittedCallbacks;
   }

    /**
     * Returns the index type.
     *
     * @return the index type.
     */
    public AuthContext.IndexType getIndexType() {
        return indexType;
    }

    /**
     * Returns the locale.
     *
     * @return the locale.
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Returns the index name.
     *
     * @return the index name.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Returns the principal.
     *
     * @return the principal.
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * Returns the password.
     *
     * @return the password.
     */
    public char[] getPassword() {
        return password;
    }

    /**
     * Returns the subject.
     *
     * @return the subject.
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * Sets the authentication context for this request.
     *
     * @param authContext Authentication context for this request.
     */
    public void setAuthContext(AuthContextLocal authContext) {
        this.authContext = authContext;
    }

    /**
     * Returns the authentication context for this request.
     *
     * @return the authentication context for this request.
     */
    public AuthContextLocal getAuthContext() {
        return authContext;
    }

    /**
     * Sets the subject.
     *
     * @param subject Subject.
     */
    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    /**
     * Sets the servlet request.
     *
     * @param servletRequest Servlet request.
     */
    public void setHttpServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    /**
     * Returns the servlet request.
     *
     * @return the servlet request.
     */
    public HttpServletRequest getHttpServletRequest() {
        return servletRequest;
    }

    /**
     * Sets the request parameters.
     * @param params will be set for the request.
     */
    public void setParams(String params) {
        this.params = params;
    }

    /**
     * Returns the request parameters.
     * @return the request parameters.
     */
    public String getParams() {
        return params;
    }
    
    /**
     * Sets the environment for the request.
     * @param env environment to be set for the request.
     */
    public void setEnvironment(List env) {
        this.env = env;
    }

    /**
     * Returns the environment setting for the request.
     * @return the environment setting for the request.
     */
    public List getEnvironment() {
        return env;
    }    
}

