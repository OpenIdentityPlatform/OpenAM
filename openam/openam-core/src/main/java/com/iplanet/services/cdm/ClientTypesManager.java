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
 * $Id: ClientTypesManager.java,v 1.3 2008/06/25 05:41:32 qcheng Exp $
 *
 */

package com.iplanet.services.cdm;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;

/**
 * Interface that needs to be implemented by external applications inorder to do
 * some special processing for client management. The implementation module is
 * pluggable and is configurable via <code>AMConfig.properties</code>. The
 * property to set is <code>com.iplanet.ClientTypesManagerImpl</code>.
 * @supported.all.api
 */
public interface ClientTypesManager {

    /**
     * Initializes the <code>ClientTypesManager</code>.
     */
    public void initManager();

    /**
     * Gets all client instance as Map.
     * 
     * @return Map of clients. Key is the client type, value is the Client
     *         object
     */
    public Map getAllClientInstances();

    /**
     * Gets client object for specified client type.
     * 
     * @param clientType
     *            requested client type.
     * @return The requested Client object
     */
    public Client getClientInstance(String clientType);

    /**
     * Gets client object for specified client type with specified token
     * 
     * @param clientType
     *            requested client type
     * @param token
     *            SSO Token
     * @return The requested Client object
     */
    public Client getClientInstance(String clientType, SSOToken token);

    /**
     * Returns properties of the requested client type
     * 
     * @param clientType
     *            requested client type
     * @return All properties of the request client type as Map
     */
    public Map getClientTypeData(String clientType);

    /**
     * Gets default client type name
     * 
     * @return The default client type name
     */
    public String getDefaultClientType();

    /**
     * Get names of all client types
     * 
     * @return Set of client types as String
     */
    public Set getAllClientTypes();

    /**
     * Reload all Client data.
     * 
     * @throws ClientException
     *             if having problem update client data
     */
    public void updateClientData() throws ClientException;

    /**
     * Save changed to persistent store.
     * 
     * @param token
     *            single sign on Token of the caller.
     * @throws SSOException
     *             if the token is not valid.
     * @throws SMSException
     *             if having problem saving changes.
     */
    public void store(SSOToken token) throws SMSException, SSOException;

    /**
     * Updates client data. Need to call <code>store()</code> after this
     * method.
     * 
     * @param clientType
     *            client type
     * @param data
     *            client data. Key is the property name and value is the
     *            property value as String.
     */
    public void setDirty(String clientType, Map data);
}
