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
 * $Id: UserAttributeDao.java,v 1.4 2009/06/24 23:47:01 farble1670 Exp $
 */

package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.model.RealmsBean;
import com.sun.identity.admin.model.UserViewAttribute;
import com.sun.identity.admin.model.ViewAttribute;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.Subject;

public class UserAttributeDao implements Serializable {

    private SubjectAttributesManager getSubjectAttributesManager() {
        SSOToken t = new Token().getSSOToken();
        Subject s = SubjectUtils.createSubject(t);
        String realmName = RealmsBean.getInstance().getRealmBean().getName();
        SubjectAttributesManager sam = SubjectAttributesManager.getInstance(s, realmName);

        return sam;
    }

    public List<ViewAttribute> getViewAttributes() {
        List<ViewAttribute> viewAttributes = new ArrayList<ViewAttribute>();

        SubjectAttributesManager sam = getSubjectAttributesManager();
        try {
            for (String s : sam.getAvailableSubjectAttributeNames()) {
                ViewAttribute va = new UserViewAttribute();
                va.setName(s);
                viewAttributes.add(va);
            }
        } catch (EntitlementException ee) {
            throw new AssertionError(ee);
        }
        return viewAttributes;
    }

    public static UserAttributeDao getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        UserAttributeDao uadao = (UserAttributeDao) mbr.resolve("userAttributeDao");
        return uadao;
    }
}
