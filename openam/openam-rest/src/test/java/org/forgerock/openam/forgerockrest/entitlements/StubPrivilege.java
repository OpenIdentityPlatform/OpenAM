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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stub {@link Privilege} subclass for testing that does nothing.
 *
 * @since 12.0.0
 */
public class StubPrivilege extends Privilege {

    public StubPrivilege() {
        super();
    }

    public StubPrivilege(String name) throws EntitlementException {
        super();
        setName(name);
    }

    @Override
    public List<Entitlement> evaluate(Subject adminSubject, String realm, Subject subject, String applicationName,
                                      String normalisedResourceName, String requestedResourceName,
                                      Set<String> actionNames, Map<String, Set<String>> environment, boolean recursive,
                                      Object context) throws EntitlementException {

        return Collections.emptyList();
    }

    @Override
    protected void init(JSONObject jo) {
        // Do nothing
    }
}
