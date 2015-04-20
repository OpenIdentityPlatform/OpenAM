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

import java.security.Principal;
import java.util.Set;
import javax.security.auth.Subject;
import org.forgerock.openam.shared.monitoring.TimingEntry;

/**
 * Used to store details on how long a policy evaluation took. If a specific policy evaluation is being recorded then
 * the policy name value will be set. If not, and this timing information pertains instead to the length of time
 * to perform all policy evaluations for a specific resource, the policy name value will be left null.
 */
public class PolicyTimingEntry implements TimingEntry {

    private final String policyName;
    private final long duration;
    private final String realm;
    private final String applicationName;
    private final String resourceName;
    private final Subject subject;

    /**
     * Constructor for non-policy-specific timing events.
     *
     * @param duration Length of time the evaluation took
     * @param realm Realm under which the evaluation took place
     * @param applicationName Application under which the evaluation took place
     * @param resourceName Resource name against which the evaluation took place
     * @param subject Subject against which the evaluation took place
     */
    public PolicyTimingEntry(long duration, String realm, String applicationName, String resourceName, Subject subject) {
        this(null, duration, realm, applicationName, resourceName, subject);
    }

    /**
     * Constructor for a policy-specific timing event.
     *
     * @param policyName Name of the policy evaluated
     * @param duration Length of time the evaluation took
     * @param realm Realm under which the evaluation took place
     * @param applicationName Application under which the evaluation took place
     * @param resourceName Resource name against which the evaluation took place
     * @param subject Subject against which the evaluation took place
     */
    public PolicyTimingEntry(String policyName, long duration, String realm, String applicationName,
                             String resourceName, Subject subject) {
        this.policyName = policyName;
        this.duration = duration;
        this.realm = realm;
        this.applicationName = applicationName;
        this.resourceName = resourceName;
        this.subject = subject;
    }

    /**
     * String representation of this {@link PolicyTimingEntry}. Used to report out the information via the
     * monitoring system.
     *
     * @return String representation of the information contained within this TimingEntry.
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        if (policyName != null ) {
            sb.append("Policy: ").append(policyName).append("; ");
        }

        sb.append("Realm: ").append(realm).append("; Application: ").append(applicationName)
                .append("; Resource: ").append(resourceName).append("; Subject: ")
                .append(getPrincipal(subject)).append("; Duration: ").append(duration).append("ms");

        return sb.toString();
    }

    /**
     * Retrieves the principal's user ID as a String.
     *
     * @param subject The subject under scrutiny
     * @return A string representation of the subject's initial principal.
     */
    private String getPrincipal(Subject subject) {
        Set<Principal> userPrincipals = subject.getPrincipals();
        return ((userPrincipals != null) && !userPrincipals.isEmpty()) ?
                userPrincipals.iterator().next().getName() : null;
    }

    /**
     * Gets the duration of this TimingEntry.
     *
     * @return the duration in ms.
     */
    public long getDuration() {
        return duration;
    }
}