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

package org.forgerock.openam.oauth2;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.AccessController;
import java.util.Collections;
import java.util.Set;

/**
 * The OpenAM OAuth2 and OpenId Connect provider's store for all client registrations.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMClientRegistrationStore implements OpenIdConnectClientRegistrationStore {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final RealmNormaliser realmNormaliser;
    private final PEMDecoder pemDecoder;

    /**
     * Constructs a new OpenAMClientRegistrationStore.
     *
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param pemDecoder A {@code PEMDecoder} instance.
     */
    @Inject
    public OpenAMClientRegistrationStore(RealmNormaliser realmNormaliser, PEMDecoder pemDecoder) {
        this.realmNormaliser = realmNormaliser;
        this.pemDecoder = pemDecoder;
    }

    /**
     * {@inheritDoc}
     */
    public OpenIdConnectClientRegistration get(String clientId, OAuth2Request request) throws InvalidClientException {

        final String realm = realmNormaliser.normalise(request.<String>getParameter("realm"));
        return new OpenAMClientRegistration(getIdentity(clientId, realm), pemDecoder);
    }

    @SuppressWarnings("unchecked")
    private AMIdentity getIdentity(String uName, String realm) throws InvalidClientException {
        final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        final AMIdentity theID;

        try {
            final AMIdentityRepository amIdRepo = new AMIdentityRepository(token, realm);

            final IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            Set<AMIdentity> results = Collections.emptySet();
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.AGENT, uName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results == null || results.size() != 1) {
                throw new InvalidClientException("Client authentication failed");

            }

            theID = results.iterator().next();

            //if the client is deactivated return null
            if (theID.isActive()){
                return theID;
            } else {
                return null;
            }
        } catch (SSOException e) {
            logger.error("ClientVerifierImpl::Unable to get client AMIdentity: ", e);
            throw new InvalidClientException("Client authentication failed");
        } catch (IdRepoException e) {
            logger.error("ClientVerifierImpl::Unable to get client AMIdentity: ", e);
            throw new InvalidClientException("Client authentication failed");
        }
    }
}
