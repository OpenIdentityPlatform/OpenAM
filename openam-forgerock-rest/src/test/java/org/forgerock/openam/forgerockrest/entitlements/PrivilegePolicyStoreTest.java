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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PrivilegePolicyStoreTest {

    private PrivilegeManager mockManager;
    private PrivilegePolicyStore testStore;

    @BeforeClass
    public void mockPrivilegeClass() {
        System.setProperty(Privilege.PRIVILEGE_CLASS_PROPERTY, StubPrivilege.class.getName());
    }

    @AfterClass
    public void unmockPrivilegeClass() {
        System.clearProperty(Privilege.PRIVILEGE_CLASS_PROPERTY);
    }

    @BeforeMethod
    public void setupMocks() {
        mockManager = mock(PrivilegeManager.class);
        testStore = new PrivilegePolicyStore(mockManager);
    }

    @Test
    public void shouldDelegateReadsToPrivilegeManager() throws Exception {
        // Given
        String id = "testPolicy";
        Privilege policy = new StubPrivilege();
        given(mockManager.getPrivilege(id)).willReturn(policy);

        // When
        Privilege response = testStore.read(id);

        // Then
        verify(mockManager).getPrivilege(id);
        assertThat(response).isSameAs(policy);
    }

    @Test
    public void shouldAddPoliciesToPrivilegeManager() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();

        // When
        Privilege response = testStore.create(policy);

        // Then
        verify(mockManager).addPrivilege(policy);
        assertThat(response).isSameAs(policy);
    }

    @Test
    public void shouldDelegateUpdatesToPrivilegeManager() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();

        // When
        Privilege response = testStore.update(policy);

        // Then
        verify(mockManager).modifyPrivilege(policy);
        assertThat(response).isSameAs(policy);
    }


    @Test
    public void shouldDelegateDeletesToPrivilegeManager() throws Exception {
        // Given
        String id = "testPolicy";

        // When
        testStore.delete(id);

        // Then
        verify(mockManager).removePrivilege(id);
    }
}
