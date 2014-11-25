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
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ApplicationTest {

    private EntitlementRegistry registry;
    private Application testApplication;

    @BeforeTest
    public void theSetUp() {
        registry = new EntitlementRegistry();
        testApplication = new Application(registry);
    }

    @Test
    public void shouldDeferToApplicationTypeComparator() throws IllegalAccessException, InstantiationException {

        //given
        ApplicationType appType = new ApplicationType(null, null, null, null, null);
        ResourceName appTypeResourceName = appType.getResourceComparator();
        testApplication.setApplicationType(appType);

        //when
        ResourceName result = testApplication.getResourceComparator();

        //then
        assertEquals(appTypeResourceName, result);
    }

    @Test
    public void shouldNotDeferToApplicationTypeComparator() throws IllegalAccessException, InstantiationException {

        //given
        ApplicationType appType = new ApplicationType(null, null, null, null, null);
        Class resourceNameClass = PrefixResourceName.class;
        testApplication.setApplicationType(appType);
        testApplication.setResourceComparator(resourceNameClass);

        //when
        ResourceName result = testApplication.getResourceComparator(false);

        //then
        assertEquals(resourceNameClass, result.getClass());

    }

}
