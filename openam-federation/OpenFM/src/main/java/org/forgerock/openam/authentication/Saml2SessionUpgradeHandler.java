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
package org.forgerock.openam.authentication;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPSession;
import com.sun.identity.saml2.profile.IDPSessionCopy;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.authentication.service.SessionUpgradeHandler;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.utils.StringUtils;

/**
 * This {@link SessionUpgradeHandler} implementation should ensure that during a session upgrade the SAML authentication
 * related cache mappings are updated, so that SLO will correctly trigger logout on all remote entities. This should
 * also prevent the original session's destroy event to trigger session synchronization with remote entities.
 * The logic implemented here was mainly based on {@link com.sun.identity.saml2.profile.IDPSessionListener}.
 */
public class Saml2SessionUpgradeHandler implements SessionUpgradeHandler {

    private static final Debug debug = Debug.getInstance("libSAML2");
    private final SSOTokenManager ssoTokenManager;

    public Saml2SessionUpgradeHandler() {
        ssoTokenManager = InjectorHolder.getInstance(SSOTokenManager.class);
    }

    @Override
    public void handleSessionUpgrade(InternalSession oldSession, InternalSession newSession) {
        final String sessionIndex = oldSession.getProperty(SAML2Constants.IDP_SESSION_INDEX);

        if (StringUtils.isNotEmpty(sessionIndex)) {
            final String oldSessionID = oldSession.getID().toString();
            final String newSessionID = newSession.getID().toString();
            final SSOToken oldSSOToken;
            final SSOToken newSSOToken;
            try {
                oldSSOToken = ssoTokenManager.createSSOToken(oldSessionID);
                newSSOToken = ssoTokenManager.createSSOToken(newSessionID);
            } catch (SSOException ssoe) {
                debug.warning("Unable to create an SSOToken for the session ID due to " + ssoe.toString());
                return;
            }

            IDPSession idpSession = IDPCache.idpSessionsByIndices.get(sessionIndex);
            if (idpSession == null) {
                if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                    try {
                        final IDPSessionCopy idpSessionCopy =
                                (IDPSessionCopy) SAML2FailoverUtils.retrieveSAML2Token(sessionIndex);
                        if (idpSessionCopy != null) {
                            idpSession = new IDPSession(idpSessionCopy);
                        }
                    } catch (SAML2TokenRepositoryException stre) {
                        debug.warning("Unable to retrieve IDPSessionCopy from SAML failover store", stre);
                    }
                }
            }

            if (idpSession != null) {
                idpSession.setSession(newSSOToken);
                if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                    try {
                        SAML2FailoverUtils.deleteSAML2Token(sessionIndex);
                        long expirationTime = currentTimeMillis() / 1000 + newSession.getTimeLeft();
                        SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(sessionIndex,
                                new IDPSessionCopy(idpSession), expirationTime);
                    } catch (SAML2TokenRepositoryException stre) {
                        debug.error("Failed to update IDPSession in SAML failover store", stre);
                    }
                }

                IDPCache.idpSessionsByIndices.put(sessionIndex, idpSession);
                IDPCache.idpSessionsBySessionID.put(newSessionID, idpSession);
            }

            IDPCache.idpSessionsBySessionID.remove(oldSessionID);
            final String partner = IDPCache.spSessionPartnerBySessionID.remove(oldSessionID);
            if (partner != null) {
                IDPCache.spSessionPartnerBySessionID.put(newSessionID, partner);
            }

            try {
                //We set the sessionIndex to a dummy value so that IDPSessionListener won't try to clear out the caches
                //for the still valid sessionIndex.
                oldSSOToken.setProperty(SAML2Constants.IDP_SESSION_INDEX, "dummy");
            } catch (SSOException ssoe) {
                debug.error("Failed to set IDP Session Index for old session", ssoe);
            }
        }
    }
}
