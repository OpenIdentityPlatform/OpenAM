/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SAMLConstants.java,v 1.17 2009/06/12 22:21:39 mallas Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */

package com.sun.identity.saml.common;

import java.util.HashSet;
import java.util.Set;

/**
 * This is a common class defining some constants common to all SAML elements.
 *
 * @supported.api
 */
public final class SAMLConstants 
{
    public static Set passwordAuthMethods = null;
    public static Set tokenAuthMethods = null;
    static {
        passwordAuthMethods = new HashSet();
        passwordAuthMethods.add("nt");
        passwordAuthMethods.add("ldap");
        passwordAuthMethods.add("membership");
        passwordAuthMethods.add("anonymous");
        tokenAuthMethods = new HashSet();
        tokenAuthMethods.add("radius");
    }

    /**
     * String to identify a quote.
     */
    public static final String QUOTE                    = "\"";

    /**
     * String to identify a new line charactor.
     */
    public static final String NL                       = "\n";

    /**
     * String to identify a left angle.
     */
    public static final String LEFT_ANGLE              = "<";

    /**
     * String to identify a right angle.
     */
    public static final String RIGHT_ANGLE              = ">";

    /**
     * String to identify "/>".
     */
    public static final String END_ELEMENT              = "/>";

    /**
     * String to identify "&lt;/".
     */
    public static final String START_END_ELEMENT = "</";

    /**
     * String to identify a space charactor.
     */
    public static final String SPACE                = " ";

    /**
     * SAML assertion namespace URI.
     *
     * @supported.api
     */
    public static final String assertionSAMLNameSpaceURI = 
                "urn:oasis:names:tc:SAML:1.0:assertion";
    /**
     * SOAP 1.1 namespace URI.
     *
     * @supported.api
     */
    public static final String SOAP_URI =
                "http://schemas.xmlsoap.org/soap/envelope/";
    
    /**
     * SOAP 1.2 namespace URI.
     *
     * @supported.api
     */
    public static final String SOAP12_URI =
                "http://www.w3.org/2003/05/soap-envelope";
    /**
     * SOAP envelope prefix.
     */
    public static final String SOAP_ENV_PREFIX = "soap-env";

    /**
     * SAML request-response protocol namespace URI.
     *
     * @supported.api
     */
    public static final String PROTOCOL_NAMESPACE_URI =
                        "urn:oasis:names:tc:SAML:1.0:protocol";

    /**
     * XML Digital Signature namespace.
     *
     * @supported.api
     */
    public static final String XMLSIG_NAMESPACE_URI =
                        "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Element name for xml signature.
     */
    public static final String XMLSIG_ELEMENT_NAME = "Signature";

    /**
     * Pointer to Signature name space.
     */
    public static final String DSSAMLNameSpace = 
        "xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"";
   
    /**
     * String which gets incorporated into
     * <code>toString(includeNS, declareNS)</code> when 
     * <code>declareNS</code> is true for any assertion element.
     */
    public static final String assertionDeclareStr = 
        " xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"";

    /**
     * String used in the <code>toString(includeNS, declareNS)</code> method.
     */
    public static final String PROTOCOL_NAMESPACE_STRING =
        " xmlns:samlp=\"urn:oasis:names:tc:SAML:1.0:protocol\"";

    /** 
     * <code>NameQualifier</code> in <code>NameIdetifier</code> for
     * <code>X509SubjectName</code>.
     */
    public static final String X509SUBJECTNAME =
        assertionSAMLNameSpaceURI+"#X509SubjectName";

    /**
     * String used in the <code>ActionNamespace</code> attribute to refer to
     * common sets of actions to perform on resources. 
     *
     * Title: Read/Write/Execute/Delete/Control
     * Defined actions: <code>Read Write Execute Delete Control</code>
     * These actions are interpreted in the normal manner, i.e. 
     * <ul>
     * <li><code>Read</code>: The subject may read the resource </li>
     * <li><code>Write</code>: The subject may modify the resource </li>
     * <li><code>Execute</code>: The subject may execute the resource </li>
     * <li><code>Delete</code>: The subject may delete the resource </li>
     * <li><code>Control</code>: The subject may specify the access control
     *     policy for the resource.</li>
     * </ul>
     *
     * @supported.api
     */
    public static final String ACTION_NAMESPACE = 
        "urn:oasis:names:tc:SAML:1.0:action:rwedc";

    /**
     * String used in the <code>ActionNamespace</code> attribute to refer to
     * common sets of actions to perform on resources. 
     *
     * Title: Read/Write/Execute/Delete/Control with Negation
     * Defined actions:
     * <code>Read Write Execute Delete Control ~Read ~Write ~Execute ~Delete
     * ~Control</code>
     * <ul>
     * <li><code>Read</code>: The subject may read the resource </li>
     * <li><code>Write</code>: The subject may modify the resource </li>
     * <li><code>Execute</code>: The subject may execute the resource </li>
     * <li><code>Delete</code>: The subject may delete the resource </li>
     * <li><code>Control</code>: The subject may specify the access control
     *     policy for the resource </li>
     * <li><code>~Read</code>:  The subject may NOT read the resource </li>
     * <li><code>~Write</code>: The subject may NOT modify the resource </li>
     * <li><code>~Execute</code>: The subject may NOT execute the resource </li>
     * <li><code>~Delete</code>: The subject may NOT delete the resource </li>
     * <li><code>~Control</code>: The subject may NOT specify the access
     *     control policy for the resource </li>
     * </ul>
     * An application MUST NOT authorize both an action and its negated form.
     *
     * @supported.api
     */
    public static final String ACTION_NAMESPACE_NEGATION = 
                "urn:oasis:names:tc:SAML:1.0:action:rwedc-negation";

