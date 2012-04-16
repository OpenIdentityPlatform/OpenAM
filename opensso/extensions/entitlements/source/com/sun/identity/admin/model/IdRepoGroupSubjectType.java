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
 * $Id: IdRepoGroupSubjectType.java,v 1.3 2009/08/04 07:38:10 hengming Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SubjectDao;
import com.sun.identity.entitlement.IdRepoRoleSubject;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.opensso.OpenSSOGroupSubject;
import java.io.Serializable;

public class IdRepoGroupSubjectType
    extends SubjectType
    implements Serializable {

    public ViewSubject newViewSubject() {
        ViewSubject vs = new IdRepoGroupViewSubject();
        vs.setSubjectType(this);

        return vs;
    }

    public ViewSubject newViewSubject(EntitlementSubject es, SubjectFactory stf) {
        assert(es instanceof OpenSSOGroupSubject);
        OpenSSOGroupSubject gs = (OpenSSOGroupSubject)es;

        IdRepoGroupViewSubject idgs = (IdRepoGroupViewSubject)newViewSubject();
        idgs.setName(gs.getID());

        SubjectDao sd = stf.getSubjectDao(idgs);
        sd.decorate(idgs);

        return idgs;
    }

}
