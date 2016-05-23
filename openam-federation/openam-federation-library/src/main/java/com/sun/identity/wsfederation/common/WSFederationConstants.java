/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WSFederationConstants.java,v 1.13 2009/12/14 23:42:48 mallas Exp $
 *
 * Portions copyright 2016 ForgeRock AS.
 */

package com.sun.identity.wsfederation.common;

/**
 * <code>WSFederationConstants</code> defines various constants for the 
 * WS-Federation implementation
 */
public final class WSFederationConstants {
    /**
     * WS-Federation data store provider name.
     */ 
    public static final String WSFEDERATION = "wsfederation";
    /**
     * WS-Federation 'sign-in' action.
     */ 
    public static final String WSIGNIN10 = "wsignin1.0";
    /**
     * WS-Federation 'sign-out' action.
     */ 
    public static final String WSIGNOUT10 = "wsignout1.0";
    /**
     * WS-Federation 'sign-out cleanup' action. This is handled identically
     * to <code>WSIGNOUT10</code>, following the WS-Federation 1.1 
     * specification.
     */ 
    public static final String WSIGNOUTCLEANUP10 = "wsignoutcleanup1.0";
    /**
     * XML tag name for <code>&lt;RequestedSecurityToken%gt;</code>.
     */ 
    public static final String RST_TAG_NAME = "RequestedSecurityToken";
    /**
     * XML tag name for <code>&lt;RequestSecurityTokenResponse%gt;</code>.
     */ 
    public static final String RSTR_TAG_NAME = "RequestSecurityTokenResponse";
    /**
     * XML tag name for <code>&lt;AppliesTo%gt;</code>.
     */ 
    public static final String APPLIESTO_TAG_NAME = "AppliesTo";
    /**
     * XML tag name for <code>&lt;Address%gt;</code>.
     */ 
    public static final String ADDRESS_TAG_NAME = "Address";
    /**
     * SAML 1.1 URN.
     */ 
    public static final String URN_OASIS_NAMES_TC_SAML_11 = 
        "urn:oasis:names:tc:SAML:1.1";
    /**
     * Claim URI.
     */ 
    public static final String CLAIMS_URI = 
        "http://schemas.xmlsoap.org/claims";
    /**
     * Group claim URI.
     */ 
    public static final String CLAIMS_GROUP_URI = 
        "http://schemas.xmlsoap.org/claims/Group";
    /**
     * WS-Addressing URI.
     */ 
    public static final String WS_ADDRESSING_URI = 
        "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    /**
     * Display name for Group claim.
     */ 
    public static final String CLAIMS_GROUP_DISPLAY_NAME = 
        "Group";
    
    /**
     * Configuration attribute for account realm selection mechanism.
     */ 
    public static final String ACCOUNT_REALM_SELECTION = 
        "AccountRealmSelection";
    /**
     * Configuration attribute for account realm cookie name.
     */ 
    public static final String ACCOUNT_REALM_COOKIE_NAME = 
        "AccountRealmCookieName";
    /**
     * Configuration attribute for home realm discovery URL.
     */ 
    public static final String HOME_REALM_DISCOVERY_SERVICE = 
        "HomeRealmDiscoveryService";

    /**
     * Configuration attribute - does the service provider require
     * a signature on the assertion? Default is true.
     */
    public static final String WANT_ASSERTION_SIGNED = "wantAssertionSigned";

    /**
     * Configuration attribute for provider display name.
     */ 
    public static final String DISPLAY_NAME = 
        "displayName";
    /**
     * Configuration attribute for default UPN domain.
     */ 
    public static final String UPN_DOMAIN = 
        "upnDomain";
    /**
     * Default value for account realm cookie name
     */ 
    public static final String ACCOUNT_REALM_COOKIE_NAME_DEFAULT = 
        "amWSFederationAccountRealm";
    /**
     * User agent HTTP header name
     */ 
    public static final String USERAGENT = "user-agent";
    /**
     * Cookie HTTP header name
     */ 
    public static final String COOKIE = "cookie";
    /**
     * Default mechanism for carrying account realm
     */ 
    public static final String ACCOUNT_REALM_SELECTION_DEFAULT = COOKIE;
    
