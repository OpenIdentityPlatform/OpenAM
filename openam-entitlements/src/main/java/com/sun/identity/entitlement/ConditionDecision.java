/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ConditionDecision.java,v 1.3 2009/09/05 00:24:04 veiming Exp $
 */

/**
 * Portions Copyrighted 2010-2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class to represent condition result and - if applicable - its advices.
 */
public class ConditionDecision {

    private boolean satisfied;
    private Map<String, Set<String>> advices;
    private long timeToLive = Long.MAX_VALUE;

    /**
     * Constructs an instance of <code>ConditionDecision</code>.
     *
     * @param satisfied Result of condition decision.
     * @param advices Advice map of condition decision.
     */
    public ConditionDecision(boolean satisfied, Map<String, Set<String>> advices) {
        this.satisfied = satisfied;
        this.advices = new HashMap<String, Set<String>>(advices);
    }

    /**
     * Constructs an instance of <code>ConditionDecision</code>.
     *
     * @param satisfied Result of condition decision.
     * @param advices Advice map of condition decision.
     * @param ttl The TTL of the decision.
     */
    public ConditionDecision(boolean satisfied, Map<String, Set<String>> advices, long ttl) {
        this(satisfied, advices);
        this.timeToLive = ttl;
    }

    /**
     * Whether the condition is satisfied.
     *
     * @return <code>true</code> if condition is fulfilled.
     */
    public boolean isSatisfied() {
        return satisfied;
    }

    /**
     * Sets satisfied state.
     *
     * @param satisfied New satisfied state.
     */
    public void setSatisfied(boolean satisfied) {
        this.satisfied = satisfied;
    }

    /**
     * Query for a list of advices associated with this condition decision.
     *
     * @return advices of condition decision.
     */
    public Map<String, Set<String>> getAdvices() {
        return Collections.unmodifiableMap(advices);
    }

    /**
     * Clears the current advices associated with this decision.
     */
    public void clearAdvices() {
        advices.clear();
    }

    /**
     * Adds an advice (represented as a ConditionDecision) to this
     * ConditionDecision.
     *
     * @param decision The decision whose advices to add.
     */
    public void addAdvices(ConditionDecision decision) {
        if (decision != null) {
            Map<String, Set<String>> otherAdvices = decision.getAdvices();
            if ((otherAdvices != null) && !otherAdvices.isEmpty()) {
                if ((advices == null) || advices.isEmpty()) {
                    advices = new HashMap<String, Set<String>>();
                }
                advices.putAll(otherAdvices);
            }
        }
    }

    /**
     * Returns the time to live (TTL) of this decision.
     *
     * @return The TTL time in ms.
     */
    public long getTimeToLive() {
        return timeToLive;
    }
}
