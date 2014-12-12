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

import com.sun.identity.entitlement.PrivilegeManager;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryAttribute;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PrivilegePolicyStoreProviderTest {

    private static final Map<String, QueryAttribute> ATTRIBUTE_MAP = new HashMap<String, QueryAttribute>();

    private PrivilegePolicyStoreProvider testProvider;
    private PrivilegePolicyStoreProvider.PrivilegeManagerFactory mockFactory;


    @BeforeMethod
    public void setupMocks() {
        mockFactory = mock(PrivilegePolicyStoreProvider.PrivilegeManagerFactory.class);
        testProvider = new PrivilegePolicyStoreProvider(mockFactory, ATTRIBUTE_MAP);
    }

    @Test
    public void shouldUseRealmAndSubjectFromContext() {
        // Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        Subject subject = new Subject();
        String realm = "/test realm";
        given(subjectContext.getCallerSubject()).willReturn(subject);
        RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm(realm, realm);
        PrivilegeManager manager = mock(PrivilegeManager.class);
        given(mockFactory.getPrivilegeManager(realm, subject)).willReturn(manager);

        // When
        PolicyStore store = testProvider.getPolicyStore(context);

        // Then
        verify(mockFactory).getPrivilegeManager(realm, subject);
        assertThat(store).isNotNull().isInstanceOf(PrivilegePolicyStore.class);
    }

}
