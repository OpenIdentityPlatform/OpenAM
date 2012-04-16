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
 * $Id: DiscoConstants.java,v 1.2 2008/06/25 05:47:11 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco.common;

import javax.xml.namespace.QName;

/**
 * Defines constants for discovery service.
 */
public final class DiscoConstants {

    /**
     * Discovery service namespace definition.
     */
    public static final String DISCO_NS = "urn:liberty:disco:2003-08";

    /**
     * Discovery service namespace prefix.
     */
    public static final String DISCO_NSPREFIX = "disco";

    /**
     * Discovery service v1.1 namespace definition.
     */
    public static final String DISCO11_NS = "urn:liberty:disco:2004-04";

    /**
     * Discovery service v1.1 namespace prefix.
     */
    public static final String DISCO11_NSPREFIX = "disco11";

    /**
     * OK status code.
     */
    public static final String STATUS_OK = "OK";

    /**
     * Status code "Failed".
     */
    public static final String STATUS_FAILED = "Failed";

    /**
     * Status code "RemoveEntry".
     */
    public static final String STATUS_REMOVEENTRY = "RemoveEntry";

    /**
     * Status code "Forbidden".
     */
    public static final String STATUS_FORBIDDEN = "Forbidden";

    /**
     * Status code "NoResults".
     */
    public static final String STATUS_NORESULTS = "NoResults";

    /**
     * Status code "Directive".
     */
    public static final String STATUS_DIRECTIVE = "Directive";

    /**
     * <code>QName</code> for "OK".
     */
    public static final QName QNAME_OK = new QName(DISCO_NS, STATUS_OK);

    /**
     * <code>QName</code> for "Failed".
     */
    public static final QName QNAME_FAILED = new QName(DISCO_NS,STATUS_FAILED);

    /**
     * <code>QName</code> for "RemoveEntry".
     */
    public static final QName QNAME_REMOVEENTRY = new QName(DISCO_NS,
                                                        STATUS_REMOVEENTRY);

    /**
     * <code>QName</code> for "Forbidden".
     */
    public static final QName QNAME_FORBIDDEN = new QName(DISCO_NS,
                                                        STATUS_FORBIDDEN);

    /**
     * <code>QName</code> for "NoResults".
     */
    public static final QName QNAME_NORESULTS = new QName(DISCO_NS,
                                                        STATUS_NORESULTS);

    /**
     * <code>QName</code> for "Directive".
     */
    public static final QName QNAME_DIRECTIVE = new QName(DISCO_NS,
                                                        STATUS_DIRECTIVE);

    /**
     * Directive "AuthenticateRequester".
     */
    public static final String AUTHN_DIRECTIVE = "AuthenticateRequester";

    /**
     * Directive "AuthorizeRequester".
     */
    public static final String AUTHZ_DIRECTIVE = "AuthorizeRequester";

    /**
     * Directive "AuthenticateSessionContext".
     */
    public static final String SESSION_DIRECTIVE = "AuthenticateSessionContext";

    /**
     * Directive "EncryptResourceID".
     */
    public static final String ENCRYPT_DIRECTIVE = "EncryptResourceID";

    /**
     * Directive "GenerateBearerToken".
     */
    public static final String BEARER_DIRECTIVE = "GenerateBearerToken";

    /**
     * Directive "SendSingleLogOut".
     */
    public static final String LOGOUT_DIRECTIVE = "SendSingleLogOut";

    /**
     * Action Lookup.
     */
    public static final String ACTION_LOOKUP = "LOOKUP";

    /**
     * Action Update.
     */
    public static final String ACTION_UPDATE = "UPDATE";

    /**
     * Provider ID format uri.
     */
    public static final String PROVIDER_ID_FORMAT =
                                        "urn:liberty:iff:nameid:entityID";

    /**
     * Implied resource uri.
     */
    public static final String IMPLIED_RESOURCE =
                "urn:liberty:isf:implied-resource";
}
