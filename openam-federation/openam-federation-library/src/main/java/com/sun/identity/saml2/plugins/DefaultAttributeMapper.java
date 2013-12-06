/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DefaultAttributeMapper.java,v 1.4 2008/06/25 05:47:50 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS
 */

package com.sun.identity.saml2.plugins;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.plugin.datastore.DataStoreProvider;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * This class <code>DefaultAttribute</code> is the base class for 
 * <code>DefaultSPAttributeMapper</code> and 
 * <code>DefaultIDPAttributeMapper</code> for sharing the common 
 * functionalities.
 */
public class DefaultAttributeMapper {

    protected static Debug debug = SAML2Utils.debug;
    protected static ResourceBundle bundle = SAML2Utils.bundle;
    protected static DataStoreProvider dsProvider = null;
    protected static final String IDP = SAML2Constants.IDP_ROLE;
    protected static final String SP = SAML2Constants.SP_ROLE;

    static {
        try {
            dsProvider = SAML2Utils.getDataStoreProvider();
        } catch (Exception ex) {
            debug.error("DefaultAttributeMapper.static init failed.", ex);
        }
    }

    /**
     * Constructor.
     */
    public DefaultAttributeMapper() {}

    /**
     * Returns the attribute map by parsing the configured map in hosted
     * provider configuration
     * @param realm realm name.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @return a map of local attributes configuration map.
     *        This map will have a key as the SAML attribute name and the value
     *        is the local attribute. 
     * @exception <code>SAML2Exception</code> if any failured.
     */
    public Map<String, String> getConfigAttributeMap(String realm, String hostEntityID,
        String role) throws SAML2Exception {

        return SAML2Utils.getConfigAttributeMap(realm, hostEntityID, role);

    }

}
