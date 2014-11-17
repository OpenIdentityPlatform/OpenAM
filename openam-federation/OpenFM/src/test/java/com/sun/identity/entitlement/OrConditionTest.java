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
 * $Id: OrConditionTest.java,v 1.2 2009/09/05 00:24:03 veiming Exp $
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

public class OrConditionTest {

    @Test
    public void testConstruction() throws Exception {
        IPv4Condition ipc = new IPv4Condition();
        ipc.setStartIpAndEndIp("192.168.0.1", "192.168.0.2");

        SimpleTimeCondition tc = new SimpleTimeCondition();
        tc.setStartTime("08:00");
        tc.setEndTime("16:00");
        tc.setStartDay("mon");
        tc.setEndDay("fri");
        tc.setStartDate("01/01/2001");
        tc.setEndDate("02/02/2002");
        tc.setEnforcementTimeZone("PST");

        Set<EntitlementCondition> conditions = new HashSet<EntitlementCondition>();
        conditions.add(ipc);
        conditions.add(tc);
        OrCondition oc = new OrCondition(conditions);
        OrCondition oc1 = new OrCondition();
        oc1.setState(oc.getState());

        if (!oc1.equals(oc)) {
            throw new Exception(
                "OrConditionTest.testConstruction():" +
                " OrCondition with setState does not equal OrCondition with " +
                "getState()");
        }
    }

    @Test
    public void NPEWhenEConditionsIsNull() throws Exception {
        OrCondition oc = new OrCondition();
        try {
            oc.toString();
        } catch (NullPointerException e) {
            throw new Exception(
                "OrConditionTest.NPEWhenEConditionsIsNull failed.");
        }
    }
}
