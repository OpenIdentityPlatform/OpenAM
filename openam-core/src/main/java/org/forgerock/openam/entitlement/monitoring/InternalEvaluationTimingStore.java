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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.entitlement.monitoring;

import javax.inject.Inject;
import javax.security.auth.Subject;

/**
 * Store for containing the timing associated with individual policy evaluations.
 */
public class InternalEvaluationTimingStore extends AbstractPolicyTimingStore {

    @Inject
    public InternalEvaluationTimingStore(EntitlementConfigurationWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Adds a new {@link PolicyTimingEntry} to our duration list.
     *
     * @param policyName The name of the policy evaluated
     * @param duration Length of time (in ms) this evaluation took
     * @param realm The realm in which the evaluation took place
     * @param applicationName The application in which the evaluation took place
     * @param resourceName The resource name against which the evaluation took place
     * @param subject The subject against which the evaluation took place
     */
    public void addTiming(String policyName, long duration, String realm, String applicationName,
                          String resourceName, Subject subject) {

        durationStore.add(new PolicyTimingEntry(policyName, duration, realm, applicationName, resourceName, subject));

    }

}
