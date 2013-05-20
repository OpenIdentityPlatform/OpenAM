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
 * Until SMSEntry is refactored to separate out its various responsibilities, for now this wrapper class delegates to it.
 * This allows consumers to not be coupled to a concrete implementation and to static method calls.
 *
 * @author apforrest
 */
public class ServiceManagementDAOWrapper implements ServiceManagementDAO {

    @Override
    public Iterator<SMSDataEntry> search(SSOToken token, String dn, String filter,
                                         int numOfEntries, int timeLimit, boolean sortResults,
                                         boolean ascendingOrder, Set<String> exclude) throws SMSException {

        // This assignment is valid.
        // The warning is flagged up due to integration with legacy code.
        @SuppressWarnings("unchecked")
        Iterator<SMSDataEntry> results = SMSEntry.search(
                token, dn, filter, numOfEntries, timeLimit, sortResults, ascendingOrder, exclude);

        return results;
    }

    @Override
    public boolean checkIfEntryExists(String dn, SSOToken token) {
        return SMSEntry.checkIfEntryExists(dn, token);
    }

    @Override
    public String getRootSuffix() {
        return SMSEntry.getRootSuffix();
    }

}
