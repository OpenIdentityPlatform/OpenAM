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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn.core;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.AuthContextLocalWrapper;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LoginProcessTest {

    private LoginProcess loginProcess;

    private LoginAuthenticator loginAuthenticator;
    private LoginConfiguration loginConfiguration;
    private AuthContextLocalWrapper authContext;
    private CoreServicesWrapper coreServicesWrapper;

    @BeforeMethod
    public void setUp() {

        loginAuthenticator = mock(LoginAuthenticator.class);
        loginConfiguration = mock(LoginConfiguration.class);
        authContext = mock(AuthContextLocalWrapper.class);
        coreServicesWrapper = mock(CoreServicesWrapper.class);

        loginProcess = new LoginProcess(loginAuthenticator, loginConfiguration, authContext, coreServicesWrapper);
    }

    @Test
    public void shouldGetLoginStageWhenAuthIndexTypeNotLevelOrComposite() {

        //Given
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.NONE);
        given(authContext.getStatus()).willReturn(AuthContext.Status.IN_PROGRESS);

        //When
        LoginStage loginStage = loginProcess.getLoginStage();

        //Then
        verify(authContext).hasMoreRequirements();
        assertEquals(loginStage, LoginStage.REQUIREMENTS_WAITING);
    }

    @Test
    public void shouldGetLoginStageWhenAuthIndexTypeLevelAndRequirementsNotSetYet() {

        //Given
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.LEVEL);
        given(authContext.getRequirements()).willReturn(null);
        given(authContext.getStatus()).willReturn(AuthContext.Status.IN_PROGRESS);

        //When
        LoginStage loginStage = loginProcess.getLoginStage();

        //Then
        verify(authContext).hasMoreRequirements();
        assertEquals(loginStage, LoginStage.REQUIREMENTS_WAITING);
    }

    @Test
    public void shouldGetLoginStageWhenAuthIndexTypeNotCompositeAndRequirementsNotSetYet() {

        //Given
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.COMPOSITE);
        given(authContext.getRequirements()).willReturn(null);
        given(authContext.getStatus()).willReturn(AuthContext.Status.IN_PROGRESS);

        //When
        LoginStage loginStage = loginProcess.getLoginStage();

        //Then
        verify(authContext).hasMoreRequirements();
        assertEquals(loginStage, LoginStage.REQUIREMENTS_WAITING);
    }

    @Test
    public void shouldGetLoginStageWhenAuthIndexTypeLevelAndRequirementsSet() {

        //Given
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.LEVEL);
        given(authContext.getRequirements()).willReturn(new Callback[0]);
        given(authContext.getStatus()).willReturn(AuthContext.Status.IN_PROGRESS);

        //When
        LoginStage loginStage = loginProcess.getLoginStage();

        //Then
        verify(authContext, never()).hasMoreRequirements();
        assertEquals(loginStage, LoginStage.REQUIREMENTS_WAITING);
    }

    @Test
    public void shouldGetLoginStageWhenAuthIndexTypeCompositeAndRequirementsSet() {

        //Given
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.COMPOSITE);
        given(authContext.getRequirements()).willReturn(new Callback[0]);
        given(authContext.getStatus()).willReturn(AuthContext.Status.IN_PROGRESS);

        //When
        LoginStage loginStage = loginProcess.getLoginStage();

        //Then
        verify(authContext, never()).hasMoreRequirements();
        assertEquals(loginStage, LoginStage.REQUIREMENTS_WAITING);
    }

    @Test
    public void shouldGetLoginStageWhenAuthIndexTypeNotLevelOrCompositeAndComplete() {

        //Given
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.NONE);
        given(authContext.getStatus()).willReturn(AuthContext.Status.COMPLETED);

        //When
        LoginStage loginStage = loginProcess.getLoginStage();

        //Then
        verify(authContext).hasMoreRequirements();
        assertEquals(loginStage, LoginStage.COMPLETE);
    }

    @Test
    public void shouldGetCallbacks() {

        //Given

        //When
        loginProcess.getCallbacks();

        //Then
        verify(authContext).getRequirements();
    }

    @Test
    public void shouldGetPagePropertiesCallback() {

        //Given
        Callback callbackOne = mock(Callback.class);
        Callback callbackTwo = mock(PagePropertiesCallback.class);
        Callback callbackThree = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callbackOne, callbackTwo, callbackThree};

        given(authContext.getRequirements(true)).willReturn(callbacks);

        //When
        PagePropertiesCallback pagePropertiesCallback = loginProcess.getPagePropertiesCallback();

        //Then
        verify(authContext).getRequirements(true);
        assertEquals(pagePropertiesCallback, callbackTwo);
    }

    @Test
    public void shouldSubmitCallbacksUsingAuthContextIndexType() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[0];

        given(authContext.getIndexType()).willReturn(AuthIndexType.USER);

        //When
        LoginProcess loginP = loginProcess.next(callbacks);

        //Then
        verify(authContext).submitRequirements(callbacks);
        assertEquals(loginP, loginProcess);
    }

    @Test
    public void shouldSubmitCallbacksUsingLoginConfigurationIndexType() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[0];

        given(authContext.getIndexType()).willReturn(null);
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.MODULE);

        //When
        LoginProcess loginP = loginProcess.next(callbacks);

        //Then
        verify(authContext).submitRequirements(callbacks);
        assertEquals(loginP, loginProcess);
    }

    @Test
    public void shouldRestartLoginProcessWhenLevelUsedToChooseAuthModuleWithAuthTypeModule() throws AuthLoginException {

        //Given
        Callback callbackOne = mock(Callback.class);
        ChoiceCallback callbackTwo = mock(ChoiceCallback.class);
        Callback callbackThree = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callbackOne, callbackTwo, callbackThree};

        given(authContext.getIndexType()).willReturn(AuthIndexType.LEVEL);
        given(callbackTwo.getSelectedIndexes()).willReturn(new int[]{0});
        given(callbackTwo.getChoices()).willReturn(new String[]{"CHOICE_ONE"});

        given(coreServicesWrapper.getDataFromRealmQualifiedData("CHOICE_ONE")).willReturn("INDEX_VALUE");
        given(coreServicesWrapper.getRealmFromRealmQualifiedData("CHOICE_ONE")).willReturn("QUALIFIED_REALM");
        given(coreServicesWrapper.orgNameToDN("QUALIFIED_REALM")).willReturn("ORG_DN");
        given(coreServicesWrapper.getCompositeAdviceType(authContext)).willReturn(AuthUtils.MODULE);

        //When
        LoginProcess loginP = loginProcess.next(callbacks);

        //Then
        verify(authContext, never()).submitRequirements(callbacks);
        verify(authContext).setOrgDN("ORG_DN");
        verify(loginConfiguration).indexType(AuthIndexType.MODULE);
        verify(loginConfiguration).indexValue("INDEX_VALUE");
        verify(loginAuthenticator).startLoginProcess(loginProcess);
        assertNotEquals(loginP, loginProcess);
    }

    @Test
    public void shouldRestartLoginProcessWhenLevelUsedToChooseAuthModuleWithAuthTypeService()
            throws AuthLoginException {

        //Given
        Callback callbackOne = mock(Callback.class);
        ChoiceCallback callbackTwo = mock(ChoiceCallback.class);
        Callback callbackThree = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callbackOne, callbackTwo, callbackThree};

        given(authContext.getIndexType()).willReturn(AuthIndexType.LEVEL);
        given(callbackTwo.getSelectedIndexes()).willReturn(new int[]{0});
        given(callbackTwo.getChoices()).willReturn(new String[]{"CHOICE_ONE"});

        given(coreServicesWrapper.getDataFromRealmQualifiedData("CHOICE_ONE")).willReturn("INDEX_VALUE");
        given(coreServicesWrapper.getRealmFromRealmQualifiedData("CHOICE_ONE")).willReturn(null);
        given(coreServicesWrapper.getCompositeAdviceType(authContext)).willReturn(AuthUtils.SERVICE);

        //When
        LoginProcess loginP = loginProcess.next(callbacks);

        //Then
        verify(authContext, never()).submitRequirements(callbacks);
        verify(authContext, never()).setOrgDN(anyString());
        verify(loginConfiguration).indexType(AuthIndexType.SERVICE);
        verify(loginConfiguration).indexValue("INDEX_VALUE");
        verify(loginAuthenticator).startLoginProcess(loginProcess);
        assertNotEquals(loginP, loginProcess);
    }

    @Test
    public void shouldRestartLoginProcessWhenCompositeUsedToChooseAuthModuleWithAuthTypeRealm()
            throws AuthLoginException {

        //Given
        Callback callbackOne = mock(Callback.class);
        ChoiceCallback callbackTwo = mock(ChoiceCallback.class);
        Callback callbackThree = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callbackOne, callbackTwo, callbackThree};

        given(authContext.getIndexType()).willReturn(AuthIndexType.COMPOSITE);
        given(callbackTwo.getSelectedIndexes()).willReturn(new int[]{0});
        given(callbackTwo.getChoices()).willReturn(new String[]{"CHOICE_ONE"});

        given(coreServicesWrapper.getDataFromRealmQualifiedData("CHOICE_ONE")).willReturn("INDEX_VALUE");
        given(coreServicesWrapper.getRealmFromRealmQualifiedData("CHOICE_ONE")).willReturn(null);
        given(coreServicesWrapper.orgNameToDN("CHOICE_ONE")).willReturn("ORG_DN");
        given(coreServicesWrapper.getCompositeAdviceType(authContext)).willReturn(AuthUtils.REALM);
        given(coreServicesWrapper.getOrgConfiguredAuthenticationChain("ORG_DN")).willReturn("SERVICE_INDEX_VALUE");

        //When
        LoginProcess loginP = loginProcess.next(callbacks);

        //Then
        verify(authContext, never()).submitRequirements(callbacks);
        verify(authContext).setOrgDN("ORG_DN");
        verify(loginConfiguration).indexType(AuthIndexType.SERVICE);
        verify(loginConfiguration).indexValue("SERVICE_INDEX_VALUE");
        verify(loginAuthenticator).startLoginProcess(loginProcess);
        assertNotEquals(loginP, loginProcess);
    }

    @Test
    public void shouldRestartLoginProcessWhenCompositeUsedToChooseAuthModule() throws AuthLoginException {

        //Given
        Callback callbackOne = mock(Callback.class);
        ChoiceCallback callbackTwo = mock(ChoiceCallback.class);
        Callback callbackThree = mock(Callback.class);
        Callback[] callbacks = new Callback[]{callbackOne, callbackTwo, callbackThree};

        given(authContext.getIndexType()).willReturn(AuthIndexType.COMPOSITE);
        given(callbackTwo.getSelectedIndexes()).willReturn(new int[]{0});
        given(callbackTwo.getChoices()).willReturn(new String[]{"CHOICE_ONE"});

        given(coreServicesWrapper.getDataFromRealmQualifiedData("CHOICE_ONE")).willReturn("INDEX_VALUE");
        given(coreServicesWrapper.getRealmFromRealmQualifiedData("CHOICE_ONE")).willReturn(null);
        given(coreServicesWrapper.getCompositeAdviceType(authContext)).willReturn(-1);

        //When
        LoginProcess loginP = loginProcess.next(callbacks);

        //Then
        verify(authContext, never()).submitRequirements(callbacks);
        verify(authContext, never()).setOrgDN("ORG_DN");
        verify(loginConfiguration).indexType(AuthIndexType.MODULE);
        verify(loginConfiguration).indexValue("INDEX_VALUE");
        verify(loginAuthenticator).startLoginProcess(loginProcess);
        assertNotEquals(loginP, loginProcess);
    }

    @Test
    public void shouldGetIsSuccessfulWhenAuthContextIsSuccess() {

        //Given
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);

        //When
        boolean success = loginProcess.isSuccessful();

        //Then
        assertTrue(success);
    }

    @Test
    public void shouldGetIsSuccessfulWhenAuthContextNotSuccess() {

        //Given
        given(authContext.getStatus()).willReturn(AuthContext.Status.FAILED);

        //When
        boolean success = loginProcess.isSuccessful();

        //Then
        assertFalse(success);
    }

    @Test
    public void shouldGetAuthContext() {

        //Given

        //When
        AuthContextLocalWrapper authContextLocalWrapper = loginProcess.getAuthContext();

        //Then
        assertEquals(authContextLocalWrapper, authContext);
    }

    @Test
    public void shouldGetLoginConfiguration() {

        //Given

        //When
        LoginConfiguration loginConfig = loginProcess.getLoginConfiguration();

        //Then
        assertEquals(loginConfig, loginConfiguration);
    }
}
