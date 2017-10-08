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

import static com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils.validate;
import static org.forgerock.openam.xacml.v3.XACMLApplicationUtils.*;
import static org.forgerock.openam.xacml.v3.XACMLResourceTypeUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.xacml3.PrivilegeSet;
import com.sun.identity.entitlement.xacml3.validation.PrivilegeValidator;

/**
 * Generates Imports Steps and resolves the dependencies among Application, Resource Types, Privilege etc.
 *
 * @since 13.5.0
 */
public class ImportStepGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ImportStepGenerator.class);

    private final ApplicationService applicationService;
    private final ResourceTypeService resourceTypeService;
    private final PrivilegeManager privilegeManager;
    private final PrivilegeValidator privilegeValidator;
    private final ApplicationTypeService applicationTypeService;
    private final String realm;
    private final Subject subject;
    private final PrivilegeSet privilegeSet;

    private final List<PersistableImportStep<Application>> importStepsApplication;
    private final List<PersistableImportStep<ResourceType>> importStepsResourceType;
    private final List<PersistableImportStep<Privilege>> importStepsPrivilege;

    private final Map<String, List<Application>> uuidResTypeVsApplicationFromFile;
    private final Map<String, Application> nameVsApplicationExisting;
    private final Map<String, ResourceType> uuidVsResourceTypeExisting;
    private final Map<String, ResourceType> uuidVsResourceTypeFromFile;

    private final Map<String, String> dummyIdVsActualUuids;

    //Testing purpose
    private final Set<ResourceType> resourceTypesExisting;

    /**
     * Constructs an ImportStepGenerator instance.
     *
     * @param applicationService
     *         The service class for Application creation.
     * @param resourceTypeService
     *         The service class for Resource Type creation.
     * @param privilegeManager
     *         The class which manages Privilege.
     * @param privilegeValidator
     *         Validator for Privilege.
     * @param realm
     *         Realm to which the privilege set data belongs.
     * @param subject
     *         Admin subject
     * @param privilegeSet
     *         Privileges read from the XACML file.
     */
    public ImportStepGenerator(ApplicationService applicationService, ResourceTypeService resourceTypeService,
            PrivilegeManager privilegeManager, PrivilegeValidator privilegeValidator,
            ApplicationTypeService applicationTypeService, String realm, Subject subject, PrivilegeSet privilegeSet) {
        Reject.checkNotNull(privilegeSet, "privilegeSet cannot be empty");

        this.applicationService = applicationService;
        this.resourceTypeService = resourceTypeService;
        this.privilegeManager = privilegeManager;
        this.privilegeValidator = privilegeValidator;
        this.applicationTypeService = applicationTypeService;
        this.realm = realm;
        this.subject = subject;
        this.privilegeSet = privilegeSet;

        this.importStepsApplication = new ArrayList<>();
        this.importStepsResourceType = new ArrayList<>();
        this.importStepsPrivilege = new ArrayList<>();

        this.uuidResTypeVsApplicationFromFile = new HashMap<>();
        this.nameVsApplicationExisting = new HashMap<>();
        this.uuidVsResourceTypeExisting = new HashMap<>();
        this.uuidVsResourceTypeFromFile = new HashMap<>();

        this.dummyIdVsActualUuids = new HashMap<>();

        this.resourceTypesExisting = new HashSet<>();
    }

    /**
     * Returns the Application Import Steps generated by this instance.
     *
     * @return Import steps
     */
    public List<PersistableImportStep<Application>> getImportStepsApplication() {
        return importStepsApplication;
    }

    /**
     * Returns the Resource Type Import Steps generated by this instance.
     *
     * @return Import steps
     */
    public List<PersistableImportStep<ResourceType>> getImportStepsResourceType() {
        return importStepsResourceType;
    }

    /**
     * Returns the Privilege Import Steps generated by this instance.
     *
     * @return Import steps
     */
    public List<PersistableImportStep<Privilege>> getImportStepsPrivilege() {
        return importStepsPrivilege;
    }

    /**
     * Returns all import steps.
     *
     * @return import steps.
     */
    public List<PersistableImportStep> getAllImportSteps() {
        List<PersistableImportStep> results = new ArrayList<>();
        results.addAll(getImportStepsResourceType());
        results.addAll(getImportStepsApplication());
        results.addAll(getImportStepsPrivilege());
        return results;
    }

    /**
     * Generates Import Steps for Application, ResourceType and Privilege.
     *
     * @throws EntitlementException
     *         when any exceptional case occurs.
     */
    public void generateImportSteps() throws EntitlementException {
        // Resource Types which are not associated with any Applications are not imported.

        indexingApplicationsExisting();
        indexingApplicationsReadFromFile();
        indexingResourceTypesReadFromFile();
        indexingResourceTypesExisting();

        assignApplicationTypeToApplication();
        generateResourceTypeImportSteps();
        resolveResourceTypeDependencies();

        generatePrivilegeImportSteps();

        generateApplicationImportSteps();
    }

    private void assignApplicationTypeToApplication() throws EntitlementException {
        for (Application application : privilegeSet.getApplication()) {
            application.setApplicationType(applicationTypeService.getApplicationType(subject, application.getName()));
        }
    }

    private void generateApplicationImportSteps() throws EntitlementException {
        for (Application application : privilegeSet.getApplication()) {
            importStepsApplication.add(
                    generateApplicationImportStep(application, applicationService.getApplicationNames()));
        }
    }

    private PersistableImportStep generateApplicationImportStep(Application application, Set<String> applicationNames)
            throws EntitlementException {
        if (applicationNames.contains(application.getName())) {
            Application applicationExisting = nameVsApplicationExisting.get(application.getName());
            copyAttributes(application, applicationExisting);
            return new ApplicationImportStep(DiffStatus.UPDATE, applicationExisting, applicationService);
        }
        else {
            return new ApplicationImportStep(DiffStatus.ADD, application, applicationService);
        }
    }

    private void indexingApplicationsExisting() throws EntitlementException {
        for (String applicationName : applicationService.getApplicationNames()) {
            Application applicationExisting = applicationService.getApplication(applicationName);
            if (applicationExisting != null) {
                nameVsApplicationExisting.put(applicationName, applicationExisting);
            }
        }
    }

    private void indexingApplicationsReadFromFile() {
        for (Application application : privilegeSet.getApplication()) {
            for (String uuid : application.getResourceTypeUuids()) {
                List<Application> applications = uuidResTypeVsApplicationFromFile.get(uuid);
                if (applications == null) {
                    applications = new ArrayList<>();
                    uuidResTypeVsApplicationFromFile.put(uuid, applications);
                }
                applications.add(application);
            }
        }
    }

    private void indexingResourceTypesReadFromFile() {
        for (ResourceType resourceType : privilegeSet.getResourceTypes()) {
            uuidVsResourceTypeFromFile.put(resourceType.getUUID(), resourceType);
        }
    }

    private void indexingResourceTypesExisting() throws EntitlementException {
        resourceTypesExisting.addAll(getAllResourceTypes(resourceTypeService, subject, realm));

        for (ResourceType resourceType: resourceTypesExisting) {
            uuidVsResourceTypeExisting.put(resourceType.getUUID(), resourceType);
        }
    }

    /*
     * See XACMLReaderWrite.fromXACML() for more details on the structure of the PrivilegeSet instance
     * and how it's attributes are populated.
     */
    private void generateResourceTypeImportSteps() {
        generateImportStepsForExistingResourceType();
        generateImportStepsForNewResourceTypes();
    }

    private void generateImportStepsForExistingResourceType() {
        // Existing resource type in the data store could be mapped to multiple dummy resource types in the file.
        Map<String, ResourceType> uuidVsResourceType = new HashMap<>();

        for (ResourceType resourceType : privilegeSet.getResourceTypes()) {
            Set<String> resources = resourceType.getPatterns();

            List<Application> applications = uuidResTypeVsApplicationFromFile.get(resourceType.getUUID());
            if (applications != null && applications.get(0)  != null) {
                Application application = applications.get(0);

                ResourceType resourceTypeExisting = findExistingResourceType(resources, application);
                if (resourceTypeExisting == null) {
                    continue;
                }

                ResourceType resourceTypeTransient = uuidVsResourceType.get(resourceTypeExisting.getUUID());
                if (resourceTypeTransient == null) {
                    resourceTypeTransient = mergeResourceType(resourceType, resourceTypeExisting);
                }
                else {
                    resourceTypeTransient = mergeResourceType(resourceType, resourceTypeTransient);
                }

                uuidVsResourceType.put(resourceTypeExisting.getUUID(), resourceTypeTransient);
                dummyIdVsActualUuids.put(resourceType.getUUID(), resourceTypeExisting.getUUID());
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Resource type with id " + resourceType.getUUID() +
                            " is not associated with any applications and hence cannot be imported.");
                }
            }
        }

        for (Map.Entry<String, ResourceType> entry : uuidVsResourceType.entrySet()) {
            importStepsResourceType.add(newResourceTypeImportStep(entry.getValue(), DiffStatus.UPDATE));
        }
    }

    private void generateImportStepsForNewResourceTypes() {
        Set<String> dummyIdsPresentInTheFile = uuidResTypeVsApplicationFromFile.keySet();
        Set<String> dummyIdsMatchedToExistingTypes = dummyIdVsActualUuids.keySet();

        Set<String> dummyIdsTobeCreatedAsNewResourceType = new HashSet<>(dummyIdsPresentInTheFile);
        dummyIdsTobeCreatedAsNewResourceType.removeAll(dummyIdsMatchedToExistingTypes);

        Map<String, ResourceType> resourceTypeTransientByApplicationName = new HashMap<>();
        for(String dummyId: dummyIdsTobeCreatedAsNewResourceType) {
            for (Application application : uuidResTypeVsApplicationFromFile.get(dummyId)) {
                ResourceType dummyInstance = uuidVsResourceTypeFromFile.get(dummyId);

                ResourceType resourceTypeTransient = resourceTypeTransientByApplicationName.get(application.getName());
                if (resourceTypeTransient == null) {
                    resourceTypeTransient = createResourceType(application.getName(), dummyInstance.getName(),
                            dummyInstance.getPatterns(), dummyInstance.getActions(), null);
                }
                else {
                    resourceTypeTransient = mergeResourceType(dummyInstance, resourceTypeTransient);
                }

                resourceTypeTransientByApplicationName.put(application.getName(), resourceTypeTransient);
                dummyIdVsActualUuids.put(dummyInstance.getUUID(), resourceTypeTransient.getUUID());
            }
        }

        for (Map.Entry<String, ResourceType> entry : resourceTypeTransientByApplicationName.entrySet()) {
            importStepsResourceType.add(newResourceTypeImportStep(entry.getValue(), DiffStatus.ADD));
        }
    }

    private ResourceType findExistingResourceType(Set<String> resources, Application application) {
        Application applicationExisting = nameVsApplicationExisting.get(application.getName());
        if (applicationExisting != null) {
            ResourceType resType = findExistingResourceTypeByApplication(application, applicationExisting, resources);
            if (resType != null) {
                return resType;
            }
        }

        Set<String> uuidsTobeMatched = new HashSet<>(uuidVsResourceTypeExisting.keySet());
        if (applicationExisting != null) {
            uuidsTobeMatched.removeAll(applicationExisting.getResourceTypeUuids());
        }
        return findExistingResourceTypeByPattern(uuidsTobeMatched, resources, application, applicationExisting);
    }

    private ResourceType findExistingResourceTypeByPattern(Set<String> resourceTypeIds, Set<String> resources,
            Application application, Application applicationExisting) {
        ResourceName resourceComparator = chooseResourceComparator(application, applicationExisting);
        return matchResourceTypeExisting(resourceTypeIds, resources, resourceComparator);
    }

    private ResourceType findExistingResourceTypeByApplication(Application application,
            Application applicationExisting, Set<String> resources) {
        Set<String> resourceTypeIdsExisting = applicationExisting.getResourceTypeUuids();
        ResourceName resourceComparator = chooseResourceComparator(application, applicationExisting);
        return matchResourceTypeExisting(resourceTypeIdsExisting, resources, resourceComparator);
    }

    private ResourceName chooseResourceComparator(Application application, Application applicationExisting) {
        if (applicationExisting != null && applicationExisting.getResourceComparator() != null) {
            return applicationExisting.getResourceComparator();
        }
        else {
            return application.getResourceComparator();
        }
    }

    private ResourceType matchResourceTypeExisting(Set<String> resourceTypeIdsExisting, Set<String> resources,
            ResourceName resourceComparator) {
        for (String resourceTypeIdExisting : resourceTypeIdsExisting) {
            ResourceType resourceType = uuidVsResourceTypeExisting.get(resourceTypeIdExisting);
            if (matchResources(resources, resourceType, resourceComparator)) {
                return resourceType;    // ResourceTypes are created at policy level and hence first pattern
                                        //  match would be sufficient.
            }
        }
        return null;
    }

    private PersistableImportStep newResourceTypeImportStep(ResourceType resourceType, DiffStatus diffStatus) {
        return new ResourceTypeImportStep(diffStatus, resourceType, resourceTypeService, realm, subject);
    }

    private void generatePrivilegeImportSteps() throws EntitlementException {
        for (Privilege privilege : privilegeSet.getPrivileges()) {
            validate(privilege, privilegeValidator);
            importStepsPrivilege.add(newPrivilegeImportStep(privilege, privilegeManager));
        }
    }

    private PersistableImportStep newPrivilegeImportStep(Privilege privilege, PrivilegeManager privilegeManager)
            throws EntitlementException {
        if (privilegeManager.canFindByName(privilege.getName())) {
            return new PrivilegeImportStep(privilegeManager, DiffStatus.UPDATE, privilege);
        } else {
            return new PrivilegeImportStep(privilegeManager, DiffStatus.ADD, privilege);
        }
    }

    private void resolveResourceTypeDependencies() {
        for (Map.Entry<String, String> entry : dummyIdVsActualUuids.entrySet()) {
            String idTobeReplaced = entry.getKey();
            String newId = entry.getValue();

            for (Application application : privilegeSet.getApplication()) {
                if (application.getResourceTypeUuids().remove(idTobeReplaced)) {
                    application.getResourceTypeUuids().add(newId);
                }
            }

            for (Privilege privilege : privilegeSet.getPrivileges()) {
                if(idTobeReplaced.equals(privilege.getResourceTypeUuid())) {
                    privilege.setResourceTypeUuid(newId);
                }
            }
        }
    }

    Set<ResourceType> getResourceTypesExisting() {
        return resourceTypesExisting;
    }

}
