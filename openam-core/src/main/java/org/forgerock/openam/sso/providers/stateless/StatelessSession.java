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

/*
* @since 13.0.0
*/
package org.forgerock.openam.sso.providers.stateless;

import static org.forgerock.util.Reject.*;

import java.util.Set;

import org.forgerock.openam.session.SessionConstants;
import org.forgerock.util.Reject;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.sun.identity.shared.debug.Debug;

/**
 * The <code>StatelessSession</code> class represents a stateless session. It is a simple overridden version of
 * <code>Session</code> which allows for the addition of extra methods, or overriding of inherited ones.
 */
public class StatelessSession extends Session {
    public static final String RESTRICTED_TOKENS_UNSUPPORTED = SessionBundle.getString("restrictedTokensUnsupported");
    public static final String SSOTOKEN_LISTENERS_UNSUPPORTED = SessionBundle.getString("ssoTokenListenersUnsupported");

    private final String stableId;
    private final StatelessSessionManager statelessSessionManager;

    // Indicates whether the session id needs to be regenerated to reflect the current state of the session.
    private volatile boolean needToRegenerateSessionId = false;

    /**
     * Constructs the stateless session with the given ID, session state and session factory.
     *
     * @param sid the initial ID of the session.
     * @param sessionInfo the initial of the session.
     * @param statelessSessionManager the factory to use for regenerating the session ID when the state of the session
     *                                changes.
     * @throws SessionException if an exception occurs.
     */
    public StatelessSession(SessionID sid, SessionInfo sessionInfo, StatelessSessionManager statelessSessionManager)
            throws SessionException {
        super(sid);
        update(sessionInfo);
        Reject.ifNull(sessionInfo.getSecret());
        this.stableId = sessionInfo.getSecret();
        this.statelessSessionManager = checkNotNull(statelessSessionManager);
    }

    @Override
    public String dereferenceRestrictedTokenID(Session s, String restrictedId) throws SessionException {
        throw new UnsupportedOperationException(RESTRICTED_TOKENS_UNSUPPORTED);
    }

    @Override
    protected void setRestriction(TokenRestriction restriction) {
        throw new UnsupportedOperationException(RESTRICTED_TOKENS_UNSUPPORTED);
    }

    @Override
    public String getStableStorageID() {
        return stableId;
    }

    /*
     * Only a value of true will cause Session#isTimedOut to refresh the session, where timeout calculations should be performed.
     * Because StatelessSession state is immutable and encapsulated in the jwt, Session#update is called from the StatelessSession ctor, which updates
     * Session#latestRefreshTime, which will prevent Session#maxCachingTimeReached from ever returning true. StatelessSessions
     * are not maintained in any state table, and thus are not cached, and are trivially refreshed from jwt state, justifying a return value of true.
     */
    @Override
    public boolean maxCachingTimeReached() {
        return true;
    }

    public Set<String> getPropertyNames() {
        return sessionProperties.keySet();
    }

    @Override
    public String getProperty(String name) throws SessionException {
        return sessionProperties.get(name);
    }

    @Override
    public void setProperty(String name, String value) throws SessionException {
        super.setProperty(name, value);
        this.needToRegenerateSessionId = true;
    }

    @Override
    public SessionState getState(boolean reset) throws SessionException {
        return sessionState;
    }

    @Override
    public SessionID getID() {
        SessionID id = super.getID();
        if (needToRegenerateSessionId) {
            try {
                SessionInfo sessionInfo = statelessSessionManager.getSessionInfo(id);
                sessionInfo.setProperties(this.sessionProperties);
                id = statelessSessionManager.updateSessionID(id, sessionInfo);
                this.setID(id);
                needToRegenerateSessionId = false;
            } catch (SessionException e) {
                Debug.getInstance(SessionConstants.SESSION_DEBUG)
                     .error("StatelessSession.getID: Unable to regenerate session id", e);
            }
        }
        return id;
    }

    @Override
    public SessionID getSessionID() {
        return getID();
    }
}
