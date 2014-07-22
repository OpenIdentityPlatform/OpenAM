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

package org.forgerock.openam.authentication.modules.scripted;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.openam.scripting.ScriptEngineConfiguration;
import org.forgerock.openam.scripting.StandardScriptEngineManager;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Listens for changes in the global configuration for the scripted auth module and propagates those changes to the
 * {@link org.forgerock.openam.scripting.StandardScriptEngineManager} so that individual engines can be configured.
 */
@Singleton
public class ScriptedAuthConfigurator implements ServiceListener {
    public static final String ATTR_NAME_PREFIX = "iplanet-am-auth-scripted-";
    public static final String SCRIPT_TIMEOUT_PARAM = ATTR_NAME_PREFIX + "server-timeout";
    public static final String THREAD_POOL_CORE_SIZE_PARAM = ATTR_NAME_PREFIX + "core-threads";
    public static final String THREAD_POOL_MAX_SIZE_PARAM = ATTR_NAME_PREFIX + "max-threads";
    public static final String THREAD_POOL_QUEUE_SIZE_PARAM = ATTR_NAME_PREFIX + "queue-size";
    public static final String THREAD_POOL_IDLE_TIMEOUT_PARAM = ATTR_NAME_PREFIX + "idle-timeout";
    public static final String WHITELIST_PARAM = ATTR_NAME_PREFIX + "white-list";
    public static final String BLACKLIST_PARAM = ATTR_NAME_PREFIX + "black-list";
    public static final String USE_SECURITYMANAGER_PARAM = ATTR_NAME_PREFIX + "use-security-manager";

    private static final int DEFAULT_CORE_THREADS = 10;
    private static final int DEFAULT_MAX_THREADS = 10;
    private static final int DEFAULT_QUEUE_SIZE = 10;
    private static final long DEFAULT_IDLE_TIMEOUT = 60l; // Seconds

    private static final String SCRIPTED_AUTH_SERVICE_NAME = "iPlanetAMAuthScriptedService";
    private static final Debug DEBUG = Debug.getInstance("amScript");

    private final StandardScriptEngineManager scriptEngineManager;
    private volatile boolean initialised = false;

    /**
     * Constructs the configurator to configure the given script engine manager. You must call
     * {@link #registerServiceListener()} to start listening for configuration changes.
     *
     * @param scriptEngineManager the script engine manager to configure. Not null.
     */
    @Inject
    public ScriptedAuthConfigurator(final StandardScriptEngineManager scriptEngineManager) {
        Reject.ifNull(scriptEngineManager);

        this.scriptEngineManager = scriptEngineManager;
    }

    /**
     * Registers this configurator with the {@link com.sun.identity.sm.ServiceConfigManager} to receive updates
     * when the scripted auth module configuration changes.
     *
     * @throws java.lang.IllegalStateException if the configuration listener cannot be registered.
     */
    public void registerServiceListener() {
        if (!initialised) {

            try {
                final SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());

                final ServiceConfigManager serviceConfigManager =
                        new ServiceConfigManager(SCRIPTED_AUTH_SERVICE_NAME, adminToken);
                final String listenerId = serviceConfigManager.addListener(this);
                if (listenerId == null) {
                    throw new SMSException("Unable to register service config listener");
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Registered service config listener: " + listenerId);
                }

                updateConfig();
                initialised = true;

            } catch (SMSException e) {
                DEBUG.error("Unable to create ServiceConfigManager", e);
                throw new IllegalStateException(e);
            } catch (SSOException e) {
                DEBUG.error("Unable to create ServiceConfigManager", e);
                throw new IllegalStateException(e);
            }
        }

    }

    /**
     * Not used.
     */
    public void schemaChanged(final String serviceName, final String version) {
        // Ignore
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Propagates scripted authentication configuration changes to the configured
     * {@link org.forgerock.openam.scripting.StandardScriptEngineManager}. Ignores all other changes.
     */
    public void globalConfigChanged(final String serviceName, final String version, final String groupName,
                                    final String serviceComponent, final int type) {

        if (!SCRIPTED_AUTH_SERVICE_NAME.equals(serviceName)) {
            return;
        }

        updateConfig();
    }

    /**
     * Not used.
     */
    public void organizationConfigChanged(final String serviceName, final String version, final String orgName,
                                          final String groupName, final String serviceComponent, final int type) {
        // Ignore
    }

    /**
     * Propagates scripted authentication module global configuration to the script engine manager.
     */
    private void updateConfig() {
        @SuppressWarnings("unchecked")
        final Map<String, Set<String>> config = AuthUtils.getGlobalAttributes(SCRIPTED_AUTH_SERVICE_NAME);

        final int coreThreadSize = CollectionHelper.getIntMapAttr(config, THREAD_POOL_CORE_SIZE_PARAM,
                DEFAULT_CORE_THREADS, DEBUG);
        final int maxThreadSize = CollectionHelper.getIntMapAttr(config, THREAD_POOL_MAX_SIZE_PARAM,
                DEFAULT_MAX_THREADS, DEBUG);
        final int queueSize = CollectionHelper.getIntMapAttr(config, THREAD_POOL_QUEUE_SIZE_PARAM,
                DEFAULT_QUEUE_SIZE, DEBUG);
        final long idleTimeout = CollectionHelper.getLongMapAttr(config, THREAD_POOL_IDLE_TIMEOUT_PARAM,
                DEFAULT_IDLE_TIMEOUT, DEBUG);
        final long scriptTimeout = CollectionHelper.getLongMapAttr(config, SCRIPT_TIMEOUT_PARAM,
                ScriptEngineConfiguration.NO_TIMEOUT, DEBUG);
        final boolean useSystemSecurityManager = CollectionHelper.getBooleanMapAttr(config, USE_SECURITYMANAGER_PARAM,
                true);
        final Set<String> whiteList = config.get(WHITELIST_PARAM);
        final Set<String> blackList = config.get(BLACKLIST_PARAM);

        final SecurityManager securityManager = useSystemSecurityManager ? System.getSecurityManager() : null;

        final ScriptEngineConfiguration configuration =
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

        scriptEngineManager.setConfiguration(configuration);
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
    static List<Pattern> compilePatternList(final Set<String> patterns) {
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
}
