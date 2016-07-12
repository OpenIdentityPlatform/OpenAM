/*
 * Copyright 2014-2016 ForgeRock AS.
 *
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
 */

package org.forgerock.openam.rest.resource;

import static org.forgerock.guava.common.base.Suppliers.*;

import javax.inject.Inject;
import javax.security.auth.Subject;

import org.forgerock.guava.common.base.Supplier;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.Reject;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * CREST context that provides a convenience method for getting hold of the caller as an authenticated subject based
 * on the OpenAM SSO Token associated with the request.
 *
 * @since 12.0.0
 */
public class SSOTokenContext extends AbstractContext implements SubjectContext {

    private final Supplier<Subject> subject;
    private final Supplier<SSOToken> token;
    private final Supplier<SessionID> sessionId;
    private final Supplier<Session> session;

    /**
     * Construct a new SSOTokenContext using a supplier for the SSOToken instance.
     * @param debug
     * @param sessionCache
     * @param parent The parent context.
     * @param token A supplier for the SSOToken to derive the other attributes of this context from.
     */
    protected SSOTokenContext(final Debug debug, final SessionCache sessionCache, Context parent,
            final Supplier<SSOToken> token) {
        super(parent, "ssoToken");
        this.token = memoize(token);
        this.subject = memoize(new Supplier<Subject>() {
            @Override
            public Subject get() {
                SSOToken ssoToken = token.get();
                return ssoToken == null ? null : SubjectUtils.createSubject(ssoToken);
            }
        });
        this.sessionId = memoize(new Supplier<SessionID>() {
            @Override
            public SessionID get() {
                SSOToken ssoToken = token.get();
                return ssoToken == null ? null : new SessionID(ssoToken.getTokenID().toString());
            }
        });
        this.session = memoize(new Supplier<Session>() {
            @Override
            public Session get() {
                try {
                    SessionID sessionID = sessionId.get();
                    return sessionID == null ? null : sessionCache.getSession(sessionID);
                } catch (SessionException e) {
                    debug.warning("Requested session but could not return one", e);
                    return null;
                }
            }
        });
    }

    /**
     * Construct a new SSOTokenContext from the parent, which is expected to contain a {@link SecurityContext}.
     * @param parent The parent context.
     */
    @Inject
    public SSOTokenContext(@Named(SessionConstants.SESSION_DEBUG) final Debug debug, SessionCache sessionCache,
            @Assisted final Context parent) {
        this(debug, sessionCache, parent, new Supplier<SSOToken>() {
            @Override
            public SSOToken get() {
                try {
                    return getSsoToken(parent);
                } catch (SSOException e) {
                    debug.message("Could not get SSOToken from context", e);
                    return null;
                }
            }
        });
        Reject.ifFalse(parent.containsContext(SecurityContext.class), "Parent context must contain a SecurityContext");
    }

    /**
     * Obtain an SSOToken from a Context.
     * @param context The context. Must contain a SecurityContext.
     * @return The SSOToken.
     * @throws SSOException If the token could not be obtained.
     */
    public static SSOToken getSsoToken(Context context) throws SSOException {
        Reject.ifFalse(context.containsContext(SecurityContext.class), "Parent context must contain a SecurityContext");
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        if (context.containsContext(SSOTokenContext.class)) {
            SSOTokenContext ssoTokenContext = context.asContext(SSOTokenContext.class);
            if (ssoTokenContext.asContext(SecurityContext.class) == securityContext) {
                return ssoTokenContext.getCallerSSOToken();
            }
        }
        return SSOTokenManager.getInstance().createSSOToken(RestUtils.getCookieFromServerContext(securityContext));
    }

    @Override
    public Subject getCallerSubject() {
        return subject.get();
    }

    @Override
    public SSOToken getCallerSSOToken() {
        return token.get();
    }

    @Override
    public SessionID getCallerSessionID() {
        return sessionId.get();
    }

    @Override
    public Session getCallerSession() {
        return session.get();
    }

    /**
     * Guice factory for SSOTokenContext instances
     */
    public interface Factory {
        /**
         * Creates a new context.
         * @param parent The parent context.
         * @return The newly created context.
         */
        SSOTokenContext create(Context parent);
    }

}
