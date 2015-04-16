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

package org.forgerock.openam.scripting;

import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static org.forgerock.openam.scripting.ScriptConstants.*;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHENTICATION_SERVER_SIDE;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHORIZATION_ENTITLEMENT_CONDITION;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Listens for changes in the global configuration for scripts and propagates those changes to the
 * {@link StandardScriptEngineManager} so that individual engines can be configured.
 *
 * @since 13.0.0
 */
@Singleton
public class ScriptEngineConfigurator implements ServiceListener {

    private final Logger logger;
    private volatile boolean initialised = false;

    /**
     * Construct a new instance of {@link org.forgerock.openam.scripting.ScriptEngineConfigurator}
     * @param logger The logger to log any error and debug messages to.
     */
    @Inject
    public ScriptEngineConfigurator(@Named("ScriptLogger")Logger logger) {
        this.logger = logger;
    }

    /**
     * Registers this configurator with the {@link com.sun.identity.sm.ServiceConfigManager} to receive updates
     * when the script configuration changes.
     *
     * @throws IllegalStateException if the configuration listener cannot be registered.
     */
    public void registerServiceListener() {
        if (!initialised) {

            try {
                String listenerId = new ServiceConfigManager(SERVICE_NAME, getAdminToken()).addListener(this);
                if (listenerId == null) {
                    throw new SMSException("Unable to register service config listener");
                }
                logger.info("Registered service config listener: %s", listenerId);

                updateConfig(AUTHORIZATION_ENTITLEMENT_CONDITION);
                updateConfig(AUTHENTICATION_SERVER_SIDE);
                initialised = true;

            } catch (SMSException e) {
                logger.error("Unable to create ServiceConfigManager", e);
                throw new IllegalStateException(e);
            } catch (SSOException e) {
                logger.error("Unable to create ServiceConfigManager", e);
                throw new IllegalStateException(e);
            }
        }

    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        // Ignore
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                                          String serviceComponent, int type) {
        // Ignore
    }

    /**
     * Propagates script configuration changes to the configured {@link StandardScriptEngineManager}.
     * Ignores all other changes.
     */
    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                                    int type) {

        if (!SERVICE_NAME.equals(serviceName) || StringUtils.isEmpty(serviceComponent)) {
            return;
        }

        serviceComponent = serviceComponent.startsWith("/") ? serviceComponent.substring(1).trim() : serviceComponent;
        String[] components = serviceComponent.toUpperCase().split("/");
        if (components.length == 2 && ENGINE_CONFIGURATION.equalsIgnoreCase(components[1])) {
            try {
                updateConfig(ScriptContext.valueOf(components[0]));
            } catch (IllegalArgumentException e) {
                logger.error("Script Context does not exist: " + components[0], e);
            }
        }

    }

    private StandardScriptEngineManager getScriptEngineManager(ScriptContext context) {
        return InjectorHolder.getInstance(Key.get(StandardScriptEngineManager.class, Names.named(context.name())));
    }

    /**
     * Propagates script global configuration to the script engine manager.
     * @param context The script context for which the config is updated.
     */
    protected void updateConfig(ScriptContext context) {

        final Map<String, Set<String>> config = getEngineConfigurationSchema(context);
        int coreThreadSize = parseInt(getMapAttr(config, THREAD_POOL_CORE_SIZE), DEFAULT_CORE_THREADS);
        int maxThreadSize = parseInt(getMapAttr(config, THREAD_POOL_MAX_SIZE), DEFAULT_MAX_THREADS);
        int queueSize = parseInt(getMapAttr(config, THREAD_POOL_QUEUE_SIZE), DEFAULT_QUEUE_SIZE);
        long idleTimeout = parseLong(getMapAttr(config, THREAD_POOL_IDLE_TIMEOUT), DEFAULT_IDLE_TIMEOUT_SECONDS);
        long scriptTimeout = parseLong(getMapAttr(config, SCRIPT_TIMEOUT), ScriptEngineConfiguration.NO_TIMEOUT);
        boolean useSystemSecurityManager = getBooleanMapAttr(config, USE_SECURITY_MANAGER, true);
        SecurityManager securityManager = useSystemSecurityManager ? System.getSecurityManager() : null;
        Set<String> whiteList = config.get(WHITE_LIST);
        Set<String> blackList = config.get(BLACK_LIST);

        ScriptEngineConfiguration configuration =
                ScriptEngineConfiguration.builder()
                    .withSecurityManager(securityManager)
                    .withThreadPoolCoreSize(coreThreadSize)
                    .withThreadPoolMaxSize(maxThreadSize)
                    .withThreadPoolQueueSize(queueSize)
                    .withThreadPoolIdleTimeout(idleTimeout, TimeUnit.SECONDS)
                    .withTimeout(scriptTimeout, TimeUnit.SECONDS)
                    .withWhiteList(compilePatternList(whiteList))
                    .withBlackList(compilePatternList(blackList))
                    .build();

        getScriptEngineManager(context).setConfiguration(configuration);
    }

    private SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    private Map<String, Set<String>> getEngineConfigurationSchema(ScriptContext context) {
        try {
            ServiceConfig globalConfig =
                    new ServiceConfigManager(SERVICE_NAME, getAdminToken()).getGlobalConfig("default");
            if (globalConfig != null) {
                ServiceConfig contextConfig = globalConfig.getSubConfig(context.name());
                if (contextConfig != null) {
                    ServiceConfig engineConfig = contextConfig.getSubConfig(ENGINE_CONFIGURATION);
                    if (engineConfig != null) {
                        return engineConfig.getAttributes();
                    }
                }
            }
        } catch (SMSException e) {
            logger.error("ScriptEngineConfigurator.updateConfig", e);
        } catch (SSOException e) {
            logger.error("ScriptEngineConfigurator.updateConfig", e);
        }

        logger.error("No engine configuration called '" + ENGINE_CONFIGURATION + "' found for context '"
                + context.name() + "' in service '" + SERVICE_NAME + "'. Using default configuration.");

        return Collections.emptyMap();
    }

    /**
     * Compiles a set of class-name patterns into equivalent regular expressions. Each pattern consists of a series of
     * name-parts separated by '.' characters. The only wild-card supported is '*' which is translated into a non-greedy
     * match ({@code .*?}) in the regular expression. All other characters are escaped. The order of the resulting list
     * is the iteration order of the set. There is no way to include a literal '*' character in the pattern, but '*' is
     * not a valid character in a Java class name identifier.
     *
     * @param patterns the list of glob-style patterns to translate.
     * @return the equivalent regular expression patterns.
     */
    protected List<Pattern> compilePatternList(Set<String> patterns) {
        final List<Pattern> result = new ArrayList<Pattern>();

        if (patterns != null) {
            for (final String pattern : patterns) {
                if (!pattern.isEmpty()) {
                    // Translate '*' characters into non-greedy wildcard (.*?) and quote everything else.
                    final String regex = "\\Q" + pattern.replace("*", "\\E.*?\\Q") + "\\E";
                    result.add(Pattern.compile(regex));
                }
            }
        }

        return result;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            logger.error("ScriptEngineConfigurator.parseInt", nfe);
            return defaultValue;
        }
    }

    private long parseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            logger.error("ScriptEngineConfigurator.parseLong", nfe);
            return defaultValue;
        }
    }
}
