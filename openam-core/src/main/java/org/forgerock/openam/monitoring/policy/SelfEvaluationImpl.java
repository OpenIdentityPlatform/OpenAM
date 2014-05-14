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
import org.forgerock.openam.entitlement.monitoring.PolicyMonitoringType;

/**
 * Implementation of the SNMP {@link SelfEvaluation} interface.
 *
 * Uses the {@link PolicyMonitor} singleton to record and access its data.
 */
public class SelfEvaluationImpl extends SelfEvaluation {

    private final PolicyMonitor policyMonitor;

    /**
     * Constructs an instance of the {@link SelfEvaluation} interface.
     * Injects a {@link PolicyMonitor} using Guice.
     *
     * @param myMib The MIB.
     */
    public SelfEvaluationImpl(SnmpMib myMib) {
        super(myMib);

        this.policyMonitor = InjectorHolder.getInstance(PolicyMonitor.class);
    }

    /**
     * Getter for the "EvaluationsMaximum" variable.
     */
    public Long getSelfEvaluationsMaximum() throws SnmpStatusException {
        return policyMonitor.getEvaluationMaxRate(PolicyMonitoringType.SELF);
    }

    /**
     * Getter for the "EvaluationsMinimum" variable.
     */
    public Long getSelfEvaluationsMinimum() throws SnmpStatusException {
        return policyMonitor.getEvaluationMinRate(PolicyMonitoringType.SELF);
    }

    /**
     * Getter for the "EvaluationsAverage" variable.
     */
    public Long getSelfEvaluationsAverage() throws SnmpStatusException {
        return policyMonitor.getEvaluationAvgRate(PolicyMonitoringType.SELF);
    }

    /**
     * Getter for the "EvaluationsCumulative" variable.
     */
    public Long getSelfEvaluationsCumulative() throws SnmpStatusException {
        return policyMonitor.getEvaluationCumulativeCount(PolicyMonitoringType.SELF);
    }


}
