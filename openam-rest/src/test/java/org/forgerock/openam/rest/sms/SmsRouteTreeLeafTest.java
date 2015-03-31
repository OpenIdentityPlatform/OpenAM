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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.sms;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Nullable;

import org.forgerock.guava.common.base.Function;
import org.forgerock.json.resource.Router;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SmsRouteTreeLeafTest {

    private SmsRouteTree routeTree;

    @BeforeClass
    public void setup() {
        Router router = new Router();
        Function<String, Boolean> handlesFunction = new Function<String, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(String serviceName) {
                return "SERVICE_NAME".equals(serviceName);
            }
        };

        routeTree = new SmsRouteTreeLeaf(router, handlesFunction);
    }

    @DataProvider(name = "handlesFunction")
    private Object[][] getHandlesFunctionData() {
        return new Object[][]{
            {"SERVICE_NAME", routeTree},
            {"OTHER_SERVICE_NAME", null},
        };
    }

    @Test(dataProvider = "handlesFunction")
    public void handlesShouldReturnThisRouteTreeInstanceIfHandlesFunctionReturnsTrue(String serviceName,
            SmsRouteTree expectedRouteTree) {

        //When
        SmsRouteTree tree = routeTree.handles(serviceName);

        //Then
        assertThat(tree).isEqualTo(expectedRouteTree);
    }
}
