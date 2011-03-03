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
 * $Id: ViewApplicationTypeDao.java,v 1.10 2009/08/12 04:35:52 farble1670 Exp $
 */
package com.sun.identity.admin.dao;

import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.model.Action;
import com.sun.identity.admin.model.BooleanAction;
import com.sun.identity.admin.model.ViewApplicationType;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;

public class ViewApplicationTypeDao implements Serializable {

    public List<ViewApplicationType> getViewApplicationTypes() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        Map<String, ViewApplicationType> entitlementApplicationTypeToViewApplicationTypeMap = (Map<String, ViewApplicationType>) mbr.resolve("entitlementApplicationTypeToViewApplicationTypeMap");

        Token token = new Token();
        Subject adminSubject = token.getAdminSubject();

        List<ViewApplicationType> viewApplicationTypes =
                new ArrayList<ViewApplicationType>();
        for (String entitlementApplicationType : entitlementApplicationTypeToViewApplicationTypeMap.keySet()) {
            ViewApplicationType vat =
                    entitlementApplicationTypeToViewApplicationTypeMap.get(entitlementApplicationType);
            ApplicationType at =
                    ApplicationTypeManager.getAppplicationType(adminSubject, entitlementApplicationType);
            if (at == null) {
                // TODO: log?
                continue;
            }
            List<Action> actions = new ArrayList<Action>();
            for (String actionName : at.getActions().keySet()) {
                Boolean value = at.getActions().get(actionName);
                BooleanAction ba = new BooleanAction();
                ba.setName(actionName);
                ba.setAllow(value.booleanValue());
                actions.add(ba);
            }
            vat.setActions(actions);
            viewApplicationTypes.add(vat);
        }

        return viewApplicationTypes;
    }

    public ApplicationType getApplicationType(String entitlementApplicationType) {
        Token token = new Token();
        Subject adminSubject = token.getAdminSubject();
        ApplicationType at =
                ApplicationTypeManager.getAppplicationType(adminSubject, entitlementApplicationType);
        assert (at != null);
        return at;
    }

    public Map<String, ViewApplicationType> getViewApplicationTypeMap() {
        Map<String, ViewApplicationType> viewApplicationTypeMap =
                new HashMap<String, ViewApplicationType>();

        for (ViewApplicationType vat : getViewApplicationTypes()) {
            viewApplicationTypeMap.put(vat.getName(), vat);
        }

        return viewApplicationTypeMap;
    }

    public static ViewApplicationTypeDao getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        ViewApplicationTypeDao vatdao = (ViewApplicationTypeDao) mbr.resolve("viewApplicationTypeDao");
        return vatdao;
    }
}
