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
 * $Id: NotConditionTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import java.util.HashSet;
import java.util.Set;
import static org.testng.Assert.assertTrue;

import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.testng.annotations.Test;

public class NotConditionTest {

    @Test
    public void testConstruction() throws Exception {

        IPv4Condition ipc = new IPv4Condition();
        ipc.setStartIpAndEndIp("100.100.100.100", "200.200.200.200");
        NotCondition ac = new NotCondition(ipc);
        NotCondition ac1 = new NotCondition();
        ac1.setState(ac.getState());

        if (!ac1.equals(ac)) {
            throw new Exception(
                "NotConditionTest.testConstruction():" +
                " NotCondition with setState does not equal NotCondition " +
                "with getState()");
        }

    }


    @Test (expectedExceptions = IllegalArgumentException.class)
    public void testSingleSubject() throws Exception {
        //given
        Set<EntitlementCondition> conditions = new HashSet<EntitlementCondition>();

        IPv4Condition ip = new IPv4Condition();
        ip.setStartIpAndEndIp("192.168.0.1", "192.168.0.2");
        IPv4Condition ip2 = new IPv4Condition();
        ip2.setStartIpAndEndIp("192.168.0.5", "192.168.0.6");

        conditions.add(ip);
        conditions.add(ip2);

        NotCondition myNotCondition = new NotCondition();

        //when
        myNotCondition.setEConditions(conditions);

        //then -- expect error

    }

    @Test
    public void testSingleSubjectEnforced() throws Exception{
        //given
        Set<EntitlementCondition> conditions = new HashSet<EntitlementCondition>();
        IPv4Condition ip = new IPv4Condition();
        ip.setStartIpAndEndIp("192.168.0.1", "192.168.0.2");
        conditions.add(ip);
        NotCondition myNotCondition = new NotCondition();

        //when
        myNotCondition.setEConditions(conditions);

        //then
        assertTrue(myNotCondition.getECondition().equals(ip));

    }

    @Test
    public void testSingleSubjectEnforcedRetrieval() throws Exception {
        //given
        IPv4Condition ip = new IPv4Condition();
        ip.setStartIpAndEndIp("192.168.0.1", "192.168.0.2");
        NotCondition myNotCondition = new NotCondition(ip);

        //when
        myNotCondition.setECondition(ip);

        //then
        assertTrue(myNotCondition.getEConditions().iterator().next().equals(ip));
    }

}
