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
package org.forgerock.openam.rest.devices.services;

import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the OATH Service. Provides all necessary configuration information
 * at a realm-wide level to OATH authentication modules underneath it.
 */
public class OathService implements DeviceService {

    final static public String SERVICE_NAME = "OATH";
    final static public String SERVICE_VERSION = "1.0";

    final static private Debug debug = Debug.getInstance("amAuthOATH");

    public static final String OATH_ATTRIBUTE_NAME = "iplanet-am-auth-oath-attr-name";

    private Map<String, Set<String>> options;

    public OathService(String realm) throws SMSException, SSOException {
        try {
            ServiceConfigManager mgr = new ServiceConfigManager(
                    AccessController.doPrivileged(AdminTokenAction.getInstance()), SERVICE_NAME, SERVICE_VERSION);
            ServiceConfig scm = mgr.getOrganizationConfig(realm, null);
            options = scm.getAttributes();
        } catch (SMSException | SSOException e) {
            if (debug.errorEnabled()) {
                debug.error("Error connecting to SMS to retrieve config for OathService.", e);
            }
            throw e;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConfigStorageAttributeName() {
        return CollectionHelper.getMapAttr(options, OATH_ATTRIBUTE_NAME);
    }
}
