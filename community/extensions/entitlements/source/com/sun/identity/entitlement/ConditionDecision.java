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
 * $Id: ConditionDecision.java,v 1.3 2009/04/07 10:25:08 veiming Exp $
 */
package com.sun.identity.entitlement;
import java.util.Map;
import java.util.Set;

/**
 * Class to represent condition result and advices
 */
public class ConditionDecision {
    private boolean satisfied;
    private Map<String, Set<String>> advices;
    public static final String TIME_TO_LIVE = "timeToLive";

    /**
     * Constructs an instance of <code>ConditionDecision</code>
     * @param satisfied boolean result of condition decision
     * @param advices advice map of condition decision
     */
    public ConditionDecision(
        boolean satisfied,
        Map<String, Set<String>> advices) {
        this.satisfied = satisfied;
        this.advices = advices;
    }

    /**
     * Returns boolean result of condition decsion
     * @return boolean result of condiiton decision
     */
    public boolean isSatisfied() {
        return satisfied;
    }

    /**
     * Returns advices of condition decsion
     * @return advices of condiiton decision
     */
    public Map<String, Set<String>> getAdvices() {
        return advices;
    }
}
