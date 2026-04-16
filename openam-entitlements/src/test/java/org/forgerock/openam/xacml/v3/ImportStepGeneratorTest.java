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

package org.forgerock.openam.xacml.v3;

import static com.sun.identity.entitlement.ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.xacml.v3.XACMLApplicationUtils.ApplicationTypeService;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ResourceMatch;
import com.sun.identity.entitlement.URLResourceName;
import com.sun.identity.entitlement.xacml3.PrivilegeSet;
import com.sun.identity.entitlement.xacml3.validation.PrivilegeValidator;

/**
 * Test class for ImportStepGenerator.
 *
 * @since 13.5.0
 */
public final class ImportStepGeneratorTest {

    private static final String ROOT_REALM = "/";

    private ImportStepGenerator importStepGenerator;

    private PrivilegeValidator validator;
    private PrivilegeManager privilegeManager;
    private Subject subject;
    private ApplicationService applicationService;
    private ResourceTypeService resourceTypeService;
    private ApplicationTypeService applicationTypeService;
    private URLResourceName urlResourceName;

    private PrivilegeSet privilegeSet;

    // Used for asserting resource types
    private Map<String, ResourceType> existingResourceTypes;

    @BeforeMethod
    public void setUp() throws EntitlementException, IllegalAccessException, InstantiationException {
        applicationService = mock(ApplicationService.class);
        resourceTypeService = mock(ResourceTypeService.class);
        privilegeManager = mock(PrivilegeManager.class);
        validator = mock(PrivilegeValidator.class);
        urlResourceName = mock(URLResourceName.class);

        applicationTypeService = mock(ApplicationTypeService.class);
        given(applicationTypeService.getApplicationType(any(Subject.class), any(String.class)))
                .willReturn(newApplicationType());
        given(urlResourceName.compare(anyString(), anyString(), anyBoolean())).willReturn(ResourceMatch.EXACT_MATCH);

        subject = new Subject();
        privilegeSet = new PrivilegeSet();

        importStepGenerator = new ImportStepGenerator(applicationService, resourceTypeService,
                privilegeManager, validator, applicationTypeService, ROOT_REALM, subject, privilegeSet);

        existingResourceTypes = new HashMap<>();
    }

    @Test
    public void importStepsEmpty() throws EntitlementException {
        // GIVEN an empty Privilege Set

        // WHEN
        importStepGenerator.generateImportSteps();

        // THEN
        assertThat(importStepGenerator.getImportStepsApplication()).isEmpty();
        assertThat(importStepGenerator.getImportStepsResourceType()).isEmpty();
        assertThat(importStepGenerator.getImportStepsPrivilege()).isEmpty();
    }

    @Test
    public void importStepsAllTestInserts() throws EntitlementException {
        // GIVEN
        givenApplication("Application1", Arrays.asList("uuid1", "uuid2"));

        givenResourceType("ResourceType1", "uuid1", Arrays.asList(
                "pattern1://:*/*",          // This will be matched with http pattern present in uuid2
                "pattern2://*:*/*"));       // This will NOT be matched with uuid2 however based on first pattern match
                                            //      uuid1 will be matched with uuid2.
        givenPrivilege("Privilege1", "Application1", "uuid1");

        givenResourceType("ResourceType2", "uuid2", Arrays.asList("pattern3://:*/*"));
        givenPrivilege("Privilege2", "Application1", "uuid2");

        givenApplication("Application2", Arrays.asList("uuid3", "uuid4"));

        givenResourceType("ResourceType3", "uuid3", Arrays.asList("light://*:*/*"));
        givenPrivilege("Privilege3", "Application2", "uuid3");

        givenResourceType("ResourceType4", "uuid4", Arrays.asList("light://*:*/*"));
        givenPrivilege("Privilege4", "Application2", "uuid4");

        int expectedCountApplications = 2;
        int expectedCountResourceTypes = 2;
        int expectedCountPrivileges = 4;

        // WHEN
        importStepGenerator.generateImportSteps();

        // THEN
        assertStepCounts(expectedCountApplications, expectedCountResourceTypes, expectedCountPrivileges);

        assertResourceType("ResourceType2", DiffStatus.ADD,
                Arrays.asList("pattern1://:*/*", "pattern2://*:*/*", "pattern3://:*/*"));
        assertResourceType("ResourceType4", DiffStatus.ADD, Arrays.asList("light://*:*/*"));

        assertApplication("Application1", DiffStatus.ADD, Arrays.asList("ResourceType2"));
        assertApplication("Application2", DiffStatus.ADD, Arrays.asList("ResourceType4"));

        assertPrivilege("Privilege2", DiffStatus.ADD, "ResourceType2");
        assertPrivilege("Privilege4", DiffStatus.ADD, "ResourceType4");
    }

