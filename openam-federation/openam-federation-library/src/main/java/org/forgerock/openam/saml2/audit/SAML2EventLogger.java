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
package org.forgerock.openam.saml2.audit;

import org.forgerock.audit.AuditException;

/**
 * A SAML2EventLogger can receive information about SAML2 request progress and success along with information to
 * track individual requests.  Security critical information should not be passed to this class.
 */
public interface SAML2EventLogger {

    /**
     * Publishes an audit event with details of the attempted SAML2 operation, if the 'access' topic is audited.
     *
     * @throws AuditException If an exception occurred that prevented the audit event from being published.
     */
    void auditAccessAttempt();

    /**
     * Publishes an event with details of the successfully completed SAML2 operation, if the 'access' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     */
    void auditAccessSuccess();

    /**
     * Publishes an event with details of the failed CREST operation, if the 'access' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     *
     * @param errorCode A unique code that identifies the error condition.
     * @param message   A human-readable description of the error that occurred.
     */
    void auditAccessFailure(String errorCode, String message);

    /**
     * @param trackingId Unique alias of session.
     */
    void setSessionTrackingId(String trackingId);

    /**
     * @param userId Identifies Subject of authentication.
     */
    void setUserId(String userId);

    /**
     * @param realm The realm for which the event is being logged.
     */
    void setRealm(String realm);

    /**
     * @param method Identifies the operation invoked.
     */
    void setMethod(String method);

    /**
     * Audits a forward to proxy action.
     */
    void auditForwardToProxy();

    /**
     * Audits a forward to local user Login Aciton
     */
    void auditForwardToLocalUserLogin();

    /**
    /**
     * @param authnRequestId the request id to log for this saml2 auth request
     */
    void setRequestId(String authnRequestId);

    /**
     * @param ssoTokenId sets the sso token id
     */
    void setSSOTokenId(Object ssoTokenId);

    /**
     * @param session the auth token id for the preceding authentication request
     */
    void setAuthTokenId(Object session);
}
