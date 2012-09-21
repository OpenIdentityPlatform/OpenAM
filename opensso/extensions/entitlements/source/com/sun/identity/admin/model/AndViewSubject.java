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
 * $Id: AndViewSubject.java,v 1.7 2009/07/31 21:53:48 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.entitlement.AndSubject;
import com.sun.identity.entitlement.EntitlementSubject;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class AndViewSubject extends ContainerViewSubject implements Serializable {

    public EntitlementSubject getEntitlementSubject() {
        AndSubject as = new AndSubject();

        Set<EntitlementSubject> eSubjects = new HashSet<EntitlementSubject>();
        for (ViewSubject vs : getViewSubjects()) {
            EntitlementSubject es = vs.getEntitlementSubject();
            if (es != null) {
                eSubjects.add(es);
            }
        }
        if (eSubjects.size() > 0) {
            as.setESubjects(eSubjects);
            return as;
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AndViewSubject)) {
            return false;
        }
        AndViewSubject avs = (AndViewSubject)o;
        for (ViewSubject vs: avs.getViewSubjects()) {
            if (!getViewSubjects().contains(vs)) {
                return false;
            }
        }
        return true;
    }
}
