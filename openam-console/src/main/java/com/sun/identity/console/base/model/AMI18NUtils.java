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
 * $Id: AMI18NUtils.java,v 1.4 2008/07/10 23:27:22 veiming Exp $
 *
 */

package com.sun.identity.console.base.model;

import com.iplanet.am.util.AMClientDetector;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.cdm.Client;
import com.iplanet.services.cdm.ClientsManager;
import com.iplanet.services.cdm.ClientException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/* - NEED NOT LOG - */

/**
 * This provides Internationalization related methods.
 */
public class AMI18NUtils
{
    /** client type property key */
    public static final String KEY_CLIENT_TYPE = "clientType";
    
    private static AMClientDetector clientDt = new AMClientDetector();

    private static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);

    /**
     * Gets client type from SSO token
     *
     * @param ssoToken - Single-Sign-On Token
     * @return client type
     */
    public static String getClientType(SSOToken ssoToken) {
        String clientType = "";
        try {
            clientType = ssoToken.getProperty(KEY_CLIENT_TYPE);
        } catch (SSOException ssoe) {
            debug.error("AMI18NUtils.getClientType", ssoe);
        }
        return clientType;
    }

    /**
     * Returns charset type from client service API
     *
     * @param clientType Client type.
     * @param loc Locale of request.
     * @return charset.
     */
    public static String getCharset(String clientType, java.util.Locale loc) {
        String charset;
        if (clientDt.isDetectionEnabled()) {
            try {
                Client client = ClientsManager.getInstance(clientType);
                charset = (client != null) ? client.getCharset(loc) : 
                    Client.CDM_DEFAULT_CHARSET;
            } catch (ClientException ce) {
                debug.warning("AMI18NUtils.getCharset - " +
                 "couldn't retrieve the client charset, reverting to default.");
                charset = Client.CDM_DEFAULT_CHARSET;
            }
        } else {
            charset = Client.CDM_DEFAULT_CHARSET;
        }
        
        return charset;
    }

    /**
     * Returns content type from client service API.
     *
     * @param clientType Client type.
     * @return content type.
     */
    public static String getContentType(String clientType) {
        String contentType;
        if (clientDt.isDetectionEnabled()) {
            try {
                Client client = ClientsManager.getInstance(clientType);
                contentType = (client != null) ? client.getProperty(
                    AMAdminConstants.CDM_CONTENT_TYPE_PROPERTY_NAME) : 
                    Client.CDM_DEFAULT_CLIENT_TYPE;
            } catch (ClientException ce) {
                debug.warning("AMI18NUtils.getContentType - " +
                    "couldn't retrieve the content type, reverting to default.");
                contentType = Client.CDM_DEFAULT_CLIENT_TYPE;
            }
        } else {
            contentType = Client.CDM_DEFAULT_CLIENT_TYPE;
        }
        return contentType;
    }
}
