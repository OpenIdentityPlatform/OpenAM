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
package org.forgerock.openam.session;

import static org.forgerock.openam.session.SessionConstants.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.services.comm.share.Response;
import java.net.URL;
import java.util.Vector;
import javax.inject.Inject;

public class SessionPLLSender {

    private final SessionCookies sessionCookies;

    @Inject
    public SessionPLLSender(SessionCookies sessionCookies) {
        this.sessionCookies = sessionCookies;
    }

    /**
     * Returns a Session Response object based on the XML document received from
     * remote Session Server. This is in response to a request that we send to
     * the session server.
     *
     * @param svcurl The URL of the Session Service.
     * @param sreq The Session Request XML document.
     * @return a Vector of responses from the remote server
     * @exception com.iplanet.dpro.session.SessionException if there was an error in sending the XML
     *            document or if the response has multiple components.
     */
    public SessionResponse sendPLLRequest(URL svcurl, SessionRequest sreq) throws SessionException {
        try {

            String cookies = sessionCookies.getCookieName() + "=" + sreq.getSessionID();

            if (!SystemProperties.isServerMode()) {
                SessionID sessionID = new SessionID(sreq.getSessionID());
                cookies = cookies + ";" + sessionCookies.getLBCookie(sessionID);
            }

            final Request req = new Request(sreq.toXMLString());

            final RequestSet set = new RequestSet(SESSION_SERVICE);
            set.addRequest(req);

            final Vector responses = PLLClient.send(svcurl, cookies, set);

            if (responses.size() != 1) {
                throw new SessionException(SessionBundle.rbName, "unexpectedResponse", null);
            }

            final Response res = (Response) responses.elementAt(0);

            return SessionResponse.parseXML(res.getContent());
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }
}
