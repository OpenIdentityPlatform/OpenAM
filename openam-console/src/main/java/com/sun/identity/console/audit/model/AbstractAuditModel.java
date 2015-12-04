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
package com.sun.identity.console.audit.model;

import static com.sun.identity.console.audit.AuditConsoleConstants.AUDIT_SERVICE;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMResBundleCacher;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.console.base.model.SMSubConfig;
import com.sun.identity.console.base.model.SMSubConfigComparator;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceSchema;

import javax.servlet.http.HttpServletRequest;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Abstract Audit configuration UI model.
 *
 * @since 13.0.0
 */
public abstract class AbstractAuditModel extends AMServiceProfileModelImpl {

    private static final String AUDIT_BUNDLE_NAME = "audit";
    private static final String SECTION_FILE_NAME_SUFFIX = ".section.properties";

    protected ResourceBundle handlerResourceBundle;

    /**
     * Create a new {@code AbstractAuditModel}.
     *
     * @param request The {@code HttpServletRequest}
     * @param sessionAttributes The session attributes.
     * @throws AMConsoleException If construction fails.
     */
    public AbstractAuditModel(HttpServletRequest request, Map sessionAttributes) throws AMConsoleException {
        super(request, AUDIT_SERVICE, sessionAttributes);
    }

    /**
     * The service schema for the configuration represented by this model.
     *
     * @return The service schema.
     * @throws SMSException If an SMS error occurred.
     * @throws SSOException If an SSO error occurred.
     */
    protected abstract ServiceSchema getServiceSchema() throws SMSException, SSOException;

    /**
     * The service config for the configuration represented by this model.
     *
     * @return The service config.
     * @throws SMSException If an SMS error occurred.
     * @throws SSOException If an SSO error occurred.
     */
    protected abstract ServiceConfig getServiceConfig() throws SMSException, SSOException;

    @Override
    protected void initialize(HttpServletRequest req, String rbName) {
        super.initialize(req, rbName);
        handlerResourceBundle = AMResBundleCacher.getBundle(AUDIT_BUNDLE_NAME, locale);
    }

    @Override
    public String getLocalizedString(String key) {
        if (handlerResourceBundle.containsKey(key)) {
            return Locale.getString(handlerResourceBundle, key, debug);
        } else {
            return super.getLocalizedString(key);
        }
    }

