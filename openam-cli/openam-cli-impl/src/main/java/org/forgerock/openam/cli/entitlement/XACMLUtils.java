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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.cli.entitlement;

import static org.forgerock.openam.utils.CollectionUtils.*;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationEvaluatorImpl;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;

public final class XACMLUtils {

    private static final Debug DEBUG = Debug.getInstance("amCLI");

    private XACMLUtils() {
    }

    public static boolean hasPermission(String realm, SSOToken adminToken, String action) {
        try {
            DelegationEvaluator de = new DelegationEvaluatorImpl();
            DelegationPermission dp = new DelegationPermission(realm, "rest", "1.0", "policies", action,
                    asSet(action), Collections.<String, String>emptyMap());
            return de.isAllowed(adminToken, dp, Collections.EMPTY_MAP);
        } catch (DelegationException de) {
            DEBUG.error("XACMLUtils.hasPermission", de);
            return false;
        } catch (SSOException ssoe) {
            DEBUG.error("XACMLUtils.hasPermission", ssoe);
            return false;
        }
    }
}