    /**
     * String used in the <code>ActionNamespace</code> attribute to refer to
     * common sets of actions to perform on resources. 
     *
     * Title: <code>Get/Head/Put/Post</code>
     * Defined actions: 
     *          <code>GET HEAD PUT POST</code>
     * These actions bind to the corresponding HTTP operations. For example a
     * subject authorized to perform the GET action on a resource is authorized
     * to retrieve it. The GET and HEAD actions loosely correspond to the 
     * conventional read permission and the PUT and POST actions to the write 
     * permission. The correspondence is not exact however since a HTTP GET 
     * operation may cause data to be modified and a POST operation may cause
     * modification to a resource other than the one specified in the request. 
     * For this reason a separate Action URI specifier is provided. 
     *
     * @supported.api
     */
    public static final String ACTION_NAMESPACE_GHPP = 
                "urn:oasis:names:tc:SAML:1.0:ghpp";

    /**
     * String used in the <code>ActionNamespace<code> attribute to refer to
     * common sets of actions to perform on resources. 
     *
     * Title: UNIX File Permissions
     * Defined actions: 
     * The defined actions are the set of UNIX file access permissions
     * expressed in the numeric (octal) notation. The action string is a four
     * digit numeric code: extended user group world 
     * Where the extended access permission has the value  
     * <ul>
     * <li><code>+2 if sgid is set</code>
     * <li><code>+4 if suid is set</code>
     * The user group and world access permissions have the value 
     * <li><code>+1 if execute permission is granted</code>
     * <li><code>+2 if write permission is granted</code>
     * <li><code>+4 if read permission is granted</code>
     * For example 0754 denotes the UNIX file access permission: user read,
     * write and execute, group read and execute and world read. 
     *
     * @supported.api
     */
    public static final String ACTION_NAMESPACE_UNIX = 
                "urn:oasis:names:tc:SAML:1.0:action:unix";

    /**
     * saml namespace prefix with ":".
     */
    public static final String ASSERTION_PREFIX = "saml:";

    /**
     * samlp namespace prefix with ":".
     */
    public static final String PROTOCOL_PREFIX = "samlp:";

    /**
     * Assertion version 1.0.
     */
    public static final String ASSERTION_VERSION_1_0 = "1.0";

    /**
     * Assertion version 1.1.
     */
    public static final String ASSERTION_VERSION_1_1 = "1.1";

    /**
     * Major version of assertion.
     */
    public static final int ASSERTION_MAJOR_VERSION = 1;
    /**
     * Default Assertion minor version.
     */
    public static int ASSERTION_MINOR_VERSION = 1;

    /**
     * Assertion minor version 0.
     */
    public static final int ASSERTION_MINOR_VERSION_ZERO = 0;

    /**
     * Assertion minor version 1.
     */
    public static final int ASSERTION_MINOR_VERSION_ONE = 1;

    /**
     * Protocol version 1.0.
     */
    public static final String PROTOCOL_VERSION_1_0 = "1.0";

    /**
     * Protocol version 1.1.
     */
    public static final String PROTOCOL_VERSION_1_1 = "1.1";

    /**
     * Protocol major version 1.
     */
    public static final int PROTOCOL_MAJOR_VERSION = 1;

    /**
     * Default protocol minor version 1.
     */
    public static int PROTOCOL_MINOR_VERSION = 1;

    /**
     * Protocol minor version 0.
     */
    public static final int PROTOCOL_MINOR_VERSION_ZERO = 0; 

    /**
     * Protocol minor version 1.
     */
    public static final int PROTOCOL_MINOR_VERSION_ONE = 1;  
 
    /**
     * Assertion handle, request id, and response id have this length.
     * If server id cannot be found, assertion id has this length also.
     * request id, response id, and assertion id will be base64 encoded for
     * printing.
     */
    public static final int ID_LENGTH = 20;

    /**
     * Length for server id. Used in load balancing.
     */
    public static final int SERVER_ID_LENGTH = 2;

    /**
     * Default encoding used in the xml string conversion.
     */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * SAML artifact confirmation method identifier URI.
     *
     * @supported.api
     */
    public static String CONFIRMATION_METHOD_ARTIFACT =
                "urn:oasis:names:tc:SAML:1.0:cm:artifact";

    /**
     * Deprecated SAML Artifact confirmation method identifier URI.
     *
     * @supported.api
     */
    public static final String DEPRECATED_CONFIRMATION_METHOD_ARTIFACT =
                "urn:oasis:names:tc:SAML:1.0:cm:artifact-01";

    /**
     * OpenAM confirmation method identifier URI.
     *
     * @supported.api
     */
    public static final String CONFIRMATION_METHOD_IS =
                "urn:com:sun:identity";

    /**
     * SAML Bearer confirmation method identifier URI.
     *
     * @supported.api
     */
    public static final String CONFIRMATION_METHOD_BEARER =
                "urn:oasis:names:tc:SAML:1.0:cm:bearer";