    @Override
    public String getPropertySheetXML(String realmName, String viewBeanName, String viewBeanClassName) throws
            AMConsoleException {

        DelegationConfig dc = DelegationConfig.getInstance();
        boolean readOnly = !dc.hasPermission(realmName, serviceName, PERMISSION_MODIFY, this, viewBeanClassName);
        xmlBuilder.setAllAttributeReadOnly(readOnly);
        xmlBuilder.setSupportSubConfig(true);
        xmlBuilder.setViewBeanName(viewBeanName);

        try {
            return xmlBuilder.getXML();
        } catch (SSOException | SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Get the JATO XML configuration for generation the UI used to edit event handlers.
     *
     * @param realmName The current realm.
     * @param handlerName The audit event handler name.
     * @param viewBeanClassName The view bean class name.
     * @return The JATO XML configuration for generation the UI.
     * @throws AMConsoleException If an error occurs during the XML creation.
     */
    public String getEditEventHandlerPropertyXML(String realmName, String handlerName, String viewBeanClassName)
            throws AMConsoleException {

        DelegationConfig dc = DelegationConfig.getInstance();
        boolean readOnly = !dc.hasPermission(realmName, serviceName, PERMISSION_MODIFY, this, viewBeanClassName);

        try {
            String schemaId = getServiceConfig().getSubConfig(handlerName).getSchemaID();
            ServiceSchema handlerSchema = getServiceSchema().getSubSchema(schemaId);
            updateHandlerResourceBundle(handlerSchema);
            xmlBuilder = new PropertyXMLBuilder(handlerSchema, this, handlerResourceBundle,
                    getSectionsForHandler(schemaId), schemaId + SECTION_FILE_NAME_SUFFIX);
            xmlBuilder.setAllAttributeReadOnly(readOnly);
            xmlBuilder.setSupportSubConfig(false);
            return xmlBuilder.getXML();
        } catch (SMSException | SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Get the JATO XML configuration for generation the UI used to add event handlers.
     *
     * @param schemaId The event handler schema ID.
     * @return The JATO XML configuration for generation the UI.
     * @throws AMConsoleException If an error occurs during the XML creation.
     */
    public String getAddEventHandlerPropertyXML(String schemaId) throws AMConsoleException {
        try {
            ServiceSchema handlerSchema = getServiceSchema().getSubSchema(schemaId);
            updateHandlerResourceBundle(handlerSchema);
            xmlBuilder = new PropertyXMLBuilder(handlerSchema, this, handlerResourceBundle,
                    getSectionsForHandler(schemaId), schemaId + SECTION_FILE_NAME_SUFFIX);
            xmlBuilder.setSupportSubConfig(false);
            String xml = xmlBuilder.getXML();
            String attributeNameXML = AMAdminUtils.getStringFromInputStream(getClass().getClassLoader()
                    .getResourceAsStream("com/sun/identity/console/propertyAuditEventHandlerName.xml"));
            return PropertyXMLBuilder.prependXMLProperty(xml, attributeNameXML);
        } catch (SMSException | SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Get the configuration properties of all the audit event handlers.
     *
     * @return A list of config objects.
     */
    public List<SMSubConfig> getEventHandlerConfigurations() throws AMConsoleException {
        List<SMSubConfig> subConfigModelList = new ArrayList<>();
        try {
            ServiceConfig serviceConfig = getServiceConfig();
            if (serviceConfig == null) {
                return subConfigModelList;
            }

            Set<String> auditHandlerNames = serviceConfig.getSubConfigNames();
            for (String name : auditHandlerNames) {
                ServiceConfig conf = serviceConfig.getSubConfig(name);
                subConfigModelList.add(new SMSubConfig(conf.getComponentName(), name, conf.getSchemaID()));
            }

            sort(subConfigModelList, new SMSubConfigComparator(Collator.getInstance(getUserLocale())));
        } catch (SMSException | SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
        return subConfigModelList;
    }

    /**
     * Get the type names (schema ID) for all the event handlers.
     *
     * @return A set of event handler type names.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getEventHandlerTypeNames() throws AMConsoleException {
        try {
            return getServiceSchema().getSubSchemaNames();
        } catch (SMSException | SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Delete the event handlers specified.
     *
     * @param names Set of event handler names that are to be deleted.
     * @throws AMConsoleException if sub configurations cannot be deleted.
     */
    public void deleteEventHandles(Set<String> names) throws AMConsoleException {
        try {
            ServiceConfig serviceConfig = getServiceConfig();
            if (serviceConfig != null) {
                for (String name : names) {
                    serviceConfig.removeSubConfig(name);
                }
            }
        } catch (SSOException | SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    @Override
    public void setAttributeValues(Map map) throws AMConsoleException {
        try {
            ServiceConfig serviceConfig = getServiceConfig();
            if (serviceConfig == null) {
                return;
            }
            serviceConfig.setAttributes(map);
        } catch (SSOException | SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    @Override
    public Map getAttributeValues() {
        try {
            ServiceConfig serviceConfig = getServiceConfig();
            if (serviceConfig == null) {
                return emptyMap();
            }
            return serviceConfig.getAttributes();
        } catch (SSOException | SMSException e) {
            debug.error("AbstractAuditModel.getAttributeValues", e);
        }
        return emptyMap();
    }

    /**
     * Get the attribute values for the specified event handler.
     *
     * @param eventHandlerName The name of the event handler.
     * @return A map of event handler attribute values.
     * @throws AMConsoleException If an error occurs whilst reading the attributes.
     */
    public Map<?, ?> getEventHandlerAttributeValues(String eventHandlerName) throws AMConsoleException {
        try {
            ServiceConfig serviceConfig = getServiceConfig();
            if (serviceConfig == null) {
                return emptyMap();
            }

            Set<String> subConfigNames = serviceConfig.getSubConfigNames();
            if (!subConfigNames.contains(eventHandlerName)) {
                return emptyMap();
            }

            return serviceConfig.getSubConfig(eventHandlerName).getAttributes();
        } catch (SSOException | SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Set the attribute values for the specified event handler.
     *
     * @param eventHandlerName The name of the event handler.
     * @param values The attribute values.
     * @throws AMConsoleException If an error occurs whilst writing the attributes.
     */
    public void setEventHandlerAttributeValues(String eventHandlerName, Map<?, ?> values) throws AMConsoleException {
        try {
            ServiceConfig serviceConfig = getServiceConfig();
            if (serviceConfig == null) {
                return;
            }

            Set<String> subConfigNames = serviceConfig.getSubConfigNames();
            if (!subConfigNames.contains(eventHandlerName)) {
                return;
            }

            serviceConfig.getSubConfig(eventHandlerName).setAttributes(values);
        } catch (SSOException | SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Get the default attribute values for the specified event handler type (schema ID).
     *
     * @param eventHandlerType The name of the event handler type.
     * @return A map of default event handler attribute values.
     * @throws AMConsoleException If an error occurs whilst reading the attributes.
     */
    public Map<String, Set<?>> getEventHandlerDefaultValues(String eventHandlerType) throws AMConsoleException {
        try {
            Map<String, Set<?>> defaultValues = new HashMap<>();
            ServiceSchema handlerSchema = getServiceSchema().getSubSchema(eventHandlerType);
            Set attributeSchemas = handlerSchema.getAttributeSchemas();

            for (Object value : attributeSchemas) {
                AttributeSchema as = (AttributeSchema) value;
                Set values = as.getDefaultValues();
                if (values != null) {
                    defaultValues.put(as.getName(), values);
                }
            }
            return defaultValues;
        } catch (SSOException | SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Get the attribute names for the specified event handler type (schema ID).
     *
     * @param eventHandlerType The name of the event handler type.
     * @return A set of attribute type names.
     * @throws AMConsoleException If an error occurs whilst reading the attributes.
     */
    public Set<String> getEventHandlerAttributeNames(String eventHandlerType) throws AMConsoleException {
        try {
            ServiceSchema handlerSchema = getServiceSchema().getSubSchema(eventHandlerType);
            Set attributeSchemas = handlerSchema.getAttributeSchemas();
            Set<String> attrNames = new HashSet<>();

            for (Object as : attributeSchemas) {
                attrNames.add(((AttributeSchema) as).getName());
            }
            return attrNames;
        } catch (SSOException | SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Create a new event handler with the given attributes.
     *
     * @param eventHandlerName The name of the new event handler.
     * @param eventHandlerType The name of the event handler type.
     * @param attributeValues  The attribute values.
     * @throws AMConsoleException If an error occurs whilst creating the event handler.
     */
    public void createEventHandler(String eventHandlerName, String eventHandlerType,
                                   Map<String, Set<String>> attributeValues) throws AMConsoleException {
        try {
            getServiceConfig().addSubConfig(eventHandlerName, eventHandlerType, 0, attributeValues);
        } catch (SSOException | SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    private void updateHandlerResourceBundle(ServiceSchema handlerSchema) {
        String handlerBundleName = handlerSchema.getI18NFileName();
        if (isNotEmpty(handlerBundleName) && !AUDIT_BUNDLE_NAME.equals(handlerBundleName)) {
            handlerResourceBundle = new MultiResourceBundle(locale, handlerBundleName, AUDIT_BUNDLE_NAME);
        }
    }

    private Map<SchemaType, List<String>> getSectionsForHandler(String schemaId) {
        Map<SchemaType, List<String>> sectionMap = new HashMap<>();
        String sections = getLocalizedString("sections." + schemaId);
        if (isNotEmpty(sections)) {
            List<String> sectionList = asList(sections.split(" "));
            sectionMap.put(SchemaType.GLOBAL, sectionList);
            sectionMap.put(SchemaType.ORGANIZATION, sectionList);

        }
        return sectionMap;
    }

    private static final class MultiResourceBundle extends ResourceBundle {

        private Set<ResourceBundle> resourceBundles = new LinkedHashSet<>();

        private MultiResourceBundle(java.util.Locale locale, String... rbNames) {
            for (String rbName : rbNames) {
                ResourceBundle rb = AMResBundleCacher.getBundle(rbName, locale);
                if (rb != null) {
                    resourceBundles.add(rb);
                }
            }
        }

        @Override
        protected Object handleGetObject(String key) {
            for (ResourceBundle rb : resourceBundles) {
                if (rb.containsKey(key)) {
                    return rb.getObject(key);
                }
            }
            return null;
        }

        @Override
        public Enumeration<String> getKeys() {
            List<String> keys = new ArrayList<>();
            for (ResourceBundle rb : resourceBundles) {
                keys.addAll(Collections.list(rb.getKeys()));
            }
            return Collections.enumeration(keys);
        }
    }

}
