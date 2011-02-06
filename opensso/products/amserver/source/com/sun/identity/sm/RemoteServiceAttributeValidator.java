/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RemoteServiceAttributeValidator.java,v 1.3 2008/06/25 05:44:04 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.jaxrpc.SMSJAXRPCObject;
import java.util.Set;

/**
 * This class makes a JAX-RPC calls to server to validate attribute values.
 */
public class RemoteServiceAttributeValidator {
    private RemoteServiceAttributeValidator() {
    }

    /**
     * Validates a set of values.
     *
     * @param token Single Sign On token.
     * @param clazz Validator class.
     * @param values Set of values to be validated.
     * @return <code>true</code> if the set of values is valid.
     * @throws SMSException if unable to connect to server.
     */
    public static boolean validate(
        SSOToken token, 
        String clazz,
        Set values
    ) throws SMSException {
        try {
            SMSJAXRPCObject smsObj = new SMSJAXRPCObject();
            return smsObj.validateServiceAttributes(token, clazz, values);
        } catch (SSOException e) {
            return false;
        }
    }
}
