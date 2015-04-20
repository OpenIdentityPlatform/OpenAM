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
package org.forgerock.openam.entitlement.conditions.environment;

import com.sun.identity.entitlement.EntitlementException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SimpleTimeConditionTest {

    private SimpleTimeCondition simpleTimeCondition;

    @BeforeMethod
    public void theSetUp() {
        simpleTimeCondition = new SimpleTimeCondition();
    }

    @Test
    public void shouldSucceedStartEndTime() throws EntitlementException {

        //given
        simpleTimeCondition.setStartTime("12:00");
        simpleTimeCondition.setEndTime("13:00");

        //when
        simpleTimeCondition.validate();

        //then -- no failures.
    }

    @Test
    public void shouldSucceedStartEndDay() throws EntitlementException {

        //given
        simpleTimeCondition.setStartDay("tues");
        simpleTimeCondition.setEndDay("mon");

        //when
        simpleTimeCondition.validate();

        //then -- no failures.
    }

    @Test
    public void shouldSucceedStartEndDate() throws EntitlementException {

        //given
        simpleTimeCondition.setStartDate("2000:01:01");
        simpleTimeCondition.setEndDate("2040:01:01");

        //when
        simpleTimeCondition.validate();

        //then -- no failures.
    }

    @Test(expectedExceptions=EntitlementException.class)
    public void shouldFailStartAfterEnd() throws EntitlementException {

        //given
        simpleTimeCondition.setEndDate("2000:01:01");
        simpleTimeCondition.setStartDate("2040:01:01");

        //when
        simpleTimeCondition.validate();

        //then -- failure expected.
    }

}
