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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashSet;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.tokens.TokenType;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaTokenStoreTest {

    private static final Token RPT_TOKEN = new Token("123", TokenType.REQUESTING_PARTY);
    private static final Token TICKET_TOKEN = new Token("123", TokenType.PERMISSION_TICKET);
    private static final AccessToken AAT;

    static {
        try {
            AAT = new AccessToken(json(object(field("tokenName", "access_token"), field("id", "123"))));
        } catch (InvalidGrantException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private UmaTokenStore store;
    private CTSPersistentStore cts;
    private JavaBeanAdapter<RequestingPartyToken> rptAdapter;
    private JavaBeanAdapter<PermissionTicket> permissionTicketAdapter;
    private UmaProviderSettingsFactory providerSettingsFactory;
    private UmaProviderSettings providerSettings;

    @BeforeMethod
    public void setup() throws Exception {
        cts = mock(CTSPersistentStore.class);
        rptAdapter = mock(JavaBeanAdapter.class);
        permissionTicketAdapter = mock(JavaBeanAdapter.class);
        providerSettingsFactory = mock(UmaProviderSettingsFactory.class);
        providerSettings = mock(UmaProviderSettings.class);
        store = new UmaTokenStore("REALM", rptAdapter, permissionTicketAdapter, cts, providerSettingsFactory);

        given(providerSettingsFactory.get(anyString())).willReturn(providerSettings);
    }

    @Test
    public void testCreateRPT() throws Exception {
        //Given
        ArgumentCaptor<RequestingPartyToken> rptCapture = ArgumentCaptor.forClass(RequestingPartyToken.class);
        when(rptAdapter.toToken(rptCapture.capture())).thenAnswer(new Answer<Token>() {
            @Override
            public Token answer(InvocationOnMock invocation) throws Throwable {
                return RPT_TOKEN;
            }
        });
        when(providerSettings.getRPTLifetime()).thenReturn(1000L);

        //When
        RequestingPartyToken rpt = store.createRPT(AAT, new PermissionTicket("123", "abc", new HashSet<String>(asList("a")), null));

        //Then
        verify(cts).create(RPT_TOKEN);
        assertThat(rpt).isSameAs(rptCapture.getValue());
    }

    @Test(expectedExceptions = ServerException.class)
    public void testCreateRPTFailed() throws Exception {
        //Given
        ArgumentCaptor<RequestingPartyToken> rptCapture = ArgumentCaptor.forClass(RequestingPartyToken.class);
        when(rptAdapter.toToken(rptCapture.capture())).thenAnswer(new Answer<Token>() {
            @Override
            public Token answer(InvocationOnMock invocation) throws Throwable {
                return RPT_TOKEN;
            }
        });
        when(providerSettings.getRPTLifetime()).thenReturn(1000L);

        doThrow(CoreTokenException.class).when(cts).create(RPT_TOKEN);

        //When
        store.createRPT(AAT, new PermissionTicket("123", "abc", new HashSet<String>(asList("a")), null));
    }

    @Test
    public void testCreatePermissionTicket() throws Exception {
        //Given
        ArgumentCaptor<PermissionTicket> ticketCapture = ArgumentCaptor.forClass(PermissionTicket.class);
        when(permissionTicketAdapter.toToken(ticketCapture.capture())).thenAnswer(new Answer<Token>() {
            @Override
            public Token answer(InvocationOnMock invocation) throws Throwable {
                return TICKET_TOKEN;
            }
        });
        when(providerSettings.getPermissionTicketLifetime()).thenReturn(1000L);

        //When
        PermissionTicket ticket = store.createPermissionTicket("123", new HashSet<String>(asList("a", "b")), null);

        //Then
        verify(cts).create(TICKET_TOKEN);
        assertThat(ticket).isSameAs(ticketCapture.getValue());
        assertThat(ticket.getResourceSetId()).isEqualTo("123");
        assertThat(ticket.getScopes()).contains("a", "b");
    }

    @Test(expectedExceptions = ServerException.class)
    public void testCreatePermissionTicketFailure() throws Exception {
        //Given
        ArgumentCaptor<PermissionTicket> ticketCapture = ArgumentCaptor.forClass(PermissionTicket.class);
        when(permissionTicketAdapter.toToken(ticketCapture.capture())).thenAnswer(new Answer<Token>() {
            @Override
            public Token answer(InvocationOnMock invocation) throws Throwable {
                return TICKET_TOKEN;
            }
        });
        when(providerSettings.getPermissionTicketLifetime()).thenReturn(1000L);

        doThrow(CoreTokenException.class).when(cts).create(TICKET_TOKEN);

        //When
        store.createPermissionTicket("123", new HashSet<String>(asList("a", "b")), null);
    }

    @Test
    public void testReadRPT() throws Exception {
        //Given
        final RequestingPartyToken rpt = new RequestingPartyToken();
        rpt.setRealm("REALM");
        when(rptAdapter.fromToken(RPT_TOKEN)).thenAnswer(new Answer<RequestingPartyToken>() {
            @Override
            public RequestingPartyToken answer(InvocationOnMock invocation) throws Throwable {
                return rpt;
            }
        });
        when(cts.read("123")).thenReturn(RPT_TOKEN);

        //When
        RequestingPartyToken answer = store.readRPT("123");

        //Then
        assertThat(answer).isSameAs(rpt);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testReadRPTFailure() throws Exception {
        //Given
        when(cts.read("123")).thenThrow(CoreTokenException.class);

        //When
        store.readRPT("123");
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldNotReadRPTInDifferentRealm() throws Exception {

        //Given
        final RequestingPartyToken rpt = new RequestingPartyToken();
        rpt.setRealm("OTHER_REALM");
        when(rptAdapter.fromToken(RPT_TOKEN)).thenAnswer(new Answer<RequestingPartyToken>() {
            @Override
            public RequestingPartyToken answer(InvocationOnMock invocation) throws Throwable {
                return rpt;
            }
        });
        when(cts.read("123")).thenReturn(RPT_TOKEN);

        //When
        store.readRPT("123");

        //Then
        //Expected NotFoundException
    }

    @Test
    public void testReadPermissionTicket() throws Exception {
        //Given
        final PermissionTicket ticket = new PermissionTicket("123", "abc", new HashSet<String>(asList("a")), null);
        ticket.setRealm("REALM");
        when(permissionTicketAdapter.fromToken(TICKET_TOKEN)).thenAnswer(new Answer<PermissionTicket>() {
            @Override
            public PermissionTicket answer(InvocationOnMock invocation) throws Throwable {
                return ticket;
            }
        });
        when(cts.read("123")).thenReturn(TICKET_TOKEN);

        //When
        PermissionTicket answer = store.readPermissionTicket("123");

        //Then
        assertThat(answer).isSameAs(ticket);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testReadPermissionTicketFailure() throws Exception {
        //Given
        when(cts.read("123")).thenThrow(CoreTokenException.class);

        //When
        store.readPermissionTicket("123");
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldNotReadPermissionTicketInDifferentRealm() throws Exception {
        //Given
        final PermissionTicket ticket = new PermissionTicket("123", "abc", new HashSet<String>(asList("a")), null);
        ticket.setRealm("OTHER_REALM");
        when(permissionTicketAdapter.fromToken(TICKET_TOKEN)).thenAnswer(new Answer<PermissionTicket>() {
            @Override
            public PermissionTicket answer(InvocationOnMock invocation) throws Throwable {
                return ticket;
            }
        });
        when(cts.read("123")).thenReturn(TICKET_TOKEN);

        //When
        store.readPermissionTicket("123");

        //Then
        //Expected NotFoundException
    }

    @Test
    public void testDeleteRPT() throws Exception {
        //Given
        final RequestingPartyToken rpt = new RequestingPartyToken();
        rpt.setRealm("REALM");
        when(cts.read("123")).thenReturn(RPT_TOKEN);
        when(rptAdapter.fromToken(RPT_TOKEN)).thenReturn(rpt);

        //When
        store.deleteRPT("123");

        //Then
        verify(cts).delete("123");
    }

    @Test(expectedExceptions = ServerException.class)
    public void testDeleteRPTFailure() throws Exception {
        //Given
        final RequestingPartyToken rpt = new RequestingPartyToken();
        rpt.setRealm("REALM");
        when(cts.read("123")).thenReturn(RPT_TOKEN);
        doThrow(CoreTokenException.class).when(cts).delete("123");
        when(rptAdapter.fromToken(RPT_TOKEN)).thenReturn(rpt);

        //When
        store.deleteRPT("123");
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testDeleteRPTMissing() throws Exception {
        //Given
        when(cts.read("123")).thenThrow(CoreTokenException.class);

        //When
        store.deleteRPT("123");
    }

    @Test
    public void testDeletePermissionTicket() throws Exception {
        //Given
        final PermissionTicket ticket = new PermissionTicket("123", "abc", new HashSet<String>(asList("a")), null);
        ticket.setRealm("REALM");
        when(cts.read("123")).thenReturn(TICKET_TOKEN);
        when(permissionTicketAdapter.fromToken(TICKET_TOKEN)).thenReturn(ticket);

        //When
        store.deletePermissionTicket("123");

        //Then
        verify(cts).delete("123");
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testDeletePermissionTicketMissing() throws Exception {
        //Given
        when(cts.read("123")).thenThrow(CoreTokenException.class);

        //When
        store.deletePermissionTicket("123");
    }

    @Test(expectedExceptions = ServerException.class)
    public void testDeletePermissionTicketFailure() throws Exception {
        //Given
        final PermissionTicket ticket = new PermissionTicket("123", "abc", new HashSet<String>(asList("a")), null);
        ticket.setRealm("REALM");
        when(cts.read("123")).thenReturn(TICKET_TOKEN);
        when(permissionTicketAdapter.fromToken(TICKET_TOKEN)).thenReturn(ticket);
        doThrow(CoreTokenException.class).when(cts).delete("123");

        //When
        store.deletePermissionTicket("123");
    }
}