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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.authentication.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.service.AMLoginContext;

public class LoginContextFactoryTest {

    private Configuration configuration;
    private AMLoginContext amLoginContext;
    private final String CONFIG_NAME = "config_name";
    private Subject subject;

    @BeforeMethod
    public void setup() {
        configuration = mock(Configuration.class);
        when(configuration.getAppConfigurationEntry(CONFIG_NAME)).thenReturn(new AppConfigurationEntry[0]);
        amLoginContext = mock(AMLoginContext.class);
        subject = new Subject();
        Configuration.setConfiguration(configuration); // Override configuration set by AMLoginContext

    }

    @Test
    public void rejectsNullConfigName() {
        // Given
        LoginContextFactory loginContextFactory = LoginContextFactory.getInstance();

        // When
        try {
            loginContextFactory.createLoginContext(amLoginContext, subject, null, true, configuration);
            fail("Should reject null config name");
        } catch (LoginException e) {
            // Then
        }
    }

    @Test
    public void generatesJAASLoginContext() throws Exception {
        // Given
        LoginContextFactory loginContextFactory = LoginContextFactory.getInstance();

        // When
        LoginContext loginContext = loginContextFactory.createLoginContext(amLoginContext, subject, CONFIG_NAME, true, configuration);

        // Then
        assertThat(loginContext).isInstanceOf(LoginContextWrapper.class);
        assertThat(loginContext.getSubject()).isEqualTo(subject);
    }

    @Test
    public void generatesJAASLoginContextWithNullSubject() throws Exception {
        // Given
        LoginContextFactory loginContextFactory = LoginContextFactory.getInstance();

        // When
        LoginContext loginContext = loginContextFactory.createLoginContext(amLoginContext, null, CONFIG_NAME, true, configuration);

        // Then
        assertThat(loginContext).isInstanceOf(LoginContextWrapper.class);
        assertThat(loginContext.getSubject()).isNull();
    }

    @Test
    public void generatesNonJAASLoginContext() throws Exception {
        // Given
        LoginContextFactory loginContextFactory = LoginContextFactory.getInstance();

        // When
        LoginContext loginContext = loginContextFactory.createLoginContext(amLoginContext, subject, CONFIG_NAME, false, configuration);

        // Then
        assertThat(loginContext).isInstanceOf(com.sun.identity.authentication.jaas.LoginContext.class);
        assertThat(loginContext.getSubject()).isEqualTo(subject);
    }

    @Test
    public void generatesNonJAASLoginContextWithNullSubject() throws Exception {
        // Given
        LoginContextFactory loginContextFactory = LoginContextFactory.getInstance();

        // When
        LoginContext loginContext = loginContextFactory.createLoginContext(amLoginContext, null, CONFIG_NAME, false, configuration);

        // Then
        assertThat(loginContext).isInstanceOf(com.sun.identity.authentication.jaas.LoginContext.class);
        assertThat(loginContext.getSubject()).isNull();
    }
}