    @Test
    public void importStepsAllTestUpdates() throws EntitlementException, InstantiationException,
            IllegalAccessException {
        // GIVEN
        givenApplication("Application1", Arrays.asList("uuid1", "uuid2"));

        givenResourceType("ResourceType1", "uuid1", Arrays.asList("http://*:*/*", "https://*:*/*"));
        givenPrivilege("Privilege1", "Application1", "uuid1");
        givenResourceType("ResourceType2", "uuid2", Arrays.asList("http://*:*/*"));
        givenPrivilege("Privilege2", "Application1", "uuid2");

        givenApplication("Application2", Arrays.asList("uuid3", "uuid4"));
        givenResourceType("ResourceType3", "uuid3", Arrays.asList("light://*:*/*"));
        givenPrivilege("Privilege3", "Application2", "uuid3");
        givenResourceType("ResourceType4", "uuid4", Arrays.asList("light://*:*/*"));
        givenPrivilege("Privilege4", "Application2", "uuid4");

        givenExistingApplication("Application1", Arrays.asList("uuid11", "uuid21"));
        givenExistingApplicationNames("Application1");
        givenExistingResourceType("ResourceType1", "uuid11", Arrays.asList("http://*:*/*"));
        givenExistingResourceType("ResourceType5", "uuid21", Arrays.asList("http://*:*/test"));
        givenExistingAllResourceTypes("ResourceType1", "ResourceType5");

        int expectedCountApplications = 2;
        int expectedCountResourceTypes = 2;
        int expectedCountPrivileges = 4;

        // WHEN
        importStepGenerator.generateImportSteps();

        // THEN
        assertStepCounts(expectedCountApplications, expectedCountResourceTypes, expectedCountPrivileges);

        assertResourceType("ResourceType1", DiffStatus.UPDATE, Arrays.asList("http://*:*/*", "https://*:*/*"));
        assertResourceType("ResourceType4", DiffStatus.ADD, Arrays.asList("light://*:*/*"));

        assertApplication("Application1", DiffStatus.UPDATE, Arrays.asList("ResourceType1", "ResourceType5"));
        assertApplication("Application2", DiffStatus.ADD, Arrays.asList("ResourceType4"));

        assertPrivilege("Privilege1", DiffStatus.ADD, "ResourceType1");
        assertPrivilege("Privilege2", DiffStatus.ADD, "ResourceType1");
        assertPrivilege("Privilege4", DiffStatus.ADD, "ResourceType4");
    }

    private void givenResourceType(String name, String uuid, List<String> patterns) {
        privilegeSet.addResourceType(newResourceType(name, uuid, patterns));
    }

    private void givenPrivilege(String name, String applicationName, String uuid) throws EntitlementException {
        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(name);
        privilege.setResourceTypeUuid(uuid);
        Entitlement entitlement = new Entitlement();
        entitlement.setName(applicationName);
        privilege.setEntitlement(entitlement);
        privilegeSet.addPrivilege(privilege);
    }

    private void givenExistingResourceType(String name, String uuid, List<String> patterns)
            throws EntitlementException {
        ResourceType resourceType = newResourceType(name, uuid, patterns);
        existingResourceTypes.put(name, resourceType);
        given(resourceTypeService.getResourceType(any(Subject.class), anyString(), eq(uuid))).
                willReturn(resourceType);
    }

    private void givenExistingAllResourceTypes(String... resourceTypeNames) throws EntitlementException {
        Set<ResourceType> mocks = new HashSet<>();
        for(String name: resourceTypeNames) {
            mocks.add(existingResourceTypes.get(name));
        }

        given(resourceTypeService.getResourceTypes(any(QueryFilter.class), any(Subject.class), anyString())).
                willReturn(mocks);
    }

    private void givenExistingApplicationNames(String... applications) throws EntitlementException {
        given(applicationService.getApplicationNames()).willReturn(new HashSet<>(Arrays.asList(applications)));
    }

    private void givenExistingApplication(String applicationName, List<String> resourceUuids)
            throws EntitlementException, IllegalAccessException, InstantiationException {
        Application application = newApplication(applicationName, resourceUuids);
        application.setApplicationType(newApplicationType());
        given(applicationService.getApplication(applicationName)).willReturn(application);
    }

    private void givenApplication(String name, List<String> uuid) {
        Application application = newApplication(name, uuid);
        privilegeSet.addApplication(application);
    }

    private void assertResourceType(String resourceTypeName, DiffStatus status, List<String> patterns) {
        boolean isPresent = false;
        for (PersistableImportStep<ResourceType> importStep : importStepGenerator.getImportStepsResourceType()) {
            if (importStep.getName().equals(resourceTypeName)) {
                assertThat(importStep.getDiffStatus()).as("Import status check for the resource type " +
                        importStep.getName()).isEqualTo(status);

                assertThat(new TreeSet<>(importStep.get().getPatterns())).as("Import status check for the patterns " +
                        importStep.getName()).isEqualTo(new TreeSet<>(patterns));

                isPresent = true;
            }
        }
        assertThat(isPresent).
                as("ResourceType Name " + resourceTypeName + " is not present in the Import Steps").
                isTrue();
    }

