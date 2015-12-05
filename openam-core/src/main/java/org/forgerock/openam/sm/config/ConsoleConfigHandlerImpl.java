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
package org.forgerock.openam.sm.config;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import com.google.inject.Injector;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.openam.core.guice.CoreGuiceModule.DNWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Provides a layer between SMS configuration and a simple POJO, by using annotations
 * to map attributes to bean properties.
 *
 * @since 13.0.0
 */
@Singleton
public final class ConsoleConfigHandlerImpl implements ConsoleConfigHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleConfigHandlerImpl.class);

    private final static String SERVICE_VERSION = "1.0";

    private final ConcurrentHashMap<CacheKey, Map<String, Set<String>>> attributeCache;
    private final ConcurrentHashMap<String, List<ConsoleConfigListener>> sourceListeners;
    private final Set<String> registeredSources;

    private final SMSConfigProvider configProvider;
    private final DNWrapper dnUtils;
    private final Injector injector;

    /**
     * Constructs a new console configuration handler.
     *
     * @param configProvider
     *         privileged action to retrieve an SSO token
     * @param dnUtils
     *         LDAP utils
     * @param injector
     *         dependency lookup
     */
    @Inject
    public ConsoleConfigHandlerImpl(SMSConfigProvider configProvider, DNWrapper dnUtils, Injector injector) {
        this.configProvider = configProvider;
        this.dnUtils = dnUtils;
        this.injector = injector;

        attributeCache = new ConcurrentHashMap<>();
        sourceListeners = new ConcurrentHashMap<>();
        registeredSources = new CopyOnWriteArraySet<>();
    }

    @Override
    public <C> C getConfig(String realm, Class<? extends ConsoleConfigBuilder<C>> builderType) {
        try {
            ConsoleConfigBuilder<C> builder = injector.getInstance(builderType);
            Map<String, Set<String>> collatedAttributes = getCollatedAttributes(realm, builder);
            populateAnnotatedMethods(builder, collatedAttributes);
            return builder.build(collatedAttributes);

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Invalid builder or method definition", e);
        }
    }

    private Map<String, Set<String>> getCollatedAttributes(String realm, ConsoleConfigBuilder<?> builder) {
        ConfigSource configSource = builder.getClass().getAnnotation(ConfigSource.class);

        if (configSource == null || isEmpty(configSource.value())) {
            throw new IllegalArgumentException("Builder does not declare any config sources");
        }

        Map<String, Set<String>> collatedAttributes = new HashMap<>();

        for (String source : configSource.value()) {
            CacheKey cacheKey = CacheKey.newInstance(source, realm);
            Map<String, Set<String>> attributes = attributeCache.get(cacheKey);

            if (attributes == null) {
                attributes = configProvider.getAttributes(source, realm);
                Map<String, Set<String>> existingAttributes = attributeCache.putIfAbsent(cacheKey, attributes);

                if (existingAttributes != null) {
                    attributes = existingAttributes;
                } else {
                    registerForSourceChanges(source);
                }
            }

            collatedAttributes.putAll(attributes);
        }

        return collatedAttributes;
    }

    private void populateAnnotatedMethods(ConsoleConfigBuilder<?> builder, Map<String, Set<String>> attributes)
            throws InvocationTargetException, IllegalAccessException {

        for (Method method : builder.getClass().getMethods()) {
            ConfigAttribute annotation = getConfigAttributeAnnotation(method);

            if (annotation == null) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();

            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException("Annotated methods should take a single parameter");
            }

            String attributeKey = annotation.value();

            if (!attributes.containsKey(attributeKey)) {
                throw new IllegalArgumentException("Expected attribute " + attributeKey);
            }

            Set<String> values = attributes.get(attributeKey);

            if (isEmpty(values)) {
                if (annotation.required()) {
                    throw new IllegalArgumentException("Required attribute " + attributeKey);
                }

                if (isEmpty(annotation.defaultValues())) {
                    continue;
                }

                values = asSet(annotation.defaultValues());
            }

            ConfigTransformer<?> transformer = injector.getInstance(annotation.transformer());
            method.invoke(builder, transformer.transform(values, parameterTypes[0]));
        }
    }

    private ConfigAttribute getConfigAttributeAnnotation(Method method) {
        ConfigAttribute result = method.getAnnotation(ConfigAttribute.class);

        if (result == null) {
            Class<?> parent = method.getDeclaringClass().getSuperclass();

            if (parent != null) {
                try {
                    Method superMethod = parent.getMethod(method.getName(), method.getParameterTypes());
                    result = getConfigAttributeAnnotation(superMethod);
                } catch (NoSuchMethodException e) {
                    // Ignore exception
                }
            }
        }

        return result;
    }

    @Override
    public void registerListener(ConsoleConfigListener listener, Class<? extends ConsoleConfigBuilder<?>> builderType) {
        ConfigSource configSource = builderType.getAnnotation(ConfigSource.class);

        if (configSource == null || isEmpty(configSource.value())) {
            throw new IllegalArgumentException("Listener does not declare any config sources");
        }

        for (String source : configSource.value()) {
            List<ConsoleConfigListener> listeners = sourceListeners.get(source);

            if (listeners == null) {
                listeners = new CopyOnWriteArrayList<>();
                List<ConsoleConfigListener> existingListeners = sourceListeners.putIfAbsent(source, listeners);

                if (existingListeners != null) {
                    listeners = existingListeners;
                } else {
                    registerForSourceChanges(source);
                }
            }

            listeners.add(listener);
        }
    }

    private void notifyListeners(String source, String orgName) {
        List<ConsoleConfigListener> listeners = sourceListeners.get(source);

        if (isEmpty(listeners)) {
            return;
        }

        String realm = dnUtils.orgNameToRealmName(orgName);
        attributeCache.remove(CacheKey.newInstance(source, realm));

        for (ConsoleConfigListener listener : listeners) {
            try {
                listener.configUpdate(source, realm);
            } catch (RuntimeException rE) {
                logger.error("Unexpected exception whilst updating self service config", rE);
            }
        }
    }

    private void registerForSourceChanges(String source) {
        if (!registeredSources.contains(source)) {
            synchronized (registeredSources) {
                if (!registeredSources.contains(source)) {
                    configProvider.registerListener(source, new ConfigChangeHandler());
                    registeredSources.add(source);
                }
            }
        }
    }

    private final class ConfigChangeHandler implements ServiceListener {

        @Override
        public void organizationConfigChanged(String serviceName, String version,
                String orgName, String groupName, String serviceComponent, int type) {
            if (SERVICE_VERSION.equals(version)) {
                notifyListeners(serviceName, orgName);
            }
        }

        @Override
        public void schemaChanged(String serviceName, String version) {
            // Do nothing
        }

        @Override
        public void globalConfigChanged(String serviceName, String version,
                String groupName, String serviceComponent, int type) {
            // Do nothing
        }

    }

}
