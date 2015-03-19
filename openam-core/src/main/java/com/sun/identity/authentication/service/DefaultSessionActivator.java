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

// Portions Copyrighted 2015 ForgeRock AS.

package com.sun.identity.authentication.service;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Enumeration;

/**
 * The default session activator
 */
class DefaultSessionActivator implements SessionActivator {
    static final DefaultSessionActivator INSTANCE = new DefaultSessionActivator();

    private static final Debug DEBUG = AuthD.debug;

    DefaultSessionActivator() {
        // Visible for same-package sub-classes only
    }

    @Override
    public boolean activateSession(final LoginState loginState, final SessionService sessionService,
                                   final InternalSession authSession, final Subject subject, final Object loginContext)
            throws AuthException {

        final SessionID oldSessId = authSession.getID();

        final InternalSession session = createSession(sessionService, loginState);

        session.removeObject(ISAuthConstants.AUTH_CONTEXT_OBJ);
        loginState.setSessionProperties(session);

        //copying over the session properties that were set on the authentication session onto the new session
        // TODO: move forceAuth (and session upgrade) into a different session activator
        final InternalSession sessionToUpdate = loginState.getForceFlag() ? loginState.getOldSession() : session;
        Enumeration<String> authSessionProperties = authSession.getPropertyNames();
        while (authSessionProperties.hasMoreElements()) {
            String key = authSessionProperties.nextElement();
            String value = authSession.getProperty(key);
            sessionToUpdate.putProperty(key, value);
        }

        //destroying the authentication session
        sessionService.destroyInternalSession(oldSessId);

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Activating session: " + session);
        }

        loginState.setSession(session);
        loginState.setSubject(addSSOTokenPrincipal(subject, session.getID()));

        if (loginState.isModulesInSessionEnabled() && loginContext != null) {
            session.setObject(ISAuthConstants.LOGIN_CONTEXT, loginContext);
        }

        try {
            return activateSession(session, loginState);
        } catch (SessionException e) {
            throw new AuthException(e);
        }
    }


    protected InternalSession createSession(SessionService sessionService, LoginState loginState) {
        return sessionService.newInternalSession(loginState.getOrgDN(), null, false);
    }

    protected boolean activateSession(InternalSession session, LoginState loginState) throws SessionException {
        return session.activate(loginState.getUserDN());
    }

    /* add the SSOTokenPrincipal to the Subject */
    private Subject addSSOTokenPrincipal(Subject subject, SessionID sid) {
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
