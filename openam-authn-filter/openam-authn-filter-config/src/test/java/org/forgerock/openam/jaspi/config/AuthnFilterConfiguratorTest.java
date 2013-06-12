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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.jaspi.config;

import org.forgerock.jaspi.container.config.Configuration;
import org.forgerock.openam.jaspi.modules.session.LocalSSOTokenSessionModule;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import javax.servlet.ServletContextEvent;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Phill Cunnington
 */
public class AuthnFilterConfiguratorTest {

    @Test
    public void shouldConfigureAuthnFilter() {

        //Given
        ServletContextEvent servletContextEvent = mock(ServletContextEvent.class);
        AuthnFilterConfigurator authnFilterConfigurator = spy(new AuthnFilterConfigurator() {
            @Override
            protected void configureAuthnFilter(Configuration configuration) {
            }
        });

        //When
        authnFilterConfigurator.contextInitialized(servletContextEvent);

        //Then
        ArgumentCaptor<Configuration> argumentCaptor = ArgumentCaptor.forClass(Configuration.class);
        verify(authnFilterConfigurator).configureAuthnFilter(argumentCaptor.capture());
        Configuration configuration = argumentCaptor.getValue();
        assertEquals(configuration.keySet().size(), 1);
        Map<String, Object> authContext = configuration.get("all");
        Map<String, String> sessionModule = (Map<String, String>) authContext.get("session-module");
        assertEquals(sessionModule.get("class-name"), LocalSSOTokenSessionModule.class.getCanonicalName());
    }
}