    /**
     * SAML "Holder of Key" confirmation method identifier URI.
     *
     * @supported.api
     */
    public static final String CONFIRMATION_METHOD_HOLDEROFKEY =
                "urn:oasis:names:tc:SAML:1.0:cm:holder-of-key";

    /**
     * SAML "Sender Vouches" confirmation method identifier URI.
     *
     * @supported.api
     */
    public static final String CONFIRMATION_METHOD_SENDERVOUCHES =
                "urn:oasis:names:tc:SAML:1.0:cm:sender-vouches";

    // used by SAML service schema related operations

    /**
     * SAML service name.
     */
    public static final String SAML_SERVICE_NAME = "SAML1";

    /**
     * 1.0 version of SAML service.
     */
    public static final String SAML_SERVICE_VERSION = "1.0";

    /**
     * SAML service attribute that specifies time skew for not before attribute
     * in assertion.
     */
    public static final String NOTBEFORE_TIMESKEW_NAME =
                                        "iplanet-am-saml-notbefore-timeskew";

    /**
     * SAML service attribute that specifies artifact timeout period.
     */
    public static final String ARTIFACT_TIMEOUT_NAME =
                                        "iplanet-am-saml-artifact-timeout";

    /**
     * SAML service attribute that specifies assertion timeout period.
     */
    public static final String ASSERTION_TIMEOUT_NAME =
                                        "iplanet-am-saml-assertion-timeout";

    /**
     * SAML service attribute that specifies whether needs to remove
     * assertion after it's being dereferenced or not.
     */
    public static final String REMOVE_ASSERTION_NAME =
                                        "com.sun.identity.saml.removeassertion";

    /**
     * Default not before time skew. It's in seconds.
     */
    public static final int NOTBEFORE_TIMESKEW_DEFAULT = 300;

    /**
     * Default artifact timeout period. It's in seconds.
     */
    public static final int ARTIFACT_TIMEOUT_DEFAULT = 120;

    /**
     * Default assertion timeout period. It's in seconds.
     */
    public static final int ASSERTION_TIMEOUT_DEFAULT = 60;

    /**
     * SAML service attribute that specifies list of saml site IDs.
     */
    public static final String SITE_ID_LIST = "iplanet-am-saml-siteid-list"; 

    /**
     * SAML service attribute that specifies list of instances.
     */
    public static final String INSTANCE_LIST = "iplanet-am-saml-instance-list"; 

    /**
     * This site's ID.
     */
    public static final String SITE_ID = "mysiteid"; 

    /**
     * SAML service attribute that specifies list of issuer names.
     */
    public static final String ISSUER_NAME_LIST =
                                        "iplanet-am-saml-issuername-list";

    /**
     * This site's issuer name.
     */
    public static final String ISSUER_NAME = "mysitename";

    /**
     * SAML service attribute name for list of site ID and issuer names.
     */
    public static final String SITE_ID_ISSUER_NAME_LIST =
                                   "iplanet-am-saml-siteid-issuername-list";

    /**
     * Constant for instance id.
     */
    public static final String INSTANCEID = "INSTANCEID";

    /**
     * Constant for site id.
     */
    public static final String SITEID = "SITEID";

    /**
     * Constant for issuer name.
     */
    public static final String ISSUERNAME = "ISSUERNAME";

    /**
     * SAML service attribute that specifies site's certificate alias.
     */
    public static final String SITE_CERT_ALIAS =
                                        "iplanet-am-saml-sitecertalias";

    /**
     * SAML service attribute that specifies whether to sign a request or not.
     */
    public static final String SIGN_REQUEST =
                                        "iplanet-am-saml-signrequest";

    /**
     * Default is to not sign request.
     */
    public static final String SIGN_REQUEST_DEFAULT = "false";

    /**
     * SAML service attribute that specifies whether to sign a response or not.
     */
    public static final String SIGN_RESPONSE =
                                        "iplanet-am-saml-signresponse";

    /**
     * Default is to not sign response.
     */
    public static final String SIGN_RESPONSE_DEFAULT = "false";

    /**
     * SAML service attribute that specifies whether to sign an assertion
     * or not.
     */
    public static final String SIGN_ASSERTION =
                                        "iplanet-am-saml-signassertion";

    /**
     * Default is to not sign the assertion.
     */
    public static final String SIGN_ASSERTION_DEFAULT = "false";

    /**
     * SAML service attribute that specifies the name for artifact.
     */
    public static final String ARTIFACT_NAME = "iplanet-am-saml-artifact-name";

    /**
     * SAML service attribute that specifies the name id format attribute map.
     */
    public static final String NAME_ID_FORMAT_MAP =
            "iplanet-am-saml-name-id-format-attr-map";

    /**
     * SAML service attribute that specifies the attribute map.
     */
    public static final String ATTRIBUTE_MAP =
            "iplanet-am-saml-attr-map";

    /**
     * Default SAML artifact name.
     *
     * @supported.api
     */
    public static final String ARTIFACT_NAME_DEFAULT = "SAMLart" ;

    /**
     * SAML service attribute that specifies target parameter.
     */
    public static final String TARGET_SPECIFIER = 
                                           "iplanet-am-saml-target-specifier" ; 

    /**
     * Default parameter for target.
     */
    public static final String TARGET_SPECIFIER_DEFAULT = "TARGET" ;

