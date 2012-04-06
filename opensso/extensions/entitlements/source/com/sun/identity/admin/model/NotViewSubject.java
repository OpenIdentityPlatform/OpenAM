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
 * $Id: NotViewSubject.java,v 1.9 2009/07/31 21:53:48 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.NotSubject;
import java.io.Serializable;

public class NotViewSubject extends ContainerViewSubject implements Serializable {

    public EntitlementSubject getEntitlementSubject() {
        if (getViewSubjects() != null && getViewSubjects().size() != 0) {
            EntitlementSubject es = getViewSubjects().get(0).getEntitlementSubject();
            if (es != null) {
                NotSubject ns = new NotSubject();
                ns.setESubject(getViewSubjects().get(0).getEntitlementSubject());
                return ns;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NotViewSubject)) {
            return false;
        }
        NotViewSubject nvs = (NotViewSubject)o;
        for (ViewSubject vs: nvs.getViewSubjects()) {
            if (!getViewSubjects().contains(vs)) {
                return false;
            }
        }
        return true;
    }
}
