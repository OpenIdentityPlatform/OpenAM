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
package org.forgerock.openam.saml2;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.logging.LogUtil;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class SAMLBase {

    protected static final String INDEX = "index";
    protected static final String ACS_URL = "acsURL";
    protected static final String SP_ENTITY_ID = "spEntityID";
    protected static final String BINDING = "binding";

    protected static final String INVALID_SAML_REQUEST = "InvalidSAMLRequest";
    protected static final String METADATA_ERROR = "metaDataError";
    protected static final String SSO_OR_FEDERATION_ERROR = "UnableToDOSSOOrFederation";

    protected void logAccess(String logId, Level logLevel, String... data) {
        LogUtil.access(logLevel, logId, data);
    }

    protected void logError(Level logLevel, String logId, Object session, Map properties, String... data) {
        LogUtil.error(logLevel, logId, data, session, properties);
    }

    protected boolean preSingleSignOn(HttpServletRequest request, HttpServletResponse response, IDPSSOFederateRequest data) {

        try {
            SAML2Utils.debug.message("Invoking the IDP Adapter preSingleSignOn hook");
            return data.getIdpAdapter().preSingleSignOn(data.getIdpEntityID(), data.getRealm(), request, response,
                    data.getAuthnRequest(), data.getRequestID());
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("Error invoking the IDP Adapter", se);
        }

        return false;
    }

    protected boolean preSendResponse(HttpServletRequest request, HttpServletResponse response, IDPSSOFederateRequest data) {

        try {
            SAML2Utils.debug.message("Invoking the IDP Adapter preSendResponse");
            return data.getIdpAdapter().preSendResponse(data.getAuthnRequest(), data.getIdpEntityID(), data.getRealm(),
                    request, response, data.getSession(), data.getRequestID(), data.getRelayState());
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("Error invoking the IDP Adapter", se);
        }

        return false;
    }

    protected boolean preAuthenticationAdapter(HttpServletRequest request, HttpServletResponse response,
                                               IDPSSOFederateRequest data) {
        try {
            SAML2Utils.debug.message("Invoking the IDP Adapter preAuthentication hook");
            return data.getIdpAdapter().preAuthentication(
                    data.getIdpEntityID(), data.getRealm(), request, response, data.getAuthnRequest(), data.getSession(),
                    data.getRequestID(), data.getRelayState());
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("Error invoking the IDP Adapter", se);
        }

        return false;

    }

}
