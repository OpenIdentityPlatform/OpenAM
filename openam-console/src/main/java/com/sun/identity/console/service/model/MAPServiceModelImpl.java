/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MAPServiceModelImpl.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.iplanet.services.cdm.clientschema.AMClientCapException;
import com.sun.identity.common.DisplayUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class MAPServiceModelImpl
    extends MAPModelBase
    implements MAPServiceModel
{
    public MAPServiceModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns a set of profile names.
     *
     * @return a set of profile names.
     */
    public Set getProfileNames() {
        return (clientTypesManager != null) 
            ? clientTypesManager.getBaseProfileNames() : Collections.EMPTY_SET;
    }

    /**
     * Returns a set of device names.
     *
     * @param profileName Name of profile.
     * @param styleName Name of style.
     * @param wildcard Filter string to filter required names.
     * @return a set of device names.
     */
    public Set getDeviceNames(
        String profileName,
        String styleName,
        String wildcard
    ) {
        Set devices = Collections.EMPTY_SET;

        if (clientTypesManager != null) {
            Map map = clientTypesManager.getClients(profileName, styleName);
            if ((map != null) && !map.isEmpty()) {
                if (wildcard == null) {
                    wildcard = "*";
                }
                String[] params = {profileName, styleName, wildcard};
                logEvent("ATTEMPT_CLIENT_DETECTION_GET_DEVICE_NAMES", params);

                Set set = map.keySet();
                devices = new HashSet(set.size() *2);

                for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                    String s = (String)iter.next();
                    if (DisplayUtils.wildcardMatch(s, wildcard)) {
                        devices.add(s);
                    }
                }
                logEvent("SUCCEED_CLIENT_DETECTION_GET_DEVICE_NAMES", params);
            }
        }

        return devices;
    }

    /**
     * Removes a client from the external database.
     *
     * @param clientType Client type to be removed.
     * @throws AMConsoleException if client type cannot be deleted.
     */
    public void removeClient(String clientType) 
        throws AMConsoleException {
        String[] param = {clientType};
        logEvent("ATTEMPT_CLIENT_DETECTION_DELETE_CLIENT", param);
        try {
            clientTypesManager.removeClientExternal(
                getUserSSOToken(), clientType);
            logEvent("SUCCEED_CLIENT_DETECTION_DELETE_CLIENT", param);
        } catch (AMClientCapException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {clientType, strError};
            logEvent("CLIENT_SDK_EXCEPTION_CLIENT_DETECTION_DELETE_CLIENT",
                paramsEx);
            throw new AMConsoleException(strError);
        }
    }
}