    /**
     * NameID info attribute.
     */ 
    public static final String NAMEID_INFO = "sun-fm-wsfed-nameid-info";

    /**
     * NameID info key attribute.
     */
    public static final String NAMEID_INFO_KEY = "sun-fm-wsfed-nameid-infokey";
    
    /**
     * Session property name for list of service provider to which this identity
     * provider has sent a token
     */
    public static final String SESSION_SP_LIST = "sun-fm-wsfed-sp-list";
    
    /**
     * Session property name for identity provider from which this service
     * provider has received a token
     */
    public static final String SESSION_IDP = "sun-fm-wsfed-idp";
    
    /**
     * Attribute name for communicating form action URL from servlet to JSP
     */
    public static final String POST_ACTION = 
        "com.sun.identity.wsfederation.post.action";
    /**
     * Attribute name for communicating WS-Federation wa parameter from servlet 
     * to JSP
     */
    public static final String POST_WA =
        "com.sun.identity.wsfederation.post.wa";
    /**
     * Attribute name for communicating WS-Federation wctx parameter from 
     * servlet to JSP
     */
    public static final String POST_WCTX =
        "com.sun.identity.wsfederation.post.wctx";
    /**
     * Attribute name for communicating WS-Federation wresult parameter from 
     * servlet to JSP
     */
    public static final String POST_WRESULT =
        "com.sun.identity.wsfederation.post.wresult";
    
    /**
     * Attribute name for communicating local provider display name from 
     * servlet to JSP
     */
    public static final String LOGOUT_DISPLAY_NAME =
        "com.sun.identity.wsfederation.logout.displayname";
    /**
     * Attribute name for communicating WS-Federation wreply parameter from 
     * servlet to JSP
     */
    public static final String LOGOUT_WREPLY =
        "com.sun.identity.wsfederation.logout.wreply";
    /**
     * Attribute name for communicating list of providers from 
     * servlet to JSP
     */
    public static final String LOGOUT_PROVIDER_LIST =
        "com.sun.identity.wsfederation.logout.providerlist";
    /**
     * Debug log name.
     */
    public static String AM_WSFEDERATION = "libWSFederation";
    /**
     * Resource bundle name.
     */
    public static final String BUNDLE_NAME = "libWSFederation";
    /**
     * Constant used to identify meta alias in URL.
     */
    public static final String NAME_META_ALIAS_IN_URI = "metaAlias";
    /**
     * Entity ID to use if WS-Federation omits it.
     */
    public static final String DEFAULT_FEDERATION_ID = 
        "sunFMWSFederationDefaultFederationID";
    /**
     * WS-Federation HTTP parameter for 'action'.
     */
    public static final String WA = "wa";
    /**
     * WS-Federation HTTP parameter for 'result'.
     */
    public static final String WRESULT = "wresult";
    /**
     * WS-Federation HTTP parameter for 'home realm'.
     */
    public static final String WHR = "whr";
    /**
     * WS-Federation HTTP parameter for 'requesting realm'.
     */
    public static final String WTREALM = "wtrealm";
    /**
     * WS-Federation HTTP parameter for 'destination url'.
     */
    public static final String WREPLY = "wreply";
    /**
     * WS-Federation HTTP parameter for 'current time'.
     */
    public static final String WCT = "wct";
    /**
     * WS-Federation HTTP parameter for 'context value'.
     */
    public static final String WCTX = "wctx";
    /**
     * HTTP request parameter for OpenAM realm
     */
    public static final String REALM_PARAM = "famrealm";
    /**
     * HTTP request parameter for ws-federation entity id
     */
    public static final String ENTITYID_PARAM = "wsfed-entityid";     
    /**
     * Claim type URIs, as defined in 'WS-Federation: Passive Requestor 
     * Interoperability Profile' document
     */
    public static final String[] NAMED_CLAIM_TYPES = {
        "http://schemas.xmlsoap.org/claims/UPN", 
        "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress", 
        "http://schemas.xmlsoap.org/claims/CommonName" };
    /**
     * Claim type Displey names
     */
    public static final String[] NAMED_CLAIM_DISPLAY_NAMES = {
        "UPN", 
        "Email Address", 
        "Common Name" };
    /**
     * Index into NAMED_CLAIM_TYPES and NAMED_CLAIM_DISPLAY_NAMES arrays for UPN
     */
    public static final int NAMED_CLAIM_UPN = 0;
    /**
     * Index into NAMED_CLAIM_TYPES and NAMED_CLAIM_DISPLAY_NAMES arrays for 
     * Email Address
     */
    public static final int NAMED_CLAIM_EMAILADDRESS = 1;
    /**
     * Index into NAMED_CLAIM_TYPES and NAMED_CLAIM_DISPLAY_NAMES arrays for 
     * Common Name
     */
    public static final int NAMED_CLAIM_COMMONNAME = 2;
    
