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
 * $Id: AMFilteredRole.java,v 1.4 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import com.iplanet.sso.SSOException;

/**
 * This interface provides methods to manage filtered role.
 * <code>AMFilteredRole</code> objects can be obtained by using
 * <code>AMStoreConnection</code>. A handle to this object can be obtained by
 * using the DN of the object.
 * 
 * <PRE>
 * 
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken); 
 * if (amsc.doesEntryExist(rDN)) { 
 *     AMFilteredRole dg = amsc.getFileteredRole(rDN); 
 * }
 * 
 * </PRE>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMFilteredRole extends AMRole {
    /**
     * Returns the search filter for the filtered role.
     * 
     * @return The filter for the filtered role.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public String getFilter() throws AMException, SSOException;

    /**
     * Sets the search filter for the filtered role.
     * 
     * @param filter
     *            filter for the filtered role.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void setFilter(String filter) throws AMException, SSOException;
}
