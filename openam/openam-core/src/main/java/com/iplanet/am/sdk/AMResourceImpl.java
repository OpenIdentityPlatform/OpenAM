/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMResourceImpl.java,v 1.3 2008/06/25 05:41:22 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/**
 * The <code>AMResourceImpl</code> implementation of interface AMResource
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */

class AMResourceImpl extends AMObjectImpl implements AMResource {
    static String statusAN = "icsStatus";

    public AMResourceImpl(SSOToken ssoToken, String DN) {
        super(ssoToken, DN, RESOURCE);
    }

    /**
     * Activates the resource
     */
    public void activate() throws AMException, SSOException {
        setStringAttribute(statusAN, "active");
        store();
    }

    /**
     * Deactivates the resource
     */
    public void deactivate() throws AMException, SSOException {
        setStringAttribute(statusAN, "inactive");
        store();
    }

    /**
     * Returns true if the resource is activated.
     * 
     * @return true if the resource is activated.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public boolean isActivated() throws AMException, SSOException {
        return getStringAttribute(statusAN).equalsIgnoreCase("active");
    }

}