    private void assertPrivilege(String privilegeName, DiffStatus status, String resourceTypeName) {
        for (PersistableImportStep<Privilege> importStep : importStepGenerator.getImportStepsPrivilege()) {
            if (importStep.getName().equals(privilegeName)) {
                assertThat(importStep.getDiffStatus()).isEqualTo(status);

                Privilege privilege = importStep.get();
                Set<String> uuids = resourceTypeNamesToUuids(Arrays.asList(resourceTypeName));
                assertThat(uuids).
                        as("ResourceType not available for the resource name " +
                                resourceTypeName + ". Possibly an error in the test data!").
                        isNotEmpty();
                String resourceTypeUuid = uuids.iterator().next();
                assertThat(privilege.getResourceTypeUuid()).isEqualTo(resourceTypeUuid);
            }
        }
    }

    private void assertApplication(String applicationName, DiffStatus status, List<String> resourceTypeNames) {
        for (PersistableImportStep<Application> importStep : importStepGenerator.getImportStepsApplication()) {
            if (applicationName.equals(importStep.getName())) {
                assertThat(importStep.getDiffStatus()).as("Import status check for the application " +
                        importStep.getName()).isEqualTo(status);

                Application application = importStep.get();

                assertThat(application.getResourceTypeUuids().size()).as("Number of resource type Ids " +
                        "check for the application " + application.getName()).isEqualTo(resourceTypeNames.size());

                Set<String> resourceTypeUuids = resourceTypeNamesToUuids(resourceTypeNames);
                Set<String> resourceTypeUuidsTmp = new HashSet<>(application.getResourceTypeUuids());
                resourceTypeUuidsTmp.removeAll(resourceTypeUuids);
                assertThat(resourceTypeUuidsTmp.size()).as("Resource Type Ids check for the application " +
                        application.getName()).isEqualTo(0);
            }
        }
    }

    private void assertStepCounts(int countApplications, int countResourceTypes, int countPrivileges) {
        assertThat(importStepGenerator.getAllImportSteps().size()).
                isEqualTo(countApplications + countResourceTypes + countPrivileges);

        assertThat(importStepGenerator.getImportStepsApplication().size()).isEqualTo(countApplications);
        assertThat(importStepGenerator.getImportStepsPrivilege().size()).isEqualTo(countPrivileges);
        assertThat(importStepGenerator.getImportStepsResourceType().size()).isEqualTo(countResourceTypes);

        //Check sequence of steps inside the import step list.
        ArrayList<PersistableImportStep> steps = new ArrayList<>(importStepGenerator.getAllImportSteps());
        assertStepSequence(steps, countResourceTypes, ResourceTypeImportStep.TYPE);
        assertStepSequence(steps, countApplications, ApplicationImportStep.TYPE);
        assertStepSequence(steps, countPrivileges, PrivilegeImportStep.TYPE);
        assertThat(steps.size()).isEqualTo(0);
    }

    private void assertStepSequence(ArrayList<PersistableImportStep> steps, int expectedCnt, String type) {
        for (int i = 0; i < expectedCnt ; i++) {
            PersistableImportStep importStep = steps.remove(0);
            assertThat(importStep.getType()).isEqualTo(type);
        }
    }

    private Set<String> resourceTypeNamesToUuids(List<String> resourceTypeNames) {
        Set<String> results = new HashSet<>();

        for(String resourceTypeName : resourceTypeNames) {
            for (PersistableImportStep<ResourceType> step : importStepGenerator.getImportStepsResourceType()) {
                if (step.get().getName().equals(resourceTypeName)) {
                    results.add(step.get().getUUID());
                    break;
                }
            }
        }

        for(String resourceTypeName : resourceTypeNames) {
            for ( ResourceType type : importStepGenerator.getResourceTypesExisting()) {
                if (type.getName().equals(resourceTypeName)) {
                    results.add(type.getUUID());
                    break;
                }
            }
        }

        return results;
    }

    private ResourceType newResourceType(String name, String uuid, List<String> patterns) {
        return ResourceType.builder().
                setName(name).
                setUUID(uuid).
                setPatterns(new HashSet<>(patterns)).
                build();
    }

    private Application newApplication(String name, List<String> uuid) {
        Application application = new Application();
        application.setName(name);
        application.addAllResourceTypeUuids(new HashSet<>(uuid));
        return application;
    }

    private ApplicationType newApplicationType() throws InstantiationException, IllegalAccessException {
        return new ApplicationType(URL_APPLICATION_TYPE_NAME, null, null, null, URLResourceName.class);
    }

}