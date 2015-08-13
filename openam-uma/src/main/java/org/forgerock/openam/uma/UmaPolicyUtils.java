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

package org.forgerock.openam.uma;

import static org.forgerock.openam.uma.UmaConstants.BackendPolicy.BACKEND_POLICY_ACTION_VALUES_KEY;
import static org.forgerock.openam.uma.UmaConstants.BackendPolicy.BACKEND_POLICY_SUBJECT_CLAIM_VALUE_KEY;
import static org.forgerock.openam.uma.UmaConstants.UmaPolicy.SUBJECT_KEY;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.JsonValue;

/**
 * Utility methods for interacting with Policies for UMA.
 */
public class UmaPolicyUtils {

    /**
     * Given the JSON representation of a policy engine UMA policy, returns the subject.
     * @param policy The JSON representation of the policy engine policy.
     * @return The subject id.
     */
    public static String getPolicySubject(JsonValue policy) {
        return policy.get(SUBJECT_KEY).get(BACKEND_POLICY_SUBJECT_CLAIM_VALUE_KEY).asString();
    }

    /**
     * Given the JSON representation of a policy engine UMA policy, returns the scopes that are granted by it.
     * @param policy The JSON representation of the policy engine policy.
     * @return The set of scope strings.
     */
    public static Set<String> getPolicyScopes(JsonValue policy) {
        return new HashSet<>(policy.get(BACKEND_POLICY_ACTION_VALUES_KEY).asMap().keySet());
    }

}
