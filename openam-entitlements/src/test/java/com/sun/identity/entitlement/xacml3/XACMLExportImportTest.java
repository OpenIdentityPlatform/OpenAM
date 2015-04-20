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
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.entitlement.xacml3.validation.PrivilegeValidator;
import com.sun.identity.shared.debug.Debug;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import static com.sun.identity.entitlement.xacml3.FactoryMethods.createArbitraryPrivilege;
import static com.sun.identity.entitlement.xacml3.FactoryMethods.createArbitraryReferralPrivilege;
import static com.sun.identity.entitlement.xacml3.XACMLExportImport.*;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.fail;

public class XACMLExportImportTest {

    private static final String ROOT_REALM = "/";
    private static final Subject NULL_SUBJECT = null;
    private static final InputStream NULL_INPUT = null;
    private final long now = Calendar.getInstance().getTimeInMillis();

    private SearchFilterFactory searchFilterFactory;
    private PrivilegeValidator validator;
    private PrivilegeManager pm;
    private ReferralPrivilegeManager rpm;
    private XACMLReaderWriter xacmlReaderWriter;
    private PrivilegeManagerFactory pmFactory;
    private ReferralPrivilegeManagerFactory rpmFactory;
    private Debug debug;

    private XACMLExportImport xacmlExportImport;

    @BeforeMethod
    public void setUp() throws EntitlementException {

        // Constructor Dependencies

        pmFactory = mock(PrivilegeManagerFactory.class);
        pm = mock(PrivilegeManager.class);

        rpmFactory = mock(ReferralPrivilegeManagerFactory.class);
        rpm = mock(ReferralPrivilegeManager.class);

        xacmlReaderWriter = mock(XACMLReaderWriter.class);
        validator = mock(PrivilegeValidator.class);
        searchFilterFactory = new SearchFilterFactory();
        debug = mock(Debug.class);

        // Class under test

        xacmlExportImport = new XACMLExportImport(pmFactory, rpmFactory,
                xacmlReaderWriter, validator, searchFilterFactory, debug);

        // Given (shared test state)

        given(pmFactory.createReferralPrivilegeManager(eq(ROOT_REALM), any(Subject.class))).willReturn(pm);
        given(rpmFactory.createPrivilegeManager(eq(ROOT_REALM), any(Subject.class))).willReturn(rpm);

    }

    @Test
    public void canImportPrivilegesIntoRealm() throws Exception {

        // Given
        // shared test state
        ReferralPrivilege referralPrivilegeToUpdate = existing(valid(referralPrivilege("rp1")));
        ReferralPrivilege referralPrivilegeToAdd = notExisting(valid(referralPrivilege("rp2")));
        Privilege privilegeToUpdate = existing(valid(privilege("p1")));
        Privilege privilegeToAdd = notExisting(valid(privilege("p2")));

        PrivilegeSet privilegeSet = new PrivilegeSet(
                asList(referralPrivilegeToUpdate, referralPrivilegeToAdd),
                asList(privilegeToUpdate, privilegeToAdd));

        given(xacmlReaderWriter.read(eq(NULL_INPUT))).willReturn(privilegeSet);

        // When
        List<ImportStep> importSteps = xacmlExportImport.importXacml(ROOT_REALM, NULL_INPUT, NULL_SUBJECT, false);

        // Then
        assertThat(importSteps).hasSize(4);
        assertImportStep(importSteps.get(0), DiffStatus.UPDATE, referralPrivilegeToUpdate);
        assertImportStep(importSteps.get(1), DiffStatus.ADD, referralPrivilegeToAdd);
        assertImportStep(importSteps.get(2), DiffStatus.UPDATE, privilegeToUpdate);
        assertImportStep(importSteps.get(3), DiffStatus.ADD, privilegeToAdd);

        verify(validator).validateReferralPrivilege(referralPrivilegeToAdd);
        verify(validator).validateReferralPrivilege(referralPrivilegeToUpdate);
        verify(validator).validatePrivilege(privilegeToAdd);
        verify(validator).validatePrivilege(privilegeToUpdate);

        verify(pm).add(privilegeToAdd);
        verify(pm).modify(privilegeToUpdate);
        verify(rpm).add(referralPrivilegeToAdd);
        verify(rpm).modify(referralPrivilegeToUpdate);
    }

