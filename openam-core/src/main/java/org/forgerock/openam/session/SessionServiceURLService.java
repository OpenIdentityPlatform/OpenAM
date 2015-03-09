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

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.services.naming.WebtopNaming;
import org.forgerock.guice.core.InjectorHolder;

import javax.inject.Singleton;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import static org.forgerock.openam.session.SessionConstants.SESSION_SERVICE;

/**
 * ClientSDK: This code is ClientSDK aware and will only use the Server SessionService
 * when invoked on the Server.
 */
@Singleton
public class SessionServiceURLService {
    private static SessionServiceURLService instance;
    public synchronized static SessionServiceURLService getInstance() {
        if (instance == null) {
            instance = new SessionServiceURLService();
        }
        return instance;
    }

    // Hidden constructor to enforce singleton.
    private SessionServiceURLService() {}

    /**
     * The session service URL table indexed by server address contained in the
     * Session ID object.
     */
    private final ConcurrentHashMap<String, URL> sessionServiceURLTable = new ConcurrentHashMap<String, URL>();

    /**
     * Returns Session Service URL.
     *
     * @param protocol Session Server protocol.
     * @param server Session Server host name.
     * @param port Session Server port.
     * @param uri Session Server URI.
     * @return URL Session Service URL.
     * @exception com.iplanet.dpro.session.SessionException
     */
    public URL getSessionServiceURL(String protocol, String server, String port, String uri)
            throws SessionException {
        String key = protocol + "://" + server + ":" + port + uri;
        URL url = sessionServiceURLTable.get(key);
        if (url == null) {
            try {
                url = WebtopNaming.getServiceURL(SESSION_SERVICE, protocol,
                        server, port, uri);
                sessionServiceURLTable.put(key, url);
                return url;
            } catch (Exception e) {
                throw new SessionException(e);
            }
        }
        return url;
    }

    /**
     * Returns Session Service URL for a given server ID.
     *
     * @param serverID server ID from the platform server list.
     * @return Session Service URL.
     * @exception SessionException
     */
    public URL getSessionServiceURL(String serverID) throws SessionException {
        try {
            URL parsedServerURL = new URL(WebtopNaming.getServerFromID(serverID));

            return getSessionServiceURL(
                    parsedServerURL.getProtocol(),
                    parsedServerURL.getHost(),
                    Integer.toString(parsedServerURL.getPort()),
                    parsedServerURL.getPath());

        } catch (Exception e) {
            throw new SessionException(e);
        }
    }


    /**
     * Returns Session Service URL for a Session ID.
     *
     * @param sid Session ID
     * @return Session Service URL.
     * @exception SessionException
     */
    public URL getSessionServiceURL(SessionID sid) throws SessionException {
        String primaryId;

        if (SystemProperties.isServerMode()) {

            /**
             * Validate that the SessionID contains valid Server and Site references.
             * This check is not appropriate for client side code as only the Site
             * reference is exposed to client code.
             */
            sid.validate();

            SessionService ss = InjectorHolder.getInstance(SessionService.class);
            if (ss.isSiteEnabled() && ss.isLocalSite(sid)) {
                if (ss.isSessionFailoverEnabled()) {
                    return getSessionServiceURL(ss.getCurrentHostServer(sid));
                } else {
                    primaryId = sid.getExtension(SessionID.PRIMARY_ID);
                    return getSessionServiceURL(primaryId);
                }
            }
        } else {
            primaryId = sid.getExtension(SessionID.PRIMARY_ID);
            if (primaryId != null) {
                String secondarysites = WebtopNaming.getSecondarySites(primaryId);

                String serverID = SessionService.getAMServerID();
                if ((secondarysites != null) && (serverID != null)) {
                    if (secondarysites.indexOf(serverID) != -1) {
                        return getSessionServiceURL(serverID);
                    }
                }
            }
        }

        return getSessionServiceURL(sid.getSessionServerProtocol(), sid.getSessionServer(), sid.getSessionServerPort(),
                sid.getSessionServerURI());
    }

}