    /**
     * SAML service attribute that specifies list of trusted partners.
     * A sample trusted server list entry is like
     * target=.iplanet.com:8080|
     *      SAMLUrl=http://dsame.red.iplanet.com:8080/amserver/SAMLAwareServlet|
     * POSTUrl=http://dsame.red.iplanet.com:8080/amserver/SAMLPOSTProfileServlet
     */
    public static final String TRUSTED_SERVER_LIST =
                                        "iplanet-am-saml-trusted-server-list"; 

    /**
     * SAML service attribute that specifies partner urls.
     * A sample Parter URL list entry is like: 
     * SourceID=encoded 20 bytes|
     * SOAPUrl=http://dsame2.red.iplanet.com:8080/amserver/SAMLSOAPReceiver
     */
    public static final String PARTNER_URLS = "iplanet-am-saml-partner-urls"; 

    /**
     * Constants for target.
     */
    public static final String TARGET = "TARGET"; 

    /**
     * Constants for url which handles saml web browser artifact profile.
     */
    public static final String SAMLURL = "SAMLURL"; 

    /**
     * Constants for url which handles saml web browser post profile.
     */
    public static final String POSTURL = "POSTURL";
    

    /**
     * A SAML service attribute parameter. The 20-byte sequence defined as in
     * the SiteID and Issuer name.
     *
     * @supported.api
     */
    public static final String SOURCEID = "SOURCEID"; 

    /**
     * A SAML service attribute parameter that defines partner's name.
     *
     * iPlanet-PUBLIC-STATIC
     */
    public static final String PARTNERNAME = "PARTNERNAME"; 

    /**
     * A SAML service attribute parameter that defines the URL that provides
     * the SAML service. The servlet specified in the URL implements the
     * Web-browser SSO with Artifact profile defined in the OASIS-SAML
     * Bindings and Profiles specification.
     *
     * @supported.api
     */
    public static final String SOAPUrl = "SOAPURL"; 

    /**
     * A SAML service attribute parameter that defines the authentication type
     * used in SAML. It should be one of the following:
     *     NOAUTH
     *     BASICAUTH
     *     SSL
     *     SSLWITHBASICAUTH
     * This parameter is optional, and if not specified, the default is NOAUTH.
     * If BASICAUTH or SSLWITHBASICAUTH is specified, the User parameter is
     * require and the SOAPUrl should be HTTPS. 
     *
     * @supported.api
     */
    public static final String AUTHTYPE = "AUTHTYPE"; 

    /**
     * A SAML service attribute parameter that defines the uid of the partner
     * which is used to protect the partner's SOAP Receiver
     *
     * @supported.api
     */
    public static final String UID = "USER"; 

    /**
     * A SAML service attribute parameter that defines the user id of the
     * partner which is used for Basic Authentication.
     *
     * @supported.api
     */
    public static final String AUTH_UID = "AUTHUSER"; 

    /**
     * A SAML service attribute parameter that defines the password of the
     * partner which is used for Basic Authentication.
     *
     * @supported.api
     */
    public static final String AUTH_PASSWORD = "AUTHPASSWORD"; 

    /**
     * A SAML service attribute parameter that specifies a pluggable class
     * which defines how the subject of an Assertion is related to an identity
     * at the destination site. By default, it is:
     *     com.sun.identity.saml.plugins.DefaultAccountMapper
     *
     * @supported.api
     */
    public static final String ACCOUNTMAPPER = "ACCOUNTMAPPER";

    /**
     * A SAML service attribute parameter that specifies a pluggable class
     * which defines how the subject of an Assertion is related to an identity
     * at the destination site and a target URL. By default, it is:
     *     com.sun.identity.saml.plugins.DefaultPartnerAccountMapper
     *
     * @supported.api
     */
    public static final String PARTNERACCOUNTMAPPER = "PARTNERACCOUNTMAPPER";

    /**
     * A SAML service attribute parameter that specifies the class with the
     * path where the siteAttributeMapper is located. Applications can develop
     * a siteAttributeMapper to obtain attributes to be included in the
     * assertion during SSO. If no siteAttributeMapper is found, then no
     * attributes will be included in the assertion during SSO. 
     *
     * @supported.api
     */
    public static final String SITEATTRIBUTEMAPPER = "siteattributemapper";
    public static final String SITEATTRIBUTEMAPPERDEFAULT =
            "com.sun.identity.saml.plugins.DefaultSiteAttributeMapper";
    
    /**
     * A SAML service attribute parameter that specifies the class with the
     * path where the partnerSiteAttributeMapper is located. Applications can
     * develop a partnerSiteAttributeMapper to obtain attributes to be included
     * in the assertion during SSO based on target URL. If no
     * partnerSiteAttributeMapper is found, then no attributes will be included
     * in the assertion during SSO. 
     *
     * @supported.api
     */
    public static final String PARTNERSITEATTRIBUTEMAPPER =
                                "partnersiteattributemapper";


    /**
     * A SAML service attribute parameter that specifies the class with the
     * path where the nameIdentifierMapper is located. Applications can develop
     * a nameIdentifierMapper to obtain a name identifier to be included in the
     * assertion during SSO. If no nameIdentifierMapper is found, then a
     * default implementation will be used.
     *
     * @supported.api
     */
    public static final String NAMEIDENTIFIERMAPPER = "nameidentifiermapper";

