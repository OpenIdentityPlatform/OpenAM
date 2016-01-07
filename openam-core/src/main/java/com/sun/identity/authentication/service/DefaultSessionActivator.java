/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: LoginState.java,v 1.56 2009/11/25 12:04:19 manish_rustagi Exp $
 *
 */

/**
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.authentication.service;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.sso.providers.stateless.StatelessSession;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Enumeration;

/**
 * The default session activator: creates a new session, sets that as the current session,
 * copies in properties from the session it's upgrading from (if appropriate) and then
 * from the auth session into this new session, deletes the auth session and returns.
 */
public class DefaultSessionActivator implements SessionActivator {
    static final DefaultSessionActivator INSTANCE = new DefaultSessionActivator();

    protected static final Debug DEBUG = AuthD.debug;

    protected DefaultSessionActivator() {
    }

    @Override
    public boolean activateSession(final LoginState loginState, final SessionService sessionService,
                                   final InternalSession authSession, final Subject subject, final Object loginContext)
            throws AuthException {

        //create our new session - the loginState needs this session as it's the one we'll be passing back to the user
        final InternalSession session = createSession(sessionService, loginState);
        loginState.setSession(session);

        return updateSessions(session, loginState, session, authSession, sessionService, subject, loginContext);
    }

    /**
     * newSession and sessionToActivate may be the same session -- e.g. in the default case for normal or stateless
     * tokens. In other circumstances they will differ (i.e. ForceAuth).
     */
    protected boolean updateSessions(InternalSession newSession, LoginState loginState,
                                     InternalSession sessionToActivate, InternalSession authSession,
                                     SessionService sessionService, Subject subject, Object loginContext)
            throws AuthException {

        final SessionID authSessionId = authSession.getID();

        newSession.removeObject(ISAuthConstants.AUTH_CONTEXT_OBJ);

        //session upgrade and anonymous conditions are handled in here
        loginState.setSessionProperties(newSession);

        //copy in our auth session properties (if any)
        putAllPropertiesFromAuthSession(authSession, sessionToActivate);

        //destroying the authentication session
        sessionService.destroyInternalSession(authSessionId);

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Activating session: " + newSession);
        }

        //ensure that we've updated the subject (if appropriate, e.g. from anonymous -> known)
        loginState.setSubject(addSSOTokenPrincipal(subject, sessionToActivate.getID()));

        //set the login context for this session
        if (loginState.isModulesInSessionEnabled() && loginContext != null) {
            newSession.setObject(ISAuthConstants.LOGIN_CONTEXT, loginContext);
        }

        try {
            return activateSession(sessionToActivate, loginState);
        } catch (SessionException e) {
            throw new AuthException(e);
        }
    }

    protected void putAllPropertiesFromAuthSession(InternalSession authSession, InternalSession sessionToUpdate) {
        Enumeration<String> authSessionProperties = authSession.getPropertyNames();
        while (authSessionProperties.hasMoreElements()) {
            String key = authSessionProperties.nextElement();
            String value = authSession.getProperty(key);
            sessionToUpdate.putProperty(key, value);
        }
    }

    protected InternalSession createSession(SessionService sessionService, LoginState loginState) {
        return sessionService.newInternalSession(loginState.getOrgDN(), null, false);
    }

    protected boolean activateSession(InternalSession session, LoginState loginState) throws SessionException {
        return session.activate(loginState.getUserDN());
    }

    protected Subject addSSOTokenPrincipal(Subject subject, SessionID sid) {
        if (subject == null) {
            subject = new Subject();
        }
        String sidStr = sid.toString();
        if (DEBUG.messageEnabled()) {
            DEBUG.message("sid string is.. " + sidStr);
        }
        Principal ssoTokenPrincipal = new SSOTokenPrincipal(sidStr);
        subject.getPrincipals().add(ssoTokenPrincipal);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Subject is.. :" + subject);
        }

        return subject;
    }


}
