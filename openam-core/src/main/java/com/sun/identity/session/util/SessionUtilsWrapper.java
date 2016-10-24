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
 * $Id: SessionUtils.java,v 1.10 2009/11/09 18:35:22 beomsuk Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package com.sun.identity.session.util;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOToken;

/**
 * Wrapper object for {@link SessionUtils} to avoid calls to static methods.
 */
public class SessionUtilsWrapper {

    /**
     * Helper method to check if owner of {@code clientToken} has permission to protected session properties.
     * <p>
     * In order for this operation to be permitted, {@code clientToken} must be a valid {@code SSOToken}
     * belonging to an administrator.
     * <p>
     * Alternatively, if {@code key} is not a protected property, then this check passes regardless of what
     * {code clientToken} is provided.
     *
     * @param clientToken
     *      SSOToken of the client wishing to set a session property.
     * @param key
     *      Name of the property the client wishes to set.
     * @param value
     *      Value of the property the client wishes to set.
     * @throws SessionException
     *      if key identifies a protected property and {@code clientToken} is not a valid admin {@code SSOToken}.
     */
    public void checkPermissionToSetProperty(SSOToken clientToken, String key, String value) throws SessionException {
        SessionUtils.checkPermissionToSetProperty(clientToken, key, value);
    }

}