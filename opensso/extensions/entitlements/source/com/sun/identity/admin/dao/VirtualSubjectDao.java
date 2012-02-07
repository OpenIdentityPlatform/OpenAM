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
 * $Id: VirtualSubjectDao.java,v 1.3 2009/06/12 22:38:13 farble1670 Exp $
 */

package com.sun.identity.admin.dao;

import com.sun.identity.admin.model.ViewSubject;
import com.sun.identity.admin.model.VirtualSubjectType;
import com.sun.identity.entitlement.VirtualSubject;
import com.sun.identity.entitlement.VirtualSubject.VirtualId;
import java.util.ArrayList;
import java.util.List;

public class VirtualSubjectDao extends SubjectDao {
    public  List<ViewSubject> getViewSubjects() {
        return getViewSubjects(null);
    }

    public List<ViewSubject> getViewSubjects(String filter) {
        // TODO: filter?
        List<ViewSubject> vss = new ArrayList<ViewSubject>();
        for (VirtualId vid: VirtualSubject.VirtualId.values()) {
            VirtualSubjectType vst = (VirtualSubjectType)getSubjectType();
            ViewSubject vs = vst.newViewSubject(vid.toString());
            vss.add(vs);
        }

        return vss;
    }

    public void decorate(ViewSubject vs) {
        // TODO?
    }
}
