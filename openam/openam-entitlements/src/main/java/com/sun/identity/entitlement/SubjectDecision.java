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
 * $Id: SubjectDecision.java,v 1.2 2009/12/22 18:00:25 veiming Exp $
 *
 * Portions copyright 2010-2014 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import java.util.Map;
import java.util.Set;

/**
 * Class to represent {@link EntitlementSubject} evaluation match result and - if applicable - its advices.
 *
 * @supported.all.api
 */
public class SubjectDecision {

    private boolean satisfied;
    private Map<String, Set<String>> advices;

    /**
     * Constructs an instance of <code>SubjectDecision</code>.
     *
     * @param satisfied Result of this <code>SubjectDecision</code>.
     * @param advices advice map of this <code>SubjectDecision</code>.
     */
    public SubjectDecision(boolean satisfied, Map<String, Set<String>> advices) {
        this.satisfied = satisfied;
        this.advices = advices;
    }

    /**
     * Whether this <code>SubjectDecision</code> is satisfied.
     *
     * @return <code>true</code> if <code>SubjectDecision</code> is fulfilled.
     */
    public boolean isSatisfied() {
        return satisfied;
    }

    /**
     * Advices associated with this <code>SubjectDecision</code>.
     *
     * @return advices of <code>ConditionDecision</code>.
     */
    Map<String, Set<String>> getAdvices() {
        return advices;
    }
}
