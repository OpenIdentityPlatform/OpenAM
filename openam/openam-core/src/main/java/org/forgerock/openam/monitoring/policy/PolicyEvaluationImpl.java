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
package org.forgerock.openam.monitoring.policy;

import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.entitlement.monitoring.PolicyMonitor;

/**
 * Implementation of the SNMP {@link SubtreeTiming} interface.
 *
 * Uses the {@link PolicyMonitor} singleton to record and access its data.
 */
public class PolicyEvaluationImpl extends PolicyEvaluation {

    private final PolicyMonitor policyMonitor;

    /**
     * Constructs an instance of the {@link PolicyEvaluation} interface.
     * Injects a {@link PolicyMonitor} using Guice.
     *
     * @param myMib The MIB.
     */
    public PolicyEvaluationImpl(SnmpMib myMib) {
        super(myMib);

        this.policyMonitor = InjectorHolder.getInstance(PolicyMonitor.class);
    }

    /**
     * Getter for the "SlowestInternalEvaluation" variable.
     */
    public String getSlowestPolicyEvaluationTime() throws SnmpStatusException {
        return policyMonitor.getSlowestInternalEvaluation();
    }
}
