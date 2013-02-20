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
package org.forgerock.identity.openam.xacml.model;

import com.sun.identity.entitlement.xacml3.XACMLConstants;

/**
 * Model XACML3 Constants
 *
 * @author jeff.schenk@forgerock.com
 *
 */
public interface XACML3Constants extends XACMLConstants {
    /**
     * Constant used to identify meta alias.
     */
    public static final String NAME_META_ALIAS_IN_URI = "metaAlias";

    /**
     * Common Globals Definitions
     */
    public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    public static final String AUTHORIZATION = "Authorization";
    public static final String DIGEST = "Digest";
    public static final String REQUEST = "Request";
    public static final String REQUEST_ABSTRACT = "RequestAbstract";
    public static final String XSI_TYPE_ATTR = "xsi:type";
    public static final String XACML_AUTHZ_QUERY = "XACMLAuthzDecisionQuery";   // [SAML4XACML]
    public static final String METAALIAS_KEY = "/metaAlias" ;

    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /**
     * RESTful XACML XACML 3 Name Space Definitions.
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
     *  ￼
     * Normative Source: GET on the home location MUST return status code 200
     * ￼
     * Target: Response to GET request on the home location
     *￼
     * Predicate: The HTTP status code in the [response] is 200
     *￼
     * Prescription Level: mandatory
     *
     */
    public static final String URN_HOME_STATUS = "urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:status";


    public static final String URN_HOME_BODY = "urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:body";

}
