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
 * $Id: AccessLevel.java,v 1.3 2009/11/21 01:54:26 veiming Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.console.base.model.AMAdminConstants;
import java.util.HashSet;
import java.util.Set;

public enum AccessLevel {

    READ(AMAdminConstants.PERMISSION_READ),
    WRITE(AMAdminConstants.PERMISSION_MODIFY),
    DELEGATE(AMAdminConstants.PERMISSION_DELEGATE);

    private String value;

    AccessLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Set<String> toStringSet(AccessLevel[] accessLevels) {
        Set<String> als = new HashSet<String>();

        for (AccessLevel ac: accessLevels) {
            als.add(ac.getValue());
        }

        return als;
    }

    public static AccessLevel[] toAccessLevelArray(String s) {
        String[] asa = s.split(",");
        AccessLevel[] accessLevels = new AccessLevel[asa.length];
        for (int i = 0; i < asa.length; i++) {
            AccessLevel al = AccessLevel.valueOf(asa[i]);
            if (al == null) {
                throw new AssertionError("no access level value for: " + asa[i]);
            }
            accessLevels[i] = al;

        }

        return accessLevels;
    }
}
