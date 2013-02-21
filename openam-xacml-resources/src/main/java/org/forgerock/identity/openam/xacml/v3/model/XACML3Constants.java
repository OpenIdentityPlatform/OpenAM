/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

import com.sun.identity.entitlement.xacml3.XACMLConstants;


/**
 * Model XACML3 Constants
 *
 * @author jeff.schenk@forgerock.com
 */
public interface XACML3Constants extends XACMLConstants {

    /**
     * XACML 3 Core Package Information.
     */
    public static final String XACML3_CORE_PKG
            = "com.sun.identity.entitlement.xacml3.core";

    /**
     *  XACML 3 Default Namespace.
     */
    public static final String XACML3_NAMESPACE = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17";
    /**
     *  XACML 3 Default PDP Realm.
     */
    public static final String XACML3_PDP_DEFAULT_REALM = "OpenAM_XACML_PDP_Realm";
    /**
     * Constant used to identify meta alias.
     */
    public static final String NAME_META_ALIAS_IN_URI = "metaAlias";

    /**
     * Digest Authentication Global Constants.
     */
    public static final String authenticationMethods = "auth,auth-int";   // See http://tools.ietf.org/html/rfc2617
    public static final String USERNAME = "username";

    /**
     * Common Globals Definitions
     */
    public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    public static final String AUTHORIZATION = "authorization";
    public static final String DIGEST = "Digest";
    public static final String REQUEST = "Request";
    public static final String XSI_TYPE_ATTR = "xsi:type";
    public static final String XACML_AUTHZ_QUERY = "XACMLAuthzDecisionQuery";   // [SAML4XACML]
    public static final String METAALIAS_KEY = "/metaAlias";

    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    public static final String PDP_AUTHORIZATION_ENDPOINT = "/xacml/pdp/"+AUTHORIZATION;

    /**
     * Current Standards Schema Resource Name.
     */
    public static final String xacmlCoreSchemaResourceName =
            "xsd/xacml-core-v3-schema-wd-17.xsd";

    /**
     * XML Core Schema Resource Name.
     */
    public static final String xmlCoreSchemaResourceName =
            "xsd/xml.xsd";

    /**
     * RESTful XACML 3.0 Name Space Definitions.
     */
    public static final String URN_HTTP = "urn:oasis:names:tc:xacml:3.0:profile:rest:http";
    public static final String URN_HOME = "urn:oasis:names:tc:xacml:3.0:profile:rest:home";
    public static final String URN_PDP = "urn:oasis:names:tc:xacml:3.0:profile:rest:pdp";

    /**
     * Network Transport
     * Client and Server MUST use HTTP as the underlying Network Transport between each other.
     * Also, SSL/TLS is encouraged to be used as well to protect Data over Transport.
     */
    public static final String URN_CLIENT = "urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:http:client";
    public static final String URN_SERVER = "urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:http:server";

    /**
     * A RESTful XACML system MUST have a single entry point at a known location
     * Each implementation of this profile MUST document the location of the entry point
     */
    public static final String URN_ENTRY_POINT = "urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:documentation";

    /**
     * ￼
     * Normative Source: GET on the home location MUST return status code 200
     * ￼
     * Target: Response to GET request on the home location
     * ￼
     * Predicate: The HTTP status code in the [response] is 200
     * ￼
     * Prescription Level: mandatory
     */
    public static final String URN_HOME_STATUS = "urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:status";
    public static final String URN_HOME_BODY = "urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:body";

}
