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
 * $Id: EntitlementTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 *
 * Portions copyright 2013 ForgeRock AS.
 */
package com.sun.identity.entitlement;


import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author dillidorai
 */
public class EntitlementTest {

    private static String SERVICE_NAME = "iPlanetAMWebAgentService";

    @Test
    public void testConstruction() throws Exception {
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("POST", Boolean.TRUE);
        String resourceName = "http://www.sun.com";
        Entitlement ent = new Entitlement(SERVICE_NAME, resourceName, actionValues);
        ent.setName("entitlement1");

        Entitlement ent1 = new Entitlement(SERVICE_NAME, resourceName, actionValues);

        if (ent.equals(ent1)) {
            throw new Exception("EntitlementTest.testConstruction(): "
                    + "equality test for false failed.");
        }
        ent1.setName("entitlement1");

        if (!ent.equals(ent1)) {
            throw new Exception("EntitlementTest.testConstruction(): " +
                    "equality test for true failed.");
        }
    }

    @Test
    public void testHashCode() throws Exception {
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("POST", Boolean.TRUE);
        String resourceName = "http://www.sun.com";
        Entitlement ent = new Entitlement(SERVICE_NAME, resourceName, actionValues);
        ent.setName("entitlement1");

        Entitlement ent1 = new Entitlement(SERVICE_NAME, resourceName, actionValues);

        if (ent.hashCode() == ent1.hashCode()) {
            throw new Exception("EntitlementTest.testConstruction(): "
                    + "hashcode test failed (hashcodes were the same, should have been different.");
        }
        ent1.setName("entitlement1");

        if (ent.hashCode() != ent1.hashCode()) {
            throw new Exception("EntitlementTest.testConstruction(): "
                    + "hashcode test failed (hashcodes were not the same, should have been.");
        }
    }

    @Test
    public void verifyAdvicePopulation() {
        Entitlement entitlement = new Entitlement("http://test.example.com/*", CollectionUtils.asSet("GET", "POST"));

        assertThat(entitlement.getAdvices()).isNull();
        assertThat(entitlement.hasAdvice()).isFalse();

        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();
        advices.put("someAdvice", CollectionUtils.asSet("property1", "property2"));
        entitlement.setAdvices(advices);

        assertThat(entitlement.getAdvices()).isEqualTo(advices);
        assertThat(entitlement.hasAdvice()).isTrue();
    }
}