    /**
     * Configuration attribute for NameID attribute.
     */ 
    public static final String NAMEID_ATTRIBUTE = "nameIdAttribute";
    
    /**
     * Configuration attribute for NameID format.
     */ 
    public static final String NAMEID_FORMAT = "nameIdFormat";
    
    /**
     * Configuration attribute for Trim domain.
     */ 
    public static final String NAME_INCLUDES_DOMAIN = "nameIncludesDomain";
    
    /**
     * Configuration attribute for Domain attribute.
     */ 
    public static final String DOMAIN_ATTRIBUTE = "domainAttribute";
    
    /**
     * The default attribute for short user name
     */
    public static final String UID = "uid";
    
    /**
     * The URL prefix for WS-Fed metadata requests
     */
    public static final String METADATA_URL_PREFIX = 
        "/FederationMetadata/2006-12";
    
    /**
     * The URL suffix for WS-Fed metadata requests
     */
    public static final String METADATA_URL_SUFFIX = "/FederationMetadata.xml";

    /**
     * The URL prefix for WS-Fed MEX requests.
     */
    public static final String MEX_ENDPOINT_PREFIX = "/WSFederationServlet/ws-trust/mex";

    /**
     * The URL prefix for WS-Fed STS requests.
     */
    public static final String STS_ENDPOINT_PREFIX = "/WSFederationServlet/sts";

     /**
     * WS-Policy URI.
     */
    public static final String WS_POLICY_URI =
                     "http://schemas.xmlsoap.org/ws/2004/09/policy";

    /**
     * List of valid wreply Urls
     */
    public static String WREPLY_URL_LIST = "wreplyList";

    /**
     * The XML namespace for WS-Addressing.
     */
    public static final String WSA_NAMESPACE = "http://www.w3.org/2005/08/addressing";

    /**
     * The XML namespace for WS-Security.
     */
    public static final String WSSE_NAMESPACE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    /**
     * The XML namespace for WS-Trust.
     */
    public static final String WST_NAMESPACE = "http://schemas.xmlsoap.org/ws/2005/02/trust";

    /**
     * The XML namespace for WS-Security Utility.
     */
    public static final String WSU_NAMESPACE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    /**
     * Configuration attribute for Active Requestor Profile Enabled setting.
     */
    public static String ACTIVE_REQUESTOR_PROFILE_ENABLED = "activeRequestorProfileEnabled";

    /**
     * Configuration attribute for Endpoint Base URL setting.
     */
    public static String ENDPOINT_BASE_URL = "endpointBaseUrl";

    /**
     * Configuration attribute for Trusted Addresses setting.
     */
    public static String TRUSTED_ADDRESSES = "trustedAddresses";

    /**
     * Configuration attribute for Authenticator Class setting.
     */
    public static String AUTHENTICATOR_CLASS = "authenticatorClass";
}
