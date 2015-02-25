/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AndConditionTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.forgerock.openam.entitlement.conditions.environment.SimpleTimeCondition;
import org.testng.annotations.Test;

/**
 *
 * @author dillidorai
 */
public class AndConditionTest {

    @Test
    public void testConstruction() throws Exception {

        IPv4Condition ipc = new IPv4Condition();
        ipc.setStartIpAndEndIp("100.100.100.100", "200.200.200.200");
        SimpleTimeCondition tc = new SimpleTimeCondition();
        tc.setStartTime("08:00");
        tc.setEndTime("16:00");
        tc.setStartDay("mon");
        tc.setEndDay("fri");
        tc.setStartDate("01/01/2001");
        tc.setEndDate("02/02/2002");
        tc.setEnforcementTimeZone("PST");
        Set<EntitlementCondition> conditions
                = new HashSet<EntitlementCondition>();
        conditions.add(ipc);
        conditions.add(tc);
        AndCondition ac = new AndCondition(conditions);

        AndCondition ac1 = new AndCondition();
        ac1.setState(ac.getState());

        if (!ac1.equals(ac1)) {
            throw new Exception("AndConditionTest.testConstruction():"
                    + "AndCondition with setState="
                    +  "does not equal AndCondition with getState()");
        }
    }

    @Test
    public void NPEWhenEConditionsIsNull() throws Exception {
        AndCondition oc = new AndCondition();
        try {
            oc.toString();
        } catch (NullPointerException e) {
            throw new Exception(
                "AndConditionTest.NPEWhenEConditionsIsNull failed.");
        }
    }
}
