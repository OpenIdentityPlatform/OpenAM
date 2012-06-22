/**
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
 * $Id: AuthnSvcConstants.java,v 1.2 2008/06/25 05:47:06 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.authnsvc; 

public final class AuthnSvcConstants
{
    /**
     * XMl namespace definition.
     */
    public static final String NS_XML = "http://www.w3.org/2000/xmlns/";

    /**
     * Namespace for Authn Service.
     */
    public static final String PREFIX_AUTHN_SVC = "sa";

    /**
     * Namespace for Discovery service.
     */
    public static final String PREFIX_DISCO = "disco";

    /**
     * Namespace for ID-FF protocol.
     */
    public static final String PREFIX_PROTOCOLS_SCHEMA = "lib";

    /**
     * Authn Service namespace definition.
     */
    public static final String NS_AUTHN_SVC = "urn:liberty:sa:2004-04";

    /**
     * ID-FF protocol namespace definition.
     */
    public static final String NS_PROTOCOLS_SCHEMA = "urn:liberty:iff:2003-08";

    /**
     * Tag name for element SASLRequest.
     */
    public static final String TAG_SASL_REQUEST = "SASLRequest";

    /**
     * Tag name for element SASLResponse.
     */
    public static final String TAG_SASL_RESPONSE = "SASLResponse";
    /**
     * Tag name for element Data.
     */
    public static final String TAG_DATA = "Data";

    /**
     * Tag name for element RequestAuthnContext.
     */
    public static final String TAG_REQUEST_AUTHN_CONTEXT =
                                              "RequestAuthnContext";

    /**
     * Tag name for element Status.
     */
    public static final String TAG_STATUS = "Status";

    /**
     * Tag name for element PasswordTranforms.
     */
    public static final String TAG_PASSWORD_TRANSFORMS = "PasswordTransforms";

    /**
     * Tag name for element Transform.
     */
    public static final String TAG_TRANSFORM = "Transform";

    /**
     * Tag name for element Parameter.
     */
    public static final String TAG_PARAMETER = "Parameter";

    /**
     * Tag name for element ResourceOffering.
     */
    public static final String TAG_RESOURCE_OFFERING = "ResourceOffering";

    /**
     * Tag name for element Credentials.
     */
    public static final String TAG_CREDENTIALS = "Credentials";


    /**
     * Attribute mechanism.
     */
    public static final String ATTR_MECHANISM = "mechanism";

    /**
     * Attribute authzID.
     */
    public static final String ATTR_AUTHZ_ID = "authzID";

    /**
     * Attribute advisoryAuthnID.
     */
    public static final String ATTR_ADVISORY_AUTHN_ID = "advisoryAuthnID";

    /**
     * Attribute id.
     */
    public static final String ATTR_id = "id";

    /**
     * Attribute serverMechanism.
     */
    public static final String ATTR_SERVER_MECHANISM = "serverMechanism";

    /**
     * Attribute code.
     */
    public static final String ATTR_CODE = "code";

    /**
     * Attribute name.
     */
    public static final String ATTR_NAME = "name";


    /**
     * Element SASLRequest with namespace prefix.
     */
    public static final String PTAG_SASL_REQUEST = PREFIX_AUTHN_SVC + ":" +
                                                   TAG_SASL_REQUEST;

    /**
     * Element SASLResponse with namespace prefix.
     */
    public static final String PTAG_SASL_RESPONSE = PREFIX_AUTHN_SVC + ":" +
                                                    TAG_SASL_RESPONSE;

    /**
     * Element PasswordTransforms with namespace prefix.
     */
    public static final String PTAG_PASSWORD_TRANSFORMS =
                              PREFIX_AUTHN_SVC + ":" + TAG_PASSWORD_TRANSFORMS;

    /**
     * Element Transform with namespace prefix.
     */
    public static final String PTAG_TRANSFORM = PREFIX_AUTHN_SVC + ":" + 
                                                TAG_TRANSFORM;

    /**
     * Element Parameter with namespace prefix.
     */
    public static final String PTAG_PARAMETER = PREFIX_AUTHN_SVC + ":" +
                                                TAG_PARAMETER;

    /**
     * Element Status with namespace prefix.
     */
    public static final String PTAG_STATUS = PREFIX_AUTHN_SVC + ":" +
                                             TAG_STATUS;

    /**
     * Element Data with namespace prefix.
     */
    public static final String PTAG_DATA = PREFIX_AUTHN_SVC + ":" + TAG_DATA;

    /**
     * Element Credentials with namespace prefix.
     */
    public static final String PTAG_CREDENTIALS = PREFIX_AUTHN_SVC + ":" +
                                                  TAG_CREDENTIALS;

    /**
     * Attribute mustUnderstand with namespace prefix.
     */
    public static final String PATTR_MUSTUNDERSTAND = "S:mustUnderstand";

    /**
     * Attribute actor with namespace prefix.
     */
    public static final String PATTR_ACTOR = "S:actor";

    /**
     * Authn Service namespace with namespace prefix.
     */
    public static final String XMLNS_AUTHN_SVC = "xmlns:" + PREFIX_AUTHN_SVC;

    /**
     * Discovery service namespace with namespace prefix.
     */
    public static final String XMLNS_DISCO = "xmlns:" + PREFIX_DISCO;

    /**
     * ID-FF 1.2 protocol schema namespace with namespace prefix.
     */
    public static final String XMLNS_PROTOCOLS_SCHEMA =
                                           "xmlns:" + PREFIX_PROTOCOLS_SCHEMA;

    /**
     * Mechanism PLAIN.
     */
    public static final String MECHANISM_PLAIN = "PLAIN";

    /**
     * Mechanism CRAM-MD5.
     */
    public static final String MECHANISM_CRAMMD5 = "CRAM-MD5";
}
