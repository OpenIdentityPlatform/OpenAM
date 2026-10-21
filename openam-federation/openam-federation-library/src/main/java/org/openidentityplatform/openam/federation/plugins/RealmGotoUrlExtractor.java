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
package org.openidentityplatform.openam.federation.plugins;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

/**
 * The realm's <em>Valid goto URL</em> list, read through the federation configuration plugin.
 * It is the list the login {@code goto} parameter is checked against; the federation library
 * cannot reach the core extractor of that list, so this one reads the same service attribute
 * through {@link ConfigurationManager}.
 * <p>
 * It answers the way {@link ValidDomainExtractor} asks, and the way the core extractor does: an
 * empty collection for a realm that was read and has no list - the attribute absent or empty, or
 * no configuration of the service in the realm - and {@code null} for a list that could not be
 * read - a read failure, or no configuration plugin to read it through. The validator holds an
 * empty list to the origins this server answers on, and never takes an unreadable one for empty.
 */
public class RealmGotoUrlExtractor implements ValidDomainExtractor<String> {

    /** The component name {@code ConfigurationInstanceImpl} maps to the validation service. */
    public static final String VALIDATION_COMPONENT = "VALIDATION";
    static final String VALID_GOTO_RESOURCES = "openam-auth-valid-goto-resources";
    private static final String ROOT_REALM = "/";
    private static final Debug DEBUG = Debug.getInstance("libSAML");

    /** Where the validation service is read from; a seam for tests. */
    interface ConfigurationSource {
        ConfigurationInstance get() throws ConfigurationException;
    }

    private final ConfigurationSource source;

    public RealmGotoUrlExtractor() {
        this(new ConfigurationSource() {
            @Override
            public ConfigurationInstance get() throws ConfigurationException {
                return ConfigurationManager.getConfigurationInstance(VALIDATION_COMPONENT);
            }
        });
    }

    RealmGotoUrlExtractor(ConfigurationSource source) {
        this.source = source;
    }

    @Override
    public Collection<String> extractValidDomains(String realm) {
        try {
            ConfigurationInstance instance = source.get();
            if (instance == null) {
                // Nothing to read the realm configuration through, so nothing is known about its list.
                DEBUG.warning("RealmGotoUrlExtractor.extractValidDomains: no configuration plugin for "
                        + VALIDATION_COMPONENT);
                return null;
            }
            Map<?, ?> attributes = instance.getConfiguration(realm == null || realm.isEmpty() ? ROOT_REALM : realm, null);
            if (attributes == null) {
                return Collections.emptySet();
            }
            Object values = attributes.get(VALID_GOTO_RESOURCES);
            if (!(values instanceof Collection)) {
                return Collections.emptySet();
            }
            Set<String> patterns = new LinkedHashSet<>();
            for (Object value : (Collection<?>) values) {
                if (value != null) {
                    patterns.add(value.toString());
                }
            }
            return patterns;
        } catch (ConfigurationException e) {
            DEBUG.error("RealmGotoUrlExtractor.extractValidDomains: cannot read the valid goto URLs of realm "
                    + realm, e);
            return null;
        }
    }
}
