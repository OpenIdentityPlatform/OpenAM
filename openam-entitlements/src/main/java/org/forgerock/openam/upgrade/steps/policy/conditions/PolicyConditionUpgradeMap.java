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

package org.forgerock.openam.upgrade.steps.policy.conditions;

import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.opensso.PolicyCondition;
import com.sun.identity.entitlement.opensso.PolicySubject;
import com.sun.identity.policy.interfaces.Condition;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.entitlement.conditions.environment.AMIdentityMembershipCondition;
import org.forgerock.openam.entitlement.conditions.environment.AuthLevelCondition;
import org.forgerock.openam.entitlement.conditions.environment.AuthSchemeCondition;
import org.forgerock.openam.entitlement.conditions.environment.AuthenticateToRealmCondition;
import org.forgerock.openam.entitlement.conditions.environment.AuthenticateToServiceCondition;
import org.forgerock.openam.entitlement.conditions.environment.ConditionConstants;
import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.forgerock.openam.entitlement.conditions.environment.IPv6Condition;
import org.forgerock.openam.entitlement.conditions.environment.LDAPFilterCondition;
import org.forgerock.openam.entitlement.conditions.environment.LEAuthLevelCondition;
import org.forgerock.openam.entitlement.conditions.environment.ResourceEnvIPCondition;
import org.forgerock.openam.entitlement.conditions.environment.SessionCondition;
import org.forgerock.openam.entitlement.conditions.environment.SessionPropertyCondition;
import org.forgerock.openam.entitlement.conditions.environment.SimpleTimeCondition;
import org.forgerock.openam.entitlement.conditions.subject.IdentitySubject;
import static org.forgerock.openam.network.ipv4.IPv4Condition.IP_RANGE;
import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.VALUE_CASE_INSENSITIVE;

/**
 * A map containing all the migration logic from an old policy condition to a new entitlement condition.
 *
 * @since 12.0.0
 */
class PolicyConditionUpgradeMap {

    private final Map<String, SubjectConditionMigrator> subjectConditionsUpgradeMap =
            new HashMap<String, SubjectConditionMigrator>();
    private final Map<String, EntitlementConditionMigrator> environmentConditionsUpgradeMap =
            new HashMap<String, EntitlementConditionMigrator>();

