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
 * $Id: RealmRole.java,v 1.3 2008/06/25 05:42:36 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos;


public class RealmRole {

    public static final long ROLES_OFF = 0;
    public static final long READ_ALL_LOG_FILES = 1;
    public static final long WRITE_ALL_LOG_FILES = 2;
    public static final long READ_WRITE_ALL_LOG_FILES = 4;
    public static final long READ_WRITE_POLICY_PROPERTIES_ONLY = 8;
    public static final long READ_WRITE_ALL_REALMS_AND_POLICY_PROPERTIES = 16;

    private String name;
    private long flag = ROLES_OFF;


    public RealmRole() {
    }

    public RealmRole(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrivilege(long privilege, boolean active) {
        if (active) {
            flag = flag | privilege;
        }
        else {
            if (isPrivilegeOn(privilege)) {
                flag = flag ^ privilege;
            }
        }
    }

    public boolean isPrivilegeOn(long privilege) {
        return privilege == (flag & privilege);
    }

}