    /**
     * A SAML service attribute parameter that lists the IP addresses and/or
     * the certAlias for all of the hosts, within the specified partner site,
     * that can send requests to this site. This ensures that the requester is
     * indeed the intended receiver for the SAML artifact. 
     *
     * @supported.api
     */
    public static final String HOST_LIST = "hostlist";

    /**
     * A POST attribute name that specifies an assertion.
     *
     * @supported.api
     */
    public static final String POST_ASSERTION_NAME = "ASSERTION";

    /**
     * A POST attribute name that specifies attribute names of an assertion.
     *
     * @supported.api
     */
    public static final String POST_ATTR_NAMES = "ATTRIBUTENAMES";

    /**
     * A SAML service attribute parameter that specifies a certAlias name used
     * for verifying the signature in an assertion, when the assertion is
     * signed by a partner and the certificate of the partner can not be found
     * in the KeyInfo portion of the signed assertion.
     *
     * @supported.api
     */
    public static final String CERTALIAS = "CERTALIAS";

    /**
     * A SAML service attribute parameter that defines the creator of an
     * assertion generated within OpenAM. The syntax is
     * hostname:port.
     *
     * @supported.api
     */
    public static final String ISSUER = "ISSUER";

    /**
     * A SAML service attribute parameter that specifies the class with the
     * path to where the attributeMapper is located. Applications can develop
     * an attributeMapper to obtain either an Session  ID or an assertion
     * containing AuthenticationStatement from the query. The mapper is then
     * used to retrieve the attributes for the subject. If no attributeMapper
     * is specified, DefaultAttributeMapper will be used. 
     *
     * @supported.api
     */
    public static final String ATTRIBUTEMAPPER = "ATTRIBUTEMAPPER";

    /**
     * A SAML service attribute parameter that specifies the class with the
     * path to where the actionMapper is located. Applications can develop an
     * actionMapper to obtain either an Session ID or an assertion containing
     * AuthenticationStatement from the query. The mapper is then used to
     * retrieve the authorization decisions for the actions defined in the
     * query. If no actionMapper is specified, DefaultActionMapper will be
     * used. 
     *
     * @supported.api
     */
    public static final String ACTIONMAPPER = "ACTIONMAPPER";

    /**
     * A SAML service attribute parameter that specifies SAML version
     *
     * @supported.api
     */
    public static final String VERSION = "VERSION";

    /**
     * SAML service attribute that specifies action service mapping.
     */
    public static final String ACTION_SERVICE_MAPPING =
                                "iplanet-am-saml-action-service-mapping";

    /**
     * SAML service attribute that specifies POST to Target URLs.
     */
    public static final String POST_TO_TARGET_URLS = 
        "iplanet-am-saml-post-to-target-urls";

    /**
     * SAML service attribute that specifies maximum number of assertions
     * the server can have at a given time.
     */
    public static final String ASSERTION_MAX_NUMBER_NAME =
                                "iplanet-am-saml-assertion-max-number";
    /**
     * Default maximum number of assertions of the server. It means no limit.
     */
    public static final int ASSERTION_MAX_NUMBER_DEFAULT = 0;

    /**
     * SAML service attribute that specifies server clean up interval.
     */
    public static final String CLEANUP_INTERVAL_NAME =
                                "iplanet-am-saml-cleanup-interval";
    /**
     * System property name that specifies cleanup interval for internal
     * cache, such as assertions, artifacts and keystore etc. 
     * This property is mapped to the CLEANUP_INTERVAL_NAME attribute in 
     * SAML service on the server side.
     */
    public static final String CLEANUP_INTERVAL_PROPERTY =
                            "com.sun.identity.saml.cleanup.interval";

    /**
     * Default server cleanup interval. It is in seconds.
     */
    public static final int CLEANUP_INTERVAL_DEFAULT = 180;

    /**
     * Kerberos authentication method.
     */
    public static final String AUTH_METHOD_KERBEROS = "Kerberos";

    /**
     * Certificate authentication method.
     */
    public static final String AUTH_METHOD_CERT = "Cert";

    /**
     * Certificate authentication method URI.
     */
    public static final String AUTH_METHOD_CERT_URI = "urn:ietf:rfc:2246";

    /**
     * Password authentication method URI.
     */
    public static final String AUTH_METHOD_PASSWORD_URI = 
        "urn:oasis:names:tc:SAML:1.0:am:password";

    /**
     * Hardware token authentication method uri.
     */
    public static final String AUTH_METHOD_HARDWARE_TOKEN_URI = 
        "urn:oasis:names:tc:SAML:1.0:am:HardwareToken";

    /**
     * Kerberos authentication method uri.
     */
    public static final String AUTH_METHOD_KERBEROS_URI = "urn:ietf:rfc:1510";

    /**
     * Private authentication method prefix.
     */
    public static final String AUTH_METHOD_URI_PREFIX =
                                "urn:com:sun:identity:";

    /**
     * SAML service attribute which identifies basic authentication user.
     */
    public static final String USER = "iplanet-am-saml-user"; 

    /**
     * SAML service attribute which identifies basic authentication password.
     */
    public static final String PASSWORD="iplanet-am-saml-password"; 

    /**
     * A SAML authentication type that uses HTTP protocol and username/password
     *
     * @supported.api
     */
    public static final String BASICAUTH= "BASICAUTH";

