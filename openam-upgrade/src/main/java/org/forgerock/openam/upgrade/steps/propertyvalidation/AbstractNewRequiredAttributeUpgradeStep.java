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
package org.forgerock.openam.upgrade.steps.propertyvalidation;

import java.security.PrivilegedAction;
import java.util.*;

import javax.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.steps.AbstractUpgradeStep;
import org.forgerock.openam.utils.CollectionUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.*;

/**
 * An upgrade step for adding validation to services and handling existing instances of those services appropriately.
 *
 * This abstract class should be extended by a class specifically for the type of services which provides
 * a list of DefaultPropertyValue implementations that will provide information on the appropriate action to take.
 */
abstract class AbstractNewRequiredAttributeUpgradeStep extends AbstractUpgradeStep {

    // I18n String Literals
    private static final String VALIDATION_ADDED_I18N_STRING = "upgrade.service.validation.addedToField";
    private static final String SERVICE_REMOVAL_I18N_STRING = "upgrade.service.validation.serviceWillBeRemoved";
    private static final String SERVICE_REMOVAL_I18N_STRING_PLURAL =
            "upgrade.service.validation.serviceWillBeRemoved.plural";
    private static final String SERVICE_TEMPORARY_ATTRIBUTE_I18N_STRING =
            "upgrade.service.validation.temporaryAttributeSet";
    private static final String SERVICE_VALIDATION_I18N_STRING = "upgrade.service.validation.updated";
    private static final String SERVICE_VALIDATION_I18N_STRING_PLURAL = "upgrade.service.validation.updated.plural";
    private static final String SERVICE_REMOVED_I18N_STRING = "upgrade.service.validation.serviceRemoved";
    private static final String SERVICE_REMOVED_I18N_STRING_PLURAL = "upgrade.service.validation.serviceRemoved.plural";
    private static final String SERVICE_TEMPORARY_ATTRIBUTES_I18N_STRING =
            "upgrade.service.validation.temporaryAttributesSet";
    private static final String SERVICE_TEMPORARY_ATTRIBUTES_I18N_STRING_PLURAL =
            "upgrade.service.validation.temporaryAttributesSet.plural";
    private static final String UPGRADE_START_SERVICE_VALIDATION = "upgrade.service.validation.upgrade.start";


    private ServiceConfigManager scm;
    private ServiceSchemaManager ssm;

    private final Map<String, Set<DefaultPropertyValue>> defaultsToApply = new HashMap<>();
    private final Set<DefaultPropertyValue> schemasToUpdate = new HashSet<>();

