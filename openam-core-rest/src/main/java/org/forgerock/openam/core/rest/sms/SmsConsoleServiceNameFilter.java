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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.sms;

import static com.sun.identity.shared.locale.Locale.*;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.forgerock.openam.sm.ServiceSchemaManagerFactory;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

/**
 * Filter for service names to be displayed via console interface.
 *
 * The 'filter' function is based off of the various mechanisms that already exist to hide knowledge about services
 * in the old JATO interface, we handle situations where the services have chosen to be hidden
 * via XML config (hiddenServices), services which are solely used for authentication purposes
 * and thus have no value being exposed to the admin via the interface (authenticationServices),
 * situations where the service's schema simply does not allow it to be rendered (by having blank
 * i18n fields).
 *
 * The 'map' function helps to a set of internal names to a map of resourcenames -> displayable names.
 * Any service where its internal name is equal to its displayable name is not included in the returned map.
 *
 */
public class SmsConsoleServiceNameFilter {

    private final Debug debug;
    private final SmsConsoleServiceConfig smsConsoleServiceConfig;
    private final Set<String> alwaysRemovedServices;
    private final Set<SchemaType> alwaysSupportedSchemaTypes;
    private final Set<String> authenticationServices;
    private final ServiceSchemaManagerFactory serviceSchemaManagerFactory;


    @Inject
    public SmsConsoleServiceNameFilter(@Named("frRest") Debug debug, SmsConsoleServiceConfig smsConsoleServiceConfig,
                                       @Named("hiddenServices") Set<String> alwaysRemovedServices,
                                       @Named("serviceSupportedSchemaTypes") Set<SchemaType> alwaysSupportedSchemaTypes,
                                       @Named("authenticationServices") Set<String> authenticationServices,
                                       ServiceSchemaManagerFactory serviceSchemaManagerFactory) {
        this.debug = debug;
        this.smsConsoleServiceConfig = smsConsoleServiceConfig;
        this.alwaysRemovedServices = alwaysRemovedServices;
        this.alwaysSupportedSchemaTypes = alwaysSupportedSchemaTypes;
        this.authenticationServices = authenticationServices;
        this.serviceSchemaManagerFactory = serviceSchemaManagerFactory;
    }

    /**
     * Filters (mutates) the provided service set to remove services which should not be displayed to the console.
     *
     * @param services The set of service identities to filter.
     * @throws AMConfigurationException If there was an issue loading internal handlers and managers used to filter.
     */
    public void filter(Set<String> services) throws SSOException, SMSException {
        filterByAuthServices(services);
        filterByKnownServices(services);
        filterByDisplayableAttribute(services);
    }

    private void filterByAuthServices (Set<String> services) throws SMSException {
        services.removeAll(authenticationServices);
    }

    private void filterByKnownServices(Set<String> services) {
        services.removeAll(alwaysRemovedServices);
    }

    private void filterByDisplayableAttribute(Set<String> services) throws SSOException, SMSException {
        final Iterator<String> it = services.iterator();
        while (it.hasNext()) {
            final String service = it.next();

            final ServiceSchemaManager serviceSchemaManager = serviceSchemaManagerFactory.build(service);

            if (serviceSchemaManager.getPropertiesViewBeanURL() == null) {
                boolean hasAttr = false;

                for (SchemaType type : alwaysSupportedSchemaTypes) {
                    if (hasDisplayableAttributeNames(type, serviceSchemaManager)) {
                        hasAttr = true;
                        break;
                    }
                }

                if (!hasAttr) {
                    it.remove();
                }
            }
        }
    }

    private boolean hasDisplayableAttributeNames(SchemaType schemaType, ServiceSchemaManager serviceSchemaManager) {
        final Set<AttributeSchema> attributeSchemas = getAttributeSchemas(serviceSchemaManager, schemaType, null);
        for (AttributeSchema attributeSchema : attributeSchemas) {
            if (!StringUtils.isBlank(attributeSchema.getI18NKey())) {
                return true;
            }
        }

        return false;
    }

    private Set<AttributeSchema> getAttributeSchemas(ServiceSchemaManager serviceSchemaManager, SchemaType schemaType,
                                                     String subSchemaName) {
        Set<AttributeSchema> attributeSchemas = null;

        try {
            ServiceSchema ss = serviceSchemaManager.getSchema(schemaType);

            if (ss != null) {
                if (subSchemaName != null) {
                    ss = ss.getSubSchema(subSchemaName);
                }
                if (ss != null) {
                    attributeSchemas = ss.getAttributeSchemas();
                }
            }
        } catch (SMSException e) {
            debug.warning("ServiceInstanceCollectionHandler.getAttributeSchemas", e);
        }

        return attributeSchemas != null ? attributeSchemas : Collections.<AttributeSchema>emptySet();
    }

    /**
     * Maps service internal names to displayable (localized) names via their resource names.
     * Services without resource names are not included in the returned map.
     *
     *
     * @param names Set of internal service names.
     * @return A map of resource service names to display names.
     * @throws SMSException In the case where the SMS cannot be queried correctly.
     * @throws SSOException In the case where we cannot use an admin token to query the schema.
     */
    public Map<String, String> mapNameToDisplayName(Set<String> names) throws SMSException, SSOException {
        final Map<String, String> map = new HashMap<>(names.size());
        for (String name : names) {
            if (smsConsoleServiceConfig.isServiceVisible(name)) {
                final ServiceSchemaManager serviceSchemaManager = serviceSchemaManagerFactory.build(name);
                String displayName = getLocalizedServiceName(serviceSchemaManager, name);
                String resourceName = serviceSchemaManager.getResourceName();
                if (!name.equals(displayName) && !StringUtils.isBlank(resourceName)) {
                    map.put(resourceName, displayName);
                }
            }
        }
        return map;
    }

    private String getLocalizedServiceName(ServiceSchemaManager serviceSchemaManager, String service) {

        try {
            String rbName = serviceSchemaManager.getI18NFileName();

            if (!StringUtils.isBlank(rbName)) {
                String i18nKey = null;
                Set<SchemaType> types = serviceSchemaManager.getSchemaTypes();
                if (!CollectionUtils.isEmpty(types)) {
                    SchemaType type = CollectionUtils.getFirstItem(types);
                    ServiceSchema schema = serviceSchemaManager.getSchema(type);
                    if (schema != null) {
                        i18nKey = schema.getI18NKey();
                    }
                }

                if (!StringUtils.isBlank(i18nKey)) {
                    return com.sun.identity.shared.locale.Locale.getString(
                            AMResourceBundleCache.getInstance().getResBundle(rbName, getDefaultLocale()), i18nKey,
                            debug);
                }
            }
        } catch (SMSException | MissingResourceException e) {
            debug.warning("ServiceInstanceCollectionHandler.getLocalizedServiceName", e);
        }

        return service;
    }

}