    /**
     * A SAML authentication type that uses HTTPS protocol and
     * username/password
     *
     * @supported.api
     */
    public static final String SSLWITHBASICAUTH = "SSLWITHBASICAUTH"; 

    /**
     * A SAML authentication type that uses HTTP protocol
     *
     * @supported.api
     */
    public static final String NOAUTH = "NOAUTH";

    /**
     * A SAML authentication type that uses HTTPS protocol
     *
     * @supported.api
     */
    public static final String SSL = "SSL"; 

    /**
     * Parameter for Name Identifier format used in SAML web browser post
     * profile and SAML web browser artifact profile.
     */
    public static final String NAME_ID_FORMAT="NameIDFormat";

    /**
     * Constant for servlet which does SAML web browser artifact profile.
     */
    public static final String SAML_AWARE_NAMING="samlawareservlet";

    /**
     * Constant for endpoint which accepts SOAP request.
     */
    public static final String SAML_SOAP_NAMING="samlsoapreceiver"; 

    /**
     * Constant for servlet which does SAML web browser post profile.
     */
    public static final String SAML_POST_NAMING="samlpostservlet";

    /**
     * Constant for process which handles all assertion related requests.
     */
    public static final String SAML_AM_NAMING = "samlassertionmanager";

    /**
     * Parameter for SAML response used in SAML web browser post profile.
     */
    public static final String POST_SAML_RESPONSE_PARAM = "SAMLResponse";

    /**
     * Parameter for SAML TARGET.
     */
    public static final String POST_TARGET_PARAM = "TARGET";

    // for SAMLPOSTProfileServlet

    /**
     * Constant which identifies source site.
     */
    public static final String SOURCE_SITE_SOAP_ENTRY = "sourceSite";

    /**
     * Constant for assertion parameter.
     */
    public static final String POST_ASSERTION = "assertion";

    /**
     * Constant for subject.
     */
    public static final String SUBJECT = "subject";

    /**
     * Success status code.
     */
    public static final String STATUS_CODE_SUCCESS = "samlp:Success";

    /**
     * Success status code with no namespace prefix.
     */
    public static final String STATUS_CODE_SUCCESS_NO_PREFIX = ":Success";

    /**
     * http protocol.
     */
    public static final String HTTP="http"; 

    /**
     * https protocol.
     */
    public static final String HTTPS="https";
 
    // Used for xml digital signing
    public static final String CANONICALIZATION_METHOD =
        "com.sun.identity.saml.xmlsig.c14nMethod";
    public static final String TRANSFORM_ALGORITHM=
        "com.sun.identity.saml.xmlsig.transformAlg";
    public static final String XMLSIG_ALGORITHM =
        "com.sun.identity.saml.xmlsig.xmlSigAlgorithm";    
    public static final String DIGEST_ALGORITHM =
        "com.sun.identity.saml.xmlsig.digestAlgorithm";
    public static final String JKS_KEY_PROVIDER = 
        "com.sun.identity.saml.xmlsig.JKSKeyProvider"; 
    public static final String KEY_PROVIDER_IMPL_CLASS =
        "com.sun.identity.saml.xmlsig.keyprovider.class";
    public static final String SIGNATURE_PROVIDER_IMPL_CLASS =
        "com.sun.identity.saml.xmlsig.signatureprovider.class";
    public static final String AM_SIGNATURE_PROVIDER =
        "com.sun.identity.saml.xmlsig.AMSignatureProvider";
   
    // constants for XML Signature SignatureMethodURI

    /**
     * MAC Algorithm HMAC-SHA1 URI - Required.
     *
     * @supported.api
     */
    public static final String ALGO_ID_MAC_HMAC_SHA1 = 
                                "http://www.w3.org/2000/09/xmldsig#hmac-sha1";

    /**
     * Signature Algorithm DSAwithSHA1 URI - Required.
     *
     * @supported.api
     */
    public static final String ALGO_ID_SIGNATURE_DSA =
                                "http://www.w3.org/2000/09/xmldsig#dsa-sha1";

    /**
     * Signature Algorithm RSAwithSHA1 URI - Recommended.
     *
     * @supported.api
     */
    public static final String ALGO_ID_SIGNATURE_RSA = 
                                "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    /**
     * Signature Algorithm RSAwithSHA1 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA1 =
                                "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

