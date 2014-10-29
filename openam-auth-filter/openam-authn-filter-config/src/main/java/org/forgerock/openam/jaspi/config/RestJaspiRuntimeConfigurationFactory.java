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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.config;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.JaspiLoggingConfigurator;
import org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.auth.shared.AuthDebugLogger;
import org.forgerock.openam.jaspi.modules.session.OpenAMSessionModule;

import javax.security.auth.message.MessageInfo;

/**
 * A singleton instance that implements both the Jaspi ModuleConfigurationFactory and JaspiLoggingConfigurator
 * interfaces, that provides all of the configuration information for the Jaspi Runtime to be configured correctly.
 *
 * @since 12.0.0
 */
public enum RestJaspiRuntimeConfigurationFactory implements JaspiLoggingConfigurator, ModuleConfigurationFactory {

    /**
     * The Singleton instance of the JaspiRuntimeConfigurationFactory.
     */
    INSTANCE;

    public static final String LOG_NAME = "restAuthenticationFilter";
    private static final Debug DEBUG = Debug.getInstance(LOG_NAME);

    /**
     * Gets the ModuleConfigurationFactory that the Jaspi Runtime will use to configure its authentication
     * modules.
     *
     * @return An instance of a ModuleConfigurationFactory.
     */
    public static ModuleConfigurationFactory getModuleConfigurationFactory() {
        return INSTANCE;
    }

    /**
     * Gets the Logging Configurator that the Jaspi Runtime will use to configure its debug and audit logger
     * instances.
     *
     * @return An instance of a JaspiLoggingConfigurator.
     */
    public static JaspiLoggingConfigurator getLoggingConfigurator() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue getConfiguration() {
        return JsonValue.json(
            JsonValue.object(
                JsonValue.field(SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                    JsonValue.field(SESSION_MODULE_KEY, JsonValue.object(
                        JsonValue.field(AUTH_MODULE_CLASS_NAME_KEY, OpenAMSessionModule.class.getName()),
                        JsonValue.field(AUTH_MODULE_PROPERTIES_KEY, JsonValue.object())
                    ))
                ))
            )
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DebugLogger getDebugLogger() {
        return new AuthDebugLogger(LOG_NAME);
    }

    /**
     * {@inheritDoc}
     * <p>
     * As this instance of the Jaspi Runtime is only protecting REST endpoints with a Session Auth Module, the call
     * to the audit logger will never occur. So it is fine to just use the NOP audit logger here.
     *
     * @return {@inheritDoc}
     */
    @Override
    public AuditLogger<MessageInfo> getAuditLogger() {
        return null;
    }
}
