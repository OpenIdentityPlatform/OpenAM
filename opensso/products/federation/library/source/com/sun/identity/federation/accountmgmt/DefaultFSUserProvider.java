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
 * $Id: DefaultFSUserProvider.java,v 1.3 2008/11/10 22:56:56 veiming Exp $
 *
 */

package com.sun.identity.federation.accountmgmt;

import java.util.Map;
import java.util.Set;

import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;

/**
 * This is a default implemetation of <code>FSUserProvider</code>.
 */
public class DefaultFSUserProvider implements FSUserProvider {
    
    private String hostedProviderId = null;
    private DataStoreProvider datastoreProvider = null;

    /**
     * Default Constructor.
     */
    public DefaultFSUserProvider() throws FSAccountMgmtException {
    }
    
    /**
     * Initializes the provider.
     *
     * @param hostedProviderId ID of hosted provider.
     * @throws FSAccountMgmtException if an error occurred during
     *         initialization.
     */
    public void init(String hostedProviderId) throws FSAccountMgmtException {
        this.hostedProviderId = hostedProviderId;
        try {
            datastoreProvider = DataStoreProviderManager.getInstance().
                getDataStoreProvider(IFSConstants.IDFF);
        } catch (Exception de) {
            FSUtils.debug.error("DefaultFSUserProvider.init: couldn't obtain "
                + "datastore provider:", de);
            throw new FSAccountMgmtException(de.getMessage());
        }
    }

    /**
     * Searches user.
     * @param orgDN The organization to search the user.
     * @param avPairs Attribute value pairs that will be used for searching
     *  the user.
     * @param env Extra parameters that can be used for user mapping.
     * @throws FSAccountMgmtException if an error occurred.
     */
    public String getUserID(
        String orgDN,
        Map avPairs,
        Map env)
        throws FSAccountMgmtException 
    {
        FSUtils.debug.message("DefaultFSUserProvider.getUserID() : called");
        try {
            return datastoreProvider.getUserID(orgDN, avPairs);
        } catch (DataStoreProviderException de) {
            FSUtils.debug.error("DefaultFSUserProvider.getUserID: Couldn't "
                + "obtain user ID:", de);
            throw new FSAccountMgmtException(de.getMessage());
        }
    }
}