    /**
     * Signature Algorithm RSA-MD5 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_SIGNATURE_NOT_RECOMMENDED_RSA_MD5 =
                              "http://www.w3.org/2001/04/xmldsig-more#rsa-md5";

    /**
     * Signature Algorithm RSA-RIPEMD160 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_SIGNATURE_RSA_RIPEMD160 = 
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-ripemd160";

    /**
     * Signature Algorithm RSA-SHA256 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA256 =
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    /**
     * Signature Algorithm RSA-SHA384 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA384 = 
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";

    /**
     * Signature Algorithm RSA-SHA512 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA512 = 
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

    /**
     * MAC Algorithm HMAC-MD5 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_MAC_HMAC_NOT_RECOMMENDED_MD5 = 
                        "http://www.w3.org/2001/04/xmldsig-more#hmac-md5";

    /**
     * MAC Algorithm HMAC-RIPEMD160 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_MAC_HMAC_RIPEMD160 = 
                       "http://www.w3.org/2001/04/xmldsig-more#hmac-ripemd160";

    /**
     * MAC Algorithm HMAC-SHA256 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_MAC_HMAC_SHA256 = 
                        "http://www.w3.org/2001/04/xmldsig-more#hmac-sha256";

    /**
     * MAC Algorithm HMAC-SHA384 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_MAC_HMAC_SHA384 =
                        "http://www.w3.org/2001/04/xmldsig-more#hmac-sha384";

    /**
     * MAC Algorithm HMAC-SHA512 URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_MAC_HMAC_SHA512 = 
                        "http://www.w3.org/2001/04/xmldsig-more#hmac-sha512";

    /**
     * Attribute that identifies server protocol in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SERVER_PROTOCOL =
                        "com.iplanet.am.server.protocol";

    /**
     * Attribute that identifies server host in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SERVER_HOST = "com.iplanet.am.server.host";

    /**
     * Attribute that identifies server port in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SERVER_PORT = "com.iplanet.am.server.port";

    /**
     * Attribute that identifies server port in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SERVER_URI =
        "com.iplanet.am.services.deploymentDescriptor";

    /**
     * Attribute that identifies default version for saml assertion in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SAML_ASSERTION_VERSION =
                        "com.sun.identity.saml.assertion.version";

    /**
     * Attribute that identifies default version for saml protocol in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SAML_PROTOCOL_VERSION =
                        "com.sun.identity.saml.protocol.version";
   
    /**
     * XML canonicalization Algorithm URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_C14N_OMIT_COMMENTS =
                        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";

    /**
     * XML canonicalization with comments Algorithm URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_C14N_WITH_COMMENTS =
                        ALGO_ID_C14N_OMIT_COMMENTS + "#WithComments";

    /**
     * Exclusive XML canonicalization Algorithm URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_C14N_EXCL_OMIT_COMMENTS =
                        "http://www.w3.org/2001/10/xml-exc-c14n#";

    /**
     * Exclusive XML canonicalization with comments Algorithm URI.
     *
     * @supported.api
     */
    public static final String ALGO_ID_C14N_EXCL_WITH_COMMENTS =
                        ALGO_ID_C14N_EXCL_OMIT_COMMENTS + "WithComments";
   
    //constants for XML Signature -Transform algorithm
    //supported in Apache xml security package 1.0.5
  
    /**
     * XML canonicalization Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_C14N_OMIT_COMMENTS =
                        ALGO_ID_C14N_OMIT_COMMENTS;

    /**
     * XML canonicalization with comments Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_C14N_WITH_COMMENTS =
                         ALGO_ID_C14N_WITH_COMMENTS;

    /**
     * Exclusive XML canonicalization Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_C14N_EXCL_OMIT_COMMENTS =
                         ALGO_ID_C14N_EXCL_OMIT_COMMENTS;

    /**
     * Exclusive XML canonicalization with comments Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_C14N_EXCL_WITH_COMMENTS =
                         ALGO_ID_C14N_EXCL_WITH_COMMENTS;

    /**
     * XSLT Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_XSLT =
                         "http://www.w3.org/TR/1999/REC-xslt-19991116";

    /**
     * Base64 decoding Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_BASE64_DECODE =
                         XMLSIG_NAMESPACE_URI + "base64";

    /**
     * XPath Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_XPATH =
                         "http://www.w3.org/TR/1999/REC-xpath-19991116";

    /**
     * Enveloped Signature Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_ENVELOPED_SIGNATURE =
                         XMLSIG_NAMESPACE_URI + "enveloped-signature";

    /**
     * XPointer Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_XPOINTER =
                         "http://www.w3.org/TR/2001/WD-xptr-20010108";

    /**
     * XPath Filter v2.0 Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_XPATH2FILTER04 =
                         "http://www.w3.org/2002/04/xmldsig-filter2";

    /**
     * XPath Filter v2.0 Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_XPATH2FILTER =
                         "http://www.w3.org/2002/06/xmldsig-filter2";

    /**
     * XPath Filter v2.0 CHGP Transform URI.
     *
     * @supported.api
     */
    public static final String TRANSFORM_XPATHFILTERCHGP =
          "http://www.nue.et-inf.uni-siegen.de/~geuer-pollmann/#xpathFilter";

    /**
     * XML schema namespace.
     *
     * @supported.api
     */
    public static final String NS_XMLNS = "http://www.w3.org/2000/xmlns/";

    /**
     * SOAP security namespace.
     *
     * @supported.api
     */
    public static final String NS_SEC = "urn:liberty:sec:2003-08";

    /**
     * SOAP utility namespace.
     *
     * @supported.api
     */
    public static final String NS_WSSE =
                         "http://schemas.xmlsoap.org/ws/2003/06/secext";

    /**
     * Liberty security namespace.
     *
     * @supported.api
     */
    public static final String NS_WSU =
                        "http://schemas.xmlsoap.org/ws/2003/06/utility";

    /**
     * String that identifies wsu prefix.
     */
    public static final String PREFIX_WSU = "wsu";

    /**
     * String that identifies ds prefix.
     */
    public static final String PREFIX_DS = "ds";

    /**
     * String that identifies tag name "SecurityTokenReference".
     */
    public static final String TAG_SECURITYTOKENREFERENCE =
                        "SecurityTokenReference";

