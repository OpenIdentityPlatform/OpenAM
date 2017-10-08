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
package org.forgerock.openam.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.providers.dpro.SSOPrincipal;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.SecurityContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.security.PrivilegedAction;

/**
 * Unit test for {@link ElevatedConnectionFactoryWrapper}.
 *
 * @since 13.0.0
 */
public final class ElevatedConnectionFactoryWrapperTest {

    @Mock
    private ConnectionFactory internalConnectionFactory;
    @Mock
    private Connection connection;
    @Mock
    private PrivilegedAction<SSOToken> ssoTokenPrivilegedAction;

    @Captor
    private ArgumentCaptor<Context> contextCaptor;

    private ConnectionFactory connectionFactory;

    @Mock
    private SSOTokenContext.Factory contextFactory;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        connectionFactory = new ElevatedConnectionFactoryWrapper(internalConnectionFactory, ssoTokenPrivilegedAction,
                contextFactory);
    }

    @Test
    public void requestGetsElevatedToAdminSession() throws Exception {
        // Given
        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoTokenPrivilegedAction.run()).willReturn(ssoToken);

        SSOPrincipal principal = new SSOPrincipal("test");
        given(ssoToken.getPrincipal()).willReturn(principal);

        SSOTokenID tokenID = mock(SSOTokenID.class);
        given(ssoToken.getTokenID()).willReturn(tokenID);

        given(internalConnectionFactory.getConnection()).willReturn(connection);

        given(contextFactory.create(any(Context.class))).willAnswer(new Answer<SSOTokenContext>() {
            @Override
            public SSOTokenContext answer(InvocationOnMock invocation) throws Throwable {
                return new SSOTokenContext(null, null, (Context) invocation.getArguments()[0]);
            }
        });

        // When
        RootContext context = new RootContext();
        ReadRequest readRequest = Requests.newReadRequest("/test", "abc");

        try (Connection connection = connectionFactory.getConnection()) {
            connection.read(context, readRequest);
        }

        // Then
        verify(connection).read(contextCaptor.capture(), eq(readRequest));

        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.containsContext(SecurityContext.class)).isTrue();

        SecurityContext securityContext = capturedContext.asContext(SecurityContext.class);
        assertThat(securityContext.getAuthenticationId()).isEqualTo("test");
        assertThat(securityContext.getAuthorization()).containsOnlyKeys("authLevel", "tokenId");
    }

}