    {
        /* This is way the migration mapping declarations will go for example:

        subjectConditionsUpgradeMap.put(AuthenticatedUsers.class.getName(), new SubjectConditionMigrator() {
                     @Override
                     public EntitlementSubject migrate(PolicySubject subject, MigrationReport migrationReport) {
                         migrationReport.migratedSubjectCondition(AuthenticatedUsers.class.getName(), AuthenticatedESubject.class.getName());
                         return new AuthenticatedESubject();
                     }
                 });

        environmentConditionsUpgradeMap.put(SimpleTimeCondition.class.getName(), new EntitlementConditionMigrator() {
                     @Override
                     public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                         migrationReport.migratedEnvironmentCondition(SimpleTimeCondition.class.getName(), TimeCondition.class.getName());
                         return new TimeCondition();
                     }
                 });
        */

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.AuthLevelCondition.class.getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        AuthLevelCondition eCondition = new AuthLevelCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        String propAuthLevel = getValue(properties.get(ConditionConstants.AUTH_LEVEL));
                        int authLevel = Integer.parseInt(AMAuthUtils.getDataFromRealmQualifiedData(propAuthLevel));

                        eCondition.setAuthLevel(authLevel);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.AuthLevelCondition.class.getName(),
                                AuthLevelCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.LEAuthLevelCondition.class.getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        LEAuthLevelCondition eCondition = new LEAuthLevelCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        String propAuthLevel = getValue(properties.get(ConditionConstants.AUTH_LEVEL));
                        int authLevel = Integer.parseInt(AMAuthUtils.getDataFromRealmQualifiedData(propAuthLevel));

                        eCondition.setAuthLevel(authLevel);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.LEAuthLevelCondition.class.getName(),
                                LEAuthLevelCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.AuthenticateToServiceCondition.class
                        .getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        AuthenticateToServiceCondition eCondition = new AuthenticateToServiceCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        String authenticationService =
                                getValue(properties.get(ConditionConstants.AUTHENTICATE_TO_SERVICE));

                        eCondition.setAuthenticateToService(authenticationService);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.AuthenticateToServiceCondition.class.getName(),
                                AuthenticateToServiceCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.AuthenticateToRealmCondition.class
                        .getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        AuthenticateToRealmCondition eCondition = new AuthenticateToRealmCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        String authenticationRealm =
                                getValue(properties.get(ConditionConstants.AUTHENTICATE_TO_REALM));

                        eCondition.setAuthenticateToRealm(authenticationRealm);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.AuthenticateToRealmCondition.class.getName(),
                                AuthenticateToRealmCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.AMIdentityMembershipCondition.class
                        .getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        AMIdentityMembershipCondition eCondition = new AMIdentityMembershipCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        Set<String> amIdentityNames = properties.get(ConditionConstants.AM_IDENTITY_NAME);

                        eCondition.setAmIdentityNames(amIdentityNames);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.AMIdentityMembershipCondition.class.getName(),
                                AMIdentityMembershipCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.SessionCondition.class
                        .getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        SessionCondition eCondition = new SessionCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        Long maxSessionTime =
                                Long.parseLong(getValue(properties.get(ConditionConstants.MAX_SESSION_TIME)));
                        boolean terminateSession =
                                getValue(properties.get(ConditionConstants.TERMINATE_SESSION)).contains("true");

                        eCondition.setMaxSessionTime(maxSessionTime);
                        eCondition.setTerminateSession(terminateSession);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.SessionCondition.class.getName(),
                                SessionCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.SimpleTimeCondition.class
                        .getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        SimpleTimeCondition eCondition = new SimpleTimeCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        String startTime = getValue(properties.get(Condition.START_TIME));
                        String endTime = getValue(properties.get(Condition.END_TIME));
                        String startDay = getValue(properties.get(Condition.START_DAY));
                        String endDay = getValue(properties.get(Condition.END_DAY));
                        String startDate = getValue(properties.get(Condition.START_DATE));
                        String endDate = getValue(properties.get(Condition.END_DATE));
                        String enforcementTimeZone = getValue(properties.get(Condition.ENFORCEMENT_TIME_ZONE));

                        eCondition.setStartTime(startTime);
                        eCondition.setEndTime(endTime);
                        eCondition.setStartDay(startDay);
                        eCondition.setEndDay(endDay);
                        eCondition.setStartDate(startDate);
                        eCondition.setEndDate(endDate);
                        eCondition.setEnforcementTimeZone(enforcementTimeZone);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.SimpleTimeCondition.class.getName(),
                                SimpleTimeCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.SessionPropertyCondition.class
                        .getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        SessionPropertyCondition eCondition = new SessionPropertyCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        Map<String, Set<String>> props = new HashMap<String, Set<String>>(properties);
                        String ignoreValueCaseString = getValue(props.remove(VALUE_CASE_INSENSITIVE));
                        boolean ignoreValueCase = true;
                        if (ignoreValueCaseString != null && !ignoreValueCaseString.isEmpty()) {
                            ignoreValueCase = Boolean.parseBoolean(ignoreValueCaseString);
                        }

                        eCondition.setProperties(props);
                        eCondition.setIgnoreValueCase(ignoreValueCase);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.SessionPropertyCondition.class.getName(),
                                SessionPropertyCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.AuthSchemeCondition.class
                        .getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        AuthSchemeCondition eCondition = new AuthSchemeCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        Set<String> authScheme = properties.get(Condition.AUTH_SCHEME);
                        String applicationName = getValue(properties.get(Condition.APPLICATION_NAME));
                        String idleTimeoutString = getValue(properties.get(Condition.APPLICATION_IDLE_TIMEOUT));
                        int idleTimeout = 0;
                        if (idleTimeoutString != null) {
                            idleTimeout = Integer.parseInt(idleTimeoutString);
                        }

                        eCondition.setAuthScheme(authScheme);
                        eCondition.setApplicationName(applicationName);
                        eCondition.setApplicationIdleTimeout(idleTimeout);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.AuthSchemeCondition.class.getName(),
                                AuthSchemeCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.IPCondition.class.getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        EntitlementCondition eCondition = null;
                        Map<String, Set<String>> properties = condition.getProperties();

                        String ipVersion = getIpConditionIpVersion(properties);
                        Set<String> ipRange = properties.get(IP_RANGE);
                        Set<String> dnsName = properties.get(Condition.DNS_NAME);
                        String startIp = getValue(properties.get(Condition.START_IP));
                        String endIp = getValue(properties.get(Condition.END_IP));

                        if (com.sun.identity.policy.plugins.IPCondition.IPV4.equals(ipVersion)) {
                            IPv4Condition ipCondition = new IPv4Condition();
                            try {
                                if (ipRange != null) {
                                    ipCondition.setIpRange(new ArrayList<String>(ipRange));
                                }
                                if (dnsName != null){
                                    ipCondition.setDnsName(new ArrayList<String>(dnsName));
                                }
                                ipCondition.setStartIpAndEndIp(startIp, endIp);
                                eCondition = ipCondition;
                            } catch (EntitlementException e) {
                                throw new RuntimeException(e);
                            }
                            migrationReport.migratedEnvironmentCondition(
                                    com.sun.identity.policy.plugins.IPCondition.class.getName(),
                                    IPv4Condition.class.getName());
                        } else if (com.sun.identity.policy.plugins.IPCondition.IPV6.equals(ipVersion)) {
                            IPv6Condition ipCondition = new IPv6Condition();
                            try {
                                if (ipRange != null) {
                                    ipCondition.setIpRange(new ArrayList<String>(ipRange));
                                }
                                if (dnsName != null){
                                    ipCondition.setDnsName(new ArrayList<String>(dnsName));
                                }
                                ipCondition.setStartIpAndEndIp(startIp, endIp);
                                eCondition = ipCondition;
                            } catch (EntitlementException e) {
                                throw new RuntimeException(e);
                            }
                            migrationReport.migratedEnvironmentCondition(
                                    com.sun.identity.policy.plugins.IPCondition.class.getName(),
                                    IPv6Condition.class.getName());
                        }

                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.LDAPFilterCondition.class
                        .getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        LDAPFilterCondition eCondition = new LDAPFilterCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        eCondition.setState(new JsonValue(properties).toString());

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.LDAPFilterCondition.class.getName(),
                                LDAPFilterCondition.class.getName());
                        return eCondition;
                    }
                });

        environmentConditionsUpgradeMap.put(com.sun.identity.policy.plugins.ResourceEnvIPCondition.class
                        .getName(),
                new EntitlementConditionMigrator() {
                    @Override
                    public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                        ResourceEnvIPCondition eCondition = new ResourceEnvIPCondition();
                        Map<String, Set<String>> properties = condition.getProperties();

                        Set<String> resourceEnvIPConditionValue =
                                properties.get(ResourceEnvIPCondition.ENV_CONDITION_VALUE);

                        eCondition.setResourceEnvIPConditionValue(resourceEnvIPConditionValue);

                        migrationReport.migratedEnvironmentCondition(
                                com.sun.identity.policy.plugins.ResourceEnvIPCondition.class.getName(),
                                ResourceEnvIPCondition.class.getName());
                        return eCondition;
                    }
                });

        subjectConditionsUpgradeMap.put(com.sun.identity.policy.plugins.AMIdentitySubject.class.getName(),
                new SubjectConditionMigrator() {
                    @Override
                    public EntitlementSubject migrate(PolicySubject subject, MigrationReport migrationReport) {

                        IdentitySubject eSubject = new IdentitySubject();

                        Set<String> subjects = subject.getValues();

                        eSubject.setSubjectValues(subjects);

                        migrationReport.migratedSubjectCondition(
                                com.sun.identity.policy.plugins.AMIdentitySubject.class.getName(),
                                IdentitySubject.class.getName());
                        return eSubject;
                    }
                });

        subjectConditionsUpgradeMap.put(com.sun.identity.policy.plugins.AuthenticatedUsers.class.getName(),
                new SubjectConditionMigrator() {
                    @Override
                    public EntitlementSubject migrate(PolicySubject subject, MigrationReport migrationReport) {

                        AuthenticatedUsers eSubject = new AuthenticatedUsers();

                        migrationReport.migratedSubjectCondition(
                                com.sun.identity.policy.plugins.AuthenticatedUsers.class.getName(),
                                AuthenticatedUsers.class.getName());
                        return eSubject;
                    }
        });
    }

    private <T> T getValue(Set<T> values) {
        if (values != null && values.iterator().hasNext()) {
            return values.iterator().next();
        }
        return null;
    }

    /**
     * Returns {@code true} if there exists an entry for migrating the specified old policy subject condition class.
     *
     * @param conditionClassName The old policy subject condition class name.
     * @return {@code true} if there exists an entry for migrating the specified old policy subject condition class.
     */
    boolean containsSubjectCondition(String conditionClassName) {
        return subjectConditionsUpgradeMap.containsKey(conditionClassName);
    }

    /**
     * Returns {@code true} if there exists an entry for migrating the specified old policy environment condition class.
     *
     * @param conditionClassName The old policy environment condition class name.
     * @return {@code true} if there exists an entry for migrating the specified old policy environment condition class.
     */
    boolean containsEnvironmentCondition(String conditionClassName) {
        return environmentConditionsUpgradeMap.containsKey(conditionClassName);
    }

    /**
     * Migrates the specified subject from the old policy subject condition class to the corresponding new entitlement
     * subject condition.
     *
     * @param conditionClassName The old policy subject condition class name.
     * @param subject The subject condition to migrate
     * @param migrationReport The migration report to update.
     * @return A new {@code EntitlementSubject} of the migrated old policy subject condition
     */
    EntitlementSubject migrateSubjectCondition(String conditionClassName, PolicySubject subject,
            MigrationReport migrationReport) {
        return subjectConditionsUpgradeMap.get(conditionClassName).migrate(subject, migrationReport);
    }

    /**
     * Migrates the specified subject from the old policy environment condition class to the corresponding new entitlement
     * environment condition.
     *
     * @param conditionClassName The old policy environment condition class name.
     * @param condition The environment condition to migrate
     * @param migrationReport The migration report to update.
     * @return A new {@code EntitlementSubject} of the migrated old policy environment condition
     */
    EntitlementCondition migrateEnvironmentCondition(String conditionClassName,
            PolicyCondition condition, MigrationReport migrationReport) {
        return environmentConditionsUpgradeMap.get(conditionClassName).migrate(condition, migrationReport);
    }

    private static String getIpConditionIpVersion(Map properties) {
        String ipVersion = com.sun.identity.policy.plugins.IPCondition.IPV4;
        Set ipVersionProp = (Set) properties.get(com.sun.identity.policy.plugins.IPCondition.IP_VERSION);
        if (ipVersionProp != null) {
            Iterator ipVerItr = ipVersionProp.iterator();
            String ip = (String) ipVerItr.next();
            if (ip.equalsIgnoreCase(com.sun.identity.policy.plugins.IPCondition.IPV6)) {
                ipVersion = com.sun.identity.policy.plugins.IPCondition.IPV6;
            }
        }
        return ipVersion;
    }

}
