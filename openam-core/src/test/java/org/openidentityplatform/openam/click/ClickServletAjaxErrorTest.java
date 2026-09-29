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
 * Copyright 2026 3A Systems, LLC.
 */
package org.openidentityplatform.openam.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openidentityplatform.openam.click.service.ConfigService;
import org.openidentityplatform.openam.click.service.LogService;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * An Ajax request that fails answers a fixed message: the exception, its message included,
 * belongs in the log, whatever the application mode.
 */
public class ClickServletAjaxErrorTest {

    private static final String SECRET = "LDAP bind as cn=Directory Manager failed: <script>x</script>";

    /** The servlet with the response writer and the services it reaches for replaced. */
    private static final class Fixture {
        final StringWriter body = new StringWriter();
        final LogService log = mock(LogService.class);
        final ClickServlet servlet = new ClickServlet() {
            @Override
            PrintWriter getPrintWriter(HttpServletResponse response) {
                return new PrintWriter(body);
            }
        };

        Fixture(boolean productionMode) {
            ConfigService config = mock(ConfigService.class);
            when(config.isProductionMode()).thenReturn(productionMode);
            when(config.isProfileMode()).thenReturn(false);
            servlet.configService = config;
            servlet.logger = log;
        }
    }

    @DataProvider
    public Object[][] modes() {
        return new Object[][] {{true}, {false}};
    }

    @Test(dataProvider = "modes")
    public void theResponseCarriesAFixedMessageAndNotTheException(boolean productionMode) {
        Fixture fixture = new Fixture(productionMode);
        HttpServletResponse response = mock(HttpServletResponse.class);
        IllegalStateException failure = new IllegalStateException(SECRET);

        fixture.servlet.handleAjaxException(mock(HttpServletRequest.class), response, false, failure, null);

        String body = fixture.body.toString();
        assertThat(body).contains("The application encountered an unexpected error.");
        assertThat(body).doesNotContain("IllegalStateException").doesNotContain("Directory Manager")
                .doesNotContain("<script>").doesNotContain("\tat ");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(fixture.log).error("handleException: ", failure);
    }
}
