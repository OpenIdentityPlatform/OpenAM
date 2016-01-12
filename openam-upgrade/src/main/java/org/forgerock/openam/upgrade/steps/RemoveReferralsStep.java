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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.upgrade.steps;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.apache.commons.lang.StringUtils.countMatches;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.VersionUtils.isCurrentVersionLessThan;
import static org.forgerock.openam.utils.CollectionUtils.isEmpty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.entitlement.service.PrivilegeManagerFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Pair;

/**
 * This upgrade step is responsible for the removal of referrals. Referrals virtualise applications into realms that
 * they refer to. As part of this upgrade, concrete applications are created into these virtualised realms, along with
 * any associated policy model entities. With all referred realms having concrete applications and local policies
 * update to reflect any model changes, referrals can safely be deleted.
 *
 * @since 13.0.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.policy.UpgradeResourceTypeStep")
public final class RemoveReferralsStep extends AbstractUpgradeStep {

    private final static String AUDIT_REFERRALS_REPORT = "upgrade.policy.referrals";
    private final static String AUDIT_CLONING_APPLICATION_START = "upgrade.policy.cloning.application.start";
    private final static String AUDIT_REMOVING_REFERRAL_START = "upgrade.policy.removing.referral.start";
    private final static String AUDIT_APPLICATIONS_CLONED = "upgrade.policy.applications.cloned";
    private final static String AUDIT_REFERRALS_REMOVED = "upgrade.policy.referrals.removed";
    private final static String AUDIT_UPGRADE_SUCCESS = "upgrade.success";
    private final static String AUDIT_UPGRADE_FAIL = "upgrade.failed";

    private final static String REFERRAL_SEARCH_FILTER
            = "(&(ou:dn:=sunEntitlementIndexes)(ou:dn:=referrals)(sunserviceID=indexes))";
    private final static int AM_13 = 1300;

    private final ObjectMapper mapper = new ObjectMapper();

    private final ApplicationManagerWrapper applicationService;
    private final ResourceTypeService resourceTypeService;
    private final PrivilegeManagerFactory policyServiceFactory;

    private final String rootDN;

    private final Map<String, Set<String>> applicationsToClone;
    private final Map<Pair<?, ?>, String> clonedResourceTypes;
    private final Set<DN> referralsToBeRemoved;

    @Inject
    public RemoveReferralsStep(ApplicationManagerWrapper applicationService,
            ResourceTypeService resourceTypeService, PrivilegeManagerFactory policyServiceFactory,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory factory,
            PrivilegedAction<SSOToken> adminTokenAction, @Named(DataLayerConstants.ROOT_DN_SUFFIX) String rootDN) {

        super(adminTokenAction, factory);

        this.applicationService = applicationService;
        this.resourceTypeService = resourceTypeService;
        this.policyServiceFactory = policyServiceFactory;

        this.rootDN = rootDN;

        applicationsToClone = new HashMap<>();
        clonedResourceTypes = new HashMap<>();
        referralsToBeRemoved = new HashSet<>();
    }

    @Override
    public void initialize() throws UpgradeException {
        if (isCurrentVersionLessThan(AM_13, true)) {
            interrogateExistingReferrals();
        }
    }

    private void interrogateExistingReferrals() throws UpgradeException {
        try (Connection connection = getConnection()) {
            searchForReferrals(connection);
        } catch (DataLayerException | SearchResultReferenceIOException | LdapException e) {
            throw new UpgradeException("Unable to complete search for referrals", e);
        }
    }

    private void searchForReferrals(Connection connection)
            throws SearchResultReferenceIOException, LdapException, UpgradeException {
        SearchRequest request = LDAPRequests.newSearchRequest(
                rootDN, SearchScope.WHOLE_SUBTREE, REFERRAL_SEARCH_FILTER, "sunKeyValue");

        try (ConnectionEntryReader reader = connection.search(request)) {
            while (reader.hasNext()) {
                extractReferralInformation(reader.readEntry());
            }
        }
    }

    private void extractReferralInformation(SearchResultEntry entry) throws UpgradeException {
        referralsToBeRemoved.add(entry.getName());
        Set<String> values = entry.parseAttribute("sunKeyValue").asSetOfString();

        JsonValue referralJson = null;

        for (String value : values) {
            if (value.startsWith("serializable=")) {
                String jsonString = value.substring("serializable=".length());

                try {
                    referralJson = JsonValue.json(mapper.readValue(jsonString, Map.class));
                    break;
                } catch (IOException e) {
                    throw new UpgradeException(format("Failed to parse json for referral %s", entry.getName()), e);
                }
            }
        }

        if (referralJson == null) {
            throw new UpgradeException(format("Expected referral %s to have serializable attribute", entry.getName()));
        }

        Set<String> listedApplications = referralJson.get("mapApplNameToResources").required().keys();
        Set<String> listedRealms = referralJson.get("realms").required().asSet(String.class);

        for (String application : listedApplications) {
            Set<String> destinationRealms = applicationsToClone.get(application);

            if (destinationRealms == null) {
                destinationRealms = new HashSet<>();
                applicationsToClone.put(application, destinationRealms);
            }

            destinationRealms.addAll(listedRealms);
        }
    }

    @Override
    public boolean isApplicable() {
        return !applicationsToClone.isEmpty() || !referralsToBeRemoved.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        instateReferredApplications();
        deleteExistingReferrals();
    }

    private void instateReferredApplications() throws UpgradeException {
        for (Map.Entry<String, Set<String>> applicationToClone : applicationsToClone.entrySet()) {
            try {
                instateReferredApplication(applicationToClone.getKey(), applicationToClone.getValue());
            } catch (EntitlementException e) {
                throw new UpgradeException("Application cloning failed");
            }
        }
    }

    private void instateReferredApplication(String applicationName, Set<String> destinationRealms)
            throws EntitlementException, UpgradeException {
        String shallowestRealm = findShallowestRealm(destinationRealms);
        String sourceRealm = shallowestRealm.substring(0, shallowestRealm.lastIndexOf('/') + 1);

        Application application = applicationService.getApplication(getAdminSubject(), sourceRealm, applicationName);

        if (application == null) {
            throw new UpgradeException(format("Expected application %s in realm %s", applicationName, sourceRealm));
        }

        if (isEmpty(application.getResourceTypeUuids())) {
            throw new UpgradeException(format("Expected application %s to have some resource types", applicationName));
        }

        if (application.getResourceTypeUuids().size() > 1) {
            throw new UpgradeException(format("Expected application %s to have a single resource type", applicationName));
        }

        for (String destinationRealm : destinationRealms) {
            enactRequiredPolicyModelChanges(application, sourceRealm, destinationRealm);
        }
    }

    private void enactRequiredPolicyModelChanges(Application application, String sourceRealm, String destinationRealm)
            throws EntitlementException, UpgradeException {
        PrivilegeManager policyManager = policyServiceFactory.get(destinationRealm, getAdminSubject());
        List<Privilege> policies = policyManager.findAllPoliciesByApplication(application.getName());

        if (policies.isEmpty()) {
            // Only necessary to reinstate application if policies exist in the realm.
            return;
        }

        try {
            UpgradeProgress.reportStart(AUDIT_CLONING_APPLICATION_START, application.getName(), destinationRealm);

            String resourceTypeId = application.getResourceTypeUuids().iterator().next();
            String clonedResourceTypeId = instateAssociatedResourceType(resourceTypeId, sourceRealm, destinationRealm);
            Application clonedApplication = cloneApplication(application, clonedResourceTypeId);

            applicationService.saveApplication(getAdminSubject(), destinationRealm, clonedApplication);

            for (Privilege policy : policies) {
                policy.setResourceTypeUuid(clonedResourceTypeId);
                policyManager.modify(policy);
            }

            UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
        } catch (EntitlementException | UpgradeException e) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw e;
        }
    }

    private String instateAssociatedResourceType(String resourceTypeId, String sourceRealm, String destinationRealm)
            throws EntitlementException, UpgradeException {

        Pair<String, String> key = Pair.of(destinationRealm, resourceTypeId);

        if (clonedResourceTypes.containsKey(key)) {
            return clonedResourceTypes.get(key);
        }

        ResourceType resourceType = resourceTypeService.getResourceType(getAdminSubject(), sourceRealm, resourceTypeId);

        if (resourceType == null) {
            throw new UpgradeException(format("Expected resource type %s in realm %s", resourceTypeId, sourceRealm));
        }

        ResourceType clonedResourceType = cloneResourceType(resourceType);
        resourceTypeService.saveResourceType(getAdminSubject(), destinationRealm, clonedResourceType);

        clonedResourceTypes.put(key, clonedResourceType.getUUID());
        return clonedResourceType.getUUID();
    }

    private Application cloneApplication(Application application, String resourceTypeId) throws UpgradeException {
        try {
            Application newApplication = new Application(application.getName(), application.getApplicationType());
            newApplication.setDescription(application.getDescription());
            newApplication.setSubjects(application.getSubjects());
            newApplication.setConditions(application.getConditions());
            newApplication.setResourceComparator(application.getResourceComparatorClass());
            newApplication.setSearchIndex(application.getSearchIndexClass());
            newApplication.setSaveIndex(application.getSaveIndexClass());
            newApplication.setEntitlementCombiner(application.getEntitlementCombinerClass());
            newApplication.setAttributeNames(application.getAttributeNames());
            newApplication.addAllResourceTypeUuids(singleton(resourceTypeId));
            return newApplication;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new UpgradeException(format("Failed to clone application %s", application.getName()), e);
        }
    }

    private ResourceType cloneResourceType(ResourceType resourceType) {
        return ResourceType
                .builder()
                .generateUUID()
                .setName(resourceType.getName())
                .setDescription(resourceType.getDescription())
                .setActions(resourceType.getActions())
                .setPatterns(resourceType.getPatterns())
                .build();
    }

    private String findShallowestRealm(Set<String> realms) {
        int segmentCount = Integer.MAX_VALUE;
        String shallowestRealm = "/";

        for (String realm : realms) {
            int currentCount = countMatches(realm, "/");

            if (currentCount < segmentCount) {
                shallowestRealm = realm;
                segmentCount = currentCount;
            }
        }

        return shallowestRealm;
    }

    private void deleteExistingReferrals() throws UpgradeException {
        try (Connection connection = getConnection()) {
            for (DN referral : referralsToBeRemoved) {
                UpgradeProgress.reportStart(AUDIT_REMOVING_REFERRAL_START, referral);

                DeleteRequest request = LDAPRequests.newDeleteRequest(referral);
                connection.delete(request);

                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            }
        } catch (DataLayerException | LdapException e) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw new UpgradeException("Failed to delete referrals", e);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder builder = new StringBuilder();

        if (!applicationsToClone.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_APPLICATIONS_CLONED))
                    .append(delimiter);
        }

        if (!referralsToBeRemoved.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_REFERRALS_REMOVED))
                    .append(delimiter);
        }

        return builder.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        StringBuilder builder = new StringBuilder();

        if (!applicationsToClone.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_APPLICATIONS_CLONED))
                    .append(':')
                    .append(delimiter);

            for (String applicationName : applicationsToClone.keySet()) {
                builder.append(applicationName)
                        .append(delimiter);
            }
        }

        if (!referralsToBeRemoved.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_REFERRALS_REMOVED))
                    .append(':')
                    .append(delimiter);

            for (DN referralDN : referralsToBeRemoved) {
                builder.append(referralDN)
                        .append(delimiter);
            }
        }

        Map<String, String> reportEntries = new HashMap<>();
        reportEntries.put("%REPORT_TEXT%", builder.toString());
        reportEntries.put(LF, delimiter);
        return UpgradeServices.tagSwapReport(reportEntries, AUDIT_REFERRALS_REPORT);
    }

}
