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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package com.sun.identity.entitlement.xacml3;

import static org.forgerock.openam.xacml.v3.XACMLApplicationUtils.ApplicationTypeService;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.xacml.v3.ImportStep;
import org.forgerock.openam.xacml.v3.PersistableImportStep;
import org.forgerock.openam.xacml.v3.ImportStepGenerator;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.entitlement.xacml3.validation.PrivilegeValidator;
import com.sun.identity.shared.debug.Debug;

/**
 * Utility for handling the Export and subsequent Import of Policies into
 * the OpenAM Entitlements framework.
 *
 * @since 12.0.0
 */
public class XACMLExportImport {

    public static final String PREFIX = XACMLExportImport.class.getSimpleName();

    // Injected
    private final XACMLReaderWriter xacmlReaderWriter;
    private final SearchFilterFactory searchFilterFactory;
    private final Debug debug;
    private final PrivilegeValidator privilegeValidator;
    private final PrivilegeManagerFactory privilegeManagerFactory;
    private final ApplicationServiceFactory applicationServiceFactory;
    private final ResourceTypeService resourceTypeService;

    /**
     * Creates an instance of the XACMLExportImport with dependencies provided.
     *
     * @param privilegeManagerFactory Non null, required to create PrivilegeManager instances.
     * @param xacmlReaderWriter Non null, required for translating privileges to/from XACML XML.
     * @param privilegeValidator Non null, required for validation of imported privileges.
     * @param searchFilterFactory Non null, required for SearchFilter operations.
     * @param debug Non null.
     * @param applicationServiceFactory Application service factory responsible for creating the application service.
     * @param resourceTypeService Resource type service responsible for creating resource types.
     */
    @Inject
    public XACMLExportImport(PrivilegeManagerFactory privilegeManagerFactory,
            XACMLReaderWriter xacmlReaderWriter,
            PrivilegeValidator privilegeValidator,
            SearchFilterFactory searchFilterFactory,
            @Named(XACMLConstants.DEBUG) Debug debug,
            ApplicationServiceFactory applicationServiceFactory,
            ResourceTypeService resourceTypeService) {
        this.privilegeManagerFactory = privilegeManagerFactory;
        this.xacmlReaderWriter = xacmlReaderWriter;
        this.searchFilterFactory = searchFilterFactory;
        this.privilegeValidator = privilegeValidator;
        this.debug = debug;
        this.applicationServiceFactory = applicationServiceFactory;
        this.resourceTypeService = resourceTypeService;
    }

    /**
     * Performs the Import based on the given Stream. The stream must contain XML in XACML.
     *
     * @param realm Non null Realm to populate with the Policies.
     * @param xacml Non null stream to read.
     * @param admin Non null admin Subject.
     * @param dryRun boolean flag, indicating import steps should be reported but not applied.
     * @return The sequence steps that could or have been used to carry out the import.
     * @throws EntitlementException If there was any unexpected error.
     */
    public List<ImportStep> importXacml(String realm, InputStream xacml, Subject admin, boolean dryRun)
            throws EntitlementException {
        PrivilegeSet privilegeSet = xacmlToPrivilegeSet(xacml);
        List<PersistableImportStep> importSteps = generateImportSteps(realm, privilegeSet, admin);
        applyIfRequired(dryRun, importSteps);

        return new ArrayList<ImportStep>(importSteps);
    }

    private PrivilegeSet xacmlToPrivilegeSet(InputStream xacml) throws EntitlementException {
        return xacmlReaderWriter.read(xacml);
    }

    /**
     * Establishes the sequence of ImportSteps required to import the provided privileges into the specified realm.
     *
     * @param realm Non null Realm to populate with the Policies.
     * @param privilegeSet Non null, collection of Privileges, ReferralPrivileges etc. to import.
     * @param admin Non null admin Subject.
     * @return The sequence steps that can be used to carry out the import.
     * @throws EntitlementException If there was any unexpected error.
     */
    private List<PersistableImportStep> generateImportSteps(String realm, PrivilegeSet privilegeSet, Subject admin)
            throws EntitlementException {
        ApplicationService applicationService = applicationServiceFactory.create(admin, realm);
        PrivilegeManager privilegeManager = privilegeManagerFactory.createReferralPrivilegeManager(realm, admin);
        ApplicationTypeService applicationTypeService = new ApplicationTypeService();

        ImportStepGenerator importStepGenerator = new ImportStepGenerator(applicationService,
                resourceTypeService, privilegeManager, privilegeValidator, applicationTypeService,
                realm, admin, privilegeSet);

        importStepGenerator.generateImportSteps();

        return importStepGenerator.getAllImportSteps();
    }

    private void applyIfRequired(boolean dryRun, List<PersistableImportStep> importSteps) throws EntitlementException {
        if (!dryRun) {
            message("Import: Policies to Import {0}", importSteps.size());

            for (PersistableImportStep importStep : importSteps) {
                message("Import: {0} {1} {2}",
                        importStep.getDiffStatus().name(), importStep.getType(), importStep.getName());
                importStep.apply();
            }

            message("Import: Complete");
        }
    }

    /**
     * Performs an export of all Policies found in the Privilege Manager that match the
     * provided filters.
     *
     * @param realm Non null realm.
     * @param admin Non null admin subject to authenticate as.
     * @param filters Non null, but maybe empty filters to select Privileges against.
     * @return A non null but possibly empty collection of Policies.
     * @throws EntitlementException If there was any problem with the generation of Policies.
     */
    public PolicySet exportXACML(String realm, Subject admin, List<String> filters)
            throws EntitlementException {
        PrivilegeManager pm = privilegeManagerFactory.createReferralPrivilegeManager(realm, admin);

        Set<SearchFilter> filterSet = new HashSet<SearchFilter>();
        if (filters != null) {
            for (String filter : filters) {
                SearchFilter searchFilter = searchFilterFactory.getFilter(filter);
                message("Export: Search Filter: {0}", searchFilter);
                filterSet.add(searchFilter);
            }
        }

        Set<String> privilegeNames = pm.searchNames(filterSet);
        message("Export: Privilege Matches {0}", privilegeNames.size());

        PrivilegeSet privilegeSet = new PrivilegeSet();
        for (String name : privilegeNames) {
            Privilege privilege = pm.findByName(name, admin);
            message("Export: Privilege {0}", privilege.getName());
            privilegeSet.addPrivilege(privilege);
        }

        PolicySet policySet = xacmlReaderWriter.toXACML(realm, privilegeSet);
        message("Export: Complete");
        return policySet;
    }

    private void message(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(PREFIX + format, args));
        }
    }

    /**
     * Factory to allow PrivilegeManager to be mocked in tests
     */
    public static class PrivilegeManagerFactory {

        PrivilegeManager createReferralPrivilegeManager(String realm, Subject admin) {
            return PrivilegeManager.getInstance(realm, admin);
        }
    }

}
