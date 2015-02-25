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
 * Portions copyright 2010-2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class to represent {@link EntitlementCondition} evaluation match result and - if applicable - its advices.
 *
 * @supported.all.api
 */
public class ConditionDecision {

    private boolean satisfied;
    private Map<String, Set<String>> advices;
    private long timeToLive = Long.MAX_VALUE;

    /**
     * Constructs an instance of <code>ConditionDecision</code>.
     *
     * @param satisfied result of this <code>ConditionDecision</code>.
     * @param advices Advice map of this <code>ConditionDecision</code>.
     */
    public ConditionDecision(boolean satisfied, Map<String, Set<String>> advices) {
        this.satisfied = satisfied;
        this.advices = new HashMap<String, Set<String>>(advices);
    }

    /**
     * Constructs an instance of <code>ConditionDecision</code>.
     *
     * @param satisfied Result of this <code>ConditionDecision</code>.
     * @param advices Advice map of this <code>ConditionDecision</code>.
     * @param ttl The TTL of this <code>ConditionDecision</code>.
     */
    public ConditionDecision(boolean satisfied, Map<String, Set<String>> advices, long ttl) {
        this(satisfied, advices);
        this.timeToLive = ttl;
    }

    /**
     * Whether this <code>ConditionDecision</code> is satisfied.
     *
     * @return <code>true</code> if <code>ConditionDecision</code> is fulfilled.
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
     * Advices associated with this <code>ConditionDecision</code>.
     *
     * @return advices of <code>ConditionDecision</code>.
     */
    public Map<String, Set<String>> getAdvices() {
        return Collections.unmodifiableMap(advices);
    }

    /**
     * Clears the current advices associated with this <code>ConditionDecision</code>.
     */
    public void clearAdvices() {
        advices.clear();
    }

    /**
     * Adds an advice (from another <code>ConditionDecision</code>) to this <code>ConditionDecision</code>.
     *
     * @param decision The <code>ConditionDecision</code> whose advices should be added to this
     *                 <code>ConditionDecision</code>.
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
     * Returns the time to live (TTL) of this <code>ConditionDecision</code>.
     *
     * @return The TTL time in ms.
     */
    public long getTimeToLive() {
        return timeToLive;
    }
}
