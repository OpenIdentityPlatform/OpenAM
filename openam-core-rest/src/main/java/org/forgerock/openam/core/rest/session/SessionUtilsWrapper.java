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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.session;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.session.util.SessionUtils;

/**
 * A wrapper class around SessionUtils to facilitate testing of code that needs to use SessionUtils.
 *
 * @since 14.0.0
 */
public class SessionUtilsWrapper {

    /**
     * Delegate check permission to SessionUtils
     *
     * @param clientToken Token of the client setting protected property.
     * @param key Property key
     * @param value Property value.
     * @throws SessionException if the key is protected property.
     */
    public void checkPermissionToSetProperty(SSOToken clientToken, String key, String value) throws SessionException {
        SessionUtils.checkPermissionToSetProperty(clientToken, key, value);
    }
}
