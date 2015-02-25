/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock Inc.
 */
package com.sun.identity.sm;

import com.iplanet.sso.SSOToken;

import java.util.Iterator;
import java.util.Set;

/**
 * DAO definition for service management.
 *
 * @author apforrest
 */
public interface ServiceManagementDAO {

    /**
     * Returns the DNs and its attribute values that match the filter. The search
     * is performed from the root suffix ie., DN. It searches for SMS objects only.
     *
     * @param token
     *         Single-Sign On token.
     * @param dn
     *         Base DN
     * @param filter
     *         Search Filter.
     * @param numOfEntries
     *         number of max entries, 0 means unlimited
     * @param timeLimit
     *         maximum number of seconds for the search to spend, 0 means unlimited
     * @param sortResults
     *         <code>true</code> to have result sorted.
     * @param ascendingOrder
     *         <code>true</code> to have result sorted in
     *         ascending order.
     * @param exclude
     *         List of DN to exclude.
     * @return DNs and its attribute values that match the filter.
     * @throws SMSException
     *         When an underlying error occurs.
     */
    public Iterator<SMSDataEntry> search(SSOToken token, String dn, String filter, int numOfEntries,
                                         int timeLimit, boolean sortResults, boolean ascendingOrder,
                                         Set<String> exclude) throws SMSException;

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     *
     * @param dn
     *         The DN in question.
     * @param token
     *         Single-Sign On token.
     * @return Whether the DN exists.
     */
    public boolean checkIfEntryExists(String dn, SSOToken token);

    /**
     * @return The root suffix (dn).
     */
    public String getRootSuffix();

}
