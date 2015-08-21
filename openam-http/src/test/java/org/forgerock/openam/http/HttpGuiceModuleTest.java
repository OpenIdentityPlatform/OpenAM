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

package org.forgerock.openam.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.fail;

import com.google.inject.ConfigurationException;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@GuiceModules(HttpGuiceModule.class)
public class HttpGuiceModuleTest extends GuiceTestCase {

    @Test
    public void shouldExposeHttpApplication() {
        //When
        HttpApplication httpApplication = InjectorHolder.getInstance(HttpApplication.class);

        //Then
        assertThat(httpApplication).isNotNull();
    }

    @DataProvider
    private Object[][] nonExposedBindings() {
        return new Object[][]{
            {Key.get(Handler.class, Names.named("HttpHandler"))},
            {Key.get(new TypeLiteral<Iterable<HttpRouteProvider>>() { })},
        };
    }

    @Test(dataProvider = "nonExposedBindings")
    public void shouldNotExposeHttpHandler(Key<?> bindingKey) {
        try {
            //When
            InjectorHolder.getInstance(bindingKey);
            fail();
        } catch (ConfigurationException e) {
            //Then
            assertThat(e.getErrorMessages().iterator().next().getMessage())
                    .contains("It was already configured on one or more child injectors or private modules")
                    .contains("bound at org.forgerock.openam.http.HttpGuiceModule")
                    .contains("If it was in a PrivateModule, did you forget to expose the binding?");
        }
    }
}
