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
 * Store for containing the information useful to monitoring timings of policy evaluations.
 */
public class EvaluationTimingStore extends AbstractPolicyTimingStore {

    @Inject
    public EvaluationTimingStore(EntitlementConfigurationWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Records a specific policy evaluation run time instance. If the duration to execute the policy evaluation took
     * longer than the current slowest recorded duration, we store the information of this evaluation as a
     * {@link PolicyTimingEntry}.
     *
     * @param duration Length of time (in ms) this evaluation took
     * @param realm The realm in which the evaluation took place
     * @param applicationName The application in which the evaluation took place
     * @param resourceName The resource name against which the evaluation took place
     * @param subject The subject against which the evaluation took place
     */
    public void addTiming(long duration, String realm, String applicationName, String resourceName, Subject subject) {
        durationStore.add(new PolicyTimingEntry(duration, realm, applicationName, resourceName, subject));
    }

}
