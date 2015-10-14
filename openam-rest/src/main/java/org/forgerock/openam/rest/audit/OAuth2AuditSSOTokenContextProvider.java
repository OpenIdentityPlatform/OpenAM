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
package org.forgerock.openam.rest.audit;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.Constants;
import org.forgerock.openam.audit.AuditConstants.TrackingIdKey;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

import static org.forgerock.openam.audit.AuditConstants.TrackingIdKey.SESSION;

/**
 * A provider which provides user id and context details for auditing purposes. This provider draws its details
 * from an {@link SSOToken} if one is available.
 *
 * @since 13.0.0
 */
public class OAuth2AuditSSOTokenContextProvider implements OAuth2AuditContextProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserId(Request request) {
        String userId = getUserIdFromSSOSessionToken(request);
        if (userId != null) {
            return userId;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTrackingId(Request request) {
        String trackingId;

        trackingId = getTrackingIdFromSSOSessionToken(request);
        if (trackingId != null) {
            return trackingId;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TrackingIdKey getTrackingIdKey() {
        return SESSION;
    }

    private String getUserIdFromSSOSessionToken(Request request) {
        String userId = null;

        SSOToken token = getSSOToken(request);
        if (token != null) {
            try {
                userId = token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
            } catch (SSOException e) {
                //Do nothing
            }
        }

        return userId;
    }

    private String getTrackingIdFromSSOSessionToken(Request request) {
        String trackingId = null;

        SSOToken token = getSSOToken(request);
        if (token != null) {
            try {
                trackingId = token.getProperty(Constants.AM_CTX_ID);
            } catch (SSOException e) {
                //Do nothing
            }
        }

        return trackingId;
    }

    private SSOToken getSSOToken(Request request) {
        SSOToken token;
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            token = mgr.createSSOToken(ServletUtils.getRequest(request));
        } catch (Exception e) {
            return null;
        }
        return token;
    }
}
