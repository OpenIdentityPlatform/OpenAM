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
 * $Id: AMAssignableDynamicGroup.java,v 1.5 2008/06/25 05:41:19 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Set;


import com.iplanet.sso.SSOException;

/**
 * <p>
 * The <code>AMAssignableDynamicGroup</code> interface provides methods to
 * manage assignable dynamic group. <code>AMAssignableDynamicGroup</code>
 * objects can be obtained by using <code>AMStoreConnection</code>. A handle
 * to this object can be obtained by using the DN of the object.
 * 
 * <pre>
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken);
 * if (amsc.doesEntryExist(aDN)) {
 *     AMAssignableDynamicGroup adg = amsc.getAssignableDynamicGroup(aDN);
 * }
 * </pre>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */

public interface AMAssignableDynamicGroup extends AMGroup {
    /**
     * Adds users to the assignable dynamic group.
     * 
     * @param users
     *            The set of user distinguished names to be added to the
     *            assignable dynamic group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void addUsers(Set users) throws AMException, SSOException;

    /**
     * Removes users from the assignable dynamic group.
     * 
     * @param users
     *            The set of user distinguished names to be removed from the
     *            assignable dynamic group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeUsers(Set users) throws AMException, SSOException;

    /**
     * Returns true if the assignable dynamic group is subscribable.
     * 
     * @return true if the assignable dynamic group is subscribable.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public boolean isSubscribable() throws AMException, SSOException;

    /**
     * Sets subscribe-ability of the assignable dynamic group.
     * 
     * @param subscribable
     *            true if the assignable dynamic group is subscribable.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void setSubscribable(boolean subscribable) throws AMException,
            SSOException;
}
