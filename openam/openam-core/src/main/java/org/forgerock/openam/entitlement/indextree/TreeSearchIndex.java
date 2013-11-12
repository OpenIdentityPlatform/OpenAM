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
package org.forgerock.openam.entitlement.indextree;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.shared.debug.Debug;

/**
 * This search index implementation acts as a go between for the entitlements framework and the delegate.
 * The benefit is that a dumb implementation can be substituted when in the client context removing the
 * need for SDK to package up third party dependencies.
 *
 * @author apforrest
 */
public class TreeSearchIndex implements ISearchIndex {

    private static final Debug DEBUG = Debug.getInstance("amEntitlements");

    private static final ISearchIndex DELEGATE;

    static {
        // Static initialisation to avoid the unnecessary recreation of instances that hold no state.
        if (SystemProperties.isServerMode()) {
            DELEGATE = new TreeSearchIndexDelegate();
        } else {
            DEBUG.message("Client context, dummy implementation of tree search index instantiated.");
            DELEGATE = new DummySearchIndex();
        }
    }

    @Override
    public ResourceSearchIndexes getIndexes(String resource, String realm) throws EntitlementException {
        return DELEGATE.getIndexes(resource, realm);
    }

    // Implementation represents a dumb service that has no behaviour. This is to work around
    // the desire to keep third part dependencies, which includes Guice, out of the client SDK.
    private static final class DummySearchIndex implements ISearchIndex {

        @Override
        public ResourceSearchIndexes getIndexes(String resource, String realm) throws EntitlementException {
            throw new UnsupportedOperationException("Behaviour not supported on the client.");
        }

    }

}
