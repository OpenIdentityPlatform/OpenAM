/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DenyOverride.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.util.Set;

/**
 *
 * Deny Override combine entitlement with <code>false</code> override
 * <code>true</code>.
 */
public class DenyOverride extends EntitlementCombiner {
    @Override
    protected boolean combine(Boolean b1, Boolean b2) {
        return b1.booleanValue() && b2.booleanValue();
    }

    @Override
    protected boolean isCompleted() {
        Entitlement e = getRootE();
        Set<String> actions = getActions();
        for (String a : actions) {
            Boolean result = e.getActionValue(a);
            if ((result == null) || result.booleanValue()) {
                return false;
            }
        }
        return true;
    }
}