    /**
     * String that identifies tag xmlns.
     */
    public static final String TAG_XMLNS = "xmlns";

    /**
     * String that identifies "xmlns:sec".
     */
    public static final String TAG_XMLNS_SEC = "xmlns:sec";

    /**
     * Usage tag name.
     */
    public static final String TAG_USAGE = "Usage";

    /**
     * MessageAuthentication tag name with namespace prefix.
     */
    public static final String TAG_SEC_MESSAGEAUTHENTICATION =
                        "sec:MessageAuthentication";

    /**
     * Tag name for <code>BinarySecurityToken</code> with namespace prefix.
     */
    public static final String TAG_WSSE_BINARYSECURITYTOKEN =
                        "wsse:BinarySecurityToken";

    /**
     * Tag name for <code>Security</code>.
     */
    public static final String TAG_SECURITY = "Security";

    /**
     * Tag name for <code>AssertionIDReference</code>.
     */
    public static final String TAG_ASSERTIONIDREFERENCE =
                        "AssertionIDReference";

    /**
     * Tag name for <code>Assertion</code>.
     */
    public static final String TAG_ASSERTION = "Assertion";

    /**
     * String that identifies <code>AssertionID</code>.
     */
    public static final String TAG_ASSERTION_ID = "AssertionID";

    /**
     * Tag name for <code>BinarySecurityToken</code>.
     */
    public static final String BINARYSECURITYTOKEN = "BinarySecurityToken";

    /**
     * Tag name for "Id".
     */
    public static final String TAG_ID = "Id";

    /**
     * Tag name for <code>Reference</code>.
     */
    public static final String TAG_REFERENCE = "Reference";

    /**
     * Tag name for <code>URI</code>.
     */
    public static final String TAG_URI = "URI";

    /**
     * Tag name for <code>ValueType</code>.
     */
    public static final String TAG_VALUETYPE = "ValueType";

    /**
     * Tag name for <code>KeyInfo</code>.
     */
    public static final String TAG_KEYINFO = "KeyInfo";

    /**
     * Tag name for <code>KeyName</code>.
     */
    public static final String TAG_KEYNAME = "KeyName";

    /**
     * Tag name for <code>KeyValue<code>.
     */
    public static final String TAG_KEYVALUE = "KeyValue";

    /**
     * Tag name for <code>PKCS7</code> with wsse namespace prefix.
     */
    public static final String TAG_PKCS7 = "wsse:PKCS7";

    /**
     * Tag name for <code>X509Data</code>.
     */
    public static final String TAG_X509DATA = "X509Data";

    /**
     * Tag name for <code>X509Certificate</code>.
     */
    public static final String TAG_X509CERTIFICATE = "X509Certificate";

    /**
     * Beginning of certificate string.
     */
    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";

    /**
     * End of certificate string.
     */
    public static final String END_CERT    = "\n-----END CERTIFICATE-----";

    /**
     * <code>DSAKeyValue</code> tag name.
     */
    public static final String TAG_DSAKEYVALUE = "DSAKeyValue";

    /**
     * <code>RSAKeyValue</code> tag name.
     */
    public static final String TAG_RSAKEYVALUE = "RSAKeyValue";

    /**
     * Attribute which identifies certificate alias of this site.
     * This value is configured through <code>AMConfig.properties</code>.
     */
    public static final String MYCERTALIAS =
                        "com.sun.identity.saml.xmlsig.certalias";
    
    /**
     * User Name attribute key in the Attribute Map.
     */
    public static final String USER_NAME = "USER_NAME";

    /**
     * SAML component name to be used to get datastore provider.
     */
    public static final String SAML = "saml";
    /**
     * SAML component default name space.
     */
    public static final String ATTR_NAME_SPACE =
        "urn:com:sun:identity:attrnamespace";

    public static final String TAG_USERNAME_TOKEN = "UsernameToken";
    /**
     * Keyname for escaping special characters in <code>AttributeValue</code>.
     * If true, escaping special characters. Otherwise, will not. Default 
     * value is "true". 
     */
    public static final String ESCAPE_ATTR_VALUE = 
        "com.sun.identity.saml.escapeattributevalue";
    
    /**
     * HTTP POST binding.
     */
    public static final String HTTP_POST = "HTTP-POST";

    /**
     * HTTP Redirect binding.
     */
    public static final String HTTP_REDIRECT = "HTTP-Redirect";

    /**
     * Property to identity the HTTP binding for displaying error page.
     */
    public static final String ERROR_PAGE_HTTP_BINDING =
                       "com.sun.identity.saml.errorpage.httpbinding";

    /**
     * Property to identify the error page url.
     */
    public static final String ERROR_PAGE_URL =
                       "com.sun.identity.saml.errorpage.url";
    /**
     * Default error page url.
     */
    public static final String DEFAULT_ERROR_PAGE_URL =
                                "/saml2/jsp/saml2error.jsp";
    /**
     * HTTP status code.
     */
    public static final String HTTP_STATUS_CODE = "httpstatuscode";

    /**
     * Error message.
     */
    public static final String ERROR_MESSAGE = "errormessage";

    /**
     * Error code.
     */
    public static final String ERROR_CODE = "errorcode";

	/**
	 * Accept Language HTTP header
	 */
	public static final String ACCEPT_LANG_HEADER = "Accept-Language";
}
