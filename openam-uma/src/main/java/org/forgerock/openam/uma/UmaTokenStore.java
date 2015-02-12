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

import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.services.comm.share.Request;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UmaTokenStore {

    private final Logger logger = LoggerFactory.getLogger("UmaProvider");

    private final String realm;
    private final JavaBeanAdapter<RequestingPartyToken> rptAdapter;
    private final JavaBeanAdapter<PermissionTicket> permissionTicketAdapter;
    private final CTSPersistentStore cts;
    private final UmaProviderSettingsFactory settingsFactory;

    @Inject
    UmaTokenStore(@Assisted String realm, JavaBeanAdapter<RequestingPartyToken> rptAdapter,
            JavaBeanAdapter<PermissionTicket> permissionTicketAdapter, CTSPersistentStore cts,
            UmaProviderSettingsFactory settingsFactory) {
        this.realm = realm;
        this.rptAdapter = rptAdapter;
        this.permissionTicketAdapter = permissionTicketAdapter;
        this.cts = cts;
        this.settingsFactory = settingsFactory;
    }

    RequestingPartyToken createRPT(AccessToken aat, PermissionTicket permissionTicket) throws ServerException {
        UmaProviderSettings settings = settingsFactory.get(realm);
        Permission permission = new Permission(permissionTicket.getResourceSetId(), permissionTicket.getScopes());
        RequestingPartyToken rpt = new RequestingPartyToken(null, permissionTicket.getClientId(), asSet(permission),
                System.currentTimeMillis() + (settings.getRPTLifetime() * 1000));
        rpt.setRealm(realm);
        try {
            cts.create(rptAdapter.toToken(rpt));
        } catch (CoreTokenException e) {
            throw new ServerException(e);
        }
        return rpt;
    }

    PermissionTicket createPermissionTicket(String resourceSetId, Set<String> scopes, String clientId)
            throws ServerException {
        UmaProviderSettings settings = settingsFactory.get(realm);
        PermissionTicket permissionTicket = new PermissionTicket(null, resourceSetId, scopes, clientId);
        permissionTicket.setRealm(realm);
        permissionTicket.setExpiryTime(System.currentTimeMillis() + (settings.getPermissionTicketLifetime() * 1000));
        try {
            cts.create(permissionTicketAdapter.toToken(permissionTicket));
        } catch (CoreTokenException e) {
            throw new ServerException(e);
        }
        return permissionTicket;
    }

    public RequestingPartyToken readRPT(String id) throws NotFoundException {
        return (RequestingPartyToken) readToken(id, rptAdapter);
    }

    public PermissionTicket readPermissionTicket(String id) throws NotFoundException {
        return (PermissionTicket) readToken(id, permissionTicketAdapter);
    }

    public UmaToken readToken(String ticketId, JavaBeanAdapter<? extends UmaToken> adapter) throws NotFoundException {
        try {
            Token token = cts.read(ticketId);
            if (token == null) {
                throw new NotFoundException("No valid ticket exists with ticketId");
            }
            UmaToken ticket = adapter.fromToken(token);
            if (!realm.equals(ticket.getRealm())) {
                throw new NotFoundException("No valid ticket exists with ticketId in the realm, " + realm);
            }
            return ticket;
        } catch (CoreTokenException e) {
            throw new NotFoundException("No valid ticket exists with ticketId");
        }
    }

    public void deleteRPT(String id) throws NotFoundException, ServerException {
        try {
            // check token is RPT
            readRPT(id);
            cts.delete(id);
        } catch (CoreTokenException e) {
            throw new ServerException("Could not delete token: " + id);
        }
    }

    public void deletePermissionTicket(String id) throws NotFoundException, ServerException {
        try {
            // check token is permission ticket
            readPermissionTicket(id);
            cts.delete(id);
        } catch (CoreTokenException e) {
            throw new ServerException("Could not delete token: " + id);
        }
    }

}
