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
package org.forgerock.openam.saml2;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.DefaultIDPAdapter;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import com.sun.identity.saml2.profile.ClientFaultException;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.profile.ServerFaultException;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Level;

/**
 * Responsible for validating and providing a handful of parameters required for
 * processing the IDP requests.  A new UtilProxyIDPRequestValidator should be created per request.
 *
 * Note: Currently all supporting classes have extensive static initialisation which
 * is preventing this class from providing more immutability.
 */
public class UtilProxyIDPRequestValidator implements IDPRequestValidator {
    private final Debug debug;
    private final String reqBinding;
    private final SAML2MetaManager saml2MetaManager;

    /**
     * Creates a new UtilProxyIDPRequestValidator for a request.
     *
     * @param reqBinding the request binding for the new UtilProxyIDPRequestValidator
     * @param isFromECP whether the validator will be validating a request from an ecp
     * @param debug the debuger to use for debug logging
     * @param saml2MetaManager
     */
    public UtilProxyIDPRequestValidator(String reqBinding, boolean isFromECP, Debug debug,
                                        SAML2MetaManager saml2MetaManager) {
        this.debug = debug;
        // When in ECP mode, can only be a SOAP binding.
        this.reqBinding = isFromECP ? SAML2Constants.SOAP : reqBinding;
        this.saml2MetaManager = saml2MetaManager;
        debug.message("Using request binding: {}", reqBinding);
    }

    /**
     * The meta alias is used to locate the provider's entity identifier and the
     * organization in which it is located.
     *
     * @param request the HttpServletRequest to get the metaAlias for
     * @return A non null string closely resembling the entities realm
     *
     * @throws ClientFaultException If the meta alias was not provided in the request
     * or could not be parsed out of the request URI
     */
    public String getMetaAlias(HttpServletRequest request) throws ClientFaultException {
        String r = request.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        debug.message("Checking for Meta Alias in Parameter: {}", r);
        if (StringUtils.isBlank(r)) {
            r = SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI());
            debug.message("Checking for Meta Alias in URI: {}", r);
        }
        if (StringUtils.isBlank(r)) {
            throw new ClientFaultException("IDPMetaAliasNotFound");
        }
        return r;
    }

    /**
     * The entity identifier for the IDP.
     *
     * @param idpMetaAlias Non null meta alias
     * @param realm the realm for the entity identifier
     * @return Non null String containing the entity identifier
     * @throws ServerFaultException If unable to read the IDP Entity ID from the realm meta.
     * @throws ClientFaultException If the client requested an invalid binding for this IDP.
     */
    public String getIDPEntity(String idpMetaAlias, String realm) throws ServerFaultException, ClientFaultException {
        String idpEntityID;

        try {
            idpEntityID = saml2MetaManager.getEntityByMetaAlias(idpMetaAlias);
            if (StringUtils.isBlank(idpEntityID)) {
                debug.error("Failed to locate IDP Entity ID\nRealm: {}\nIDP Meta Alias: {}", realm, idpMetaAlias);
                LogUtil.error(Level.INFO, LogUtil.INVALID_IDP, new String[]{idpEntityID}, null);
                throw new ClientFaultException("nullIDPEntityID");
            }

            boolean profileEnabled = SAML2Utils.isIDPProfileBindingSupported(
                    realm, idpEntityID, SAML2Constants.SSO_SERVICE, reqBinding);

            if (!profileEnabled) {
                debug.error("SSO Binding {} is not enabled for {}", reqBinding, idpEntityID);
                LogUtil.error(Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, new String[]{ idpEntityID, reqBinding }, null);
                throw new ClientFaultException("unsupportedBinding");
            }
        } catch (SAML2MetaException sme) {
            debug.error("Unable to get IDP Entity ID from meta: {}", sme.getMessage());
            LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, new String[]{ idpMetaAlias }, null);
            throw new ServerFaultException("nullIDPEntityID", sme.getMessage());
        }

        return idpEntityID;
    }

    /**
     * Loads the {@link SAML2IdentityProviderAdapter} IDP adapter which will be called as part
     * of IDP processing.
     *
     * @param realm Possibly null realm.
     * @param idpEntityID Non null idpEntityID.
     *
     * @return The loaded {@link SAML2IdentityProviderAdapter} if it could be loaded otherwise
     * the default implementation {@link DefaultIDPAdapter}.
     */
    public SAML2IdentityProviderAdapter getIDPAdapter(String realm, String idpEntityID) {
        SAML2IdentityProviderAdapter r;
        if (idpEntityID == null) {
            debug.error("No IDP Entity ID provided");
            r = new DefaultIDPAdapter();
        } else {
            try {
                r = IDPSSOUtil.getIDPAdapterClass(realm, idpEntityID);
            } catch (SAML2Exception se2) {
                debug.error("Unexpected error instantiating IDP Adapter: {}", se2.getMessage(), se2);
                r = new DefaultIDPAdapter();
            }
        }
        debug.message("Using IDP Adapter class: {}", r.getClass().getSimpleName());
        return r;
    }

    @Override
    public String getRealmByMetaAlias(String idpMetaAlias) {
        return SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
    }
}