    @Inject
    AbstractNewRequiredAttributeUpgradeStep(final PrivilegedAction<SSOToken> adminTokenAction,
                               @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public void initialize() throws UpgradeException {
        final SSOToken token = getAdminToken();
        try {
            scm = new ServiceConfigManager(getServiceName(), token);
            ssm = new ServiceSchemaManager(getServiceName(), token);

            findSchemaAttributesThatNeedValidators();
            findEmptyValuesInNeedOfDefaults();
        } catch (ServiceNotFoundException ex) {
            DEBUG.message("amAdminConsole service not found. Nothing to upgrade", ex);
        } catch (SMSException | SSOException ex) {
            DEBUG.error("An error occurred while trying to create Service Config and Schema Managers.", ex);
            throw new UpgradeException("Unable to create Service Config and Schema Managers.", ex);
        }
    }

    private void findSchemaAttributesThatNeedValidators() throws UpgradeException {
        try {
            final ServiceSchema serviceSchema = ssm.getOrganizationSchema();
            String newValidatorName = getValidatorName();

            for (DefaultPropertyValue defaultValue : getNewRequiredAttributeDefaultValues()) {
                AttributeSchema attributeSchema = serviceSchema.getAttributeSchema(defaultValue.getFieldName());
                String validator = attributeSchema.getValidator();

                if (null == validator || validator.isEmpty() || !validator.contains(newValidatorName)) {
                    schemasToUpdate.add(defaultValue);
                }
            }
        } catch (SMSException e) {
            DEBUG.error("An error occurred while trying to find service scemas in need to new validators.", e);
            throw new UpgradeException("Unable to retrieve OAuth2 Providers.", e);
        }
    }

    private void findEmptyValuesInNeedOfDefaults() throws UpgradeException {
        try {

            for (String realm : getRealmNames()) {
                Set<DefaultPropertyValue> defaultsForRealm = defaultsToApply.get(realm);
                if (null == defaultsForRealm) {
                    defaultsForRealm = new HashSet<>();
                }
                final ServiceConfig serviceConfig = scm.getOrganizationConfig(realm, null);
                Map<String, Set<String>> attributesForRead = serviceConfig.getAttributesForRead();

                for (DefaultPropertyValue defaultValue : getNewRequiredAttributeDefaultValues()) {
                    Set<String> values = attributesForRead.get(defaultValue.getFieldName());
                    if (CollectionUtils.isEmpty(values) || (values.contains(null) && 1 == values.size()) ) {
                        defaultsForRealm.add(defaultValue);
                    }
                }
                if (!CollectionUtils.isEmpty(defaultsForRealm)) {
                    defaultsToApply.put(realm, defaultsForRealm);
                } else {
                    defaultsForRealm.remove(realm);
                }
            }
        } catch (Exception e) {
            DEBUG.error("An error occurred while trying to look for upgradable OAuth2 Providers.", e);
            throw new UpgradeException("Unable to retrieve OAuth2 Providers.", e);
        }
    }

    @Override
    public boolean isApplicable() {
        return !(defaultsToApply.isEmpty() && schemasToUpdate.isEmpty());
    }

    @Override
    public void perform() throws UpgradeException {
        UpgradeProgress.reportStart(UPGRADE_START_SERVICE_VALIDATION, getServiceName());
        try {
            addValidatorToSchemaAttributes();

            dealWithExistingServices();

        } catch (Exception e) {
            UpgradeProgress.reportEnd("upgrade.failed");
            DEBUG.error("An error occurred while trying to upgrade Mail Service Settings", e);
            throw new UpgradeException(e.getMessage() + " - Unable to upgrade Mail Service Settings.", e);
        }
    }

    private void addValidatorToSchemaAttributes() throws SMSException, SSOException {

        final ServiceSchema serviceSchema = ssm.getOrganizationSchema();
        String newValidatorName = getValidatorName();

        for (DefaultPropertyValue defaultValue : schemasToUpdate) {
            AttributeSchema attributeSchema = serviceSchema.getAttributeSchema(defaultValue.getFieldName());
            String validator = attributeSchema.getValidator();

            if (null == validator || validator.isEmpty()) {
                validator = newValidatorName;
            } else if (!validator.contains(newValidatorName)) {
                validator = validator + "|" + newValidatorName;
            }
            attributeSchema.setValidator(validator);
        }
    }

    private void dealWithExistingServices() throws SMSException, SSOException {

        for (Map.Entry<String, Set<DefaultPropertyValue>> entry : defaultsToApply.entrySet()) {

            final ServiceConfig configForServiceInRealm = scm.getOrganizationConfig(entry.getKey(), null);
            Map<String, Set<String>> allServiceAttributes = configForServiceInRealm.getAttributes();

            boolean serviceIsOk = true;
            for (DefaultPropertyValue defaultValue : entry.getValue()) {
                Set<String> valueSetOfAttribute = allServiceAttributes.get(defaultValue.getFieldName());
                if (CollectionUtils.isEmpty(valueSetOfAttribute)) {
                    valueSetOfAttribute = new HashSet<>();
                }
                if (valueSetOfAttribute.contains(null)) {
                    valueSetOfAttribute.remove(null);
                }
                String newValue = defaultValue.getNewDefault();
                if (null == newValue) { // this service is not valid without this attributes and should be deleted
                    DEBUG.warning("Warning - unable to find suitable value for required field %s for " +
                                    "service %s in realm %s.  The service will be removed from this realm",
                            defaultValue.getFieldName(), getServiceName(), entry.getKey());
                    allServiceAttributes.remove(defaultValue.getFieldName());
                    serviceIsOk = false;
                    // We don't break here so we continue to log warning statements for any other missing values
                } else {
                    DEBUG.warning("Warning - added temporary value %s to required field %s for " +
                            "service %s in realm %s",  defaultValue.getNewDefault(), defaultValue.getFieldName(),
                            getServiceName(), entry.getKey());
                    valueSetOfAttribute.add(defaultValue.getNewDefault());
                    allServiceAttributes.put(defaultValue.getFieldName(), valueSetOfAttribute);
                }
            }
            if (serviceIsOk) {
                configForServiceInRealm.setAttributes(allServiceAttributes);
            } else {
                scm.removeOrganizationConfiguration(entry.getKey(), null);
            }
        }
    }

    @Override
    public String getDetailedReport(String delimiter) {

        StringBuilder builder = new StringBuilder();

        if (!schemasToUpdate.isEmpty()) {
            for (DefaultPropertyValue field : schemasToUpdate) {
                builder.append(
                        String.format(BUNDLE.getString(VALIDATION_ADDED_I18N_STRING),
                                field.getFieldName(),
                                getServiceName()))
                        .append(delimiter);
            }
        }

        if (!defaultsToApply.isEmpty()) {
            for (Map.Entry<String, Set<DefaultPropertyValue>> entry : defaultsToApply.entrySet()) {

                Set<DefaultPropertyValue> nullPropertyDefaults = new HashSet<DefaultPropertyValue>();
                for (DefaultPropertyValue propertyChange : entry.getValue()) {
                    if (null == propertyChange.getNewDefault()) {
                        nullPropertyDefaults.add(propertyChange);
                    }
                }

                if (CollectionUtils.isNotEmpty(nullPropertyDefaults)) {
                    StringBuilder builder2 = new StringBuilder();
                    boolean plural = false;
                    for (DefaultPropertyValue propertyCausingServiceRemoval : nullPropertyDefaults) {
                        if (0 != builder2.length()) {
                            builder2.append(", ");
                            plural = true;
                        }
                        builder2.append(propertyCausingServiceRemoval.getFieldName());
                    }

                    builder.append(
                            String.format(
                                    BUNDLE.getString(plural?SERVICE_REMOVAL_I18N_STRING_PLURAL:(SERVICE_REMOVAL_I18N_STRING)),
                                    builder2.toString(),
                                    getServiceName(),
                                    entry.getKey()))
                            .append(delimiter);
                } else {
                    for (DefaultPropertyValue propertyChange : entry.getValue()) {
                        builder.append(
                                String.format(BUNDLE.getString(SERVICE_TEMPORARY_ATTRIBUTE_I18N_STRING),
                                        propertyChange.getFieldName(),
                                        getServiceName(),
                                        entry.getKey(),
                                        propertyChange.getNewDefault()))
                                .append(delimiter);
                    }
                }

            }
        }

        return builder.toString();
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder builder = new StringBuilder();

        if (!schemasToUpdate.isEmpty()) {

            builder.append(
                    String.format(
                            (1 == schemasToUpdate.size())?
                            BUNDLE.getString(SERVICE_VALIDATION_I18N_STRING) :
                                    BUNDLE.getString(SERVICE_VALIDATION_I18N_STRING_PLURAL),
                            getServiceName(), schemasToUpdate.size()))
                    .append(delimiter);
        }

        if (!defaultsToApply.isEmpty()) {
            for (Map.Entry<String, Set<DefaultPropertyValue>> entry : defaultsToApply.entrySet()) {

                int nullPropertyDefaults = 0;
                for (DefaultPropertyValue propertyChange : entry.getValue()) {
                    if (null == propertyChange.getNewDefault()) {
                        nullPropertyDefaults++;
                    }
                }

                if (0 < nullPropertyDefaults) {
                    boolean plural = 1 < nullPropertyDefaults;

                    builder.append(
                            String.format(
                                    BUNDLE.getString(plural
                                            ? SERVICE_REMOVED_I18N_STRING_PLURAL
                                            : SERVICE_REMOVED_I18N_STRING),
                                    getServiceName(),
                                    entry.getKey(),
                                    nullPropertyDefaults))
                            .append(delimiter);
                } else {
                    if(!CollectionUtils.isEmpty(entry.getValue())) {
                        builder.append(
                                String.format(
                                        BUNDLE.getString((1 < entry.getValue().size())
                                                ? SERVICE_TEMPORARY_ATTRIBUTES_I18N_STRING_PLURAL
                                                : SERVICE_TEMPORARY_ATTRIBUTES_I18N_STRING),
                                        getServiceName(),
                                        entry.getKey(),
                                        entry.getValue().size()))
                                .append(delimiter);
                    }
                }
            }
        }

        return builder.toString();
    }

    protected abstract DefaultPropertyValue[] getNewRequiredAttributeDefaultValues();

    protected abstract String getServiceName();

    protected abstract String getValidatorName();

    // interface to allow implementing classes to communicate the config changes to the methods above
    interface DefaultPropertyValue {
        String getFieldName();
        String getNewDefault();
    }
}