    @Test
    public void canPerformAnImportDryRun() throws Exception {

        // Given
        // shared test state
        ReferralPrivilege referralPrivilegeToUpdate = existing(valid(referralPrivilege("rp1")));
        ReferralPrivilege referralPrivilegeToAdd = notExisting(valid(referralPrivilege("rp2")));
        Privilege privilegeToUpdate = existing(valid(privilege("p1")));
        Privilege privilegeToAdd = notExisting(valid(privilege("p2")));

        PrivilegeSet privilegeSet = new PrivilegeSet(
                asList(referralPrivilegeToUpdate, referralPrivilegeToAdd),
                asList(privilegeToUpdate, privilegeToAdd));

        given(xacmlReaderWriter.read(eq(NULL_INPUT))).willReturn(privilegeSet);

        // When
        List<ImportStep> importSteps = xacmlExportImport.importXacml(ROOT_REALM, NULL_INPUT, NULL_SUBJECT, true);

        // Then
        assertThat(importSteps).hasSize(4);
        assertImportStep(importSteps.get(0), DiffStatus.UPDATE, referralPrivilegeToUpdate);
        assertImportStep(importSteps.get(1), DiffStatus.ADD, referralPrivilegeToAdd);
        assertImportStep(importSteps.get(2), DiffStatus.UPDATE, privilegeToUpdate);
        assertImportStep(importSteps.get(3), DiffStatus.ADD, privilegeToAdd);

        verify(validator).validatePrivilege(privilegeToAdd);
        verify(validator).validatePrivilege(privilegeToUpdate);
        verify(validator).validateReferralPrivilege(referralPrivilegeToAdd);
        verify(validator).validateReferralPrivilege(referralPrivilegeToUpdate);

        verify(pm, times(0)).add(any(Privilege.class));
        verify(pm, times(0)).modify(any(Privilege.class));
        verify(rpm, times(0)).add(any(ReferralPrivilege.class));
        verify(rpm, times(0)).modify(any(ReferralPrivilege.class));
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void throwsAnExceptionIfPrivilegeValidationFails() throws EntitlementException {
        // Given
        // shared test state
        Privilege invalidPrivilege = invalid(privilege("p1"));
        PrivilegeSet set = new PrivilegeSet();
        set.addPrivilege(invalidPrivilege);

        given(xacmlReaderWriter.read(eq(NULL_INPUT))).willReturn(set);

        // When
        xacmlExportImport.importXacml(ROOT_REALM, NULL_INPUT, NULL_SUBJECT, true);

        // Then
        fail("Expected validation exception");
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void throwsAnExceptionIfReferralPrivilegeValidationFails() throws EntitlementException {
        // Given
        // shared test state
        ReferralPrivilege invalidReferral = invalid(referralPrivilege("rp1"));
        PrivilegeSet set = new PrivilegeSet();
        set.addReferralPrivilege(invalidReferral);

        given(xacmlReaderWriter.read(eq(NULL_INPUT))).willReturn(set);

        // When
        xacmlExportImport.importXacml(ROOT_REALM, NULL_INPUT, NULL_SUBJECT, true);

        // Then
        fail("Expected validation exception");
    }

    @Test
    public void testUndesirablePrivilegeNames() {
        assertThat(xacmlExportImport.containsUndesiredCharacters("ordinary-name")).isFalse();
        assertThat(xacmlExportImport.containsUndesiredCharacters("ordinary+name")).isTrue();
        assertThat(xacmlExportImport.containsUndesiredCharacters("+")).isTrue();
        assertThat(xacmlExportImport.containsUndesiredCharacters("+name")).isTrue();
        assertThat(xacmlExportImport.containsUndesiredCharacters("ordinary-name+")).isTrue();
        assertThat(xacmlExportImport.containsUndesiredCharacters("ordinary>name")).isTrue();
        assertThat(xacmlExportImport.containsUndesiredCharacters("ordinary<name")).isTrue();
        assertThat(xacmlExportImport.containsUndesiredCharacters("ordinary\\name")).isTrue();
    }

    private Privilege privilege(String name) throws EntitlementException {
        return createArbitraryPrivilege(name, now);
    }

    private ReferralPrivilege referralPrivilege(String name) throws EntitlementException {
        return createArbitraryReferralPrivilege(name, now);
    }

    private <T extends IPrivilege> T valid(T privilege) {
        return privilege;
    }

    private <T extends IPrivilege> T invalid(T privilege) throws EntitlementException {
        if (privilege instanceof Privilege) {
            doThrow(new EntitlementException(EntitlementException.INVALID_SEARCH_FILTER))
                    .when(validator).validatePrivilege(any(Privilege.class));
        } else {
            doThrow(new EntitlementException(EntitlementException.INVALID_SEARCH_FILTER))
                    .when(validator).validateReferralPrivilege(any(ReferralPrivilege.class));
        }
        return privilege;
    }

    private <T extends IPrivilege> T existing(T privilege) throws EntitlementException {
        if (privilege instanceof Privilege) {
            given(pm.canFindByName(eq(privilege.getName()))).willReturn(true);
        } else {
            given(rpm.canFindByName(eq(privilege.getName()))).willReturn(true);
        }
        return privilege;
    }

    private <T extends IPrivilege> T notExisting(T privilege) throws EntitlementException {
        if (privilege instanceof Privilege) {
            given(pm.canFindByName(eq(privilege.getName()))).willReturn(false);
        } else {
            given(rpm.canFindByName(eq(privilege.getName()))).willReturn(false);
        }
        return privilege;
    }

    public static void assertImportStep(ImportStep importStep, DiffStatus diffStatus, IPrivilege privilege) {
        assertThat(importStep.getPrivilege()).isEqualTo(privilege).as("privilege");
        assertThat(importStep.getDiffStatus()).isEqualTo(diffStatus).as("diffStatus");
    }

}
