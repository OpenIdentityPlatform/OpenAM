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
 * $Id: AttributeSubjectDao.java,v 1.1 2009/06/12 22:38:13 farble1670 Exp $
 */
package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.model.AttributeViewSubject;
import com.sun.identity.admin.model.RealmsBean;
import com.sun.identity.admin.model.ViewSubject;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.Subject;

public class AttributeSubjectDao extends SubjectDao {

    public List<ViewSubject> getViewSubjects() {
        return getViewSubjects(null);
    }

    private SubjectAttributesManager getSubjectAttributesManager() {
        SSOToken t = new Token().getSSOToken();
        Subject s = SubjectUtils.createSubject(t);
        String realmName = RealmsBean.getInstance().getRealmBean().getName();
        SubjectAttributesManager sam = SubjectAttributesManager.getInstance(s, realmName);

        return sam;
    }

    public List<ViewSubject> getViewSubjects(String filter) {
        List<ViewSubject> vss = new ArrayList<ViewSubject>();

        SubjectAttributesManager sam = getSubjectAttributesManager();
        try {
            for (String s : sam.getAvailableSubjectAttributeNames()) {
                if (s != null && s.length() > 0 && s.toLowerCase().contains(filter.toLowerCase())) {
                    AttributeViewSubject avs = (AttributeViewSubject) getSubjectType().newViewSubject();
                    avs.setName(s);
                    vss.add(avs);
                }
            }
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }

        return vss;
    }

    public void decorate(ViewSubject vs) {
        // TODO?
    }
}
