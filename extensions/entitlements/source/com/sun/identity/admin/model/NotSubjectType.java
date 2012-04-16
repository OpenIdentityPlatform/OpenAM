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
 * $Id: NotSubjectType.java,v 1.6 2009/07/31 20:38:42 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.NotSubject;
import java.io.Serializable;

public class NotSubjectType 
    extends SubjectType
    implements Serializable {

    public ViewSubject newViewSubject() {
        NotViewSubject nvs = new NotViewSubject();
        nvs.setSubjectType(this);

        return nvs;
    }

    public ViewSubject newViewSubject(EntitlementSubject es, SubjectFactory stf) {
        assert(es instanceof NotSubject);
        NotSubject ns = (NotSubject)es;
        EntitlementSubject nottedSubject = ns.getESubject();

        NotViewSubject nvs = (NotViewSubject)newViewSubject();
        if (nottedSubject != null) {
        ViewSubject nottedViewSubject = stf.getViewSubject(nottedSubject);
            nvs.addViewSubject(nottedViewSubject);
        }

        return nvs;
    }
}
