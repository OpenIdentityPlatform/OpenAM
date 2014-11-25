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

package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.IPrivilegeManager;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.entitlement.xacml3.validation.PrivilegeValidator;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final ReferralPrivilegeManagerFactory referralPrivilegeManagerFactory;

    /**
     * Creates an instance of the XACMLExportImport with dependencies provided.
     *
     * @param privilegeManagerFactory Non null, required to create PrivilegeManager instances.
     * @param referralPrivilegeManagerFactory Non null, required to create ReferralPrivilegeManager instances.
     * @param xacmlReaderWriter Non null, required for translating privileges to/from XACML XML.
     * @param privilegeValidator Non null, required for validation of imported privileges.
     * @param searchFilterFactory Non null, required for SearchFilter operations.
     * @param debug Non null.
     */
    @Inject
    public XACMLExportImport(PrivilegeManagerFactory privilegeManagerFactory,
                             ReferralPrivilegeManagerFactory referralPrivilegeManagerFactory,
                             XACMLReaderWriter xacmlReaderWriter,
                             PrivilegeValidator privilegeValidator,
                             SearchFilterFactory searchFilterFactory,
                             @Named(XACMLConstants.DEBUG) Debug debug) {

        this.privilegeManagerFactory = privilegeManagerFactory;
        this.referralPrivilegeManagerFactory = referralPrivilegeManagerFactory;
        this.xacmlReaderWriter = xacmlReaderWriter;
        this.searchFilterFactory = searchFilterFactory;
        this.privilegeValidator = privilegeValidator;
        this.debug = debug;
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

        PrivilegeSet privilegeSet = xacmlReaderWriter.read(xacml);
        List<ImportStep> importSteps = generateImportSteps(realm, privilegeSet, admin);

        if (!dryRun) {
            message("Import: Policies to Import {0}", importSteps.size());
            for (ImportStep importStep : importSteps) {
                ((ImportStepImpl) importStep).apply();
            }
            message("Import: Complete");
        }

        return importSteps;
    }

    /**
     * Establishes the sequence of ImportSteps required to import the provided privileges into the specified realm.
     *
     * @param realm Non null Realm to populate with the Policies.
     * @param privilegeSet Non null, collection of Privileges and ReferralPrivileges to import.
     * @param admin Non null admin Subject.
     * @return The sequence steps that can be used to carry out the import.
     * @throws EntitlementException If there was any unexpected error.
     */
    private List<ImportStep> generateImportSteps(String realm, PrivilegeSet privilegeSet, Subject admin)
            throws EntitlementException {

        List<ImportStep> importSteps = new ArrayList<ImportStep>();

        PrivilegeManager pm = privilegeManagerFactory.createReferralPrivilegeManager(realm, admin);
        ReferralPrivilegeManager rpm = referralPrivilegeManagerFactory.createPrivilegeManager(realm, admin);

        for (ReferralPrivilege referralPrivilege : privilegeSet.getReferralPrivileges()) {
            privilegeValidator.validateReferralPrivilege(referralPrivilege);

            // OPENAM-5031
            // For the moment, fail the whole import if any single referral is found to have a name which doesn't
            // suit LDAP.
            if (containsUndesiredCharacters(referralPrivilege.getName())) {
                throw new EntitlementException(EntitlementException.INVALID_VALUE,
                        new Object[] { "referral name " + referralPrivilege.getName() });
            }

            if (rpm.canFindByName(referralPrivilege.getName())) {
                importSteps.add(referralImportStep(rpm, DiffStatus.UPDATE, referralPrivilege));
            } else {
                importSteps.add(referralImportStep(rpm, DiffStatus.ADD, referralPrivilege));
            }
        }

        for (Privilege privilege : privilegeSet.getPrivileges()) {

            // OPENAM-5031
            // For the moment, fail the whole import if any single referral is found to have a name which doesn't
            // suit LDAP.
            if (containsUndesiredCharacters(privilege.getName())) {
                throw new EntitlementException(EntitlementException.INVALID_VALUE,
                        new Object[] { "privilege name " + privilege.getName() });
            }

            privilegeValidator.validatePrivilege(privilege);
            if (pm.canFindByName(privilege.getName())) {
                importSteps.add(privilegeImportStep(pm, DiffStatus.UPDATE, privilege));
            } else {
                importSteps.add(privilegeImportStep(pm, DiffStatus.ADD, privilege));
            }
        }

        return importSteps;
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
        ReferralPrivilegeManager rpm = referralPrivilegeManagerFactory.createPrivilegeManager(realm, admin);

        Set<SearchFilter> filterSet = new HashSet<SearchFilter>();
        if (filters != null) {
            for (String filter : filters) {
                SearchFilter searchFilter = searchFilterFactory.getFilter(filter);
                message("Export: Search Filter: {0}", searchFilter);
                filterSet.add(searchFilter);
            }
        }

        Set<String> privilegeNames = pm.searchNames(filterSet);
        Set<String> referralNames = rpm.searchNames(filterSet);
        message("Export: Privilege Matches {0}", privilegeNames.size());
        message("Export: Referral Matches {0}", referralNames.size());

        PrivilegeSet privilegeSet = new PrivilegeSet();
        for (String name : privilegeNames) {
            Privilege privilege = pm.findByName(name, admin);
            message("Export: Privilege {0}", privilege.getName());
            privilegeSet.addPrivilege(privilege);
        }

        for (String name : referralNames) {
            ReferralPrivilege referralPrivilege = rpm.findByName(name);
            privilegeSet.addReferralPrivilege(referralPrivilege);
            message("Export: Referral {0}", referralPrivilege.getName());
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
     * OPENAM-5031: We would have used DN.escapeAttributeValue to encode the incoming string and compare with the
     * original string - if there are differences then the incoming string contains characters which LDAP requires
     * quoted.  However ssoadm doesn't include the jar that the DN class ends up in.  In order to avoid the
     * overhead of adding a whole jar just for one function in one class, this is provided here.  Thus, this
     * function returns true if the incoming string contains any character which LDAP requires to be quoted.
     *
     * @param s The specified string.
     * @return true if the string contains characters which require quotation for LDAP to work, false otherwise
     */
    @VisibleForTesting
    boolean containsUndesiredCharacters(String s) {

        // This is done with strings rather than characters because the initialisation of the set is much easier.
        // Otherwise we end up with a Set<Character> being initialised from a List<char>
        //
        final String[] DODGY_LDAP_CHARS = { ",", "+", "\"", "\\", "<", ">", ";" };
        Set<String> dodgyChars = new HashSet<String>(Arrays.asList(DODGY_LDAP_CHARS));
        for(int i = 0; i < s.length(); i++) {
            String sub = s.substring(i, i + 1);
            if (dodgyChars.contains(sub)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Factory method for ReferralPrivilege ImportStep
     */
    private ImportStep referralImportStep(ReferralPrivilegeManager rpm, DiffStatus type, ReferralPrivilege referral) {
        return new ImportStepImpl<ReferralPrivilege>(rpm, type, referral, "Referral");
    }

    /**
     * Factory method for Privilege ImportStep
     */
    private ImportStep privilegeImportStep(PrivilegeManager pm, DiffStatus type, Privilege privilege) {
        return new ImportStepImpl<Privilege>(pm, type, privilege, "Privilege");
    }

    /**
     * Factory to allow PrivilegeManager to be mocked in tests
     */
    public static class PrivilegeManagerFactory {

        PrivilegeManager createReferralPrivilegeManager(String realm, Subject admin) {
            return PrivilegeManager.getInstance(realm, admin);
        }
    }

    /**
     * Factory to allow ReferralPrivilegeManager to be mocked in tests
     */
    public static class ReferralPrivilegeManagerFactory {

        ReferralPrivilegeManager createPrivilegeManager(String realm, Subject admin) {
            return new ReferralPrivilegeManager(realm, admin);
        }
    }

    /**
     * Diff status types used to describe the change in state of a single resource.
     */
    public static enum DiffStatus {

        ADD('A'), UPDATE('U');

        private final char code;

        private DiffStatus(char code) {
            this.code = code;
        }

        /**
         * Single character description of diff status.
         *
         * @return Character code description of diff status.
         */
        public char getCode() {
            return code;
        }

    }

    /**
     * Describes how a Privilege or ReferralPrivilege read from XACML will be imported into OpenAM.
     */
    public interface ImportStep {

        public DiffStatus getDiffStatus();

        public IPrivilege getPrivilege();

    }

    /**
     * {@inheritDoc}
     */
    private final class ImportStepImpl<T extends IPrivilege> implements ImportStep {

        private final IPrivilegeManager<T> privilegeManager;
        private final DiffStatus diffStatus;
        private final T privilege;
        private final String privilegeType;

        public ImportStepImpl(IPrivilegeManager<T> manager, DiffStatus diffStatus, T privilege, String privilegeType) {
            this.privilegeManager = manager;
            this.diffStatus = diffStatus;
            this.privilege = privilege;
            this.privilegeType = privilegeType;
        }

        @Override
        public DiffStatus getDiffStatus() {
            return diffStatus;
        }

        @Override
        public IPrivilege getPrivilege() {
            return privilege;
        }

        private void apply() throws EntitlementException {

            message("Import: {0} {1} {2}", diffStatus.name(), privilegeType, privilege.getName());
            switch (diffStatus) {
                case ADD:
                    privilegeManager.add(privilege);
                    break;
                case UPDATE:
                    privilegeManager.modify(privilege);
                    break;
            }
        }

    }

}
