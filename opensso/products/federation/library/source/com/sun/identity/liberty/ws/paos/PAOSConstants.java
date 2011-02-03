/**
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
 * $Id: PAOSConstants.java,v 1.2 2008/06/25 05:47:19 qcheng Exp $
 *
 */
package com.sun.identity.liberty.ws.paos;

/**
 * This interface defines constants common to all PAOS elements.
 *
 * @supported.all.api
 */
public interface PAOSConstants {

    /**
     * PAOS mime type
     */
    public static final String PAOS_MIME_TYPE =
        "application/vnd.paos+xml";

    /**
     * String used to declare PAOS namespace prefix.
     */
    public static final String PAOS_PREFIX = "paos";

    /**
     * PAOS namespace URI.
     */
    public static final String PAOS_NAMESPACE = "urn:liberty:paos:2003-08";

    /**
     * PAOS Request element local name.
     */
    public static final String REQUEST = "Request";

    /**
     * PAOS Request element local name.
     */
    public static final String RESPONSE = "Response";

    /**
     * PAOS Request responseConsumerURL attribute name.
     */
    public static final String RESPONSE_CONSUMER_URL = "responseConsumerURL";

    /**
     * PAOS Request service attribute name.
     */
    public static final String SERVICE = "service";

    /**
     * PAOS Request messageID attribute name.
     */
    public static final String MESSAGE_ID = "messageID";

    /**
     * PAOS Response refToMessageID attribute name.
     */
    public static final String REF_TO_MESSAGE_ID = "refToMessageID";

    /**
     * SOAP mustUnderstand attribute name.
     */
    public static final String MUST_UNDERSTAND = "mustUnderstand";
    
    /**
     * SOAP actor attribute name
     */
    public static final String ACTOR = "actor";

    /**
     * String used to declare SOAP envelope namespace prefix.
     */
    public static final String SOAP_ENV_PREFIX = "soap-env";
    
    /**
     * SOAP envelope namespace URI.
     */
    public static final String SOAP_ENV_NAMESPACE =
        "http://schemas.xmlsoap.org/soap/envelope/";

}
