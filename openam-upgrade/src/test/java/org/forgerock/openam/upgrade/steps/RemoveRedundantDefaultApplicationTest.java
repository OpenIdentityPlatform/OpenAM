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
package org.forgerock.openam.upgrade.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.Constants;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;

/**
 * Unit test for {@link RemoveRedundantDefaultApplication}.
 *
 * @since 13.0.0
 */
public final class RemoveRedundantDefaultApplicationTest {

    @Mock
    private ApplicationManagerWrapper applicationService;
    @Mock
    private PrivilegedAction<SSOToken> ssoTokenAction;
    @Mock
    private ConnectionFactory connectionFactory;

    private UpgradeStep upgradeStep;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        System.setProperty("com.iplanet.am.version", "12.0.0");

        SSOToken token = mock(SSOToken.class);
        given(token.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("abc");
        given(ssoTokenAction.run()).willReturn(token);

        upgradeStep = new RemoveRedundantDefaultApplication(asSet("app1", "app2"),
                applicationService, ssoTokenAction, connectionFactory);
    }

    @Test
    public void successfullyRemovesRedundantApplications() throws Exception {
        // Given
        given(applicationService.getApplicationNames(isA(Subject.class), eq("/"))).willReturn(asSet("app2", "someOtherApp"));

        // When
        upgradeStep.initialize();
        boolean isApplicable = upgradeStep.isApplicable();
        upgradeStep.perform();

        // Then
        assertThat(isApplicable).isTrue();
        // Intersection of the two sets is app2.
        verify(applicationService).deleteApplication(isA(Subject.class), eq("/"), eq("app2"));
    }

    @Test
    public void checkReportForSuccessAndFailureApplications() throws Exception {
        // Given
        given(applicationService.getApplicationNames(isA(Subject.class), eq("/"))).willReturn(asSet("app1", "app2", "someOtherApp"));
        doThrow(EntitlementException.class).when(applicationService).deleteApplication(isA(Subject.class), eq("/"), eq("app2"));

        // When
        upgradeStep.initialize();
        boolean isApplicable = upgradeStep.isApplicable();
        upgradeStep.perform();
        String report = upgradeStep.getDetailedReport("");

        // Then
        assertThat(isApplicable).isTrue();
        verify(applicationService).deleteApplication(isA(Subject.class), eq("/"), eq("app1"));
        verify(applicationService).deleteApplication(isA(Subject.class), eq("/"), eq("app2"));
        assertThat(report).containsSequence("successfully removed", "app1", "failed to be removed", "app2");
    }

}