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
 * $Id: IdRepoUserViewSubject.java,v 1.10 2009/08/11 00:56:30 hengming Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;

public class IdRepoUserViewSubject extends IdRepoViewSubject {
    private String employeeNumber;
    private String sn;

    public EntitlementSubject getEntitlementSubject() {
        OpenSSOUserSubject idus = new OpenSSOUserSubject();
        idus.setID(getName());

        return idus;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IdRepoUserViewSubject)) {
            return false;
        }
        IdRepoUserViewSubject idruvs = (IdRepoUserViewSubject) o;
        return idruvs.getName().equals(getName());
    }
}
