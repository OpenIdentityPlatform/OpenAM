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
* Copyright 2013-2014 ForgeRock Inc.
*/

package org.forgerock.openam.jaspi.config;

import junit.framework.Assert;
import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.JaspiLoggingConfigurator;
import org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.auth.shared.AuthDebugLogger;
import org.forgerock.openam.jaspi.modules.session.OpenAMSessionModule;
import org.testng.annotations.Test;

import javax.security.auth.message.MessageInfo;

import static org.forgerock.openam.jaspi.config.RestJaspiRuntimeConfigurationFactory.AUTH_MODULE_CLASS_NAME_KEY;
import static org.forgerock.openam.jaspi.config.RestJaspiRuntimeConfigurationFactory.AUTH_MODULE_PROPERTIES_KEY;
import static org.forgerock.openam.jaspi.config.RestJaspiRuntimeConfigurationFactory.SERVER_AUTH_CONTEXT_KEY;
import static org.forgerock.openam.jaspi.config.RestJaspiRuntimeConfigurationFactory.SESSION_MODULE_KEY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class RestJaspiRuntimeConfigurationFactoryTest {

    @Test
    public void shouldGetConfiguration() {

        //Given

        //When
        JsonValue configuration = RestJaspiRuntimeConfigurationFactory.INSTANCE.getConfiguration();

        //Then
        assertEquals(configuration.get(SERVER_AUTH_CONTEXT_KEY)
                .get(SESSION_MODULE_KEY)
                .get(AUTH_MODULE_CLASS_NAME_KEY).asString(), OpenAMSessionModule.class.getName());
        assertTrue(configuration.get(SERVER_AUTH_CONTEXT_KEY)
                .get(SESSION_MODULE_KEY)
                .get(AUTH_MODULE_PROPERTIES_KEY).asMap().isEmpty());
    }

    @Test
    public void shouldGetDebugLogger() {

        //Given

        //When
        DebugLogger debugLogger = RestJaspiRuntimeConfigurationFactory.INSTANCE.getDebugLogger();

        //Then
        Assert.assertTrue(debugLogger.getClass().isAssignableFrom(AuthDebugLogger.class));
    }

    @Test
    public void shouldGetAuditLogger() {

        //Given

        //When
        AuditLogger<MessageInfo> auditLogger = RestJaspiRuntimeConfigurationFactory.INSTANCE.getAuditLogger();

        //Then
        assertNull(auditLogger);
    }

    @Test
    public void shouldBeSingleton() {

        //Given

        //When
        JaspiLoggingConfigurator loggingConfigurator = RestJaspiRuntimeConfigurationFactory.getLoggingConfigurator();
        ModuleConfigurationFactory configurationFactory =
                RestJaspiRuntimeConfigurationFactory.getModuleConfigurationFactory();

        //Then
        assertEquals(loggingConfigurator, configurationFactory);
    }
}

